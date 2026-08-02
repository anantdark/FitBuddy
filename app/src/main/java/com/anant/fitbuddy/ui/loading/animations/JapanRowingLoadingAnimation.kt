package com.anant.fitbuddy.ui.loading.animations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.ui.loading.LoadingAnimation
import com.anant.fitbuddy.ui.loading.LoadingAnimationScope
import com.anant.fitbuddy.ui.loading.LoadingAnimationSlot
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay

/** Compact Japan rowing scene used inside insight / AI-target buttons. */
object JapanRowingLoadingAnimation : LoadingAnimation {
    override val id: String = "japan_rowing"
    override val displayName: String = "Japan rowing"
    override val slots: Set<LoadingAnimationSlot> = setOf(LoadingAnimationSlot.INSIGHT)
    override val defaultCaptions: List<String> = listOf(
        "Generating insight…",
        "Reading your charts…",
        "Crunching the numbers…",
        "Spotting trends…",
        "Almost there…",
        "Weighing the data…",
        "Consulting the AI…",
        "Mapping your progress…"
    )

    @Composable
    override fun Content(scope: LoadingAnimationScope) {
        JapanRowingContent(
            captions = scope.captions.ifEmpty { defaultCaptions },
            modifier = scope.modifier
        )
    }
}

@Composable
private fun JapanRowingContent(
    captions: List<String>,
    modifier: Modifier = Modifier
) {
    val boatTransition = rememberInfiniteTransition(label = "insight-boat")
    val boatProgress by boatTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 15_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "insight-boat-progress"
    )

    var captionIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(captions) {
        if (captions.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(8_000L)
            captionIndex = (captionIndex + 1) % captions.size
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        JapanRowingCanvas(
            boatProgress = boatProgress,
            modifier = Modifier.fillMaxSize()
        )
        if (captions.isNotEmpty()) {
            Text(
                text = captions[captionIndex % captions.size],
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                modifier = Modifier
                    .background(
                        color = Color(0x99000000),
                        shape = MaterialTheme.shapes.small
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun JapanRowingCanvas(boatProgress: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "japan-rowing")

    val waveOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave-offset"
    )
    val oarAngle by transition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oar-angle"
    )
    val armAngle by transition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "arm-angle"
    )

    val japanRed = Color(0xFFBC002D)
    val waterBlue = Color(0xFF4A90D9)
    val waterDark = Color(0xFF2E6DA4)
    val skinColor = Color(0xFFFFD5A8)
    val robeColor = Color(0xFF1A3A5C)
    val hatColor = Color(0xFF5C3D1E)
    val boatColor = Color(0xFF8B4513)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val waterLineY = h * 0.65f
        val waveAmplitude = h * 0.07f

        drawRect(color = Color(0xFFF5EDE0), size = size)

        val sunRadius = h * 0.22f
        val sunCx = w * 0.87f
        val sunCy = waterLineY - sunRadius * 0.6f
        drawCircle(color = japanRed, radius = sunRadius, center = Offset(sunCx, sunCy))

        drawRect(
            color = waterBlue,
            topLeft = Offset(0f, waterLineY),
            size = androidx.compose.ui.geometry.Size(w, h - waterLineY)
        )

        drawWaves(waveOffset, waterLineY, w, h, waveAmplitude, waterDark, alpha = 0.55f)
        drawWaves(
            waveOffset + 0.5f, waterLineY + waveAmplitude * 0.4f, w, h,
            waveAmplitude * 0.5f, Color.White, alpha = 0.30f
        )

        val boatX = boatProgress * (w + w * 0.3f) - w * 0.15f
        val boatW = w * 0.22f
        val boatH = h * 0.12f
        val boatY = waterLineY - boatH * 0.5f

        drawBoat(boatX, boatY, boatW, boatH, boatColor)
        drawRower(
            cx = boatX + boatW * 0.38f,
            baseY = boatY,
            scale = boatH * 1.5f,
            oarAngle = oarAngle,
            armAngle = armAngle,
            skinColor = skinColor,
            robeColor = robeColor,
            hatColor = hatColor
        )
    }
}

private fun DrawScope.drawWaves(
    offsetFraction: Float,
    baseY: Float,
    w: Float,
    h: Float,
    amplitude: Float,
    color: Color,
    alpha: Float
) {
    val waveLength = w * 0.35f
    val path = Path()
    val steps = 200
    for (i in 0..steps) {
        val x = i / steps.toFloat() * w
        val phase = (x / waveLength + offsetFraction) * 2f * PI.toFloat()
        val y = baseY - amplitude * sin(phase)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.lineTo(w, h)
    path.lineTo(0f, h)
    path.close()
    drawPath(path, color = color.copy(alpha = alpha))
}

private fun DrawScope.drawBoat(
    cx: Float, topY: Float, boatW: Float, boatH: Float, color: Color
) {
    val path = Path().apply {
        moveTo(cx - boatW * 0.5f, topY)
        lineTo(cx + boatW * 0.5f, topY)
        lineTo(cx + boatW * 0.38f, topY + boatH)
        lineTo(cx - boatW * 0.38f, topY + boatH)
        close()
    }
    drawPath(path, color = color)
    drawPath(path, color = Color(0xFF5A2D0C), style = Stroke(width = boatH * 0.08f))
}

private fun DrawScope.drawRower(
    cx: Float,
    baseY: Float,
    scale: Float,
    oarAngle: Float,
    armAngle: Float,
    skinColor: Color,
    robeColor: Color,
    hatColor: Color
) {
    val unit = scale * 0.08f

    val hipY = baseY - unit * 1.2f
    val shoulderY = hipY - unit * 2.5f
    val headY = shoulderY - unit * 1.8f

    drawLine(
        color = robeColor, start = Offset(cx, hipY), end = Offset(cx, shoulderY),
        strokeWidth = unit * 2.2f, cap = StrokeCap.Round
    )
    drawCircle(color = skinColor, radius = unit * 1.0f, center = Offset(cx, headY))

    val hatPath = Path().apply {
        moveTo(cx, headY - unit * 2.2f)
        lineTo(cx - unit * 2.2f, headY - unit * 0.2f)
        lineTo(cx + unit * 2.2f, headY - unit * 0.2f)
        close()
    }
    drawPath(hatPath, color = hatColor)
    drawLine(
        color = Color(0xFF3A2000),
        start = Offset(cx - unit * 2.2f, headY - unit * 0.2f),
        end = Offset(cx + unit * 2.2f, headY - unit * 0.2f),
        strokeWidth = unit * 0.5f
    )

    val armRad = Math.toRadians(armAngle.toDouble()).toFloat()
    val armLen = unit * 2.5f
    val elbowX = cx + armLen * sin(armRad)
    val elbowY = shoulderY + armLen * (1 - 0.3f * sin(armRad))
    drawLine(
        color = skinColor, start = Offset(cx, shoulderY),
        end = Offset(elbowX, elbowY), strokeWidth = unit * 0.9f, cap = StrokeCap.Round
    )

    val oarRad = Math.toRadians(oarAngle.toDouble()).toFloat()
    val oarLen = unit * 5.5f
    val oarEndX = elbowX - oarLen * sin(oarRad)
    val oarEndY = elbowY + oarLen * 0.6f
    drawLine(
        color = Color(0xFF8B6914), start = Offset(elbowX, elbowY),
        end = Offset(oarEndX, oarEndY), strokeWidth = unit * 0.6f, cap = StrokeCap.Round
    )
    drawCircle(
        color = Color(0xFFA0783C), radius = unit * 0.8f,
        center = Offset(oarEndX, oarEndY)
    )
}
