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
import com.anant.fitbuddy.ui.loading.LoadingAnimation
import com.anant.fitbuddy.ui.loading.LoadingAnimationScope
import com.anant.fitbuddy.ui.loading.LoadingAnimationSlot
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Dashboard analyzing banner: orchard sky with a rustling apple tree on the right. */
object AppleTreeLoadingAnimation : LoadingAnimation {
    override val id: String = "apple_tree"
    override val displayName: String = "Apple tree"
    override val slots: Set<LoadingAnimationSlot> = setOf(LoadingAnimationSlot.ANALYZING)
    override val defaultCaptions: List<String> = analyzingCaptions

    @Composable
    override fun Content(scope: LoadingAnimationScope) {
        AppleTreeBanner(
            modelId = scope.label,
            captions = scope.captions.ifEmpty { defaultCaptions },
            modifier = scope.modifier
        )
    }
}

private data class CanopyLeaf(
    val ax: Float,
    val ay: Float,
    val size: Float,
    val phase: Float,
    val freq: Float,
    val swayAmp: Float,
    val color: Color,
    val tipSkew: Float
)

private data class FallingLeaf(
    val startX: Float,
    val startY: Float,
    val drift: Float,
    val fallDurationMs: Double,
    val startAtMs: Double,
    val spin: Float,
    val size: Float,
    val phase: Float,
    val color: Color
)

/**
 * Fixed set of apples on the tree. Each hangs until [dropAtMs], then falls once
 * straight down under gravity and fades out near the ground — never respawns.
 */
private data class SceneApple(
    val hangAx: Float,
    val hangAy: Float,
    val scale: Float,
    val swayPhase: Float,
    val dropAtMs: Double,
    val fallDurationMs: Double
)

