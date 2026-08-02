package com.anant.fitbuddy.ui.loading.animations

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anant.fitbuddy.ui.loading.LoadingAnimation
import com.anant.fitbuddy.ui.loading.LoadingAnimationScope
import com.anant.fitbuddy.ui.loading.LoadingAnimationSlot
import kotlinx.coroutines.delay

/** Compact tiranga wash used inside insight / AI-target buttons. */
object TirangaInsightLoadingAnimation : LoadingAnimation {
    override val id: String = "tiranga_insight"
    override val displayName: String = "Indian flag"
    override val slots: Set<LoadingAnimationSlot> = setOf(LoadingAnimationSlot.INSIGHT)
    override val defaultCaptions: List<String> = JapanRowingLoadingAnimation.defaultCaptions

    @Composable
    override fun Content(scope: LoadingAnimationScope) {
        TirangaInsightContent(
            captions = scope.captions.ifEmpty { defaultCaptions },
            modifier = scope.modifier
        )
    }
}

@Composable
private fun TirangaInsightContent(
    captions: List<String>,
    modifier: Modifier = Modifier
) {
    var fabricTime by remember { mutableStateOf(0.0) }
    var chakraTime by remember { mutableStateOf(0.0) }
    var lastFrame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                val delta = if (lastFrame == 0L) 0L else (now - lastFrame)
                lastFrame = now
                fabricTime = accumulateScaledTime(fabricTime, delta, 1f)
                chakraTime = accumulateScaledTime(chakraTime, delta, 1f)
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

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawTirangaInsightFabric(
                timeMs = fabricTime,
                columns = 48,
                seamWobbleScale = 1.35f
            )
            val outerR = 11.dp.toPx()
            drawSpinningAshokaChakra(
                center = Offset(size.width / 2f, size.height / 2f),
                outerRadius = outerR,
                timeMs = chakraTime
            )
        }
        if (captions.isNotEmpty()) {
            Text(
                text = captions[captionIndex % captions.size],
                style = MaterialTheme.typography.labelLarge,
                color = TirangaChakraNavy,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp, end = 56.dp)
            )
        }
        Text(
            text = "CRAFTED IN INDIA",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            ),
            color = TirangaChakraNavy,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp, start = 56.dp)
        )
    }
}
