package com.anant.fitbuddy.ui.region

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import com.anant.fitbuddy.data.region.AppRegion
import com.anant.fitbuddy.ui.loading.animations.accumulateScaledTime
import com.anant.fitbuddy.ui.loading.animations.drawSpinningAshokaChakra
import com.anant.fitbuddy.ui.loading.animations.drawTirangaFabric
import com.anant.fitbuddy.ui.loading.animations.lerpColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val UsRed = Color(0xFFB22234)
private val UsWhite = Color(0xFFF8F5EF)
private val UsCanton = Color(0xFF3C3B6E)
private val EuropeBlue = Color(0xFF003399)
private val EuropeYellow = Color(0xFFFFCC00)

/**
 * Small waving-flag preview for the region picker, reusing the tiranga column-wave fabric
 * technique ([drawTirangaFabric] / [drawSpinningAshokaChakra] are `internal`, same module).
 * Kept to a single infinite-frame clock per instance — cheap enough for a few on screen.
 */
@Composable
fun RegionFlagCanvas(region: AppRegion, modifier: Modifier = Modifier) {
    var timeMs by remember { mutableStateOf(0.0) }
    var lastFrame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                val delta = if (lastFrame == 0L) 0L else now - lastFrame
                lastFrame = now
                timeMs = accumulateScaledTime(timeMs, delta, 1f)
            }
        }
    }

    Canvas(modifier = modifier) {
        when (region) {
            AppRegion.INDIA -> drawIndiaFlag(timeMs)
            AppRegion.US -> drawUsFlag(timeMs)
            AppRegion.EUROPE -> drawEuropeFlag(timeMs)
        }
    }
}

private fun DrawScope.drawIndiaFlag(timeMs: Double) {
    drawTirangaFabric(timeMs = timeMs, columns = 56)
    val center = Offset(size.width / 2f, size.height / 2f)
    drawSpinningAshokaChakra(
        center = center,
        outerRadius = min(size.width, size.height) * 0.14f,
        timeMs = timeMs
    )
}

private fun DrawScope.drawUsFlag(timeMs: Double) {
    val w = size.width
    val h = size.height
    val columns = 56
    val colW = w / columns + 1f
    val stripeCount = 13

    // 13 horizontal stripes, red on top and bottom (official).
    for (i in 0 until columns) {
        val x = i * (w / columns)
        val nx = i.toFloat() / columns
        val boundaries = FloatArray(stripeCount + 1)
        boundaries[0] = 0f
        boundaries[stripeCount] = 1f
        for (s in 1 until stripeCount) {
            val wobble = columnWave(nx, timeMs, seed = s * 0.6) * 0.011f
            boundaries[s] = (s.toFloat() / stripeCount + wobble)
        }
        for (s in 0 until stripeCount) {
            val y0 = boundaries[s] * h
            val y1 = boundaries[s + 1] * h
            drawRect(
                color = if (s % 2 == 0) UsRed else UsWhite,
                topLeft = Offset(x, y0),
                size = Size(colW, (y1 - y0).coerceAtLeast(0.5f))
            )
        }
    }

    // Union (canton): height = 7/13 of hoist; width ≈ 2/5 of fly (official when fly:hoist = 1.9).
    val cantonW = w * 0.4f
    val cantonHBase = (7f / 13f) * h
    val cantonColumns = 26
    val cantonColW = cantonW / cantonColumns + 1f
    for (i in 0 until cantonColumns) {
        val x = i * (cantonW / cantonColumns)
        val nx = (x / w).coerceIn(0f, 1f)
        val wobble = columnWave(nx, timeMs, seed = 4.2) * 0.012f
        val bottom = cantonHBase + wobble * h
        drawRect(color = UsCanton, topLeft = Offset(x, 0f), size = Size(cantonColW, bottom))
    }

    // 50 white five-point stars: 9 rows alternating 6 and 5 (6-5-6-5-6-5-6-5-6).
    // Construction grid: 12 horizontal half-units, 10 vertical half-units of the canton.
    val starOuter = min(cantonW / 12f, cantonHBase / 10f) * 0.72f
    for (row in 0 until 9) {
        val starsInRow = if (row % 2 == 0) 6 else 5
        val yUnit = (row + 1).toFloat() // 1..9 of 10
        for (col in 0 until starsInRow) {
            // 6-star rows at odd twelfths (1,3,…,11); 5-star rows at even (2,4,…,10).
            val xUnit = if (starsInRow == 6) {
                (col * 2 + 1).toFloat()
            } else {
                (col * 2 + 2).toFloat()
            }
            val dx = cantonW * (xUnit / 12f)
            val nx = (dx / w).coerceIn(0f, 1f)
            val wobble = columnWave(nx, timeMs, seed = 4.2) * 0.012f * h
            val dy = cantonHBase * (yUnit / 10f) + wobble
            drawFivePointStar(
                center = Offset(dx, dy),
                outerRadius = starOuter,
                color = UsWhite,
                rotationRad = 0.0 // point-up, as on the US flag
            )
        }
    }
}

