package com.anant.fitbuddy.ui.region

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anant.fitbuddy.data.region.AppRegion
import com.anant.fitbuddy.data.region.RegionDetector
import com.anant.fitbuddy.ui.loading.animations.accumulateScaledTime
import com.anant.fitbuddy.ui.loading.animations.drawSpinningAshokaChakra
import com.anant.fitbuddy.ui.loading.animations.drawTirangaFabric
import kotlin.math.min
import kotlinx.coroutines.delay

private const val MEMBER_FLAG_CYCLE_MS = 1_000L

/**
 * Region preview: India draws the Tiranga; multi-country packs cycle official member-country
 * flag emoji (Unicode regional indicators).
 */
@Composable
fun RegionFlagCanvas(region: AppRegion, modifier: Modifier = Modifier) {
    when {
        region.hasOfficialFlag() -> IndiaFlagCanvas(modifier = modifier)
        region.cyclesMemberFlags() -> RegionMemberFlagsCarousel(
            region = region,
            modifier = modifier,
            showCountryName = true,
            flagFontSize = 72.sp
        )
        else -> Box(modifier)
    }
}

/** Compact thumbnail for region list rows (flag only, no country label). */
@Composable
fun RegionFlagThumbnail(region: AppRegion, modifier: Modifier = Modifier) {
    when {
        region.hasOfficialFlag() -> IndiaFlagCanvas(modifier = modifier)
        region.cyclesMemberFlags() -> RegionMemberFlagsCarousel(
            region = region,
            modifier = modifier,
            showCountryName = false,
            flagFontSize = 22.sp
        )
        else -> Box(modifier)
    }
}

@Composable
private fun IndiaFlagCanvas(modifier: Modifier = Modifier) {
    var timeMs by remember { mutableStateOf(0.0) }
    var lastFrame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                val delta = if (lastFrame == 0L) 0L else now - lastFrame
                lastFrame = now
                timeMs = accumulateScaledTime(timeMs, delta, 1f)
            }
        }
    }

    Canvas(modifier = modifier) {
        drawIndiaFlag(timeMs)
    }
}

@Composable
private fun RegionMemberFlagsCarousel(
    region: AppRegion,
    modifier: Modifier = Modifier,
    showCountryName: Boolean,
    flagFontSize: TextUnit
) {
    val codes = remember(region) { region.memberCountryCodes() }
    if (codes.isEmpty()) {
        Box(modifier)
        return
    }

    var index by remember(region) { mutableIntStateOf(0) }
    LaunchedEffect(region, codes) {
        index = 0
        if (codes.size <= 1) return@LaunchedEffect
        while (true) {
            delay(MEMBER_FLAG_CYCLE_MS)
            index = (index + 1) % codes.size
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = index,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "region-member-flag"
        ) { i ->
            val iso = codes[i.coerceIn(0, codes.lastIndex)]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = isoToFlagEmoji(iso),
                    fontSize = flagFontSize,
                    textAlign = TextAlign.Center
                )
                if (showCountryName) {
                    Text(
                        text = RegionDetector.isoDisplayName(iso),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** ISO 3166-1 alpha-2 → regional-indicator flag emoji (e.g. US → 🇺🇸). */
internal fun isoToFlagEmoji(iso2: String): String {
    val code = iso2.trim().uppercase()
    if (code.length != 2) return ""
    val a = code[0]
    val b = code[1]
    if (a !in 'A'..'Z' || b !in 'A'..'Z') return ""
    val first = 0x1F1E6 + (a - 'A')
    val second = 0x1F1E6 + (b - 'A')
    return String(intArrayOf(first, second), 0, 2)
}

private fun DrawScope.drawIndiaFlag(timeMs: Double) {
    drawTirangaFabric(timeMs = timeMs, columns = 56)
    val center = Offset(size.width / 2f, size.height / 2f)
    drawSpinningAshokaChakra(
        center = center,
        outerRadius = min(size.width, size.height) * 0.14f,
        timeMs = timeMs
    )
}
