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
import com.anant.fitbuddy.ui.loading.LoadingAnimation
import com.anant.fitbuddy.ui.loading.LoadingAnimationScope
import com.anant.fitbuddy.ui.loading.LoadingAnimationSlot
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Dashboard analyzing banner: vertical DNA helix on the right. */
object DnaHelixLoadingAnimation : LoadingAnimation {
    override val id: String = "dna_helix"
    override val displayName: String = "DNA helix"
    override val slots: Set<LoadingAnimationSlot> = setOf(LoadingAnimationSlot.ANALYZING)
    override val defaultCaptions: List<String> = analyzingCaptions

    @Composable
    override fun Content(scope: LoadingAnimationScope) {
        DnaHelixBanner(
            modelId = scope.label,
            captions = scope.captions.ifEmpty { defaultCaptions },
            vertical = true,
            modifier = scope.modifier
        )
    }
}

/** Dashboard analyzing banner: full-width horizontal DNA helix. */
object DnaHelixHorizontalLoadingAnimation : LoadingAnimation {
    override val id: String = "dna_helix_horizontal"
    override val displayName: String = "DNA helix horizontal"
    override val slots: Set<LoadingAnimationSlot> = setOf(LoadingAnimationSlot.ANALYZING)
    override val defaultCaptions: List<String> = analyzingCaptions

    @Composable
    override fun Content(scope: LoadingAnimationScope) {
        DnaHelixBanner(
            modelId = scope.label,
            captions = scope.captions.ifEmpty { defaultCaptions },
            vertical = false,
            modifier = scope.modifier
        )
    }
}

private val DnaBgDeep = Color(0xFF030812)
private val DnaBgMid = Color(0xFF071422)
private val DnaStrandA = Color(0xFF5CE1E6)
private val DnaStrandB = Color(0xFFFF6BCB)
private val DnaPairWarm = Color(0xFFFFD166)
private val DnaPairCool = Color(0xFF7CFFB2)
private val DnaGlow = Color(0xFF3D8BFF)

@Composable
private fun DnaHelixBanner(
    modelId: String?,
    captions: List<String>,
    vertical: Boolean,
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

    data class Spark(
        val x: Float,
        val y: Float,
        val speed: Float,
        val phase: Float,
        val size: Float,
        val tint: Color
    )
    val sparks = remember {
        val rng = kotlin.random.Random(0xD4A_7E71)
        val tints = listOf(DnaStrandA, DnaStrandB, DnaPairCool, DnaGlow, Color.White)
        List(48) {
            Spark(
                x = rng.nextFloat(),
                y = rng.nextFloat(),
                speed = 0.00008f + rng.nextFloat() * 0.00022f,
                phase = rng.nextFloat() * (2f * PI.toFloat()),
                size = 0.6f + rng.nextFloat() * 1.6f,
                tint = tints[rng.nextInt(tints.size)]
            )
        }
    }

    val firstLine = analyzingLabel(modelId)
    val textEndPad = if (vertical) 72.dp else 12.dp

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DnaBgDeep)
    ) {
        Box {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(82.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
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
                val now = scaledTime
                val boost = isBoosting
                val twist = now * 0.0024
                val travel = (now * 0.00011).toFloat()

                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(DnaBgMid, DnaBgDeep, Color(0xFF01040A))
                    )
                )

                val glowCenter = if (vertical) {
                    Offset(W - H * 0.28f - 16.dp.toPx(), H * 0.48f)
                } else {
                    Offset(W * 0.55f, H * 0.48f)
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            DnaGlow.copy(alpha = if (boost) 0.22f else 0.12f),
                            DnaStrandA.copy(alpha = 0.05f),
                            Color.Transparent
                        ),
                        center = glowCenter,
                        radius = W * 0.42f
                    ),
                    radius = W * 0.42f,
                    center = glowCenter,
                    blendMode = BlendMode.Plus
                )

                sparks.forEach { s ->
                    val drift = ((s.x + travel * (0.4f + s.speed * 4000f)) % 1f + 1f) % 1f
                    val bob = 0.035f * sin(now * 0.0018 + s.phase).toFloat()
                    val twinkle = 0.35f + 0.65f * (0.5f + 0.5f * sin(now * 0.003 + s.phase).toFloat())
                    val a = (if (boost) 0.55f else 0.28f) * twinkle
                    drawCircle(
                        color = s.tint.copy(alpha = a),
                        radius = s.size * if (boost) 1.25f else 1f,
                        center = Offset(drift * W, (s.y + bob).coerceIn(0.05f, 0.95f) * H),
                        blendMode = BlendMode.Plus
                    )
                }

                drawDnaHelix(
                    width = W,
                    height = H,
                    twist = twist,
                    travel = travel,
                    boosting = boost,
                    vertical = vertical
                )

                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Transparent,
                            0.48f to Color.Transparent,
                            1f to Color.Black.copy(alpha = 0.62f)
                        )
                    )
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = textEndPad, bottom = 8.dp)
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
                        label = "dna-caption"
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

