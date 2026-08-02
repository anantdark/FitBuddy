package com.anant.fitbuddy.ui.loading.animations

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal val TirangaSaffron = Color(0xFFFF9933)
internal val TirangaFlagWhite = Color(0xFFFFF8F0)
internal val TirangaIndiaGreen = Color(0xFF138808)
internal val TirangaChakraNavy = Color(0xFF000080)

/**
 * Analyzing-banner tiranga wash: classic look with a slightly wider white band,
 * seam weights like the original, but incommensurate frequencies so it doesn't
 * settle into a short repeating loop.
 */
internal fun DrawScope.drawTirangaFabric(
    timeMs: Double,
    columns: Int = 72,
    seamWobbleScale: Float = 1f,
) {
    val W = size.width
    val H = size.height
    val t = timeMs
    val cols = columns.coerceAtLeast(8)
    val colW = W / cols + 1f

    for (i in 0 until cols) {
        val x = i * (W / cols)
        val nx = i.toFloat() / cols
        val drift = t * 0.00013
        val wave1 = sin(nx * PI * 2.618 + t * 0.00203 + drift).toFloat()
        val wave2 = sin(nx * PI * 1.732 - t * 0.00161 + 1.2 + drift * 0.7).toFloat()
        val wave3 = cos(nx * PI * 3.142 + t * 0.00097 - drift * 1.3).toFloat()
        val seamWobble = (wave1 * 0.045f + wave2 * 0.028f) * seamWobbleScale
        val saffronEnd = (0.31f + seamWobble).coerceIn(0.20f, 0.45f)
        val greenStart = (0.69f + seamWobble * 0.85f + wave3 * 0.02f)
            .coerceIn(0.55f, 0.82f)

        val huePhase = (t * 0.00035 + nx * 1.8).toFloat()
        drawTirangaColumnBands(
            x = x,
            colW = colW,
            height = H,
            saffronEnd = saffronEnd,
            greenStart = greenStart,
            saffron = flowingBandColor(
                base = TirangaSaffron,
                hueShiftDeg = sin(huePhase * 2f) * 4.5f,
                lightness = 0.94f + 0.05f * sin(huePhase + 0.4f)
            ),
            white = flowingBandColor(
                base = TirangaFlagWhite,
                hueShiftDeg = sin(huePhase * 1.3f + 1f) * 2f,
                lightness = 0.97f + 0.025f * cos(huePhase * 0.8f)
            ),
            green = flowingBandColor(
                base = TirangaIndiaGreen,
                hueShiftDeg = cos(huePhase * 1.7f) * 4f,
                lightness = 0.93f + 0.06f * sin(huePhase * 1.1f + 2f)
            )
        )
    }

    val sheenX = ((t * 0.08) % (W * 1.4) - W * 0.2).toFloat()
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = 0.14f),
                Color.Transparent
            ),
            startX = sheenX,
            endX = sheenX + W * 0.35f
        )
    )
}

/**
 * Insights-button tiranga wash: freer stacked incommensurate waves with
 * per-column phase salt so saffron and green seams drift independently.
 */
