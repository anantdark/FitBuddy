package com.anant.fitbuddy.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.R
import com.anant.fitbuddy.data.database.WaterDrinkKind
import com.anant.fitbuddy.data.database.WaterLog
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.haptics.rememberAppHaptics
import com.anant.fitbuddy.ui.theme.appShape
import com.anant.fitbuddy.util.WaterFillLayers
import com.anant.fitbuddy.util.WaterFormat
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/** Soft cyan used for water accents (Flow — Hydrate inspired; works in light/dark). */
val WaterAccentColor = Color(0xFF4FC3F7)
internal val WaterDeepColor = Color(0xFF0288D1)
internal val WaterFoamColor = Color(0xFFB3E5FC)
private val BottleAccent = Color(0xFF26A69A)
private val CoffeeAccent = Color(0xFFA1887F)
private val CoffeeDeepColor = Color(0xFF6D4C41)
private val TeaAccent = Color(0xFF81C784)
private val TeaDeepColor = Color(0xFF2E7D32)

/** Soft blend width as a fraction of the filled axis (hue-ish drink boundaries). */
private const val LayerBlendFrac = 0.10f

enum class WaterMeterOrientation { Horizontal, Vertical }

/**
 * Compact water strip: wave meter + quick-adds.
 * Long-press a chip to change that drink's quick-add amount.
 */
@Composable
fun DashboardWaterSection(
    consumedMl: Int,
    goalMl: Int,
    quickAddMl: (WaterDrinkKind) -> Int,
    onQuickAdd: (WaterDrinkKind) -> Unit,
    onChangeQuickAdd: (WaterDrinkKind, Int) -> Unit,
    modifier: Modifier = Modifier,
    waterLogs: List<WaterLog> = emptyList(),
    showMeter: Boolean = true,
    meterOrientation: WaterMeterOrientation = WaterMeterOrientation.Horizontal,
) {
    val layers = remember(waterLogs, goalMl, consumedMl) {
        WaterFillLayers.fromLogs(waterLogs, goalMl).let { computed ->
            if (computed.totalMl > 0 || consumedMl <= 0) computed
            else WaterFillLayers(waterMl = consumedMl, goalMl = goalMl)
        }
    }
    val consumedLabel = remember(consumedMl) { WaterFormat.formatLiters(consumedMl) }
    val goalLabel = remember(goalMl) { WaterFormat.formatLiters(goalMl) }
    var editKind by remember { mutableStateOf<WaterDrinkKind?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.widget_water_title_short),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.water_progress_ratio, consumedLabel, goalLabel),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (showMeter) {
            WaveWaterMeter(
                layers = layers,
                orientation = meterOrientation,
                modifier = when (meterOrientation) {
                    WaterMeterOrientation.Horizontal -> Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                    WaterMeterOrientation.Vertical -> Modifier
                        .width(36.dp)
                        .height(200.dp)
                },
            )
        }
        WaterQuickAddRow(
            quickAddMl = quickAddMl,
            onQuickAdd = onQuickAdd,
            onLongPress = { editKind = it },
        )
    }

    editKind?.let { kind ->
        QuickAddAmountDialog(
            kind = kind,
            initialMl = quickAddMl(kind),
            onDismiss = { editKind = null },
            onConfirm = { ml ->
                onChangeQuickAdd(kind, ml)
                editKind = null
            },
        )
    }
}

/**
 * Full-height vertical water column for the extended calorie hero (right side).
 * Quick-add chips live below the dashboard card — not beside the meter.
 */
@Composable
fun VerticalWaterSidePanel(
    consumedMl: Int,
    goalMl: Int,
    modifier: Modifier = Modifier,
    waterLogs: List<WaterLog> = emptyList(),
) {
    val layers = remember(waterLogs, goalMl, consumedMl) {
        WaterFillLayers.fromLogs(waterLogs, goalMl).let { computed ->
            if (computed.totalMl > 0 || consumedMl <= 0) computed
            else WaterFillLayers(waterMl = consumedMl, goalMl = goalMl)
        }
    }
    val consumedLabel = remember(consumedMl) { WaterFormat.formatLiters(consumedMl) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = consumedLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = WaterDeepColor,
            maxLines = 1,
        )
        WaveWaterMeter(
            layers = layers,
            orientation = WaterMeterOrientation.Vertical,
            modifier = Modifier
                .width(44.dp)
                .weight(1f, fill = true)
                .fillMaxHeight(),
        )
    }
}

