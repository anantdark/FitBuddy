package com.anant.fitbuddy.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

private const val TumblerMl = 250
private const val MaxTumblers = 12
private const val StripScale = 0.9f
private const val PetiteScale = 0.95f
private val CoffeeLiquidColor = Color(0xFF8D6E63)
private val TeaLiquidColor = Color(0xFF66BB6A)

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
                        .height(34.dp)
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
 * Row (or single column, bottom-up, when vertical) of tumblers that fill one by one toward the goal.
 * Each tumbler holds 250 ml; drink layers stack water → coffee → tea.
 */
@Composable
fun WaveWaterMeter(
    layers: WaterFillLayers,
    modifier: Modifier = Modifier,
    orientation: WaterMeterOrientation = WaterMeterOrientation.Horizontal,
) {
    val spec = tween<Float>(700, easing = FastOutSlowInEasing)
    val animWater by animateFloatAsState(layers.waterMl.toFloat(), spec, label = "layerWater")
    val animCoffee by animateFloatAsState(layers.coffeeMl.toFloat(), spec, label = "layerCoffee")
    val animTea by animateFloatAsState(layers.teaMl.toFloat(), spec, label = "layerTea")
    val count = kotlin.math.ceil(layers.goalMl.coerceAtLeast(1) / TumblerMl.toDouble())
        .toInt().coerceIn(1, MaxTumblers)
    val outline = WaterAccentColor.copy(alpha = 0.7f)

    Canvas(modifier = modifier) {
        val vertical = orientation == WaterMeterOrientation.Vertical
        val cols = if (vertical) 1 else count
        val rows = if (vertical) count else 1
        val gap = (if (vertical) 3.dp else 5.dp).toPx()
        val cellW = (size.width - gap * (cols - 1)) / cols
        val cellH = (size.height - gap * (rows - 1)) / rows
        val aspect = if (vertical) 1.05f else 0.8f
        val tw = minOf(cellW, cellH * aspect) * (if (vertical) PetiteScale else StripScale)
        val th = tw / aspect
        val layerStarts = floatArrayOf(0f, animWater, animWater + animCoffee)
        val layerEnds = floatArrayOf(animWater, animWater + animCoffee, animWater + animCoffee + animTea)
        val layerColors = arrayOf(WaterAccentColor, CoffeeLiquidColor, TeaLiquidColor)

        for (i in 0 until count) {
            val col = if (vertical) 0 else i
            val row = if (vertical) rows - 1 - i else 0
            val left = col * (cellW + gap) + (cellW - tw) / 2f
            val top = row * (cellH + gap) + (cellH - th) / 2f
            val glass = tumblerPath(left, top, tw, th)

            val sliceStart = i * TumblerMl.toFloat()
            val sliceEnd = sliceStart + TumblerMl
            clipPath(glass) {
                var filledTop = top + th
                for (l in 0..2) {
                    val overlap = (minOf(layerEnds[l], sliceEnd) - maxOf(layerStarts[l], sliceStart))
                        .coerceAtLeast(0f)
                    if (overlap <= 0f) continue
                    val h = th * overlap / TumblerMl
                    drawRect(
                        color = layerColors[l],
                        topLeft = Offset(left, filledTop - h),
                        size = Size(tw, h),
                    )
                    filledTop -= h
                }
            }
            drawPath(glass, color = outline, style = Stroke(width = 1.dp.toPx()))
        }
    }
}

/** Sharp-cornered tapered glass: wider rim, narrower base. */
private fun tumblerPath(left: Float, top: Float, w: Float, h: Float): Path {
    val inset = w * 0.12f
    val bottom = top + h
    return Path().apply {
        moveTo(left, top)
        lineTo(left + w, top)
        lineTo(left + w - inset, bottom)
        lineTo(left + inset, bottom)
        close()
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

/** Soft dual-sine for a mellow leading edge (low amplitude + long wavelength); shared with the widget. */
internal fun softWave(t: Float): Float {
    val a = sin(t.toDouble()).toFloat()
    val b = sin(t * 0.55 + 1.1).toFloat() * 0.35f
    return a * 0.7f + b
}
