package com.anant.fitbuddy.ui.loading.animations

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Dashboard analyzing banner: miniature Keplerian solar system crossing the frame. */
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
    var speedMultiplier by remember { mutableFloatStateOf(BANNER_SPEED_NATURAL) }
    var isBoosting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                val delta = if (lastFrame == 0L) 0L else (now - lastFrame)
                lastFrame = now
                scaledTime = accumulateScaledTime(scaledTime, delta, speedMultiplier)
            }
        }
    }

    var captionIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(captions) {
        if (captions.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(8_000L)
            captionIndex = (captionIndex + 1) % captions.size
        }
    }

    val amber = Color(0xFFFFB300)
    val sunCore = Color(0xFFFFF8E1)
    val sunHot = Color(0xFFFFE082)
    val sunPlasma = Color(0xFFFF6D00)
    val sunLimb = Color(0xFFBF360C)
    val spaceDeep = Color(0xFF02030A)
    val spaceMid = Color(0xFF070B16)
    val planets = remember { solarSystemPlanets(MacroProteinColor, MacroCarbsColor, MacroFatsColor) }
    val firstLine = analyzingLabel(modelId)

    data class Star(
        val x: Float, val y: Float, val baseAlpha: Float,
        val freq: Double, val phase: Double, val radius: Float,
        val layer: Float, val tint: Color
    )
    val stars = remember {
        val rng = kotlin.random.Random(0xA57A_2024)
        val tints = listOf(
            Color(0xFFFFFFFF), Color(0xFFE3ECFF), Color(0xFFFFF1DD),
            Color(0xFFC9D7FF), Color(0xFFFFE0C2)
        )
        List(120) {
            Star(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                baseAlpha = 0.10f + rng.nextFloat() * 0.70f,
                freq = 0.0005 + rng.nextDouble() * 0.0030,
                phase = rng.nextDouble() * (2.0 * PI),
                radius = 0.28f + rng.nextFloat() * 1.35f,
                layer = 0.15f + rng.nextFloat() * 0.85f,
                tint = tints[rng.nextInt(tints.size)]
            )
        }
    }

    data class ShootingStar(
        val startX: Float, val startY: Float,
        val dx: Float, val dy: Float,
        val periodMs: Double, val offsetMs: Double,
        val alpha: Float, val length: Float
    )
    val shootingStars = remember {
        val rng = kotlin.random.Random(0xB33F_2025)
        List(4) {
            val angle = Math.PI + (-0.28 + rng.nextDouble() * 0.56)
            ShootingStar(
                startX = 0.25f + rng.nextFloat() * 0.7f,
                startY = rng.nextFloat() * 0.55f,
                dx = cos(angle).toFloat(),
                dy = sin(angle).toFloat() * 0.35f,
                periodMs = 3200.0 + rng.nextDouble() * 4500.0,
                offsetMs = rng.nextDouble() * 7000.0,
                alpha = 0.45f + rng.nextFloat() * 0.40f,
                length = 0.06f + rng.nextFloat() * 0.08f // short white streaks only
            )
        }
    }

    // Arc blur covers a fixed angle of orbit so fast inners don't smear into rings
    // and slow outers still show a readable motion trail.
    val arcRadians = 0.55
    val arcSteps = 12

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = spaceDeep)
    ) {
        Box {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                // Hold → boost tempo + optical-flow star streaks.
                                speedMultiplier = BANNER_SPEED_BOOST
                                isBoosting = true
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    isBoosting = false
                                    speedMultiplier = BANNER_SPEED_NATURAL
                                }
                            }
                        )
                    }
            ) {
                val W = size.width
                val H = size.height
                val halfH = H / 2f
                val now = scaledTime
                val progress = sunProgress(now)
                // Wrap fade only for the sun's teleport — planets stay lit at the edges.
                val wrapFade = edgeFade(progress)
                val rush = streakScale(speedMultiplier)
                val pulse = 0.94f + 0.06f * sin(now * 0.0022).toFloat()
                val boost = isBoosting

                val sx = sunX(now, W)
                val sy = sunY(now, H)
                val sunPos = Offset(sx, sy)
                val sunDiskR = 5.6.dp.toPx() * pulse

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(spaceMid, spaceDeep, Color(0xFF010208))
                    )
                )

                // Soft speed haze while boosting (reads as motion, not clutter).
                if (boost) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colorStops = arrayOf(
                                0f to Color.White.copy(alpha = 0.03f),
                                0.45f to Color.Transparent,
                                1f to Color.White.copy(alpha = 0.02f)
                            )
                        )
                    )
                }

                stars.forEach { s ->
                    val y = s.y * H
                    if (boost) {
                        // Optical flow: sun flies L→R ⇒ stars stream left.
                        // Head = current position; trail fades behind (to the right).
                        val scroll = (now * (0.00022 + 0.00070 * s.layer)).toFloat()
                        val xNorm = ((s.x - scroll) % 1f + 1f) % 1f
                        val headX = xNorm * W
                        // Modest lengths — long warp rays looked messy in an 82dp banner.
                        val len = W * (0.028f + 0.10f * s.layer)
                        val a = (s.baseAlpha * (0.45f + 0.55f * s.layer)).coerceIn(0.12f, 0.90f)
                        val head = Offset(headX, y)
                        val tail = Offset(headX + len, y)
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = a * 0.55f)
                                ),
                                start = tail,
                                end = head
                            ),
                            start = tail,
                            end = head,
                            strokeWidth = (0.7f + s.radius * 0.35f).coerceIn(0.7f, 1.6f),
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = a),
                            radius = (s.radius * 0.55f).coerceIn(0.6f, 1.4f),
                            center = head
                        )
                    } else {
                        val twinkle = sin(now * s.freq + s.phase).toFloat()
                        val a = (s.baseAlpha + twinkle * s.baseAlpha * 0.75f).coerceIn(0.02f, 1f)
                        val parallax = progress * 0.08f * rush
                        val sxStar = ((s.x - parallax * s.layer) % 1f + 1f) % 1f
                        drawCircle(
                            color = s.tint.copy(alpha = a),
                            radius = s.radius,
                            center = Offset(sxStar * W, y)
                        )
                    }
                }

                // Short white meteors — calm only (boost already has a full streak field).
                if (!boost) {
                    shootingStars.forEach { ss ->
                        val t = (((now + ss.offsetMs) % ss.periodMs) / ss.periodMs).toFloat()
                        if (t < 0.10f) {
                            val u = t / 0.10f
                            val travel = ss.length * u
                            val head = Offset(
                                (ss.startX + ss.dx * travel) * W,
                                (ss.startY + ss.dy * travel) * H
                            )
                            val tailU = (u - 0.55f).coerceAtLeast(0f)
                            val tail = Offset(
                                (ss.startX + ss.dx * ss.length * tailU) * W,
                                (ss.startY + ss.dy * ss.length * tailU) * H
                            )
                            val a = ss.alpha * (1f - abs(u - 0.45f) * 2.2f).coerceIn(0f, 1f)
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color.Transparent, Color.White.copy(alpha = a)),
                                    start = tail, end = head
                                ),
                                start = tail, end = head,
                                strokeWidth = 1.1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Soft solar glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0f to amber.copy(alpha = 0.12f * pulse * wrapFade),
                            0.4f to amber.copy(alpha = 0.04f * wrapFade),
                            1f to Color.Transparent
                        ),
                        center = sunPos,
                        radius = W * 0.30f
                    ),
                    radius = W * 0.30f,
                    center = sunPos,
                    blendMode = BlendMode.Plus
                )

                planets.forEach { spec ->
                    drawEllipseOrbit(sunPos, spec, W, halfH, fade = if (boost) 0.55f else 1f)
                }

                val states = planets.map { spec ->
                    spec to orbitalPoint(spec, now, sx, sy, halfH, W)
                }
                val (backPlanets, frontPlanets) = orderByDepth(states)

                fun drawPlanet(spec: PlanetSpec, pos: Triple<Float, Float, Float>) {
                    val (hx, hy, hz) = pos
                    val distToSun = hypot(hx - sx, hy - sy)
                    val occulted = hz < 0f && distToSun < sunDiskR * 0.95f
                    val transiting = hz >= 0f && distToSun < sunDiskR * 1.08f

                    if (!occulted && !boost) {
                        val stepMs = (arcRadians / spec.angularVel.coerceAtLeast(1e-9)) / arcSteps
                        drawOrbitArc(
                            spec = spec,
                            now = now,
                            sx = sx,
                            sy = sy,
                            halfH = halfH,
                            canvasW = W,
                            steps = arcSteps,
                            stepMs = stepMs,
                            depthZ = hz,
                            fade = 1f
                        )
                    }
                    if (occulted) return

                    drawLitPlanet(
                        center = Offset(hx, hy),
                        sunPos = sunPos,
                        color = spec.color,
                        scale = depthScale(hz) * spec.bodyScale,
                        alpha = depthAlpha(hz) * if (transiting) 0.80f else 1f,
                        transit = transiting,
                        hasRing = spec.hasRing
                    )
                }

                backPlanets.forEach { (spec, pos) -> drawPlanet(spec, pos) }
                drawSun(sunPos, amber, sunHot, sunCore, sunPlasma, sunLimb, pulse, now, sunDiskR, wrapFade)
                frontPlanets.forEach { (spec, pos) -> drawPlanet(spec, pos) }

                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.50f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.58f)
                        )
                    )
                )
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
                    Crossfade(
                        targetState = captionIndex % captions.size,
                        animationSpec = tween(durationMillis = 450),
                        label = "solar-caption"
                    ) { index ->
                        Text(
                            text = captions[index],
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
}

private fun DrawScope.drawEllipseOrbit(
    sunPos: Offset,
    spec: PlanetSpec,
    canvasW: Float,
    halfH: Float,
    fade: Float
) {
    val a = spec.orbitA * canvasW
    val b = spec.orbitB * halfH
    val path = Path()
    val samples = 72
    for (i in 0..samples) {
        val th = (i.toDouble() / samples) * 2.0 * PI
        val cosT = cos(th).toFloat()
        val sinT = sin(th).toFloat()
        val foreshorten = 0.86f + 0.14f * ((sinT + 1f) / 2f)
        val x = sunPos.x + a * cosT * foreshorten
        val y = sunPos.y + b * sinT
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    drawPath(
        path = path,
        color = spec.color.copy(alpha = 0.055f * fade),
        style = Stroke(width = 0.75.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    val front = Path()
    var started = false
    for (i in 0..samples) {
        val th = (i.toDouble() / samples) * 2.0 * PI
        val sinT = sin(th).toFloat()
        if (sinT < 0f) {
            started = false
            continue
        }
        val cosT = cos(th).toFloat()
        val foreshorten = 0.86f + 0.14f * ((sinT + 1f) / 2f)
        val x = sunPos.x + a * cosT * foreshorten
        val y = sunPos.y + b * sinT
        if (!started) {
            front.moveTo(x, y)
            started = true
        } else {
            front.lineTo(x, y)
        }
    }
    drawPath(
        path = front,
        color = spec.color.copy(alpha = 0.13f * fade),
        style = Stroke(width = 1.0.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawOrbitArc(
    spec: PlanetSpec,
    now: Double,
    sx: Float,
    sy: Float,
    halfH: Float,
    canvasW: Float,
    steps: Int,
    stepMs: Double,
    depthZ: Float,
    fade: Float
) {
    val pts = ArrayList<Offset>(steps + 1)
    for (step in steps downTo 1) {
        val past = now - step * stepMs
        val psx = sunX(past, canvasW)
        val psy = sunY(past, halfH * 2f)
        val (qx, qy, _) = orbitalPoint(spec, past, psx, psy, halfH, canvasW)
        pts.add(Offset(qx, qy))
    }
    val (hx, hy, _) = orbitalPoint(spec, now, sx, sy, halfH, canvasW)
    pts.add(Offset(hx, hy))

    val baseW = 2.0.dp.toPx() * depthScale(depthZ) * spec.bodyScale.coerceAtMost(1.2f)
    for (i in 0 until pts.size - 1) {
        val frac = (i + 1).toFloat() / pts.size
        val a = frac * 0.38f * depthAlpha(depthZ) * fade
        drawLine(
            color = spec.color.copy(alpha = a),
            start = pts[i],
            end = pts[i + 1],
            strokeWidth = (baseW * frac).coerceAtLeast(0.7.dp.toPx()),
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus
        )
    }
}

private fun DrawScope.drawSun(
    sunPos: Offset,
    amber: Color,
    sunHot: Color,
    sunCore: Color,
    sunPlasma: Color,
    sunLimb: Color,
    pulse: Float,
    now: Double,
    diskR: Float,
    fade: Float
) {
    val f = fade * pulse

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(amber.copy(alpha = 0.18f * f), Color.Transparent),
            center = sunPos,
            radius = diskR * 5.2f
        ),
        radius = diskR * 5.2f,
        center = sunPos,
        blendMode = BlendMode.Plus
    )

    // Equatorial coronal streamers
    val rayCount = 14
    for (i in 0 until rayCount) {
        val baseAng = (i * (2.0 * PI) / rayCount) + now * 0.00028
        val flutter = 0.50f + 0.50f * sin(now * 0.0025 + i * 1.4).toFloat()
        val equatorBias = 0.45f + 0.55f * abs(cos(baseAng)).toFloat()
        val len = diskR * (2.0f + 3.2f * flutter) * equatorBias
        val dx = cos(baseAng).toFloat()
        val dy = sin(baseAng).toFloat() * 0.68f
        val tip = Offset(sunPos.x + dx * len, sunPos.y + dy * len)
        val root = Offset(sunPos.x + dx * diskR * 0.88f, sunPos.y + dy * diskR * 0.88f)
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(
                    sunPlasma.copy(alpha = 0.38f * flutter * f),
                    amber.copy(alpha = 0.12f * f),
                    Color.Transparent
                ),
                start = root,
                end = tip
            ),
            start = root,
            end = tip,
            strokeWidth = (1.6.dp.toPx() * flutter).coerceAtLeast(0.6.dp.toPx()),
            cap = StrokeCap.Round,
            blendMode = BlendMode.Plus
        )
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                sunHot.copy(alpha = 0.50f * f),
                sunPlasma.copy(alpha = 0.22f * f),
                Color.Transparent
            ),
            center = sunPos,
            radius = diskR * 1.9f
        ),
        radius = diskR * 1.9f,
        center = sunPos,
        blendMode = BlendMode.Plus
    )

    // Photosphere with stronger limb darkening
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to sunCore.copy(alpha = f),
                0.35f to sunHot.copy(alpha = f),
                0.72f to amber.copy(alpha = f),
                1.0f to sunLimb.copy(alpha = 0.92f * f)
            ),
            center = Offset(sunPos.x - diskR * 0.08f, sunPos.y - diskR * 0.10f),
            radius = diskR * 1.08f
        ),
        radius = diskR,
        center = sunPos
    )

    // Granulation / faculae
    for (i in 0 until 8) {
        val ang = i * 0.95 + now * 0.0007
        val r = diskR * (0.18f + 0.22f * ((i * 37) % 5) / 5f)
        val c = Offset(
            sunPos.x + cos(ang).toFloat() * r * 0.55f,
            sunPos.y + sin(ang).toFloat() * r * 0.50f
        )
        drawCircle(
            color = sunCore.copy(alpha = 0.12f * f),
            radius = diskR * (0.12f + 0.04f * (i % 3)),
            center = c,
            blendMode = BlendMode.Plus
        )
    }

    // Soft sunspot pair
    val spotPhase = now * 0.00055
    for (i in 0 until 2) {
        val ang = spotPhase + i * 2.2
        val c = Offset(
            sunPos.x + cos(ang).toFloat() * diskR * 0.35f,
            sunPos.y + sin(ang).toFloat() * diskR * 0.28f
        )
        drawCircle(
            color = Color(0xFF5D4037).copy(alpha = 0.28f * f),
            radius = diskR * 0.11f,
            center = c
        )
        drawCircle(
            color = Color(0xFF3E2723).copy(alpha = 0.35f * f),
            radius = diskR * 0.055f,
            center = c
        )
    }

    drawCircle(
        color = Color.White.copy(alpha = 0.70f * f),
        radius = diskR * 0.24f,
        center = Offset(sunPos.x - diskR * 0.12f, sunPos.y - diskR * 0.10f),
        blendMode = BlendMode.Plus
    )
}