internal fun DrawScope.drawTirangaInsightFabric(
    timeMs: Double,
    columns: Int = 48,
    seamWobbleScale: Float = 1.35f,
) {
    val W = size.width
    val H = size.height
    val t = timeMs
    val cols = columns.coerceAtLeast(8)
    val colW = W / cols + 1f
    val gust = 0.72f + 0.28f * fabricNoise(t * 0.00041, 0.37)

    for (i in 0 until cols) {
        val x = i * (W / cols)
        val nx = i.toFloat() / cols
        val salt = columnSalt(i)
        val saffronWobble =
            fabricField(nx, t, salt, seed = 0.0) * 0.055f * seamWobbleScale * gust
        val greenWobble =
            fabricField(nx, t, salt, seed = 2.7) * 0.052f * seamWobbleScale * gust
        val whiteDrift = fabricField(nx, t, salt, seed = 5.1) * 0.022f * seamWobbleScale

        val saffronEnd = (0.34f + saffronWobble).coerceIn(0.20f, 0.50f)
        val greenStart = (0.66f + greenWobble + whiteDrift * 0.35f)
            .coerceIn(0.50f, 0.82f)
            .coerceAtLeast(saffronEnd + 0.12f)

        val huePhase = fabricField(nx, t * 0.85, salt, seed = 8.3) * 0.55f +
            (t * 0.00028 + nx * 1.3).toFloat()
        drawTirangaColumnBands(
            x = x,
            colW = colW,
            height = H,
            saffronEnd = saffronEnd,
            greenStart = greenStart,
            saffron = flowingBandColor(
                base = TirangaSaffron,
                hueShiftDeg = sin(huePhase * 2.1f + salt) * 5.5f,
                lightness = 0.93f + 0.06f * sin(huePhase + salt * 0.7f)
            ),
            white = flowingBandColor(
                base = TirangaFlagWhite,
                hueShiftDeg = sin(huePhase * 1.4f + 1.3f) * 2.4f,
                lightness = 0.96f + 0.03f * cos(huePhase * 0.9f + salt)
            ),
            green = flowingBandColor(
                base = TirangaIndiaGreen,
                hueShiftDeg = cos(huePhase * 1.9f + salt * 1.1f) * 5f,
                lightness = 0.92f + 0.07f * sin(huePhase * 1.2f + 2.1f)
            )
        )
    }

    val sheenDrift = fabricNoise(t * 0.00019, 1.1) * W * 0.45f
    val sheenBase = ((t * 0.055 + sheenDrift) % (W * 1.6) - W * 0.3).toFloat()
    val sheenAlpha = 0.08f + 0.10f * (0.5f + 0.5f * fabricNoise(t * 0.00033, 2.4))
    val sheenWidth = W * (0.28f + 0.12f * fabricNoise(t * 0.00027, 3.2))
    drawRect(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                Color.White.copy(alpha = sheenAlpha),
                Color.Transparent
            ),
            startX = sheenBase,
            endX = sheenBase + sheenWidth
        )
    )
}

private fun DrawScope.drawTirangaColumnBands(
    x: Float,
    colW: Float,
    height: Float,
    saffronEnd: Float,
    greenStart: Float,
    saffron: Color,
    white: Color,
    green: Color,
) {
    val y0 = 0f
    val y1 = saffronEnd * height
    val y2 = greenStart * height
    val y3 = height

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                saffron.copy(alpha = 0.95f),
                saffron,
                lerpColor(saffron, white, 0.55f)
            ),
            startY = y0,
            endY = y1
        ),
        topLeft = Offset(x, y0),
        size = Size(colW, y1 - y0)
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                lerpColor(saffron, white, 0.7f),
                white,
                lerpColor(white, green, 0.35f)
            ),
            startY = y1,
            endY = y2
        ),
        topLeft = Offset(x, y1),
        size = Size(colW, (y2 - y1).coerceAtLeast(1f))
    )
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                lerpColor(white, green, 0.45f),
                green,
                green.copy(alpha = 0.92f)
            ),
            startY = y2,
            endY = y3
        ),
        topLeft = Offset(x, y2),
        size = Size(colW, y3 - y2)
    )
}

/** Continuous ≈[-1, 1] field from stacked incommensurate sines. */
private fun fabricField(nx: Float, tMs: Double, salt: Float, seed: Double): Float {
    val t = tMs
    val s = seed + salt
    val a = sin(nx * PI * 2.17 + t * 0.0019 + s).toFloat()
    val b = sin(nx * PI * 3.73 - t * 0.00131 + s * 1.7).toFloat()
    val c = cos(nx * PI * 1.19 + t * 0.00077 + s * 0.4).toFloat()
    val d = sin(nx * PI * 5.41 + t * 0.00263 - s * 2.1).toFloat()
    val e = cos(t * 0.00053 + nx * 7.9 + s).toFloat()
    return (a * 0.38f + b * 0.27f + c * 0.18f + d * 0.12f + e * 0.05f)
}

private fun fabricNoise(tMs: Double, seed: Double): Float =
    sin(tMs * 2.17 + seed).toFloat() * 0.55f +
        cos(tMs * 3.41 - seed * 1.3).toFloat() * 0.30f +
        sin(tMs * 0.97 + seed * 2.7).toFloat() * 0.15f

/** Deterministic [0, 2π)-ish salt from column index. */
private fun columnSalt(i: Int): Float {
    val n = (i * 1103515245 + 12345) and 0x7fffffff
    return (n % 10_000) / 10_000f * (2f * PI.toFloat())
}

/** Spinning Ashoka chakra with a soft navy halo. */
internal fun DrawScope.drawSpinningAshokaChakra(
    center: Offset,
    outerRadius: Float,
    timeMs: Double,
    color: Color = TirangaChakraNavy,
) {
    val degrees = ((timeMs * 0.12) % 360.0).toFloat()

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                color.copy(alpha = 0.18f),
                Color.Transparent
            ),
            center = center,
            radius = outerRadius * 1.55f
        ),
        radius = outerRadius * 1.55f,
        center = center
    )

    rotate(degrees = degrees, pivot = center) {
        drawAshokaChakra(
            center = center,
            outerRadius = outerRadius,
            color = color
        )
    }
}