@Composable
private fun AppleTreeBanner(
    modelId: String?,
    captions: List<String>,
    modifier: Modifier = Modifier
) {
    var scaledTime by remember { mutableStateOf(0.0) }
    var lastFrame by remember { mutableLongStateOf(0L) }
    var windMultiplier by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                val delta = if (lastFrame == 0L) 0L else (now - lastFrame)
                lastFrame = now
                scaledTime = accumulateScaledTime(scaledTime, delta, windMultiplier)
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

    val skyTop = Color(0xFFA8D4EA)
    val skyMid = Color(0xFFCDE8D8)
    val skyBottom = Color(0xFFE8F2C8)
    val ground = Color(0xFF7EAD5C)
    val groundDark = Color(0xFF5E8A42)
    val trunk = Color(0xFF6A3F22)
    val trunkMid = Color(0xFF8B5A2B)
    val trunkDark = Color(0xFF3E2410)
    val foliageDeep = Color(0xFF1B5E20)
    val foliageMid = Color(0xFF2E7D32)
    val foliageLit = Color(0xFF4CAF50)
    val foliageBright = Color(0xFF66BB6A)
    val leafColors = remember {
        listOf(
            Color(0xFF2E7D32),
            Color(0xFF388E3C),
            Color(0xFF43A047),
            Color(0xFF4CAF50),
            Color(0xFF558B2F),
            Color(0xFF689F38),
            Color(0xFF1B5E20)
        )
    }
    val appleRed = Color(0xFFD32F2F)
    val appleLit = Color(0xFFE57373)
    val appleShadow = Color(0xFF8E1A1A)
    val appleCheek = Color(0xFFFF8A80)

    val canopyLeaves = remember {
        val rng = kotlin.random.Random(0xA91E_2026)
        List(56) {
            CanopyLeaf(
                ax = -0.48f + rng.nextFloat() * 0.96f,
                ay = -0.58f + rng.nextFloat() * 0.92f,
                size = 0.048f + rng.nextFloat() * 0.070f,
                phase = rng.nextFloat() * (2f * PI.toFloat()),
                freq = 0.0016f + rng.nextFloat() * 0.0018f,
                swayAmp = 0.010f + rng.nextFloat() * 0.022f,
                color = leafColors[rng.nextInt(leafColors.size)],
                tipSkew = -0.40f + rng.nextFloat() * 0.80f
            )
        }
    }

    // Leaves may keep drifting; apples are a fixed cast (see [apples]).
    val fallingLeaves = remember {
        val rng = kotlin.random.Random(0x1EAF_FA11)
        List(10) { i ->
            FallingLeaf(
                startX = 0.58f + rng.nextFloat() * 0.34f,
                startY = 0.10f + rng.nextFloat() * 0.32f,
                drift = 0.05f + rng.nextFloat() * 0.10f,
                fallDurationMs = 3800.0 + rng.nextDouble() * 3200.0,
                startAtMs = 600.0 + i * 900.0 + rng.nextDouble() * 1400.0,
                spin = 90f + rng.nextFloat() * 180f,
                size = 0.026f + rng.nextFloat() * 0.020f,
                phase = rng.nextFloat() * (2f * PI.toFloat()),
                color = leafColors[rng.nextInt(leafColors.size)]
            )
        }
    }

    // Fixed apples: hang, drop once at staggered times, fade out off-screen — never respawn.
    val apples = remember {
        listOf(
            SceneApple(hangAx = -0.22f, hangAy = -0.12f, scale = 0.118f, swayPhase = 0.4f, dropAtMs = 2_800.0, fallDurationMs = 1_550.0),
            SceneApple(hangAx = 0.10f, hangAy = -0.22f, scale = 0.126f, swayPhase = 1.2f, dropAtMs = 5_400.0, fallDurationMs = 1_600.0),
            SceneApple(hangAx = 0.28f, hangAy = 0.02f, scale = 0.112f, swayPhase = 2.1f, dropAtMs = 8_200.0, fallDurationMs = 1_500.0),
            SceneApple(hangAx = -0.06f, hangAy = 0.18f, scale = 0.120f, swayPhase = 2.9f, dropAtMs = 11_500.0, fallDurationMs = 1_650.0),
            SceneApple(hangAx = 0.20f, hangAy = 0.22f, scale = 0.108f, swayPhase = 3.6f, dropAtMs = 15_000.0, fallDurationMs = 1_580.0),
            SceneApple(hangAx = -0.30f, hangAy = 0.14f, scale = 0.114f, swayPhase = 4.4f, dropAtMs = 18_800.0, fallDurationMs = 1_620.0),
            SceneApple(hangAx = 0.02f, hangAy = -0.02f, scale = 0.122f, swayPhase = 5.1f, dropAtMs = Double.POSITIVE_INFINITY, fallDurationMs = 1_600.0)
        )
    }

    val firstLine = analyzingLabel(modelId)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = skyMid)
    ) {
        Box {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                windMultiplier = 2.2f
                                try {
                                    tryAwaitRelease()
                                } finally {
                                    windMultiplier = 1f
                                }
                            }
                        )
                    }
            ) {
                val W = size.width
                val H = size.height
                val t = scaledTime
                val wind = naturalWind(t, windMultiplier)

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(skyTop, skyMid, skyBottom)
                    )
                )

                drawHill(W * 0.20f, H * 0.80f, W * 0.58f, H * 0.26f, ground.copy(alpha = 0.32f))
                drawHill(W * 0.58f, H * 0.84f, W * 0.72f, H * 0.28f, groundDark.copy(alpha = 0.26f))

                val groundY = H * 0.86f
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(ground.copy(alpha = 0.60f), groundDark.copy(alpha = 0.90f))
                    ),
                    topLeft = Offset(0f, groundY),
                    size = Size(W, H - groundY)
                )

                val treeBase = Offset(W * 0.83f, H * 0.92f)
                val canopyCenter = Offset(W * 0.805f, H * 0.36f)
                val canopyR = H * 0.44f

                drawAppleTree(
                    base = treeBase,
                    canopyCenter = canopyCenter,
                    canopyR = canopyR,
                    wind = wind,
                    t = t,
                    trunk = trunk,
                    trunkMid = trunkMid,
                    trunkDark = trunkDark,
                    foliageDeep = foliageDeep,
                    foliageMid = foliageMid,
                    foliageLit = foliageLit,
                    foliageBright = foliageBright
                )

                canopyLeaves.forEach { leaf ->
                    // Layered frequencies → irregular, breezy rustle instead of a metronome sway.
                    val local = naturalLeafSway(t, leaf.phase, leaf.freq, wind)
                    val sway = local * leaf.swayAmp * canopyR
                    val bob = cos(t * leaf.freq * 0.72 + leaf.phase * 1.7).toFloat() *
                        leaf.swayAmp * canopyR * 0.35f * wind
                    val lx = canopyCenter.x + leaf.ax * canopyR + sway
                    val ly = canopyCenter.y + leaf.ay * canopyR + bob
                    val angle = leaf.tipSkew * 28f + local * 22f
                    drawLeaf(
                        center = Offset(lx, ly),
                        length = leaf.size * canopyR * 2.1f,
                        width = leaf.size * canopyR * 1.05f,
                        angleDeg = angle,
                        color = leaf.color
                    )
                }

                // Apples drawn after foliage so they sit in front of leaves.
                apples.forEach { apple ->
                    val hangX = canopyCenter.x + apple.hangAx * canopyR
                    val hangY = canopyCenter.y + apple.hangAy * canopyR
                    val sizePx = apple.scale * canopyR
                    // Fall past the grass line so they exit the banner.
                    val exitY = H + sizePx * 1.2f

                    val pose = applePose(
                        t = t,
                        hangX = hangX,
                        hangY = hangY,
                        exitY = exitY,
                        apple = apple,
                        wind = wind
                    ) ?: return@forEach
                    val (cx, cy, rot, alpha) = pose
                    drawAppleShape(
                        center = Offset(cx, cy),
                        size = sizePx,
                        red = appleRed.copy(alpha = alpha),
                        lit = appleLit.copy(alpha = alpha),
                        shadow = appleShadow.copy(alpha = alpha),
                        cheek = appleCheek.copy(alpha = alpha),
                        stem = trunkDark.copy(alpha = alpha),
                        rotationDeg = rot
                    )
                }

                // Falling leaves keep looping lightly; apples never respawn.
                fallingLeaves.forEach { leaf ->
                    val elapsed = t - leaf.startAtMs
                    if (elapsed < 0) return@forEach
                    // Recycle leaves only (light debris), not apples.
                    val cycle = (elapsed % (leaf.fallDurationMs + 2_200.0))
                    if (cycle > leaf.fallDurationMs) return@forEach
                    val u = (cycle / leaf.fallDurationMs).toFloat().coerceIn(0f, 1f)
                    val flutter = sin(u * PI.toFloat() * 3.2f + leaf.phase)
                    val x = (leaf.startX + leaf.drift * flutter * (0.55f + 0.45f * wind)) * W
                    val y = (leaf.startY + u * u * (0.95f - leaf.startY)) * H
                    val alpha = when {
                        u < 0.06f -> u / 0.06f
                        u > 0.82f -> (1f - u) / 0.18f
                        else -> 1f
                    }.coerceIn(0f, 1f)
                    val len = leaf.size * H * 2.3f
                    drawLeaf(
                        center = Offset(x, y),
                        length = len,
                        width = len * 0.52f,
                        angleDeg = u * leaf.spin + flutter * 30f,
                        color = leaf.color.copy(alpha = alpha * 0.90f)
                    )
                }

                drawRect(
                    brush = Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0f to Color.White.copy(alpha = 0.22f),
                            0.42f to Color.Transparent,
                            1f to Color.Transparent
                        )
                    )
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1f to Color(0xFF1B3A1F).copy(alpha = 0.28f)
                        )
                    )
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 96.dp, bottom = 8.dp)
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
                    color = Color(0xFF1B3A1F),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (captions.isNotEmpty()) {
                    Crossfade(
                        targetState = captionIndex % captions.size,
                        animationSpec = tween(durationMillis = 450),
                        label = "apple-tree-caption"
                    ) { index ->
                        Text(
                            text = captions[index],
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color(0xFF1B3A1F).copy(alpha = 0.58f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/** Soft multi-frequency breeze; hold boosts amplitude without speeding the clock feel. */
private fun naturalWind(tMs: Double, boost: Float): Float {
    val slow = sin(tMs * 0.00055).toFloat()
    val mid = sin(tMs * 0.00135 + 1.1).toFloat()
    val gust = sin(tMs * 0.00028 + 0.4).toFloat().coerceAtLeast(0f)
    val base = 0.55f + 0.28f * slow + 0.12f * mid + 0.18f * gust * gust
    return (base * (0.75f + 0.25f * boost)).coerceIn(0.35f, 1.85f)
}

private fun naturalLeafSway(tMs: Double, phase: Float, freq: Float, wind: Float): Float {
    val a = sin(tMs * freq + phase).toFloat()
    val b = sin(tMs * freq * 1.73 + phase * 2.1).toFloat() * 0.45f
    val c = sin(tMs * freq * 0.41 + phase * 0.7).toFloat() * 0.30f
    return (a + b + c) * wind
}

/**
 * Returns (x, y, rotationDeg, alpha), or null once the apple has fallen off-screen.
 * Apples drop on a vertical line (no lateral drift) and disappear past the banner.
 */
private data class ApplePose(val x: Float, val y: Float, val rotationDeg: Float, val alpha: Float)

private fun applePose(
    t: Double,
    hangX: Float,
    hangY: Float,
    exitY: Float,
    apple: SceneApple,
    wind: Float
): ApplePose? {
    val hangSway = sin(t * 0.0011 + apple.swayPhase).toFloat() * 2.2f * wind
    if (t < apple.dropAtMs) {
        // Branch sway is tiny — heavy fruit barely swings.
        return ApplePose(
            x = hangX + hangSway * 0.35f,
            y = hangY,
            rotationDeg = hangSway * 0.15f,
            alpha = 1f
        )
    }
    val fallU = ((t - apple.dropAtMs) / apple.fallDurationMs).toFloat()
    if (fallU >= 1f) return null // gone — never respawns
    // Gravity: ease-in quadratic, straight down past the ground.
    val eased = fallU * fallU
    val y = hangY + (exitY - hangY) * eased
    val rot = fallU * 12f
    // Fade as it leaves the frame so it doesn't hard-cut.
    val alpha = when {
        fallU > 0.72f -> ((1f - fallU) / 0.28f).coerceIn(0f, 1f)
        else -> 1f
    }
    return ApplePose(hangX, y, rot, alpha)
}

private fun DrawScope.drawHill(
    cx: Float,
    cy: Float,
    width: Float,
    height: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(cx - width / 2f, cy)
        quadraticTo(cx, cy - height, cx + width / 2f, cy)
        lineTo(cx + width / 2f, size.height)
        lineTo(cx - width / 2f, size.height)
        close()
    }
    drawPath(path, color = color)
}

private fun DrawScope.drawAppleTree(
    base: Offset,
    canopyCenter: Offset,
    canopyR: Float,
    wind: Float,
    t: Double,
    trunk: Color,
    trunkMid: Color,
    trunkDark: Color,
    foliageDeep: Color,
    foliageMid: Color,
    foliageLit: Color,
    foliageBright: Color
) {
    val sway = sin(t * 0.0009).toFloat() * canopyR * 0.018f * wind
    val tip = Offset(canopyCenter.x + sway * 0.4f, canopyCenter.y + canopyR * 0.22f)

    // Flared root / trunk
    val trunkW = canopyR * 0.16f
    val trunkPath = Path().apply {
        moveTo(base.x - trunkW * 0.95f, base.y)
        quadraticTo(
            base.x - trunkW * 0.55f,
            (base.y + tip.y) * 0.55f,
            tip.x - trunkW * 0.38f,
            tip.y
        )
        lineTo(tip.x + trunkW * 0.38f, tip.y)
        quadraticTo(
            base.x + trunkW * 0.50f,
            (base.y + tip.y) * 0.55f,
            base.x + trunkW * 1.05f,
            base.y
        )
        close()
    }
    drawPath(
        path = trunkPath,
        brush = Brush.horizontalGradient(
            colors = listOf(trunkDark, trunkMid, trunk, trunkDark.copy(alpha = 0.85f)),
            startX = base.x - trunkW,
            endX = base.x + trunkW
        )
    )
    // Bark groove
    drawPath(
        path = Path().apply {
            moveTo(base.x - trunkW * 0.12f, base.y - canopyR * 0.04f)
            quadraticTo(
                base.x + trunkW * 0.05f,
                (base.y + tip.y) * 0.5f,
                tip.x - trunkW * 0.05f,
                tip.y + canopyR * 0.02f
            )
        },
        color = trunkDark.copy(alpha = 0.55f),
        style = Stroke(width = trunkW * 0.18f, cap = StrokeCap.Round)
    )

    // Branching scaffold
    fun branch(from: Offset, to: Offset, width: Float, color: Color) {
        drawLine(color, from, to, strokeWidth = width, cap = StrokeCap.Round)
    }
    val b0 = Offset(tip.x, tip.y - canopyR * 0.02f)
    val left = Offset(canopyCenter.x - canopyR * 0.42f + sway * 0.6f, canopyCenter.y + canopyR * 0.05f)
    val right = Offset(canopyCenter.x + canopyR * 0.46f + sway, canopyCenter.y - canopyR * 0.02f)
    val up = Offset(canopyCenter.x + sway * 0.5f, canopyCenter.y - canopyR * 0.42f)
    val leftLow = Offset(canopyCenter.x - canopyR * 0.28f + sway * 0.4f, canopyCenter.y + canopyR * 0.32f)
    val rightLow = Offset(canopyCenter.x + canopyR * 0.30f + sway * 0.8f, canopyCenter.y + canopyR * 0.28f)
    branch(b0, left, trunkW * 0.42f, trunk)
    branch(b0, right, trunkW * 0.40f, trunk)
    branch(b0, up, trunkW * 0.34f, trunkMid)
    branch(Offset((b0.x + left.x) / 2f, (b0.y + left.y) / 2f), leftLow, trunkW * 0.28f, trunk)
    branch(Offset((b0.x + right.x) / 2f, (b0.y + right.y) / 2f), rightLow, trunkW * 0.26f, trunkMid)

    // Irregular canopy clusters (organic silhouette, not one ball)
    val clusters = listOf(
        Triple(Offset(canopyCenter.x + sway * 0.3f, canopyCenter.y - canopyR * 0.08f), 1.05f, foliageMid),
        Triple(Offset(canopyCenter.x - canopyR * 0.36f + sway * 0.5f, canopyCenter.y + canopyR * 0.02f), 0.78f, foliageDeep),
        Triple(Offset(canopyCenter.x + canopyR * 0.40f + sway, canopyCenter.y - canopyR * 0.04f), 0.74f, foliageLit),
        Triple(Offset(canopyCenter.x + sway * 0.2f, canopyCenter.y - canopyR * 0.40f), 0.70f, foliageBright),
        Triple(Offset(canopyCenter.x - canopyR * 0.14f + sway * 0.4f, canopyCenter.y + canopyR * 0.30f), 0.66f, foliageMid),
        Triple(Offset(canopyCenter.x + canopyR * 0.18f + sway * 0.7f, canopyCenter.y + canopyR * 0.26f), 0.62f, foliageLit),
        Triple(Offset(canopyCenter.x + canopyR * 0.08f + sway * 0.5f, canopyCenter.y - canopyR * 0.22f), 0.58f, foliageBright)
    )
    clusters.forEach { (c, scale, color) ->
        val rx = canopyR * scale * 0.95f
        val ry = canopyR * scale * 0.82f
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.95f),
                    color.copy(alpha = 0.88f),
                    foliageDeep.copy(alpha = 0.55f)
                ),
                center = Offset(c.x - rx * 0.18f, c.y - ry * 0.22f),
                radius = min(rx, ry) * 1.15f
            ),
            topLeft = Offset(c.x - rx, c.y - ry),
            size = Size(rx * 2f, ry * 2f)
        )
    }
}

