package com.anant.fitbuddy.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Converts camera/gallery images into small JPEG bytes for base64 upload to the vision model.
 *
 * Vision models tile/downsample images to ~768–1024px anyway, so we cap the longest edge and then
 * step JPEG quality down until the payload fits under [TARGET_MAX_BYTES]. Smaller payloads mean
 * fewer image tokens, faster inference and less rate-limit (429) pressure.
 */
object ImageUtils {

    private const val MAX_DIMENSION = 768
    private const val INITIAL_QUALITY = 80
    private const val MIN_QUALITY = 40
    private const val QUALITY_STEP = 10
    private const val TARGET_MAX_BYTES = 200 * 1024 // ~200 KB before base64 (~270 KB encoded)

    fun bitmapToJpeg(bitmap: Bitmap): ByteArray {
        val scaled = downscale(bitmap)
        var quality = INITIAL_QUALITY
        var bytes = compress(scaled, quality)
        // Progressively lower quality until we're under the target (or hit the floor).
        while (bytes.size > TARGET_MAX_BYTES && quality > MIN_QUALITY) {
            quality -= QUALITY_STEP
            bytes = compress(scaled, quality)
        }
        if (scaled !== bitmap) scaled.recycle()
        return bytes
    }

    fun uriToJpeg(context: Context, uri: Uri): ByteArray? {
        val resolver = context.contentResolver

        // First pass: read only the bounds so we can downsample during decode (memory-efficient
        // for large gallery photos). NOTE: decodeStream returns null here by design (it just
        // fills `bounds`), so we must NOT treat that null as a failure.
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = resolver.openInputStream(uri) ?: return null
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        }
        val decodeStream = resolver.openInputStream(uri) ?: return null
        val bitmap = decodeStream.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        return bitmapToJpeg(bitmap)
    }

    private fun compress(bitmap: Bitmap, quality: Int): ByteArray =
        ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
            stream.toByteArray()
        }

    private fun downscale(bitmap: Bitmap): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= MAX_DIMENSION) return bitmap
        val ratio = MAX_DIMENSION.toFloat() / largest
        val width = (bitmap.width * ratio).roundToInt().coerceAtLeast(1)
        val height = (bitmap.height * ratio).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    /** Largest power-of-two sample size that keeps the image at/above [maxDimension] on its long edge. */
    private fun calculateInSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        var largest = maxOf(width, height)
        while (largest / 2 >= maxDimension) {
            largest /= 2
            sample *= 2
        }
        return sample
    }
}