/**
 * Ashoka-style wheel: rim, hub, 24 tapered spokes with tip beads —
 * closer to the national emblem than plain radial lines.
 */
internal fun DrawScope.drawAshokaChakra(
    center: Offset,
    outerRadius: Float,
    color: Color
) {
    val rimStroke = (outerRadius * 0.095f).coerceAtLeast(1.1f)
    val rimOuter = outerRadius
    val rimInner = outerRadius * 0.88f
    val hubR = outerRadius * 0.13f
    val spokeRoot = hubR * 1.15f
    val spokeTip = rimInner * 0.96f
    val halfAngle = (PI / 24.0 * 0.22).toFloat()

    drawCircle(
        color = color,
        radius = rimOuter,
        center = center,
        style = Stroke(width = rimStroke, cap = StrokeCap.Round)
    )
    drawCircle(
        color = color.copy(alpha = 0.85f),
        radius = rimInner,
        center = center,
        style = Stroke(width = rimStroke * 0.45f)
    )

    for (i in 0 until 24) {
        val a = (i * (2.0 * PI / 24.0)).toFloat()
        val left = a - halfAngle
        val right = a + halfAngle
        val tipLeft = a - halfAngle * 0.55f
        val tipRight = a + halfAngle * 0.55f

        val path = Path().apply {
            moveTo(
                center.x + cos(left) * spokeRoot,
                center.y + sin(left) * spokeRoot
            )
            lineTo(
                center.x + cos(tipLeft) * spokeTip,
                center.y + sin(tipLeft) * spokeTip
            )
            lineTo(
                center.x + cos(a) * (spokeTip + rimStroke * 0.35f),
                center.y + sin(a) * (spokeTip + rimStroke * 0.35f)
            )
            lineTo(
                center.x + cos(tipRight) * spokeTip,
                center.y + sin(tipRight) * spokeTip
            )
            lineTo(
                center.x + cos(right) * spokeRoot,
                center.y + sin(right) * spokeRoot
            )
            close()
        }
        drawPath(path, color = color, style = Fill)

        val bead = Offset(
            center.x + cos(a) * ((rimInner + rimOuter) * 0.5f),
            center.y + sin(a) * ((rimInner + rimOuter) * 0.5f)
        )
        drawCircle(
            color = color,
            radius = rimStroke * 0.55f,
            center = bead
        )
    }

    drawCircle(color = color, radius = hubR, center = center)
    drawCircle(
        color = color,
        radius = hubR * 0.42f,
        center = center,
        style = Stroke(width = rimStroke * 0.35f, join = StrokeJoin.Round)
    )
}

/** Shift a base RGB toward a nearby hue while keeping saturation soft. */
internal fun flowingBandColor(base: Color, hueShiftDeg: Float, lightness: Float): Color {
    val (h, s, l) = rgbToHsl(base.red, base.green, base.blue)
    val nh = (h + hueShiftDeg / 360f).mod(1f)
    val ns = (s * 0.92f).coerceIn(0f, 1f)
    val nl = (l * lightness).coerceIn(0.15f, 0.97f)
    val (r, g, b) = hslToRgb(nh, ns, nl)
    return Color(r, g, b, base.alpha)
}

internal fun lerpColor(a: Color, b: Color, t: Float): Color {
    val u = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * u,
        green = a.green + (b.green - a.green) * u,
        blue = a.blue + (b.blue - a.blue) * u,
        alpha = a.alpha + (b.alpha - a.alpha) * u
    )
}

private fun rgbToHsl(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return Triple(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> ((g - b) / d + if (g < b) 6f else 0f) / 6f
        g -> ((b - r) / d + 2f) / 6f
        else -> ((r - g) / d + 4f) / 6f
    }
    return Triple(h, s, l)
}

private fun hslToRgb(h: Float, s: Float, l: Float): Triple<Float, Float, Float> {
    if (s == 0f) return Triple(l, l, l)
    fun hue2rgb(p: Float, q: Float, tIn: Float): Float {
        var t = tIn
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    return Triple(
        hue2rgb(p, q, h + 1f / 3f),
        hue2rgb(p, q, h),
        hue2rgb(p, q, h - 1f / 3f)
    )
}