private fun DrawScope.drawDnaHelix(
    width: Float,
    height: Float,
    twist: Double,
    travel: Float,
    boosting: Boolean,
    vertical: Boolean
) {
    val samples = 96
    val turns = 2.35
    val amplitude = height * 0.28f
    val span = width * (0.96f - 0.08f)
    val strandWidth = (2.1.dp.toPx() * if (boosting) 1.15f else 1f).coerceAtLeast(1.4f)
    val rungEvery = 4

    fun drawAt(startX: Float, cy: Float) {
        data class Node(val x: Float, val y: Float, val z: Float, val t: Float)

        fun node(i: Int, phaseOffset: Double): Node {
            val t = i / samples.toFloat()
            val angle = 2.0 * PI * turns * t + twist + phaseOffset
            val z = cos(angle).toFloat()
            val foreshorten = 0.82f + 0.18f * ((z + 1f) * 0.5f)
            val x = startX + t * span
            val y = cy + amplitude * sin(angle).toFloat() * foreshorten
            return Node(x, y, z, t)
        }

        val strandA = List(samples + 1) { node(it, 0.0) }
        val strandB = List(samples + 1) { node(it, PI) }

        fun drawStrandBack(nodes: List<Node>, color: Color) {
            val path = Path()
            var started = false
            for (n in nodes) {
                if (n.z >= 0f) {
                    started = false
                    continue
                }
                if (!started) {
                    path.moveTo(n.x, n.y)
                    started = true
                } else {
                    path.lineTo(n.x, n.y)
                }
            }
            drawPath(
                path = path,
                color = color.copy(alpha = 0.28f),
                style = Stroke(
                    width = strandWidth * 0.85f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        fun drawStrandFront(nodes: List<Node>, color: Color) {
            val glow = Path()
            nodes.forEachIndexed { i, n ->
                if (i == 0) glow.moveTo(n.x, n.y) else glow.lineTo(n.x, n.y)
            }
            drawPath(
                path = glow,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        color.copy(alpha = 0.05f),
                        color.copy(alpha = if (boosting) 0.28f else 0.16f),
                        color.copy(alpha = 0.05f)
                    )
                ),
                style = Stroke(
                    width = strandWidth * 3.2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                ),
                blendMode = BlendMode.Plus
            )

            for (i in 0 until nodes.lastIndex) {
                val a = nodes[i]
                val b = nodes[i + 1]
                if (a.z < 0f && b.z < 0f) continue
                val depth = ((a.z + b.z) * 0.5f + 1f) * 0.5f
                val alpha = 0.35f + 0.65f * depth
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            color.copy(alpha = alpha * 0.85f),
                            Color.White.copy(alpha = alpha * 0.55f),
                            color.copy(alpha = alpha)
                        ),
                        start = Offset(a.x, a.y),
                        end = Offset(b.x, b.y)
                    ),
                    start = Offset(a.x, a.y),
                    end = Offset(b.x, b.y),
                    strokeWidth = strandWidth * (0.75f + 0.45f * depth),
                    cap = StrokeCap.Round,
                    blendMode = BlendMode.Plus
                )
            }
        }

        drawStrandBack(strandA, DnaStrandA)
        drawStrandBack(strandB, DnaStrandB)

        val pulseCenter = ((travel * 1.8f) % 1.2f) - 0.1f
        for (i in 0..samples step rungEvery) {
            val a = strandA[i]
            val b = strandB[i]
            if (abs(a.z) > 0.92f) continue
            val depth = ((a.z + b.z) * 0.5f + 1f) * 0.5f
            val pairColor = if ((i / rungEvery) % 2 == 0) DnaPairWarm else DnaPairCool
            val dist = abs(a.t - pulseCenter)
            val pulse = (1f - (dist / 0.14f).coerceIn(0f, 1f))
            val alpha = (0.18f + 0.55f * depth + 0.35f * pulse) * if (boosting) 1.15f else 1f

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        DnaStrandA.copy(alpha = alpha * 0.7f),
                        pairColor.copy(alpha = alpha),
                        DnaStrandB.copy(alpha = alpha * 0.7f)
                    ),
                    start = Offset(a.x, a.y),
                    end = Offset(b.x, b.y)
                ),
                start = Offset(a.x, a.y),
                end = Offset(b.x, b.y),
                strokeWidth = (1.1.dp.toPx() + pulse * 1.4.dp.toPx()),
                cap = StrokeCap.Round,
                blendMode = BlendMode.Plus
            )

            if (pulse > 0.15f) {
                val mx = (a.x + b.x) * 0.5f
                val my = (a.y + b.y) * 0.5f
                drawCircle(
                    color = pairColor.copy(alpha = 0.55f * pulse),
                    radius = 1.6.dp.toPx() + pulse * 1.8.dp.toPx(),
                    center = Offset(mx, my),
                    blendMode = BlendMode.Plus
                )
            }
        }

        drawStrandFront(strandA, DnaStrandA)
        drawStrandFront(strandB, DnaStrandB)

        for (i in 0..samples step 3) {
            listOf(strandA[i] to DnaStrandA, strandB[i] to DnaStrandB).forEach { (n, c) ->
                if (n.z < 0.15f) return@forEach
                val depth = (n.z + 1f) * 0.5f
                val r = (1.35.dp.toPx() + depth * 1.1.dp.toPx()) * if (boosting) 1.15f else 1f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.75f * depth),
                            c.copy(alpha = 0.85f * depth),
                            c.copy(alpha = 0.05f)
                        ),
                        center = Offset(n.x - r * 0.2f, n.y - r * 0.2f),
                        radius = r * 1.8f
                    ),
                    radius = r,
                    center = Offset(n.x, n.y),
                    blendMode = BlendMode.Plus
                )
            }
        }
    }

    if (vertical) {
        val pivot = Offset(
            x = width - amplitude - 16.dp.toPx(),
            y = height / 2f
        )
        rotate(degrees = -90f, pivot = pivot) {
            drawAt(startX = pivot.x - span / 2f, cy = pivot.y)
        }
    } else {
        drawAt(startX = width * 0.08f, cy = height * 0.42f)
    }
}
