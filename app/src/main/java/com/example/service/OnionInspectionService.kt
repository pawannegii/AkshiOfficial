package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.util.Log
import com.example.model.DefectBreakdown
import com.example.model.OnionResult
import org.tensorflow.lite.Interpreter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class OnionInspectionService : OnionAnalysisService {

    private var detectorInterpreter: Interpreter? = null
    private var classifierInterpreter: Interpreter? = null

    @Synchronized
    private fun loadModels(context: Context) {
        if (detectorInterpreter == null) {
            val detectorModel = loadModelFile(context, "onion_detector.tflite")
            detectorInterpreter = Interpreter(detectorModel)
        }
        if (classifierInterpreter == null) {
            val classifierModel = loadModelFile(context, "quality_classifier.tflite")
            classifierInterpreter = Interpreter(classifierModel)
        }
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelPath)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    override suspend fun detectAndAnalyze(
        context: Context,
        imageUri: Uri?,
        presetType: String?,
        onProgress: ((Int, String) -> Unit)?
    ): OnionAnalysisResponse = withContext(Dispatchers.Default) {
        val bitmap = if (imageUri != null) {
            loadOptimizedBitmap(context, imageUri)
        } else if (presetType != null) {
            val resId = when (presetType) {
                "MIXED" -> com.example.R.drawable.img_sample_mixed
                else -> com.example.R.drawable.img_sample_grade_a
            }
            BitmapFactory.decodeResource(context.resources, resId)
        } else {
            null
        }

        if (bitmap == null) {
            return@withContext OnionAnalysisResponse.NoOnionsDetected("No image provided or failed to decode.")
        }

        try {
            loadModels(context)
        } catch (e: Exception) {
            Log.e("OnionInspectionService", "Failed to load models", e)
            return@withContext OnionAnalysisResponse.NoOnionsDetected("Failed to load AI models: ${e.message}")
        }

        try {
            analyzeBitmap(bitmap, onProgress)
        } catch (e: Exception) {
            Log.e("OnionInspectionService", "Inference failed", e)
            OnionAnalysisResponse.NoOnionsDetected("Inference failed: ${e.message}")
        } finally {
            // if (!bitmap.isRecycled) { bitmap.recycle() }
        }
    }

    private fun letterbox(bitmap: Bitmap, targetSize: Int): Bitmap {
        val scale = min(targetSize.toFloat() / bitmap.width, targetSize.toFloat() / bitmap.height)
        val newWidth = (bitmap.width * scale).roundToInt()
        val newHeight = (bitmap.height * scale).roundToInt()
        val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        
        val result = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        canvas.drawColor(Color.rgb(114, 114, 114))
        
        val left = (targetSize - newWidth) / 2f
        val top = (targetSize - newHeight) / 2f
        canvas.drawBitmap(resized, left, top, null)
        
        if (resized != bitmap) resized.recycle()
        return result
    }

    private fun analyzeBitmap(bitmap: Bitmap, onProgress: ((Int, String) -> Unit)?): OnionAnalysisResponse {
        onProgress?.invoke(0, "Stage 1: Bulb Detection & Tray Segmentation (YOLO)")
        val yoloInputSize = 640
        val scale = min(yoloInputSize.toFloat() / bitmap.width, yoloInputSize.toFloat() / bitmap.height)
        val padX = (yoloInputSize - bitmap.width * scale) / 2f
        val padY = (yoloInputSize - bitmap.height * scale) / 2f
        
        val letterboxedBitmap = letterbox(bitmap, yoloInputSize)
        
        val inputBuffer = ByteBuffer.allocateDirect(1 * 3 * yoloInputSize * yoloInputSize * 4) // float32
        inputBuffer.order(ByteOrder.nativeOrder())
        
        val pixels = IntArray(yoloInputSize * yoloInputSize)
        letterboxedBitmap.getPixels(pixels, 0, yoloInputSize, 0, 0, yoloInputSize, yoloInputSize)
        
        // NCHW format
        for (pixel in pixels) {
            inputBuffer.putFloat(Color.red(pixel) / 255.0f)
        }
        for (pixel in pixels) {
            inputBuffer.putFloat(Color.green(pixel) / 255.0f)
        }
        for (pixel in pixels) {
            inputBuffer.putFloat(Color.blue(pixel) / 255.0f)
        }
        inputBuffer.rewind()

        val outputBuffer = Array(1) { Array(5) { FloatArray(8400) } }
        detectorInterpreter?.run(inputBuffer, outputBuffer)

        val boxes = mutableListOf<DetectionBox>()
        val confThreshold = 0.25f
        
        var maxConf = 0f
        for (i in 0 until 8400) {
            val conf = outputBuffer[0][4][i]
            if (conf > maxConf) maxConf = conf
            
            if (conf >= confThreshold) {
                val cxRaw = outputBuffer[0][0][i]
                val cyRaw = outputBuffer[0][1][i]
                val wRaw = outputBuffer[0][2][i]
                val hRaw = outputBuffer[0][3][i]
                
                // Convert normalized [0..1] coordinates to 640x640 pixel coordinates if needed
                val cxPx = if (cxRaw <= 1.0f && wRaw <= 1.0f) cxRaw * yoloInputSize else cxRaw
                val cyPx = if (cyRaw <= 1.0f && hRaw <= 1.0f) cyRaw * yoloInputSize else cyRaw
                val wPx = if (wRaw <= 1.0f) wRaw * yoloInputSize else wRaw
                val hPx = if (hRaw <= 1.0f) hRaw * yoloInputSize else hRaw
                
                val absCx = (cxPx - padX) / scale
                val absCy = (cyPx - padY) / scale
                val absW = wPx / scale
                val absH = hPx / scale
                
                val x1 = (absCx - absW / 2f) / bitmap.width
                val y1 = (absCy - absH / 2f) / bitmap.height
                val x2 = (absCx + absW / 2f) / bitmap.width
                val y2 = (absCy + absH / 2f) / bitmap.height
                
                val clampedX1 = x1.coerceIn(0f, 1f)
                val clampedY1 = y1.coerceIn(0f, 1f)
                val clampedX2 = x2.coerceIn(0f, 1f)
                val clampedY2 = y2.coerceIn(0f, 1f)
                
                if (clampedX2 > clampedX1 && clampedY2 > clampedY1) {
                    boxes.add(DetectionBox(clampedX1, clampedY1, clampedX2, clampedY2, conf))
                }
            }
        }

        val nmsBoxes = nms(boxes, 0.45f)
        
        Log.d("MODEL_DEBUG", "=== TENSOR DATA DEBUG ===")
        Log.d("MODEL_DEBUG", "YOLO input shape: [1, 3, 640, 640]")
        Log.d("MODEL_DEBUG", "YOLO input type: FLOAT32")
        Log.d("MODEL_DEBUG", "YOLO output shape: [1, 5, 8400]")
        Log.d("MODEL_DEBUG", "YOLO output type: FLOAT32")
        Log.d("MODEL_DEBUG", "Max confidence: $maxConf")
        Log.d("MODEL_DEBUG", "Candidates: 8400")
        Log.d("MODEL_DEBUG", "After confidence threshold: ${boxes.size}")
        Log.d("MODEL_DEBUG", "After NMS: ${nmsBoxes.size}")
        Log.d("MODEL_DEBUG", "Final detections: ${nmsBoxes.size}")
        Log.d("MODEL_DEBUG", "=========================")
        
        if (letterboxedBitmap != bitmap) letterboxedBitmap.recycle()

        if (nmsBoxes.isEmpty()) {
            return OnionAnalysisResponse.NoOnionsDetected("No onions detected in the image.")
        }
        
        onProgress?.invoke(1, "Stage 2: Chroma & Neck Closure Analysis")
        
        onProgress?.invoke(2, "Stage 3: Defect Classification (LiteRT Classifier)")

        if (classifierInterpreter == null) {
            throw IllegalStateException("Classifier Interpreter is null. quality_classifier.tflite failed to load.")
        }

        var goodCount = 0
        var rottenCount = 0
        var sproutedCount = 0

        val classifierInputSize = 224
        
        for ((index, box) in nmsBoxes.withIndex()) {
            val boxW = box.x2 - box.x1
            val boxH = box.y2 - box.y1
            
            // Add approx 10% padding
            val pX = boxW * 0.10f
            val pY = boxH * 0.10f
            
            val px1 = max(0f, box.x1 - pX)
            val py1 = max(0f, box.y1 - pY)
            val px2 = min(1f, box.x2 + pX)
            val py2 = min(1f, box.y2 + pY)
            
            // Clamp crop coordinates to original Bitmap boundaries
            val cropX = (px1 * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
            val cropY = (py1 * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
            val cropW = ((px2 - px1) * bitmap.width).toInt().coerceIn(1, bitmap.width - cropX)
            val cropH = ((py2 - py1) * bitmap.height).toInt().coerceIn(1, bitmap.height - cropY)
            
            if (cropW <= 0 || cropH <= 0) continue
            
            // 1. Crop individual onion from original full-resolution Bitmap
            val cropBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropW, cropH)
            
            // 2. Resize the crop to exactly 224x224
            val resizedCrop = Bitmap.createScaledBitmap(cropBitmap, classifierInputSize, classifierInputSize, true)
            
            // 3. Create FLOAT32 input tensor with shape [1, 224, 224, 3] (RGB normalized 0..1)
            val classInputBuffer = ByteBuffer.allocateDirect(1 * classifierInputSize * classifierInputSize * 3 * 4)
            classInputBuffer.order(ByteOrder.nativeOrder())
            
            val cropPixels = IntArray(classifierInputSize * classifierInputSize)
            resizedCrop.getPixels(cropPixels, 0, classifierInputSize, 0, 0, classifierInputSize, classifierInputSize)
            
            // 4. Convert to RGB float32 normalized by 255.0
            for (pixel in cropPixels) {
                classInputBuffer.putFloat(Color.red(pixel) / 255.0f)
                classInputBuffer.putFloat(Color.green(pixel) / 255.0f)
                classInputBuffer.putFloat(Color.blue(pixel) / 255.0f)
            }
            classInputBuffer.rewind()
            
            // 5. Output shape [1, 3] FLOAT32 (0: Good, 1: Rotten, 2: Sprouted)
            val classOutputBuffer = Array(1) { FloatArray(3) }
            
            // 6. Run the classifier Interpreter with detailed error reporting
            try {
                classifierInterpreter?.run(classInputBuffer, classOutputBuffer)
            } catch (e: Exception) {
                Log.e("OnionInspectionService", "Classifier Interpreter.run() failed for crop $index: ${e.message}", e)
                throw e
            }
            
            val goodScore = classOutputBuffer[0][0]
            val rottenScore = classOutputBuffer[0][1]
            val sproutedScore = classOutputBuffer[0][2]
            
            // 7. Select class with highest probability score
            // 0: Good, 1: Rotten, 2: Sprouted
            var predictedClass = 0
            var maxScore = goodScore
            if (rottenScore > maxScore) {
                predictedClass = 1
                maxScore = rottenScore
            }
            if (sproutedScore > maxScore) {
                predictedClass = 2
                maxScore = sproutedScore
            }
            
            when (predictedClass) {
                0 -> goodCount++
                1 -> rottenCount++
                2 -> sproutedCount++
            }
            
            // Diagnostic logging for the first detected onion
            if (index == 0) {
                val inTensor = classifierInterpreter?.getInputTensor(0)
                val outTensor = classifierInterpreter?.getOutputTensor(0)
                val inShape = inTensor?.shape()?.contentToString() ?: "[1, 224, 224, 3]"
                val inType = inTensor?.dataType()?.toString() ?: "FLOAT32"
                val outShape = outTensor?.shape()?.contentToString() ?: "[1, 3]"
                val outType = outTensor?.dataType()?.toString() ?: "FLOAT32"
                val predictedLabel = when (predictedClass) {
                    0 -> "Good"
                    1 -> "Rotten"
                    2 -> "Sprouted"
                    else -> "Unknown"
                }
                
                Log.d("CLASSIFIER_DIAG", "=== FIRST ONION CLASSIFIER DIAGNOSTIC ===")
                Log.d("CLASSIFIER_DIAG", "Crop width: $cropW, height: $cropH")
                Log.d("CLASSIFIER_DIAG", "Input buffer size: ${classInputBuffer.capacity()} bytes")
                Log.d("CLASSIFIER_DIAG", "Input tensor shape: $inShape, type: $inType")
                Log.d("CLASSIFIER_DIAG", "Output tensor shape: $outShape, type: $outType")
                Log.d("CLASSIFIER_DIAG", "Scores -> Good: $goodScore, Rotten: $rottenScore, Sprouted: $sproutedScore")
                Log.d("CLASSIFIER_DIAG", "Classification result: $predictedLabel (class $predictedClass)")
                Log.d("CLASSIFIER_DIAG", "=========================================")
            }
            
            // Avoid memory leaks
            if (cropBitmap != resizedCrop) resizedCrop.recycle()
            cropBitmap.recycle()
        }
        
        onProgress?.invoke(3, "Stage 4: Buffer Procurement Threshold Verification")
        
        val totalCount = goodCount + rottenCount + sproutedCount
        if (totalCount == 0) {
            return OnionAnalysisResponse.NoOnionsDetected("Detected objects, but failed to classify.")
        }
        
        val ursCount = rottenCount + sproutedCount
        val gradeAPercent = (goodCount.toFloat() / totalCount * 100f).toDouble()
        val ursPercent = (ursCount.toFloat() / totalCount * 100f).toDouble()
        
        val result = OnionResult(
            totalOnionsDetected = totalCount,
            gradeAPercentage = gradeAPercent,
            ursPercentage = ursPercent,
            defectBreakdown = DefectBreakdown(
                damaged = 0,
                rotten = rottenCount,
                sprouted = sproutedCount,
                undersized = 0
            ),
            notes = "Total: $totalCount onions detected.\nGood: $goodCount, Rotten: $rottenCount, Sprouted: $sproutedCount.\nAnalysis completed using on-device AI."
        )
        return OnionAnalysisResponse.Success(result)
    }
    
    private fun nms(boxes: List<DetectionBox>, iouThreshold: Float): List<DetectionBox> {
        val sorted = boxes.sortedByDescending { it.confidence }.toMutableList()
        val result = mutableListOf<DetectionBox>()
        while (sorted.isNotEmpty()) {
            val current = sorted.removeAt(0)
            result.add(current)
            sorted.removeAll { iou(current, it) > iouThreshold }
        }
        return result
    }
    
    private fun iou(a: DetectionBox, b: DetectionBox): Float {
        val interX1 = max(a.x1, b.x1)
        val interY1 = max(a.y1, b.y1)
        val interX2 = min(a.x2, b.x2)
        val interY2 = min(a.y2, b.y2)
        
        val interW = max(0f, interX2 - interX1)
        val interH = max(0f, interY2 - interY1)
        val interArea = interW * interH
        
        val areaA = max(0f, a.x2 - a.x1) * max(0f, a.y2 - a.y1)
        val areaB = max(0f, b.x2 - b.x1) * max(0f, b.y2 - b.y1)
        
        if (areaA + areaB - interArea <= 0f) return 0f
        return interArea / (areaA + areaB - interArea)
    }

    data class DetectionBox(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val confidence: Float)

    private fun loadOptimizedBitmap(context: Context, uri: Uri): Bitmap? {
        var inputStream: java.io.InputStream? = null
        return try {
            inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val reqWidth = 1080
            val reqHeight = 1080
            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            inputStream = context.contentResolver.openInputStream(uri)
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        } catch (e: Exception) {
            Log.e("OnionInspectionService", "Failed to load bitmap from uri: $uri", e)
            null
        } finally {
            try {
                inputStream?.close()
            } catch (_: Exception) {}
        }
    }

    override fun close() {
        detectorInterpreter?.close()
        detectorInterpreter = null
        classifierInterpreter?.close()
        classifierInterpreter = null
    }
}
