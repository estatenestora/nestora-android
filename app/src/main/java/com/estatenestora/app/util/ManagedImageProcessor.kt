package com.estatenestora.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Creates a small upload source without ever decoding the camera original at
 * full resolution. The backend remains authoritative and creates the exact
 * thumbnail/card/hero crops; this first pass protects phone memory and upload
 * time on low-end devices.
 */
object ManagedImageProcessor {
    // This is the single client-side image boundary for every user upload.
    // The result is deliberately no larger than the backend's largest HERO
    // rendition needs, which avoids spending network, Telegram storage, or
    // device memory on pixels that will never be displayed.
    private const val MAX_SOURCE_BYTES = 2L * 1024L * 1024L
    private const val MAX_UPLOAD_BYTES = 2 * 1024 * 1024
    private const val MAX_EDGE = 1600

    fun prepare(context: Context, uri: Uri): Result<File> = runCatching {
        val original = File.createTempFile("nestora_media_source_", ".image", context.cacheDir)
        val output = File.createTempFile("nestora_media_upload_", ".jpg", context.cacheDir)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                original.outputStream().use { sink ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var copied = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        copied += count
                        require(copied <= MAX_SOURCE_BYTES) { "Choose an image smaller than 2 MB." }
                        sink.write(buffer, 0, count)
                    }
                }
            } ?: error("The selected image is unavailable.")

            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(original.absolutePath, bounds)
            require(bounds.outWidth >= 120 && bounds.outHeight >= 120) { "Choose an image at least 120 by 120 pixels." }
            var sample = 1
            while (max(bounds.outWidth / sample, bounds.outHeight / sample) > MAX_EDGE * 2) sample *= 2
            val decoded = BitmapFactory.decodeFile(original.absolutePath, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }) ?: error("This image format is not supported.")
            val oriented = orient(decoded, original)
            val scale = minOf(1f, MAX_EDGE.toFloat() / max(oriented.width, oriented.height).toFloat())
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(oriented, (oriented.width * scale).roundToInt(), (oriented.height * scale).roundToInt(), true)
            } else oriented
            val flattened = Bitmap.createBitmap(resized.width, resized.height, Bitmap.Config.RGB_565)
            Canvas(flattened).apply {
                drawColor(Color.WHITE)
                drawBitmap(resized, 0f, 0f, null)
            }
            output.writeBytes(encodeForUpload(flattened))
            if (flattened !== resized) flattened.recycle()
            if (resized !== oriented) resized.recycle()
            if (oriented !== decoded) oriented.recycle()
            if (!decoded.isRecycled) decoded.recycle()
            output
        } catch (error: Throwable) {
            output.delete()
            throw error
        } finally {
            original.delete()
        }
    }

    /** Enforces the on-wire 2 MB ceiling as well as the picker-size ceiling.
     * It lowers JPEG quality first, then only reduces dimensions if a highly
     * detailed image still exceeds the limit. */
    private fun encodeForUpload(source: Bitmap): ByteArray {
        var bitmap = source
        try {
            while (true) {
                for (quality in 82 downTo 60 step 4) {
                    val encoded = ByteArrayOutputStream().use { buffer ->
                        check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, buffer)) {
                            "The image could not be compressed."
                        }
                        buffer.toByteArray()
                    }
                    if (encoded.size <= MAX_UPLOAD_BYTES) return encoded
                }
                val nextWidth = (bitmap.width * 0.85f).roundToInt().coerceAtLeast(120)
                val nextHeight = (bitmap.height * 0.85f).roundToInt().coerceAtLeast(120)
                if (nextWidth == bitmap.width && nextHeight == bitmap.height) {
                    error("This image could not be reduced below 2 MB.")
                }
                val smaller = Bitmap.createScaledBitmap(bitmap, nextWidth, nextHeight, true)
                if (bitmap !== source) bitmap.recycle()
                bitmap = smaller
            }
        } finally {
            if (bitmap !== source && !bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun orient(bitmap: Bitmap, file: File): Bitmap {
        val orientation = runCatching {
            ExifInterface(file).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.preScale(-1f, 1f); matrix.postRotate(270f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.preScale(-1f, 1f); matrix.postRotate(90f) }
        }
        return if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
