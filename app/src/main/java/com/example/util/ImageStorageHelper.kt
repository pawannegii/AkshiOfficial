package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max

object ImageStorageHelper {
    private const val TAG = "ImageStorageHelper"
    private const val MAX_DIMENSION = 1080
    private const val JPEG_QUALITY = 85

    /**
     * Ensures the given URI is stored in persistent, app-private internal storage
     * (context.filesDir/inspections). If it is already a persistent internal file, returns its URI.
     * Otherwise copies and optimizes (downsamples/rotates) it into app-private storage.
     */
    fun savePersistentInspectionImage(
        context: Context,
        sourceUri: Uri,
        batchId: String
    ): Uri? {
        return try {
            val inspectionsDir = File(context.filesDir, "inspections").apply {
                if (!exists()) mkdirs()
            }

            // If it's already an existing persistent file in filesDir/inspections, return it
            if (sourceUri.scheme == "file") {
                val path = sourceUri.path
                if (path != null && path.startsWith(inspectionsDir.absolutePath)) {
                    val existingFile = File(path)
                    if (existingFile.exists() && existingFile.length() > 0) {
                        return sourceUri
                    }
                }
            }

            val safeBatch = batchId.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val destFile = File(inspectionsDir, "inspection_${safeBatch}_${System.currentTimeMillis()}.jpg")

            // 1. Read bounds to compute downsampling
            var inputStream: InputStream? = null
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            try {
                inputStream = context.contentResolver.openInputStream(sourceUri)
                BitmapFactory.decodeStream(inputStream, null, options)
            } finally {
                inputStream?.close()
            }

            var inSampleSize = 1
            val maxDim = max(options.outWidth, options.outHeight)
            if (maxDim > MAX_DIMENSION) {
                while ((maxDim / inSampleSize) > MAX_DIMENSION) {
                    inSampleSize *= 2
                }
            }

            // 2. Decode with sample size
            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap: Bitmap? = try {
                inputStream = context.contentResolver.openInputStream(sourceUri)
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            } finally {
                inputStream?.close()
            }

            if (rawBitmap == null) {
                // Fallback: direct stream copy if bitmap decoding failed
                copyStreamToFile(context, sourceUri, destFile)
                return if (destFile.exists() && destFile.length() > 0) {
                    Uri.fromFile(destFile)
                } else {
                    sourceUri
                }
            }

            // 3. Check EXIF rotation
            val rotation = getExifRotation(context, sourceUri)
            val finalBitmap = if (rotation != 0f) {
                val matrix = Matrix().apply { postRotate(rotation) }
                Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            } else {
                rawBitmap
            }

            // 4. Save to destination file as JPEG
            FileOutputStream(destFile).use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }

            if (finalBitmap != rawBitmap) {
                rawBitmap.recycle()
            }
            finalBitmap.recycle()

            Log.i(TAG, "Persisted inspection image to ${destFile.absolutePath} (${destFile.length()} bytes)")
            Uri.fromFile(destFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist inspection image: ${e.message}", e)
            sourceUri
        }
    }

    private fun getExifRotation(context: Context, uri: Uri): Float {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        } catch (e: Exception) {
            0f
        }
    }

    private fun copyStreamToFile(context: Context, sourceUri: Uri, destFile: File) {
        try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy stream directly: ${e.message}", e)
        }
    }
}
