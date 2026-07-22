package com.example.visiting_cardbusiness_card

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.visiting_cardbusiness_card.model.CardResult
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MainViewModel: The central brain of the application.
 * It manages the state for Jetpack Compose (UI) and orchestrates the complex ML Kit pipeline.
 * Crucially, it handles the double-pass OCR logic to automatically fix skewed business cards
 * by applying an Android Matrix rotation before extracting the final data.
 */
class MainViewModel : ViewModel() {
    val scannedCards = mutableStateListOf<ScannedCard>()
    
    val currentImageUri = mutableStateOf<Uri?>(null)
    val currentResults = mutableStateOf<List<CardResult>>(emptyList())
    val isProcessing = mutableStateOf(false)

    // Initialize Google ML Kit Text Recognition with default options.
    // This is incredibly lightweight (~300KB impact) as it downloads dynamically via Google Play Services.
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * getDominantRotation
     * Scans the initial raw output from ML Kit to detect if the text is physically sideways.
     * @param visionText The raw text output from the first ML Kit pass.
     * @return The angle to rotate the image (e.g., 90, 180, 270) to make it perfectly upright.
     */
    private fun getDominantRotation(visionText: Text): Int {
        val angles = visionText.textBlocks.flatMap { it.lines }.map { it.angle }
        if (angles.isEmpty()) return 0
        
        val counts = mutableMapOf<Int, Int>()
        angles.forEach { angle ->
            val bucket = when {
                angle >= -45 && angle < 45 -> 0
                angle >= 45 && angle < 135 -> 90
                angle >= -135 && angle < -45 -> 270
                else -> 180
            }
            counts[bucket] = (counts[bucket] ?: 0) + 1
        }
        
        return counts.maxByOrNull { it.value }?.key ?: 0
    }

    /**
     * runPipelines
     * Initiates the custom Spatial Heuristic Engine (Pipeline 3) to extract structured fields.
     * @param context App context for initialization.
     * @param uri The original URI for saving history.
     * @param visionText The perfected, perfectly upright text from ML Kit.
     */
    private suspend fun runPipelines(context: Context, uri: Uri, visionText: Text) {
        val pipelines = ExtractionPipelines(context)
        val result = pipelines.processPipeline3(visionText)
        
        withContext(Dispatchers.Main) {
            currentResults.value = listOf(result)
            isProcessing.value = false
            scannedCards.add(0, ScannedCard(uri, currentResults.value))
        }
    }

    /**
     * processImage
     * The master orchestrator. Takes the image from CameraX or Gallery, passes it to ML Kit,
     * checks for rotation, fixes it if necessary via Android Matrix, and triggers extraction.
     */
    fun processImage(context: Context, uri: Uri) {
        currentImageUri.value = uri
        isProcessing.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // PASS 1: Send the raw, unedited image directly into ML Kit
                val image = InputImage.fromFilePath(context, uri)
                val visionText = Tasks.await(recognizer.process(image))
                
                // Check if the text is sideways (e.g., phone held portrait, card is landscape)
                val rotationNeeded = getDominantRotation(visionText)
                
                if (rotationNeeded != 0) {
                    val bitmap = loadBitmap(context, uri)
                    if (bitmap != null) {
                        val matrix = Matrix()
                        // Rotate the bitmap in the opposite direction of the detected text angle
                        // This uses hardware acceleration to perfectly remap every pixel.
                        matrix.postRotate(-rotationNeeded.toFloat())
                        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                        val rotatedImage = InputImage.fromBitmap(rotatedBitmap, 0)
                        
                        // PASS 2: Send the fixed, upright image back to ML Kit!
                        val newVisionText = Tasks.await(recognizer.process(rotatedImage))
                        
                        // Finally, send this perfect text to our Extraction Engine
                        runPipelines(context, uri, newVisionText)
                    } else {
                        runPipelines(context, uri, visionText)
                    }
                } else {
                    runPipelines(context, uri, visionText)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isProcessing.value = false
                }
            }
        }
    }

    private fun loadBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }
}

data class ScannedCard(
    val imageUri: Uri,
    val results: List<CardResult>,
    val timestamp: Long = System.currentTimeMillis()
)
