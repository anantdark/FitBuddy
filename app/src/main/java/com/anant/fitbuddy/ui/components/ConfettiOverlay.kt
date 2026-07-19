package com.anant.fitbuddy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private enum class ConfettiShape { RECT, CIRCLE, RIBBON }

/** Origin corner for a blast piece (normalized screen coords). */
private enum class BlastCorner(
    val x: Float,
    val y: Float,
    /** Base aim angle into the screen (radians). */
    val aimAngle: Float
) {
    TOP_LEFT(0f, 0f, Math.PI.toFloat() / 4f),
    TOP_RIGHT(1f, 0f, Math.PI.toFloat() * 0.75f),
    BOTTOM_LEFT(0f, 1f, -Math.PI.toFloat() / 4f),
    BOTTOM_RIGHT(1f, 1f, -Math.PI.toFloat() * 0.75f)
}

private data class ConfettiPiece(
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val width: Float,
    val height: Float,
    val spin: Float,
    val shape: ConfettiShape,
    val drift: Float
)

private val ConfettiPalette = listOf(
    Color(0xFF66BB6A),
    Color(0xFF29B6F6),
    Color(0xFFFF7043),
    Color(0xFFFFCA28),
    Color(0xFFAB47BC),
    Color(0xFF26A69A),
    Color(0xFFEF5350),
    Color(0xFFFF80AB),
    Color(0xFFFFD54F),
    Color(0xFF80D8FF),
    Color(0xFFFF8A65),
    Color(0xFFE040FB),
    Color(0xFFFFFFFF)
)

/** Explosive ease-out: most travel in the first ~20%, then soft coast. */
private fun easeOutQuint(t: Float): Float {
    val u = 1f - t.coerceIn(0f, 1f)
    return 1f - u * u * u * u * u
}

/** Softer late ease for spin / residual drift. */
private fun easeOutCubic(t: Float): Float {
    val u = 1f - t.coerceIn(0f, 1f)
    return 1f - u * u * u
}

@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    durationMillis: Int = 3200,
    /** Bigger burst: corner cannons, more pieces, mixed shapes. */
    grand: Boolean = false
) {
    val particleCount = if (grand) 360 else 120
    val pieces = remember(grand) {
        if (grand) createCornerBlast(particleCount) else createTopRain(particleCount)
    }
    val progress = remember(grand, durationMillis) { Animatable(0f) }

    LaunchedEffect(grand, durationMillis) {
        progress.snapTo(0f)
        // Linear clock — burst/settle curves below own the feel.
        progress.animateTo(
            1f,
            animationSpec = tween(durationMillis = durationMillis, easing = LinearEasing)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val clock = progress.value
        // Sudden cannon blast, then gradual slowdown (air drag).
        val blast = easeOutQuint(clock)
        val coast = easeOutCubic(clock)
        // Gravity ramps up after the burst so pieces arcing then tumble down.
        val gravity = if (grand) {
            clock * clock * (0.35f + 0.65f * clock)
        } else {
            clock * clock
        }
        val spread = if (grand) 0.72f else 0.12f
        val fall = if (grand) 0.58f else 0.16f
        val gravityPx = (if (grand) 1.15f else 0.55f) * size.height
        pieces.forEach { piece ->
            val x = (piece.startX * size.width) +
                piece.vx * blast * size.width * spread +
                piece.drift * coast * size.width * 0.06f
            val y = (piece.startY * size.height) +
                piece.vy * blast * size.height * fall +
                gravity * gravityPx
            val alpha = if (grand) {
                when {
                    clock < 0.62f -> 1f
                    else -> ((1f - clock) / 0.38f).coerceIn(0f, 1f)
                }
            } else {
                (1f - clock).coerceIn(0f, 1f)
            }
            val color = piece.color.copy(alpha = alpha)
            // Fast whip at launch, then spin settles with the coast.
            val spinDegrees = piece.spin * (0.75f * blast + 0.25f * coast)
            rotate(degrees = spinDegrees, pivot = Offset(x, y)) {
                when (piece.shape) {
                    ConfettiShape.CIRCLE -> {
                        drawCircle(
                            color = color,
                            radius = piece.width / 2f,
                            center = Offset(x, y)
                        )
                    }
                    ConfettiShape.RIBBON, ConfettiShape.RECT -> {
                        drawRect(
                            color = color,
                            topLeft = Offset(x - piece.width / 2f, y - piece.height / 2f),
                            size = Size(piece.width, piece.height)
                        )
                    }
                }
            }
        }
    }
}

/** Pieces start just off each corner and fire into / across the screen. */
private fun createCornerBlast(count: Int): List<ConfettiPiece> {
    val corners = BlastCorner.entries
    return List(count) { index ->
        val corner = corners[index % corners.size]
        // Cone of fire from that corner toward the interior.
        val cone = (Random.nextFloat() - 0.5f) * 1.35f
        val angle = corner.aimAngle + cone
        val speed = Random.nextFloat() * 1.45f + 1.2f
        val shape = when (index % 5) {
            0 -> ConfettiShape.CIRCLE
            1, 2 -> ConfettiShape.RIBBON
            else -> ConfettiShape.RECT
        }
        val (w, h) = when (shape) {
            ConfettiShape.CIRCLE -> {
                val d = Random.nextFloat() * 14f + 8f
                d to d
            }
            ConfettiShape.RIBBON -> {
                val len = Random.nextFloat() * 30f + 14f
                (Random.nextFloat() * 5f + 3f) to len
            }
            ConfettiShape.RECT -> {
                (Random.nextFloat() * 16f + 9f) to (Random.nextFloat() * 10f + 5f)
            }
        }
        // Spawn slightly outside the corner so the blast reads as entering the screen.
        val jitter = 0.04f
        ConfettiPiece(
            startX = corner.x + (Random.nextFloat() - 0.5f) * jitter +
                when (corner) {
                    BlastCorner.TOP_LEFT, BlastCorner.BOTTOM_LEFT -> -0.04f
                    BlastCorner.TOP_RIGHT, BlastCorner.BOTTOM_RIGHT -> 0.04f
                },
            startY = corner.y + (Random.nextFloat() - 0.5f) * jitter +
                when (corner) {
                    BlastCorner.TOP_LEFT, BlastCorner.TOP_RIGHT -> -0.04f
                    BlastCorner.BOTTOM_LEFT, BlastCorner.BOTTOM_RIGHT -> 0.04f
                },
            vx = cos(angle) * speed,
            vy = sin(angle) * speed,
            color = ConfettiPalette.random(),
            width = w,
            height = h,
            spin = Random.nextFloat() * 1440f - 720f,
            shape = shape,
            drift = Random.nextFloat() * 1.6f - 0.8f
        )
    }
}

/** Lighter top-down rain for the default (non-grand) overlay. */
private fun createTopRain(count: Int): List<ConfettiPiece> {
    return List(count) {
        val angle = Random.nextFloat() * 6.28f
        val speed = Random.nextFloat() * 6f + 4f
        ConfettiPiece(
            startX = Random.nextFloat(),
            startY = Random.nextFloat() * 0.22f - 0.05f,
            vx = cos(angle) * speed * 0.35f,
            vy = Random.nextFloat() * 4f + 3f,
            color = ConfettiPalette.random(),
            width = Random.nextFloat() * 10f + 6f,
            height = Random.nextFloat() * 6f + 4f,
            spin = Random.nextFloat() * 720f - 360f,
            shape = ConfettiShape.RECT,
            drift = 0f
        )
    }
}