@Composable
fun WaterQuickAddRow(
    quickAddMl: (WaterDrinkKind) -> Int,
    onQuickAdd: (WaterDrinkKind) -> Unit,
    onLongPress: (WaterDrinkKind) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuickAddDrinkChip(
            kind = WaterDrinkKind.WATER,
            amountMl = quickAddMl(WaterDrinkKind.WATER),
            icon = Icons.Filled.LocalDrink,
            accent = WaterAccentColor,
            onClick = { onQuickAdd(WaterDrinkKind.WATER) },
            onLongClick = { onLongPress(WaterDrinkKind.WATER) },
            modifier = Modifier.weight(1f),
        )
        QuickAddDrinkChip(
            kind = WaterDrinkKind.BOTTLE,
            amountMl = quickAddMl(WaterDrinkKind.BOTTLE),
            icon = Icons.Filled.WaterDrop,
            accent = BottleAccent,
            onClick = { onQuickAdd(WaterDrinkKind.BOTTLE) },
            onLongClick = { onLongPress(WaterDrinkKind.BOTTLE) },
            modifier = Modifier.weight(1f),
        )
        QuickAddDrinkChip(
            kind = WaterDrinkKind.COFFEE,
            amountMl = quickAddMl(WaterDrinkKind.COFFEE),
            icon = Icons.Filled.Coffee,
            accent = CoffeeAccent,
            onClick = { onQuickAdd(WaterDrinkKind.COFFEE) },
            onLongClick = { onLongPress(WaterDrinkKind.COFFEE) },
            modifier = Modifier.weight(1f),
        )
        QuickAddDrinkChip(
            kind = WaterDrinkKind.TEA,
            amountMl = quickAddMl(WaterDrinkKind.TEA),
            icon = Icons.Filled.Spa,
            accent = TeaAccent,
            onClick = { onQuickAdd(WaterDrinkKind.TEA) },
            onLongClick = { onLongPress(WaterDrinkKind.TEA) },
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Capsule meter with soft hue-blended drink layers, a low slow wave, and
 * horizontal-swimming fish sprites (OpenGameArt “2D Retro Fish” by EXCITESZZ, CC0).
 */
@Composable
fun WaveWaterMeter(
    layers: WaterFillLayers,
    modifier: Modifier = Modifier,
    orientation: WaterMeterOrientation = WaterMeterOrientation.Horizontal,
) {
    val animWater by animateFloatAsState(
        targetValue = layers.waterShare,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "layerWater",
    )
    val animCoffee by animateFloatAsState(
        targetValue = layers.coffeeShare,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "layerCoffee",
    )
    val animTea by animateFloatAsState(
        targetValue = layers.teaShare,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "layerTea",
    )
    val transition = rememberInfiniteTransition(label = "waterWave")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )
    val fishPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "fishPhase",
    )
    val fishRed = ImageBitmap.imageResource(R.drawable.ic_fish_red)
    val fishOrange = ImageBitmap.imageResource(R.drawable.ic_fish_orange)
    val fishTeal = ImageBitmap.imageResource(R.drawable.ic_fish_teal)
    val fishBitmaps = remember(fishRed, fishOrange, fishTeal) {
        listOf(fishRed, fishOrange, fishTeal)
    }
    val track = WaterAccentColor.copy(alpha = 0.16f)

    Canvas(modifier = modifier) {
        val vertical = orientation == WaterMeterOrientation.Vertical
        val radius = if (vertical) size.width / 2f else size.height / 2f
        val capsule = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    cornerRadius = CornerRadius(radius, radius),
                )
            )
        }
        drawPath(capsule, color = track)

        val fill = (animWater + animCoffee + animTea).coerceIn(0f, 1f)
        if (fill > 0.001f) {
            clipPath(capsule) {
                if (vertical) {
                    drawVerticalHueWater(animWater, animCoffee, animTea, wavePhase)
                } else {
                    drawHorizontalHueWater(animWater, animCoffee, animTea, wavePhase)
                }
                drawSwimmingFishSprites(
                    fishPhase = fishPhase,
                    fillShare = fill,
                    verticalMeter = vertical,
                    bitmaps = fishBitmaps,
                )
            }
        }
    }
}

/** Soft dual-sine for a mellow leading edge (low amplitude + long wavelength). */
internal fun softWave(t: Float): Float {
    val a = sin(t.toDouble()).toFloat()
    val b = sin(t * 0.55 + 1.1).toFloat() * 0.35f
    return a * 0.7f + b
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val u = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * u,
        green = a.green + (b.green - a.green) * u,
        blue = a.blue + (b.blue - a.blue) * u,
        alpha = a.alpha + (b.alpha - a.alpha) * u,
    )
}