private fun DrawScope.drawLeaf(
    center: Offset,
    length: Float,
    width: Float,
    angleDeg: Float,
    color: Color
) {
    rotate(degrees = angleDeg, pivot = center) {
        val path = Path().apply {
            moveTo(center.x, center.y - length * 0.55f)
            quadraticTo(center.x + width, center.y, center.x, center.y + length * 0.55f)
            quadraticTo(center.x - width, center.y, center.x, center.y - length * 0.55f)
            close()
        }
        drawPath(path, color = color)
        drawLine(
            color = Color(0xFF1B5E20).copy(alpha = 0.35f * color.alpha),
            start = Offset(center.x, center.y - length * 0.45f),
            end = Offset(center.x, center.y + length * 0.40f),
            strokeWidth = (length * 0.08f).coerceAtLeast(0.6f),
            cap = StrokeCap.Round
        )
    }
}

/** Classic apple silhouette: dimpled crown, two upper lobes, tapered body. */
private fun DrawScope.drawAppleShape(
    center: Offset,
    size: Float,
    red: Color,
    lit: Color,
    shadow: Color,
    cheek: Color,
    stem: Color,
    rotationDeg: Float
) {
    val s = size
    rotate(degrees = rotationDeg, pivot = center) {
        val path = applePath(center, s)
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(lit, red, shadow),
                center = Offset(center.x - s * 0.22f, center.y - s * 0.28f),
                radius = s * 1.55f
            )
        )
        // Vertical cleft shadow
        drawPath(
            path = Path().apply {
                moveTo(center.x, center.y - s * 0.42f)
                quadraticTo(
                    center.x + s * 0.02f,
                    center.y - s * 0.05f,
                    center.x,
                    center.y + s * 0.18f
                )
            },
            color = shadow.copy(alpha = 0.28f * red.alpha),
            style = Stroke(width = s * 0.08f, cap = StrokeCap.Round)
        )
        // Cheek highlight
        drawOval(
            color = cheek.copy(alpha = 0.55f * red.alpha),
            topLeft = Offset(center.x - s * 0.42f, center.y - s * 0.32f),
            size = Size(s * 0.28f, s * 0.22f)
        )
        // Specular glint
        drawOval(
            color = Color.White.copy(alpha = 0.45f * red.alpha),
            topLeft = Offset(center.x - s * 0.30f, center.y - s * 0.28f),
            size = Size(s * 0.14f, s * 0.10f)
        )
        // Stem
        drawLine(
            color = stem,
            start = Offset(center.x + s * 0.02f, center.y - s * 0.52f),
            end = Offset(center.x + s * 0.10f, center.y - s * 0.78f),
            strokeWidth = s * 0.10f,
            cap = StrokeCap.Round
        )
        // Stem leaf
        val leaf = Path().apply {
            moveTo(center.x + s * 0.08f, center.y - s * 0.70f)
            quadraticTo(
                center.x + s * 0.38f,
                center.y - s * 0.88f,
                center.x + s * 0.34f,
                center.y - s * 0.58f
            )
            quadraticTo(
                center.x + s * 0.16f,
                center.y - s * 0.62f,
                center.x + s * 0.08f,
                center.y - s * 0.70f
            )
            close()
        }
        drawPath(leaf, color = Color(0xFF43A047).copy(alpha = red.alpha))
        drawPath(
            path = path,
            color = shadow.copy(alpha = 0.18f * red.alpha),
            style = Stroke(width = s * 0.04f, join = StrokeJoin.Round)
        )
    }
}

private fun applePath(center: Offset, s: Float): Path {
    val cx = center.x
    val cy = center.y
    return Path().apply {
        // Start at the dimple between the two crown lobes.
        moveTo(cx, cy - s * 0.48f)
        // Right crown lobe → right cheek → bottom → left cheek → left crown lobe.
        cubicTo(
            cx + s * 0.22f, cy - s * 0.62f,
            cx + s * 0.62f, cy - s * 0.38f,
            cx + s * 0.58f, cy - s * 0.02f
        )
        cubicTo(
            cx + s * 0.56f, cy + s * 0.38f,
            cx + s * 0.28f, cy + s * 0.62f,
            cx, cy + s * 0.58f
        )
        cubicTo(
            cx - s * 0.28f, cy + s * 0.62f,
            cx - s * 0.56f, cy + s * 0.38f,
            cx - s * 0.58f, cy - s * 0.02f
        )
        cubicTo(
            cx - s * 0.62f, cy - s * 0.38f,
            cx - s * 0.22f, cy - s * 0.62f,
            cx, cy - s * 0.48f
        )
        close()
    }
}
