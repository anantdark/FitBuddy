package com.anant.fitbuddy.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.Decoder
import coil.decode.ImageSource
import coil.fetch.SourceResult
import coil.request.Options
import coil.size.Dimension
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer

/**
 * Decodes only the first frame of a GIF as a static bitmap for list thumbnails, so scrolling a
 * large catalog does not animate dozens of GIFs at once. Activated when the request sets
 * [PARAM_STATIC_GIF] to true.
 */
class GifFirstFrameDecoder(
    private val source: ImageSource,
    private val options: Options
) : Decoder {

    override suspend fun decode(): DecodeResult = withContext(Dispatchers.IO) {
        // Read via BufferedSource.readByteArray() — NOT source.buffer.readByteArray(), which only
        // returns already-buffered bytes and was yielding empty arrays (dumbbell placeholders).
        val bytes = source.source().use { it.readByteArray() }
        require(bytes.isNotEmpty()) { "GIF source was empty" }
        val maxSide = maxTargetSide(options)
        val bitmap = decodeFirstFrame(bytes, maxSide)
        DecodeResult(
            drawable = BitmapDrawable(options.context.resources, bitmap),
            isSampled = true
        )
    }

    class Factory : Decoder.Factory {
        override fun create(
            result: SourceResult,
            options: Options,
            imageLoader: ImageLoader
        ): Decoder? {
            if (options.parameters.value<Boolean>(PARAM_STATIC_GIF) != true) return null
            return GifFirstFrameDecoder(result.source, options)
        }
    }

    companion object {
        const val PARAM_STATIC_GIF = "fitbuddy_static_gif"

        private fun maxTargetSide(options: Options): Int {
            fun Dimension.toPxOrNull(): Int? = (this as? Dimension.Pixels)?.px
            val w = options.size.width.toPxOrNull()
            val h = options.size.height.toPxOrNull()
            return listOfNotNull(w, h).maxOrNull()?.coerceAtLeast(48) ?: 128
        }

        private fun decodeFirstFrame(bytes: ByteArray, maxSide: Int): Bitmap {
            decodeWithMovie(bytes, maxSide)?.let { return it }
            if (Build.VERSION.SDK_INT >= 28) {
                decodeWithImageDecoderFile(bytes, maxSide)?.let { return it }
                decodeWithImageDecoderBuffer(bytes, maxSide)?.let { return it }
            }
            error("Could not decode GIF first frame (${bytes.size} bytes)")
        }

        private fun decodeWithMovie(bytes: ByteArray, maxSide: Int): Bitmap? {
            @Suppress("DEPRECATION")
            val movie = Movie.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val w = movie.width().coerceAtLeast(1)
            val h = movie.height().coerceAtLeast(1)
            val scale = minOf(1f, maxSide.toFloat() / maxOf(w, h).toFloat())
            val tw = (w * scale).toInt().coerceAtLeast(1)
            val th = (h * scale).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            canvas.scale(scale, scale)
            movie.setTime(0)
            movie.draw(canvas, 0f, 0f)
            return bmp
        }

        private fun decodeWithImageDecoderFile(bytes: ByteArray, maxSide: Int): Bitmap? {
            if (Build.VERSION.SDK_INT < 28) return null
            val tmp = File.createTempFile("fitbuddy_gif_", ".gif")
            return try {
                tmp.writeBytes(bytes)
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(tmp)) { decoder, info, _ ->
                    configureImageDecoder(decoder, info, maxSide)
                }
            } catch (_: Exception) {
                null
            } finally {
                tmp.delete()
            }
        }

        private fun decodeWithImageDecoderBuffer(bytes: ByteArray, maxSide: Int): Bitmap? {
            if (Build.VERSION.SDK_INT < 28) return null
            return try {
                val decoderSource = ImageDecoder.createSource(ByteBuffer.wrap(bytes))
                ImageDecoder.decodeBitmap(decoderSource) { decoder, info, _ ->
                    configureImageDecoder(decoder, info, maxSide)
                }
            } catch (_: Exception) {
                null
            }
        }

        private fun configureImageDecoder(
            decoder: ImageDecoder,
            info: ImageDecoder.ImageInfo,
            maxSide: Int
        ) {
            val w = info.size.width.coerceAtLeast(1)
            val h = info.size.height.coerceAtLeast(1)
            val scale = minOf(1f, maxSide.toFloat() / maxOf(w, h).toFloat())
            decoder.setTargetSize(
                (w * scale).toInt().coerceAtLeast(1),
                (h * scale).toInt().coerceAtLeast(1)
            )
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
        }
    }
}