/** Build soft color stops water → coffee → tea with hue-ish blend zones. */
private fun softLayerStops(
    waterShare: Float,
    coffeeShare: Float,
    teaShare: Float,
): List<Pair<Float, Color>> {
    val total = (waterShare + coffeeShare + teaShare).coerceAtLeast(0.001f)
    val wEnd = waterShare / total
    val cEnd = (waterShare + coffeeShare) / total
    val blend = LayerBlendFrac.coerceAtMost(0.18f)
    val stops = mutableListOf<Pair<Float, Color>>()

    fun add(pos: Float, color: Color) {
        val p = pos.coerceIn(0f, 1f)
        if (stops.isEmpty() || kotlin.math.abs(stops.last().first - p) > 0.001f) {
            stops += p to color
        }
    }

    add(0f, WaterDeepColor)
    if (waterShare > 0.001f) {
        val mid = (wEnd * 0.55f).coerceAtMost(wEnd)
        add(mid, WaterAccentColor)
    }
    if (coffeeShare > 0.001f) {
        val blendStart = (wEnd - blend * 0.5f).coerceAtLeast(0f)
        val blendEnd = (wEnd + blend * 0.5f).coerceAtMost(cEnd)
        add(blendStart, lerpColor(WaterAccentColor, CoffeeDeepColor, 0.35f))
        add((blendStart + blendEnd) * 0.5f, lerpColor(WaterAccentColor, CoffeeAccent, 0.55f))
        add(blendEnd, CoffeeAccent)
        add((wEnd + cEnd) * 0.5f, CoffeeDeepColor)
    }
    if (teaShare > 0.001f) {
        val blendStart = (cEnd - blend * 0.5f).coerceAtLeast(wEnd)
        val blendEnd = (cEnd + blend * 0.5f).coerceAtMost(1f)
        add(blendStart, lerpColor(CoffeeAccent, TeaDeepColor, 0.35f))
        add((blendStart + blendEnd) * 0.5f, lerpColor(CoffeeAccent, TeaAccent, 0.55f))
        add(blendEnd, TeaAccent)
        add(1f, TeaDeepColor)
    } else if (coffeeShare > 0.001f) {
        add(1f, CoffeeAccent)
    } else {
        add(1f, WaterFoamColor)
    }
    return stops
}

private fun DrawScope.drawHorizontalHueWater(
    waterShare: Float,
    coffeeShare: Float,
    teaShare: Float,
    wavePhase: Float,
) {
    val total = (waterShare + coffeeShare + teaShare).coerceIn(0f, 1f)
    val fillEnd = size.width * total
    // Lower wave amplitude than before.
    val waveAmp = (size.height * 0.045f).coerceAtMost(fillEnd * 0.12f)
    val stops = softLayerStops(waterShare, coffeeShare, teaShare)
    val colors = stops.map { it.second }
    val colorStops = stops.map { it.first }.toFloatArray()

    val path = Path().apply {
        moveTo(0f, 0f)
        lineTo(0f, size.height)
        var y = size.height
        while (y >= 0f) {
            val t = y / size.height * 1.05f * PI.toFloat() + wavePhase
            val x = (fillEnd + softWave(t) * waveAmp).coerceAtLeast(0f)
            lineTo(x, y)
            y -= 2f
        }
        close()
    }
    drawPath(
        path = path,
        brush = Brush.horizontalGradient(
            colorStops = colorStops.zip(colors).toTypedArray(),
            startX = 0f,
            endX = fillEnd,
        ),
    )
    drawBubbles(waterWidth = fillEnd, waterHeight = size.height, horizontal = true)
}

private fun DrawScope.drawVerticalHueWater(
    waterShare: Float,
    coffeeShare: Float,
    teaShare: Float,
    wavePhase: Float,
) {
    val total = (waterShare + coffeeShare + teaShare).coerceIn(0f, 1f)
    val fillHeight = size.height * total
    val surfaceY = size.height - fillHeight
    val waveAmp = (size.width * 0.055f).coerceAtMost(fillHeight * 0.10f)
    val stops = softLayerStops(waterShare, coffeeShare, teaShare)
    // Vertical: 0 = bottom (water), 1 = top surface — reverse stop positions for Brush.
    val colors = stops.map { it.second }.asReversed()
    val colorStops = stops.map { 1f - it.first }.asReversed().toFloatArray()

    val path = Path().apply {
        moveTo(0f, size.height)
        lineTo(size.width, size.height)
        var x = size.width
        while (x >= 0f) {
            val t = x / size.width * 1.05f * PI.toFloat() + wavePhase
            val y = (surfaceY + softWave(t) * waveAmp).coerceAtMost(size.height)
            lineTo(x, y)
            x -= 2f
        }
        close()
    }
    drawPath(
        path = path,
        brush = Brush.verticalGradient(
            colorStops = colorStops.zip(colors).toTypedArray(),
            startY = surfaceY - waveAmp,
            endY = size.height,
        ),
    )
    drawBubbles(
        waterWidth = size.width,
        waterHeight = fillHeight,
        horizontal = false,
        waterTop = surfaceY,
    )
}

