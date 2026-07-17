package com.example.visiting_cardbusiness_card

import android.net.Uri
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BoundingBoxDebug {
    @Test
    fun debugImage() = runBlocking {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val imagePath = "/sdcard/Download/visiting_card business_card/test_images/118248734-vc-1783928648089.png"
        val file = File(imagePath)
        if (!file.exists()) {
            Log.e("DEBUG_BB", "File not found: " + imagePath)
            return@runBlocking
        }
        val image = InputImage.fromFilePath(appContext, Uri.fromFile(file))
        val text = recognizer.process(image).await()
        for (block in text.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                val h = box?.height() ?: 0
                val w = box?.width() ?: 0
                Log.d("DEBUG_BB", "Line: '${line.text}' | Height: $h | Width: $w | Box: $box")
            }
        }
    }
}
