package com.anant.fitbuddy.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiPiece(
    val startX: Float,
    val startY: Float,
    val vx: Float,
    val vy: Float,
    val color: Color,
    val width: Float,
    val height: Float,
    val spin: Float
)

private val ConfettiPalette = listOf(
    Color(0xFF66BB6A),
    Color(0xFF29B6F6),
    Color(0xFFFF7043),
    Color(0xFFFFCA28),
    Color(0xFFAB47BC),
    Color(0xFF26A69A),
    Color(0xFFEF5350)
)

@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    durationMillis: Int = 3200
) {
    val pieces = remember {
        List(120) {
            val angle = Random.nextFloat() * 6.28f
            val speed = Random.nextFloat() * 6f + 4f
            ConfettiPiece(
                startX = Random.nextFloat(),
                startY = Random.nextFloat() * 0.35f,
                vx = cos(angle) * speed,
                vy = sin(angle) * speed + Random.nextFloat() * 4f + 2f,
                color = ConfettiPalette.random(),
                width = Random.nextFloat() * 10f + 6f,
                height = Random.nextFloat() * 6f + 4f,
                spin = Random.nextFloat() * 720f - 360f
            )
        }
    }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val t = progress.value
        val gravity = 520f * t * t
        pieces.forEach { piece ->
            val x = (piece.startX * size.width) + piece.vx * t * size.width * 0.08f
            val y = (piece.startY * size.height) + piece.vy * t * size.height * 0.12f + gravity * 0.001f
            val alpha = (1f - t).coerceIn(0f, 1f)
            rotate(degrees = piece.spin * t, pivot = Offset(x, y)) {
                drawRect(
                    color = piece.color.copy(alpha = alpha),
                    topLeft = Offset(x - piece.width / 2f, y - piece.height / 2f),
                    size = androidx.compose.ui.geometry.Size(piece.width, piece.height)
                )
            }
        }
    }
}