private fun DrawScope.drawBubbles(
    waterWidth: Float,
    waterHeight: Float,
    horizontal: Boolean,
    waterTop: Float = 0f,
) {
    val bubbleColor = Color.White.copy(alpha = 0.28f)
    if (horizontal) {
        listOf(
            Offset(waterWidth * 0.22f, size.height * 0.62f) to size.height * 0.08f,
            Offset(waterWidth * 0.48f, size.height * 0.72f) to size.height * 0.05f,
            Offset(waterWidth * 0.70f, size.height * 0.55f) to size.height * 0.06f,
        ).forEach { (center, r) ->
            if (center.x in 4f..(waterWidth - 2f)) {
                drawCircle(color = bubbleColor, radius = r, center = center)
            }
        }
    } else {
        listOf(
            Offset(size.width * 0.35f, waterTop + waterHeight * 0.35f) to size.width * 0.08f,
            Offset(size.width * 0.62f, waterTop + waterHeight * 0.55f) to size.width * 0.06f,
            Offset(size.width * 0.45f, waterTop + waterHeight * 0.75f) to size.width * 0.07f,
        ).forEach { (center, r) ->
            if (center.y in waterTop..(size.height - 2f)) {
                drawCircle(color = bubbleColor, radius = r, center = center)
            }
        }
    }
}

/**
 * Fish always swim left↔right. In the vertical meter they still move
 * horizontally across the narrow column (tiny bob on Y).
 */
private fun DrawScope.drawSwimmingFishSprites(
    fishPhase: Float,
    fillShare: Float,
    verticalMeter: Boolean,
    bitmaps: List<ImageBitmap>,
) {
    if (bitmaps.isEmpty() || fillShare < 0.08f) return

    data class Spec(val phaseOff: Float, val lane: Float, val sizeFrac: Float, val speed: Float, val bitmapIndex: Int)

    val school = listOf(
        Spec(0.00f, 0.32f, 0.55f, 1.0f, 0),
        Spec(0.42f, 0.58f, 0.42f, 0.78f, 1),
        Spec(0.71f, 0.44f, 0.36f, 1.18f, 2),
    )

    if (verticalMeter) {
        val waterTop = size.height * (1f - fillShare)
        val waterBottom = size.height - 2f
        val waterH = (waterBottom - waterTop).coerceAtLeast(1f)
        if (waterH < size.width * 1.4f) return
        school.forEach { spec ->
            val bmp = bitmaps[spec.bitmapIndex % bitmaps.size]
            val t = ((fishPhase * spec.speed) + spec.phaseOff) % 1f
            val goingRight = t < 0.5f
            val travel = if (goingRight) t * 2f else (1f - t) * 2f
            val bob = softWave(fishPhase * 2f * PI.toFloat() + spec.phaseOff * 5f) * waterH * 0.03f
            val fishH = (size.width * spec.sizeFrac).coerceIn(8f, size.width * 0.85f)
            val aspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1)
            val fishW = fishH * aspect
            val cx = fishW * 0.55f + (size.width - fishW * 1.1f) * travel
            val cy = waterTop + waterH * spec.lane + bob
            drawFishBitmap(bmp, cx, cy, fishW, fishH, facingRight = goingRight)
        }
    } else {
        val waterRight = size.width * fillShare
        if (waterRight < size.height * 1.6f) return
        school.forEach { spec ->
            val bmp = bitmaps[spec.bitmapIndex % bitmaps.size]
            val t = ((fishPhase * spec.speed) + spec.phaseOff) % 1f
            val goingRight = t < 0.5f
            val travel = if (goingRight) t * 2f else (1f - t) * 2f
            val bob = softWave(fishPhase * 2f * PI.toFloat() + spec.phaseOff * 5f) * size.height * 0.05f
            val fishH = (size.height * spec.sizeFrac).coerceIn(6f, size.height * 0.7f)
            val aspect = bmp.width.toFloat() / bmp.height.coerceAtLeast(1)
            val fishW = fishH * aspect
            val cx = 4f + fishW * 0.5f + (waterRight - fishW - 8f).coerceAtLeast(0f) * travel
            val cy = size.height * spec.lane + bob
            if (cx + fishW * 0.4f < waterRight) {
                drawFishBitmap(bmp, cx, cy, fishW, fishH, facingRight = goingRight)
            }
        }
    }
}

