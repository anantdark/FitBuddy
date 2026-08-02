package com.anant.fitbuddy.ui.loading.animations

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anant.fitbuddy.ui.loading.LoadingAnimation
import com.anant.fitbuddy.ui.loading.LoadingAnimationScope
import com.anant.fitbuddy.ui.loading.LoadingAnimationSlot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Dashboard analyzing banner: flowing tiranga wash with a steady spinning chakra. */
object TirangaLoadingAnimation : LoadingAnimation {
    override val id: String = "tiranga"
    override val displayName: String = "Tiranga"
    override val slots: Set<LoadingAnimationSlot> = setOf(LoadingAnimationSlot.ANALYZING)
    override val defaultCaptions: List<String> = analyzingCaptions

    @Composable
    override fun Content(scope: LoadingAnimationScope) {
        TirangaBanner(
            modelId = scope.label,
            captions = scope.captions.ifEmpty { defaultCaptions },
            modifier = scope.modifier
        )
    }
}

private val Saffron = Color(0xFFFF9933)
private val FlagWhite = Color(0xFFFFF8F0)
private val IndiaGreen = Color(0xFF138808)
private val ChakraNavy = Color(0xFF000080)

@Composable
private fun TirangaBanner(
    modelId: String?,
    captions: List<String>,
    modifier: Modifier = Modifier
) {
    var fabricTime by remember { mutableStateOf(0.0) }
    var chakraTime by remember { mutableStateOf(0.0) }
    var lastFrame by remember { mutableLongStateOf(0L) }
    /** Speeds chakra spin only; fabric always advances at 1×. */
    var chakraSpinMultiplier by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                val delta = if (lastFrame == 0L) 0L else (now - lastFrame)
                lastFrame = now
                fabricTime = accumulateScaledTime(fabricTime, delta, 1f)
                chakraTime = accumulateScaledTime(chakraTime, delta, chakraSpinMultiplier)
            }
        }
    }

    var captionIndex by remember { mutableStateOf(0) }
    LaunchedEffect(captions) {
        if (captions.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(8_000L)
            captionIndex = (captionIndex + 1) % captions.size
        }
    }

    val firstLine = analyzingLabel(modelId)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black)
    ) {
        Box {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                chakraSpinMultiplier = 3f
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    chakraSpinMultiplier = 1f
                                }
                            }
                        )
                    }
            ) {
                val W = size.width
                val H = size.height
                val t = fabricTime
                // Columns for a silk / fabric flow; denser than pixels for soft look.
                val cols = 72
                val colW = W / cols + 1f

                for (i in 0 until cols) {
                    val x = i * (W / cols)
                    val nx = i.toFloat() / cols
                    // Traveling wave along the flag: boundaries undulate left→right.
                    val wave1 = sin(nx * PI * 2.4 + t * 0.0022).toFloat()
                    val wave2 = sin(nx * PI * 1.6 - t * 0.0017 + 1.2).toFloat()
                    val wave3 = cos(nx * PI * 3.1 + t * 0.0011).toFloat()
                    val seamWobble = wave1 * 0.045f + wave2 * 0.028f
                    val saffronEnd = (0.34f + seamWobble).coerceIn(0.22f, 0.48f)
                    val greenStart = (0.66f + seamWobble * 0.85f + wave3 * 0.02f)
                        .coerceIn(0.52f, 0.80f)

                    // Hue drift within each band — flowing dye, not flat paint.
                    val huePhase = (t * 0.00035 + nx * 1.8).toFloat()
                    val saffron = flowingBandColor(
                        base = Saffron,
                        hueShiftDeg = sin(huePhase * 2f) * 4.5f,
                        lightness = 0.94f + 0.05f * sin(huePhase + 0.4f)
                    )
                    val white = flowingBandColor(
                        base = FlagWhite,
                        hueShiftDeg = sin(huePhase * 1.3f + 1f) * 2f,
                        lightness = 0.97f + 0.025f * cos(huePhase * 0.8f)
                    )
                    val green = flowingBandColor(
                        base = IndiaGreen,
                        hueShiftDeg = cos(huePhase * 1.7f) * 4f,
                        lightness = 0.93f + 0.06f * sin(huePhase * 1.1f + 2f)
                    )

                    val y0 = 0f
                    val y1 = saffronEnd * H
                    val y2 = greenStart * H
                    val y3 = H

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
                        size = androidx.compose.ui.geometry.Size(colW, y1 - y0)
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
                        size = androidx.compose.ui.geometry.Size(colW, (y2 - y1).coerceAtLeast(1f))
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
                        size = androidx.compose.ui.geometry.Size(colW, y3 - y2)
                    )
                }

                // Soft traveling sheen across the fabric (does not move the chakra).
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

                // Chakra: fixed position; spin uses chakraTime (press speeds only this).
                val chakraCenter = Offset(W - 28.dp.toPx(), H / 2f)
                val outerR = 16.dp.toPx()
                val degrees = ((chakraTime * 0.12) % 360.0).toFloat()

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ChakraNavy.copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = chakraCenter,
                        radius = outerR * 1.55f
                    ),
                    radius = outerR * 1.55f,
                    center = chakraCenter
                )

                rotate(degrees = degrees, pivot = chakraCenter) {
                    drawAshokaChakra(
                        center = chakraCenter,
                        outerRadius = outerR,
                        color = ChakraNavy
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 14.dp, end = 56.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = firstLine,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = ChakraNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (captions.isNotEmpty()) {
                    Text(
                        text = captions[captionIndex % captions.size],
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        color = ChakraNavy.copy(alpha = 0.62f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Ashoka-style wheel: rim, hub, 24 tapered spokes with tip beads —
 * closer to the national emblem than plain radial lines.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAshokaChakra(
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
    val halfAngle = (PI / 24.0 * 0.22).toFloat() // spoke half-width in radians

    // Outer rim (annulus via two strokes).
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

        // Small bead where each spoke meets the rim (classic chakra detail).
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

    // Central boss.
    drawCircle(color = color, radius = hubR, center = center)
    drawCircle(
        color = color,
        radius = hubR * 0.42f,
        center = center,
        style = Stroke(width = rimStroke * 0.35f, join = StrokeJoin.Round)
    )
}

/** Shift a base RGB toward a nearby hue while keeping saturation soft. */
private fun flowingBandColor(base: Color, hueShiftDeg: Float, lightness: Float): Color {
    val (h, s, l) = rgbToHsl(base.red, base.green, base.blue)
    val nh = (h + hueShiftDeg / 360f).mod(1f)
    val ns = (s * 0.92f).coerceIn(0f, 1f)
    val nl = (l * lightness).coerceIn(0.15f, 0.97f)
    val (r, g, b) = hslToRgb(nh, ns, nl)
    return Color(r, g, b, base.alpha)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
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
