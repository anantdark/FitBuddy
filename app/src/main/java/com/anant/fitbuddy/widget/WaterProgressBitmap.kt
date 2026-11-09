package com.anant.fitbuddy.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/** Flat, solid progress fill: left [fraction] of the width in [fillArgb], rest transparent. */
internal object WaterProgressBitmap {
    fun render(fraction: Float, fillArgb: Int, widthPx: Int = 800, heightPx: Int = 16): Bitmap {
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val filled = (widthPx * fraction.coerceIn(0f, 1f)).coerceAtLeast(0f)
        if (filled > 0f) {
            Canvas(bitmap).drawRect(
                0f, 0f, filled, heightPx.toFloat(),
                Paint().apply { color = fillArgb },
            )
        }
        return bitmap
    }
}