private fun DrawScope.drawEuropeFlag(timeMs: Double) {
    val w = size.width
    val h = size.height
    val columns = 48
    val colW = w / columns + 1f

    for (i in 0 until columns) {
        val x = i * (w / columns)
        val nx = i.toFloat() / columns
        val wave = columnWave(nx, timeMs, seed = 1.7)
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    lerpColor(EuropeBlue, Color.White, (0.10f + 0.05f * wave).coerceIn(0.02f, 0.20f)),
                    EuropeBlue,
                    lerpColor(EuropeBlue, Color.Black, (0.08f + 0.04f * wave).coerceIn(0.0f, 0.18f))
                )
            ),
            topLeft = Offset(x, 0f),
            size = Size(colW, h)
        )
    }

    val sheenX = ((timeMs * 0.07) % (w * 1.4) - w * 0.2).toFloat()
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent),
            startX = sheenX,
            endX = sheenX + w * 0.35f
        )
    )

    // Official EU: 12 gold five-point stars, point-up, equally spaced on a circle
    // (one star at 12 o'clock). Ring diameter is 2/3 of the flag's hoist.
    val center = Offset(w / 2f, h / 2f)
    val ringRadius = min(w, h) * (1f / 3f)
    val starRadius = ringRadius * 0.18f // each star's diameter ≈ 1/9 of the flag height
    for (k in 0 until 12) {
        val angle = -PI / 2.0 + k * (2.0 * PI / 12.0)
        val sway = sin(timeMs * 0.0012 + k * 0.9).toFloat() * ringRadius * 0.03f
        val cx = center.x + cos(angle).toFloat() * ringRadius + sway
        val cy = center.y + sin(angle).toFloat() * ringRadius
        drawFivePointStar(
            center = Offset(cx, cy),
            outerRadius = starRadius,
            color = EuropeYellow,
            rotationRad = 0.0
        )
    }
}

/** Continuous ~[-1, 1] ripple used for column-wave seam wobble. */
private fun columnWave(nx: Float, timeMs: Double, seed: Double = 0.0): Float {
    val t = timeMs
    val a = sin(nx * PI * 2.6 + t * 0.0021 + seed).toFloat()
    val b = sin(nx * PI * 1.7 - t * 0.0016 + 1.1 + seed * 1.3).toFloat()
    return a * 0.6f + b * 0.4f
}

/**
 * Point-up five-point star. Inner/outer radius ratio ≈ 0.382 matches regular pentagram stars
 * used on the US and EU flags.
 */
private fun DrawScope.drawFivePointStar(
    center: Offset,
    outerRadius: Float,
    color: Color,
    rotationRad: Double = 0.0
) {
    val innerRadius = outerRadius * 0.382f
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outerRadius else innerRadius
        val angle = -PI / 2.0 + rotationRad + i * (PI / 5.0)
        val x = center.x + (cos(angle) * r).toFloat()
        val y = center.y + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color = color, style = Fill)
}
