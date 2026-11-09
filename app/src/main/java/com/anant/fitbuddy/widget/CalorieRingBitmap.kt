package com.anant.fitbuddy.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF

/** Open-bottom gauge ring (270°) with rounded caps, drawn off-screen because Glance has no canvas. */
internal object CalorieRingBitmap {
    private const val START_ANGLE = 135f
    private const val SWEEP = 270f

    fun render(sizePx: Int, progress: Float, trackColor: Int, progressColor: Int): Bitmap {
        val size = sizePx.coerceAtLeast(32)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val stroke = size * 0.09f
        val inset = stroke / 2f + size * 0.01f
        val oval = RectF(inset, inset, size - inset, size - inset)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
        }
        paint.color = trackColor
        canvas.drawArc(oval, START_ANGLE, SWEEP, false, paint)
        val sweep = SWEEP * progress.coerceIn(0f, 1f)
        if (sweep > 0.5f) {
            paint.color = progressColor
            canvas.drawArc(oval, START_ANGLE, sweep, false, paint)
        }
        return bitmap
    }
}