private fun DrawScope.drawLitPlanet(
    center: Offset,
    sunPos: Offset,
    color: Color,
    scale: Float,
    alpha: Float,
    transit: Boolean,
    hasRing: Boolean
) {
    val bodyR = 3.2.dp.toPx() * scale
    val dx = sunPos.x - center.x
    val dy = sunPos.y - center.y
    val dist = hypot(dx, dy).coerceAtLeast(0.001f)
    val nx = dx / dist
    val ny = dy / dist

    if (hasRing) {
        val ringAngle = Math.toDegrees(atan2(ny.toDouble(), nx.toDouble())).toFloat()
        rotate(degrees = ringAngle * 0.15f, pivot = center) {
            drawOval(
                color = color.copy(alpha = 0.28f * alpha),
                topLeft = Offset(center.x - bodyR * 2.2f, center.y - bodyR * 0.45f),
                size = Size(bodyR * 4.4f, bodyR * 0.9f),
                style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
            )
            drawOval(
                color = Color.White.copy(alpha = 0.10f * alpha),
                topLeft = Offset(center.x - bodyR * 1.9f, center.y - bodyR * 0.32f),
                size = Size(bodyR * 3.8f, bodyR * 0.64f),
                style = Stroke(width = 0.7.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = 0.28f * alpha), Color.Transparent),
            center = center,
            radius = bodyR * 2.4f
        ),
        radius = bodyR * 2.4f,
        center = center,
        blendMode = BlendMode.Plus
    )

    if (transit) {
        drawCircle(
            color = Color.Black.copy(alpha = 0.60f * alpha),
            radius = bodyR,
            center = center
        )
    }

    // Night side base
    drawCircle(
        color = color.copy(alpha = 0.18f * alpha),
        radius = bodyR,
        center = center
    )

    // Day hemisphere toward the sun
    val litCenter = Offset(center.x + nx * bodyR * 0.42f, center.y + ny * bodyR * 0.42f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.48f * alpha),
                color.copy(alpha = 0.96f * alpha),
                color.copy(alpha = 0.08f * alpha)
            ),
            center = litCenter,
            radius = bodyR * 1.55f
        ),
        radius = bodyR,
        center = center
    )

    // Terminator
    val darkCenter = Offset(center.x - nx * bodyR * 0.58f, center.y - ny * bodyR * 0.58f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.Black.copy(alpha = 0.50f * alpha), Color.Transparent),
            center = darkCenter,
            radius = bodyR * 1.15f
        ),
        radius = bodyR * 1.15f,
        center = darkCenter
    )

    // Sunward rim
    val rim = Offset(center.x + nx * bodyR * 0.70f, center.y + ny * bodyR * 0.70f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.38f * alpha),
                color.copy(alpha = 0.16f * alpha),
                Color.Transparent
            ),
            center = rim,
            radius = bodyR * 0.70f
        ),
        radius = bodyR * 0.70f,
        center = rim,
        blendMode = BlendMode.Plus
    )

    drawCircle(
        color = Color.White.copy(alpha = 0.60f * alpha),
        radius = bodyR * 0.22f,
        center = Offset(center.x + nx * bodyR * 0.30f, center.y + ny * bodyR * 0.30f),
        blendMode = BlendMode.Plus
    )
}