private fun DrawScope.drawFishBitmap(
    bitmap: ImageBitmap,
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    facingRight: Boolean,
) {
    val dstW = width.roundToInt().coerceAtLeast(1)
    val dstH = height.roundToInt().coerceAtLeast(1)
    withTransform({
        if (!facingRight) {
            scale(scaleX = -1f, scaleY = 1f, pivot = Offset(centerX, centerY))
        }
    }) {
        drawImage(
            image = bitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(bitmap.width, bitmap.height),
            dstOffset = IntOffset(
                (centerX - dstW / 2f).roundToInt(),
                (centerY - dstH / 2f).roundToInt(),
            ),
            dstSize = IntSize(dstW, dstH),
            alpha = 0.92f,
        )
    }
}

@Composable
private fun QuickAddDrinkChip(
    kind: WaterDrinkKind,
    amountMl: Int,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = drinkKindLabel(kind)
    val haptics = rememberAppHaptics()
    Column(
        modifier = modifier
            .background(accent.copy(alpha = 0.14f), appShape(14.dp))
            .combinedClickable(
                onClick = {
                    haptics.confirm()
                    onClick()
                },
                onLongClick = {
                    haptics.longPress()
                    onLongClick()
                },
            )
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = stringResource(R.string.water_plus_ml, amountMl),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun QuickAddAmountDialog(
    kind: WaterDrinkKind,
    initialMl: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
    titleOverride: String? = null,
    hintOverride: String? = null,
    minMl: Int = AppSettings.QUICK_ADD_ML_MIN,
    maxMl: Int = AppSettings.QUICK_ADD_ML_MAX,
) {
    var text by remember(kind, initialMl) { mutableStateOf(initialMl.toString()) }
    val parsed = text.trim().toIntOrNull()
    val valid = parsed != null && parsed in minMl..maxMl

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                titleOverride ?: stringResource(
                    R.string.water_edit_amount_title,
                    drinkKindLabel(kind),
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = hintOverride ?: stringResource(
                        R.string.water_edit_amount_hint,
                        minMl,
                        maxMl,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { raw ->
                        text = raw.filter { it.isDigit() }.take(4)
                    },
                    singleLine = true,
                    label = { Text(stringResource(R.string.water_edit_amount_label)) },
                    suffix = { Text("ml") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { parsed?.let(onConfirm) },
            ) {
                Text(stringResource(R.string.water_edit_amount_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        },
    )
}

@Composable
fun EditWaterLogDialog(
    log: WaterLog,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val kind = WaterDrinkKind.fromStored(log.drinkKind)
    QuickAddAmountDialog(
        kind = kind,
        initialMl = log.amountMl,
        onDismiss = onDismiss,
        onConfirm = onConfirm,
        titleOverride = stringResource(R.string.water_edit_log_title, drinkKindLabel(kind)),
        hintOverride = stringResource(
            R.string.water_edit_log_hint,
            AppSettings.QUICK_ADD_ML_MIN,
            AppSettings.QUICK_ADD_ML_MAX,
        ),
    )
}

@Composable
fun drinkKindLabel(kind: WaterDrinkKind): String = when (kind) {
    WaterDrinkKind.WATER -> stringResource(R.string.water_kind_water)
    WaterDrinkKind.BOTTLE -> stringResource(R.string.water_kind_bottle)
    WaterDrinkKind.COFFEE -> stringResource(R.string.water_kind_coffee)
    WaterDrinkKind.TEA -> stringResource(R.string.water_kind_tea)
    WaterDrinkKind.CUSTOM -> stringResource(R.string.water_kind_custom)
}

fun drinkKindIcon(kind: WaterDrinkKind): ImageVector = when (kind) {
    WaterDrinkKind.WATER -> Icons.Filled.LocalDrink
    WaterDrinkKind.BOTTLE -> Icons.Filled.WaterDrop
    WaterDrinkKind.COFFEE -> Icons.Filled.Coffee
    WaterDrinkKind.TEA -> Icons.Filled.Spa
    WaterDrinkKind.CUSTOM -> Icons.Filled.LocalDrink
}

fun drinkKindAccent(kind: WaterDrinkKind): Color = when (kind) {
    WaterDrinkKind.WATER -> WaterAccentColor
    WaterDrinkKind.BOTTLE -> BottleAccent
    WaterDrinkKind.COFFEE -> CoffeeAccent
    WaterDrinkKind.TEA -> TeaAccent
    WaterDrinkKind.CUSTOM -> WaterAccentColor
}
