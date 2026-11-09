package com.anant.fitbuddy.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import com.anant.fitbuddy.ui.components.softWave
import com.anant.fitbuddy.util.WaterFillLayers
import kotlin.math.PI
import kotlin.math.min

/**
 * Renders a vertical soft-wave fill (water → coffee → tea, hue-blended) for the
 * home-screen water widget. No section ticks — just track + fill + border.
 */
internal object WaterWaveBitmap {

    private const val TRACK = 0x334FC3F7.toInt()
    private const val WATER_DEEP = 0xFF0288D1.toInt()
    private const val WATER_ACCENT = 0xFF4FC3F7.toInt()
    private const val WATER_FOAM = 0xFFB3E5FC.toInt()
    private const val COFFEE_DEEP = 0xFF6D4C41.toInt()
    private const val COFFEE_ACCENT = 0xFFA1887F.toInt()
    private const val TEA_DEEP = 0xFF2E7D32.toInt()
    private const val TEA_ACCENT = 0xFF81C784.toInt()
    private const val BORDER = 0x99FFFFFF.toInt()
    private const val BUBBLE = 0x48FFFFFF.toInt()

    fun render(
        widthPx: Int,
        heightPx: Int,
        layers: WaterFillLayers,
        wavePhase: Float = 0.6f,
        cornerRadiusPx: Float = -1f,
        drawBorder: Boolean = true,
    ): Bitmap {
        val w = widthPx.coerceAtLeast(8)
        val h = heightPx.coerceAtLeast(8)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val radius = if (cornerRadiusPx >= 0f) cornerRadiusPx else min(w, h) * 0.12f
        val capsule = Path().apply {
            addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, Path.Direction.CW)
        }

        canvas.drawPath(capsule, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = TRACK })

        val waterShare = layers.waterShare
        val coffeeShare = layers.coffeeShare
        val teaShare = layers.teaShare
        val total = layers.totalShare

        if (total > 0.001f) {
            canvas.save()
            canvas.clipPath(capsule)
            val fillHeight = h * total
            val surfaceY = h - fillHeight
            val waveAmp = min(w * 0.05f, fillHeight * 0.10f)

            val path = Path().apply {
                moveTo(0f, h.toFloat())
                lineTo(w.toFloat(), h.toFloat())
                var x = w.toFloat()
                while (x >= 0f) {
                    val t = x / w * 1.05f * PI.toFloat() + wavePhase
                    val y = (surfaceY + softWave(t) * waveAmp).coerceAtMost(h.toFloat())
                    lineTo(x, y)
                    x -= 2f
                }
                close()
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f,
                    h.toFloat(),
                    0f,
                    surfaceY - waveAmp,
                    softVerticalColors(waterShare, coffeeShare, teaShare),
                    softVerticalPositions(waterShare, coffeeShare, teaShare),
                    Shader.TileMode.CLAMP,
                )
            }
            canvas.drawPath(path, paint)

            val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BUBBLE }
            listOf(
                floatArrayOf(w * 0.35f, surfaceY + fillHeight * 0.35f, w * 0.06f),
                floatArrayOf(w * 0.62f, surfaceY + fillHeight * 0.55f, w * 0.045f),
                floatArrayOf(w * 0.45f, surfaceY + fillHeight * 0.75f, w * 0.055f),
            ).forEach { (cx, cy, r) ->
                if (cy in surfaceY..(h - 2f)) {
                    canvas.drawCircle(cx, cy, r, bubblePaint)
                }
            }
            canvas.restore()
        }

        if (drawBorder) {
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = BORDER
                strokeWidth = (min(w, h) * 0.025f).coerceIn(2f, 5f)
            }
            canvas.drawRoundRect(
                RectF(border.strokeWidth / 2f, border.strokeWidth / 2f, w - border.strokeWidth / 2f, h - border.strokeWidth / 2f),
                radius,
                radius,
                border,
            )
        }
        return bitmap
    }

    /** Bottom → top color stops (water → coffee → tea with soft blends). */
    private fun softVerticalColors(water: Float, coffee: Float, tea: Float): IntArray {
        val colors = mutableListOf<Int>()
        colors += WATER_DEEP
        if (water > 0.001f) colors += WATER_ACCENT
        if (coffee > 0.001f) {
            colors += lerpArgb(WATER_ACCENT, COFFEE_DEEP, 0.45f)
            colors += COFFEE_ACCENT
            colors += COFFEE_DEEP
        }
        if (tea > 0.001f) {
            colors += lerpArgb(COFFEE_ACCENT, TEA_DEEP, 0.45f)
            colors += TEA_ACCENT
            colors += TEA_DEEP
        } else if (coffee <= 0.001f) {
            colors += WATER_FOAM
        }
        return colors.toIntArray()
    }

    private fun softVerticalPositions(water: Float, coffee: Float, tea: Float): FloatArray {
        val total = (water + coffee + tea).coerceAtLeast(0.001f)
        val wEnd = water / total
        val cEnd = (water + coffee) / total
        val blend = 0.10f
        val pos = mutableListOf(0f)
        if (water > 0.001f) pos += (wEnd * 0.55f).coerceAtMost(wEnd)
        if (coffee > 0.001f) {
            pos += (wEnd - blend * 0.5f).coerceAtLeast(0f)
            pos += wEnd
            pos += ((wEnd + cEnd) * 0.5f)
        }
        if (tea > 0.001f) {
            pos += (cEnd - blend * 0.5f).coerceAtLeast(wEnd)
            pos += cEnd
            pos += 1f
        } else if (coffee <= 0.001f) {
            pos += 1f
        } else {
            pos += 1f
        }
        // Normalize to strictly increasing 0..1 matching color count.
        val colors = softVerticalColors(water, coffee, tea)
        if (pos.size == colors.size) return pos.map { it.coerceIn(0f, 1f) }.toFloatArray()
        // Fallback evenly spaced.
        return FloatArray(colors.size) { i ->
            if (colors.size == 1) 0f else i / (colors.size - 1f)
        }
    }

    private fun lerpArgb(a: Int, b: Int, t: Float): Int {
        val u = t.coerceIn(0f, 1f)
        val ar = (a shr 16) and 0xFF
        val ag = (a shr 8) and 0xFF
        val ab = a and 0xFF
        val aa = (a ushr 24) and 0xFF
        val br = (b shr 16) and 0xFF
        val bg = (b shr 8) and 0xFF
        val bb = b and 0xFF
        val ba = (b ushr 24) and 0xFF
        val r = (ar + (br - ar) * u).toInt()
        val g = (ag + (bg - ag) * u).toInt()
        val bl = (ab + (bb - ab) * u).toInt()
        val al = (aa + (ba - aa) * u).toInt()
        return (al shl 24) or (r shl 16) or (g shl 8) or bl
    }
}
