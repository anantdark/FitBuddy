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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anant.fitbuddy.ui.loading.LoadingAnimation
import com.anant.fitbuddy.ui.loading.LoadingAnimationScope
import com.anant.fitbuddy.ui.loading.LoadingAnimationSlot
import kotlinx.coroutines.delay

/** Dashboard analyzing banner: flowing tiranga wash with a steady spinning chakra. */
object TirangaLoadingAnimation : LoadingAnimation {
    override val id: String = "tiranga"
    override val displayName: String = "Indian flag"
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
                drawTirangaFabric(timeMs = fabricTime, columns = 72)
                val chakraCenter = Offset(size.width - 28.dp.toPx(), size.height / 2f)
                drawSpinningAshokaChakra(
                    center = chakraCenter,
                    outerRadius = 16.dp.toPx(),
                    timeMs = chakraTime
                )
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
                    color = TirangaChakraNavy,
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
                        color = TirangaChakraNavy.copy(alpha = 0.62f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}
