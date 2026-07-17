package com.example.visiting_cardbusiness_card

import android.net.Uri
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.visiting_cardbusiness_card.model.CardResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class BatchScannerTest {

    private val DEVICE_INPUT_FOLDER = "/data/local/tmp/BusinessCards"
    
    @Test
    fun runBatchProcessing() = runBlocking {
        val scenario = androidx.test.core.app.ActivityScenario.launch(MainActivity::class.java)
        try {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val outputFile = File(context.getExternalFilesDir(null), "scanning_results.csv")
            val OUTPUT_CSV_PATH = outputFile.absolutePath

            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            val pipelines = ExtractionPipelines(context)
            
            var inputDir = File(DEVICE_INPUT_FOLDER)
            if (!inputDir.exists() || inputDir.listFiles() == null || inputDir.listFiles()?.isEmpty() == true) {
                val fallbackDir = context.getExternalFilesDir(null)!!
                Log.d("BatchScanner", "Primary input dir $DEVICE_INPUT_FOLDER not found, not readable, or empty. Trying fallback: ${fallbackDir.absolutePath}")
                inputDir = fallbackDir
            }
            if (!inputDir.exists()) {
                Log.e("BatchScanner", "Input directory does not exist: ${inputDir.absolutePath}")
                return@runBlocking
            }

            val images = inputDir.listFiles { _, name -> 
                name.lowercase().endsWith(".jpg") || name.lowercase().endsWith(".png") || name.lowercase().endsWith(".jpeg")
            }?.sortedBy { it.name } ?: emptyList<File>()

            if (images.isEmpty()) {
                Log.e("BatchScanner", "No images found in ${inputDir.absolutePath}")
                return@runBlocking
            }

            Log.d("BatchScanner", "STARTING: Found ${images.size} images. Saving to: $OUTPUT_CSV_PATH")

            val csvBuilder = StringBuilder()
            csvBuilder.append("image_name,pipeline,company_name,person_name,designation,phone,email,website,address,fax,gstin,other_info,raw_ocr_text\n")

            var count = 0
            for (imageFile in images) {
                count++
                try {
                    Log.d("BatchScanner", "[$count/${images.size}] Processing: ${imageFile.name}")
                    val image = InputImage.fromFilePath(context, Uri.fromFile(imageFile))
                    
                    // OCR Step
                    val visionText = try {
                        recognizer.process(image).await()
                    } catch (e: Exception) {
                        Log.e("BatchScanner", "OCR Failed for ${imageFile.name}: ${e.message}")
                        continue
                    }

                    // Extraction Step
                    val results = mutableListOf<CardResult>()
                    
                    Log.d("BatchScanner", "Running Pipeline 1...")
                    results.add(pipelines.processPipeline1(visionText))
                    
                    Log.d("BatchScanner", "Running Pipeline 2...")
                    results.add(pipelines.processPipeline2(visionText))
                    
                    Log.d("BatchScanner", "Running Pipeline 3...")
                    results.add(pipelines.processPipeline3(visionText))

                    for (res in results) {
                        csvBuilder.append(formatCsvRow(imageFile.name, res))
                    }
                    
                    // Partial save every 10 images to avoid data loss on crash
                    if (count % 10 == 0) {
                        FileOutputStream(outputFile).use { it.write(csvBuilder.toString().toByteArray()) }
                        Log.d("BatchScanner", "Checkpoint: Saved $count images.")
                    }

                } catch (e: Exception) {
                    Log.e("BatchScanner", "Critical Error on ${imageFile.name}: ${e.message}")
                }
            }

            // Final Write
            try {
                FileOutputStream(outputFile).use { it.write(csvBuilder.toString().toByteArray()) }
                Log.d("BatchScanner", "SUCCESS! Batch complete. Total: $count")
                Log.d("BatchScanner", "PULL COMMAND: adb pull $OUTPUT_CSV_PATH .")
            } catch (e: Exception) {
                Log.e("BatchScanner", "Failed to write final CSV: ${e.message}")
            }
        } finally {
            scenario.close()
        }
    }

    private fun formatCsvRow(fileName: String, res: CardResult): String {
        val row = listOf(
            fileName,
            res.pipelineName,
            res.company,
            res.name,
            res.designation,
            res.phone.joinToString(" | "),
            res.email.joinToString(" | "),
            res.website.joinToString(" | "),
            res.address.replace("\n", " ").replace(",", ";"),
            res.fax,
            res.gstin,
            res.extras.replace("\n", " ").replace(",", ";"),
            res.rawOcrText.replace("\n", " ").replace(",", ";")
        )
        return row.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" } + "\n"
    }
}
