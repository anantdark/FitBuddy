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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anant.fitbuddy.ui.components.MacroCarbsColor
import com.anant.fitbuddy.ui.components.MacroFatsColor
import com.anant.fitbuddy.ui.components.MacroProteinColor
import com.anant.fitbuddy.ui.loading.LoadingAnimation
import com.anant.fitbuddy.ui.loading.LoadingAnimationScope
import com.anant.fitbuddy.ui.loading.LoadingAnimationSlot
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Dashboard analyzing banner: sun + macro planets on a starfield. */
object SolarSystemLoadingAnimation : LoadingAnimation {
    override val id: String = "solar_system"
    override val displayName: String = "Solar system"
    override val slots: Set<LoadingAnimationSlot> = setOf(LoadingAnimationSlot.ANALYZING)
    override val defaultCaptions: List<String> = analyzingCaptions

    @Composable
    override fun Content(scope: LoadingAnimationScope) {
        SolarSystemBanner(
            modelId = scope.label,
            captions = scope.captions.ifEmpty { defaultCaptions },
            modifier = scope.modifier
        )
    }
}

@Composable
private fun SolarSystemBanner(
    modelId: String?,
    captions: List<String>,
    modifier: Modifier = Modifier
) {
    var scaledTime by remember { mutableStateOf(0.0) }
    var lastFrame by remember { mutableLongStateOf(0L) }
    var speedMultiplier by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                val delta = if (lastFrame == 0L) 0L else (now - lastFrame)
                lastFrame = now
                scaledTime = accumulateScaledTime(scaledTime, delta, speedMultiplier)
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

    val amber = Color(0xFFFFC107)
    val sunGlowColor = Color(0xFFFFE082)
    val planets = remember { solarSystemPlanets(MacroProteinColor, MacroCarbsColor, MacroFatsColor) }
    val firstLine = analyzingLabel(modelId)

    data class Star(
        val x: Float, val y: Float, val baseAlpha: Float,
        val freq: Double, val phase: Double, val radius: Float
    )
    val stars = remember {
        val rng = kotlin.random.Random(0xA57A_2024)
        List(80) {
            Star(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                baseAlpha = 0.25f + rng.nextFloat() * 0.70f,
                freq = 0.0008 + rng.nextDouble() * 0.003,
                phase = rng.nextDouble() * 6.283,
                radius = 0.5f + rng.nextFloat() * 1.8f
            )
        }
    }

    data class ShootingStar(
        val startX: Float, val startY: Float,
        val dx: Float, val dy: Float,
        val periodMs: Double,
        val offsetMs: Double,
        val alpha: Float,
        val length: Float
    )
    val shootingStars = remember {
        val rng = kotlin.random.Random(0xB33F_2025)
        List(6) {
            val angle = rng.nextDouble() * 2.0 * Math.PI
            ShootingStar(
                startX = rng.nextFloat(),
                startY = rng.nextFloat(),
                dx = cos(angle).toFloat(),
                dy = sin(angle).toFloat(),
                periodMs = 1800.0 + rng.nextDouble() * 3200.0,
                offsetMs = rng.nextDouble() * 5000.0,
                alpha = 0.55f + rng.nextFloat() * 0.45f,
                length = 0.12f + rng.nextFloat() * 0.22f
            )
        }
    }

    val tailSteps = 55
    val tailStepMs = 14.0
    val sunTailSteps = 40
    val sunTailStepMs = 20.0

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
                                speedMultiplier = 3f
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    speedMultiplier = 1f
                                }
                            }
                        )
                    }
            ) {
                val W = size.width
                val H = size.height
                val cy = H / 2f
                val halfW = W / 2f
                val halfH = H / 2f
                val now = scaledTime

                stars.forEach { s ->
                    val twinkle = sin(now * s.freq + s.phase).toFloat()
                    val a = (s.baseAlpha + twinkle * s.baseAlpha * 0.9f).coerceIn(0.02f, 1.0f)
                    drawCircle(
                        color = Color.White.copy(alpha = a),
                        radius = s.radius,
                        center = Offset(s.x * W, s.y * H)
                    )
                }

                shootingStars.forEach { ss ->
                    val t = (((now + ss.offsetMs) % ss.periodMs) / ss.periodMs).toFloat()
                    if (t < 0.15f) {
                        val progress = t / 0.15f
                        val headX = (ss.startX + ss.dx * ss.length * progress) * W
                        val headY = (ss.startY + ss.dy * ss.length * H / W * progress) * H
                        val tailX = (ss.startX + ss.dx * ss.length * (progress - 0.4f).coerceAtLeast(0f)) * W
                        val tailY = (ss.startY + ss.dy * ss.length * H / W * (progress - 0.4f).coerceAtLeast(0f)) * H
                        val a = ss.alpha * (1f - abs(progress - 0.5f) * 2f)
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = a)),
                                start = Offset(tailX, tailY),
                                end = Offset(headX, headY)
                            ),
                            start = Offset(tailX, tailY),
                            end = Offset(headX, headY),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                val sx = sunX(now, W)
                val sunPos = Offset(sx, cy)

                val sunTrailPts = mutableListOf<Offset>()
                for (step in sunTailSteps downTo 1) {
                    val past = now - step * sunTailStepMs
                    val psx = sunX(past, W)
                    if (psx > sx) continue
                    sunTrailPts.add(Offset(psx, cy))
                }
                sunTrailPts.add(Offset(sx, cy))

                val sunTrailW = 6.dp.toPx()
                for (i in 0 until sunTrailPts.size - 1) {
                    val p0 = sunTrailPts[i]
                    val p1 = sunTrailPts[i + 1]
                    val a0 = (i.toFloat() / sunTrailPts.size) * 0.45f
                    val a1 = ((i + 1).toFloat() / sunTrailPts.size) * 0.45f
                    val w = sunTrailW * (i.toFloat() / sunTrailPts.size)
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(sunGlowColor.copy(alpha = a0), sunGlowColor.copy(alpha = a1)),
                            start = p0, end = p1
                        ),
                        start = p0, end = p1,
                        strokeWidth = w.coerceAtLeast(1.dp.toPx()),
                        cap = StrokeCap.Round
                    )
                }

                val states = planets.map { spec ->
                    spec to helicalPoint(spec, now, cy, halfW, halfH, W)
                }
                val (backPlanets, frontPlanets) = orderByDepth(states)

                fun drawPlanet(spec: PlanetSpec, pos: Triple<Float, Float, Float>) {
                    val (hx, hy, hz) = pos
                    val trailPts = mutableListOf<Pair<Offset, Float>>()
                    for (step in tailSteps downTo 1) {
                        val past = now - step * tailStepMs
                        if (sunX(past, W) > sx) continue
                        val (px, py, pz) = helicalPoint(spec, past, cy, halfW, halfH, W)
                        val f = step.toFloat() / tailSteps
                        val alpha = (1f - f) * 0.55f * depthAlpha(pz)
                        trailPts.add(Pair(Offset(px, py), alpha))
                    }
                    trailPts.add(Pair(Offset(hx, hy), 0.55f * depthAlpha(hz)))

                    val baseWidth = 4.dp.toPx() * depthScale(hz)
                    for (i in 0 until trailPts.size - 1) {
                        val (p0, a0) = trailPts[i]
                        val (p1, a1) = trailPts[i + 1]
                        val segWidth = baseWidth * (i.toFloat() / trailPts.size)
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    spec.color.copy(alpha = a0),
                                    spec.color.copy(alpha = a1)
                                ),
                                start = p0,
                                end = p1
                            ),
                            start = p0,
                            end = p1,
                            strokeWidth = segWidth.coerceAtLeast(1.5.dp.toPx()),
                            cap = StrokeCap.Round
                        )
                    }

                    val scale = depthScale(hz)
                    val alpha = depthAlpha(hz)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(spec.color.copy(alpha = 0.45f * alpha), Color.Transparent),
                            center = Offset(hx, hy), radius = 8.dp.toPx() * scale
                        ),
                        radius = 8.dp.toPx() * scale, center = Offset(hx, hy)
                    )
                    drawCircle(
                        color = spec.color.copy(alpha = 0.98f * alpha),
                        radius = 3.dp.toPx() * scale,
                        center = Offset(hx, hy)
                    )
                }

                backPlanets.forEach { (spec, pos) -> drawPlanet(spec, pos) }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(amber.copy(alpha = 0.22f), Color.Transparent),
                        center = sunPos, radius = 20.dp.toPx()
                    ), radius = 20.dp.toPx(), center = sunPos
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(amber.copy(alpha = 0.60f), Color.Transparent),
                        center = sunPos, radius = 9.dp.toPx()
                    ), radius = 9.dp.toPx(), center = sunPos
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFBE7), amber),
                        center = sunPos, radius = 4.5.dp.toPx()
                    ), radius = 4.5.dp.toPx(), center = sunPos
                )

                frontPlanets.forEach { (spec, pos) -> drawPlanet(spec, pos) }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = firstLine,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White,
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
                        color = Color.White.copy(alpha = 0.55f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
