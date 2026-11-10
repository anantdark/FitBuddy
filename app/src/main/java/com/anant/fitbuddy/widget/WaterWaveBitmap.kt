package com.anant.fitbuddy.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.anant.fitbuddy.util.WaterFillLayers
import kotlin.math.min

/** Flat single-colour fill (no hue blending, wave or bubbles) for the home-screen water widget. */
internal object WaterWaveBitmap {

    private const val TRACK = 0x334FC3F7
    private const val FILL = 0xFF4FC3F7.toInt()
    private const val BORDER = 0x99FFFFFF.toInt()

    fun render(
        widthPx: Int,
        heightPx: Int,
        layers: WaterFillLayers,
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

        if (layers.totalShare > 0.001f) {
            canvas.save()
            canvas.clipPath(capsule)
            val top = h * (1f - layers.totalShare)
            canvas.drawRect(0f, top, w.toFloat(), h.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG).apply { color = FILL })
            canvas.restore()
        }

        if (drawBorder) {
            val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                color = BORDER
                strokeWidth = (min(w, h) * 0.025f).coerceIn(2f, 5f)
            }
            val half = border.strokeWidth / 2f
            canvas.drawRoundRect(RectF(half, half, w - half, h - half), radius, radius, border)
        }
        return bitmap
    }
}
