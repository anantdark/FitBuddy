package com.anant.fitbuddy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anant.fitbuddy.data.database.ExerciseDailySummary
import com.anant.fitbuddy.data.database.FoodDailySummary
import com.anant.fitbuddy.util.DateUtils
import java.util.Calendar
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
/** Soft Material palette accents for P/C/F (Red 200 / Teal 200 / Orange 200). */
val MacroProteinColor = Color(0xFFEF9A9A)
val MacroCarbsColor = Color(0xFF80CBC4)
val MacroFatsColor = Color(0xFFFFCC80)

@Composable
fun CalorieRing(
    progress: Float, // 0.0f to 1.0f or more if exceeding
    centerText: String,
    subText: String,
    modifier: Modifier = Modifier,
    topText: String? = null,
    strokeWidth: Float = 36f,
    progressColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceAtLeast(0f),
        animationSpec = tween(durationMillis = 800),
        label = "progress"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (size.minDimension - strokeWidth) / 2
            val center = Offset(size.width / 2, size.height / 2)

            // Track circle
            drawCircle(
                color = trackColor,
                radius = radius,
                center = center,
                style = Stroke(width = strokeWidth)
            )

            // Progress arc starting from top (-90 degrees)
            val sweepAngle = (animatedProgress * 360f).coerceAtMost(360f)
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2)
            )
        }

        // Labels in center
        Box(
            modifier = Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (topText != null) {
                    Text(
                        text = topText,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = centerText,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * True when shortfall vs the calorie target is the bad side (GAIN_MUSCLE / RECOMP).
 * LOSE_WEIGHT (and unknown) treat the target as a ceiling — surplus is bad.
 */
fun calorieTargetPrefersSurplus(goal: String): Boolean =
    when (goal.trim().uppercase()) {
        "GAIN_MUSCLE", "RECOMP" -> true
        else -> false
    }

/**
 * Food calories vs the full-day target. Equidistant points, no X labels; scrub for date + vs-target.
 *
 * Values within ±100 kcal are green. Beyond that range, [preferSurplus] makes overages yellow
 * and shortfalls red; loss goals make shortfalls yellow and overages red.
 */
@Composable
fun CustomLineChart(
    foodSummaries: List<FoodDailySummary>,
    targetCalories: Int,
    modifier: Modifier = Modifier,
    /** True for GAIN_MUSCLE / RECOMP; false for LOSE_WEIGHT. */
    preferSurplus: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var selectedIndex by remember(foodSummaries) { mutableIntStateOf(-1) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val dataPoints = remember(foodSummaries) {
        foodSummaries.map { summary ->
            summary.dateString to summary.totalCalories
        }.asReversed()
    }

    if (dataPoints.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No log data available yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val rawMax = max(targetCalories, dataPoints.maxOf { it.second.coerceAtLeast(0) })
    val maxVal = (rawMax * 1.18f).coerceAtLeast(1f)
    val minVal = 0f

    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val targetLineColor = MaterialTheme.colorScheme.outline
    val markerCoreColor = MaterialTheme.colorScheme.surface
    val goodColor = TrendGreen
    val badColor = TrendRed
    val fillTop = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val fillBottom = MaterialTheme.colorScheme.primary.copy(alpha = 0.02f)

    val leftPadPx = with(density) { 44.dp.toPx() }
    val rightPadPx = with(density) { 8.dp.toPx() }
    val topPadPx = with(density) { 16.dp.toPx() }
    val bottomPadPx = with(density) { 8.dp.toPx() }

    fun indexForX(x: Float): Int {
        val count = dataPoints.size
        if (count <= 1) return 0
        val plotW = (canvasSize.width - leftPadPx - rightPadPx).coerceAtLeast(1f)
        val t = ((x - leftPadPx) / plotW).coerceIn(0f, 1f)
        return (t * (count - 1)).roundToInt().coerceIn(0, count - 1)
    }

    fun pointOffset(index: Int): Offset {
        val count = dataPoints.size
        val plotW = (canvasSize.width - leftPadPx - rightPadPx).coerceAtLeast(1f)
        val plotH = (canvasSize.height - topPadPx - bottomPadPx).coerceAtLeast(1f)
        val range = (maxVal - minVal).coerceAtLeast(1f)
        val value = dataPoints[index].second.coerceAtLeast(0).toFloat()
        val x = if (count <= 1) {
            leftPadPx + plotW / 2f
        } else {
            leftPadPx + plotW * (index.toFloat() / (count - 1).toFloat())
        }
        val y = topPadPx + plotH * (1f - ((value - minVal) / range)).coerceIn(0f, 1f)
        return Offset(x, y)
    }

    fun dayColor(net: Int): Color {
        val difference = net - targetCalories
        val cautionColor = Color(0xFFF59E0B)
        return when {
            difference in -100..100 -> goodColor
            preferSurplus && difference > 100 -> cautionColor
            !preferSurplus && difference < -100 -> cautionColor
            else -> badColor
        }
    }
    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(dataPoints) {
                detectTapGestures { pos -> selectedIndex = indexForX(pos.x) }
            }
            .pointerInput(dataPoints) {
                detectDragGestures(
                    onDragStart = { pos -> selectedIndex = indexForX(pos.x) },
                    onDrag = { change, _ ->
                        selectedIndex = indexForX(change.position.x)
                        change.consume()
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPad = leftPadPx
            val rightPad = rightPadPx
            val topPad = topPadPx
            val bottomPad = bottomPadPx
            val graphWidth = size.width - leftPad - rightPad
            val graphHeight = size.height - topPad - bottomPad
            if (graphWidth <= 0f || graphHeight <= 0f) return@Canvas

            val range = (maxVal - minVal).coerceAtLeast(1f)
            val stepX = if (dataPoints.size <= 1) 0f else graphWidth / (dataPoints.size - 1)

            val gridCount = 4
            for (i in 0..gridCount) {
                val ratio = i.toFloat() / gridCount
                val y = topPad + graphHeight * (1f - ratio)
                val valueLabel = (minVal + range * ratio).roundToInt().toString()
                drawLine(
                    color = gridLineColor,
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = valueLabel,
                    topLeft = Offset(8f, y - 14f),
                    style = TextStyle(color = textColor, fontSize = 10.sp),
                )
            }

            val targetY = topPad + graphHeight *
                (1f - ((targetCalories - minVal) / range)).coerceIn(0f, 1f)
            drawLine(
                color = targetLineColor,
                start = Offset(leftPad, targetY),
                end = Offset(size.width - rightPad, targetY),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
            )
            drawText(
                textMeasurer = textMeasurer,
                text = "Target · $targetCalories",
                topLeft = Offset(leftPad + 6f, targetY - 28f),
                style = TextStyle(
                    color = targetLineColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )

            val pts = dataPoints.mapIndexed { idx, pair ->
                val x = if (dataPoints.size <= 1) {
                    leftPad + graphWidth / 2f
                } else {
                    leftPad + idx * stepX
                }
                val value = pair.second.coerceAtLeast(0).toFloat()
                val y = topPad + graphHeight * (1f - ((value - minVal) / range)).coerceIn(0f, 1f)
                Offset(x, y)
            }

            if (pts.isNotEmpty()) {
                val curve = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        val pPrev = pts[i - 1]
                        val pCurr = pts[i]
                        val midX = (pPrev.x + pCurr.x) / 2f
                        cubicTo(midX, pPrev.y, midX, pCurr.y, pCurr.x, pCurr.y)
                    }
                }
                val fillPath = Path().apply {
                    addPath(curve)
                    lineTo(pts.last().x, topPad + graphHeight)
                    lineTo(pts.first().x, topPad + graphHeight)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(fillTop, fillBottom),
                        startY = topPad,
                        endY = topPad + graphHeight,
                    ),
                )

                // Colored straight segments (good/bad vs target); curve fill keeps the soft look.
                for (i in 1 until pts.size) {
                    val endNet = dataPoints[i].second
                    drawLine(
                        color = dayColor(endNet),
                        start = pts[i - 1],
                        end = pts[i],
                        strokeWidth = 7f,
                        cap = StrokeCap.Round,
                    )
                }
            }

            pts.forEachIndexed { idx, point ->
                val net = dataPoints[idx].second
                val color = dayColor(net)
                val isSelected = idx == selectedIndex
                if (isSelected) {
                    drawLine(
                        color = color.copy(alpha = 0.35f),
                        start = Offset(point.x, topPad),
                        end = Offset(point.x, topPad + graphHeight),
                        strokeWidth = 3f,
                    )
                }
                drawCircle(color = color, radius = if (isSelected) 14f else 8f, center = point)
                drawCircle(
                    color = markerCoreColor,
                    radius = if (isSelected) 5f else 3f,
                    center = point,
                )
            }
        }

        if (selectedIndex in dataPoints.indices && canvasSize.width > 0) {
            val pair = dataPoints[selectedIndex]
            val pos = pointOffset(selectedIndex)
            val net = pair.second
            val vsTarget = net - targetCalories
            val statusColor = dayColor(net)
            val statusText = when {
                vsTarget > 0 -> "Over by $vsTarget kcal"
                vsTarget < 0 -> "Under by ${-vsTarget} kcal"
                else -> "On target"
            }
            val bubbleMaxWidth = with(density) { 220.dp.toPx() }
            val bubbleApproxHeight = with(density) { 78.dp.toPx() }
            val x = (pos.x - bubbleMaxWidth / 2f)
                .coerceIn(0f, (canvasSize.width - bubbleMaxWidth).coerceAtLeast(0f))
            val y = (pos.y - bubbleApproxHeight - with(density) { 10.dp.toPx() })
                .coerceAtLeast(0f)

            Surface(
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        pair.first,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "$net kcal net",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

/**
 * Custom-drawn Stacked Bar Chart showcasing macronutrient trend breakdowns
 * (Protein, Carbs, Fats distribution stacked in a clean, vertical canvas).
 *
 * Prefer [WeekMacroBarChart] for Progress / Dashboard (calorie-height P/C/F share).
 */
@Composable
fun CustomStackedBarChart(
    foodSummaries: List<FoodDailySummary>,
    modifier: Modifier = Modifier
) {
    val days = remember(foodSummaries) {
        foodSummaries.asReversed().map { s ->
            WeekDayMacroBar(
                date = s.dateString,
                weekdayLabel = s.dateString.substringAfterLast('-'),
                calories = s.totalCalories,
                proteinG = s.totalProtein,
                carbsG = s.totalCarbs,
                fatsG = s.totalFats
            )
        }
    }
    var selectedDate by remember(days) {
        mutableStateOf(days.lastOrNull()?.date.orEmpty())
    }
    if (days.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No log data available yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    WeekMacroBarChart(
        days = days,
        selectedDate = selectedDate,
        onSelectDate = { selectedDate = it },
        showDayLabels = days.size <= 10,
        modifier = modifier
    )
}

/** One day of macros for the week-history stacked bar chart. */
@Immutable
data class WeekDayMacroBar(
    val date: String,
    val weekdayLabel: String,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatsG: Int
)

/**
 * Interactive macro chart: each bar’s height is total calories, colored by P/C/F share.
 * Scrub horizontally to select a day and show a floating macro popup.
 */
@Composable
fun WeekMacroBarChart(
    days: List<WeekDayMacroBar>,
    selectedDate: String,
    onSelectDate: (String) -> Unit,
    modifier: Modifier = Modifier,
    showDayLabels: Boolean = true,
    proteinColor: Color = MacroProteinColor,
    carbsColor: Color = MacroCarbsColor,
    fatsColor: Color = MacroFatsColor
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var chartSize by remember { mutableStateOf(Size.Zero) }
    var popupDay by remember { mutableStateOf<WeekDayMacroBar?>(null) }

    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val selectedHue = MaterialTheme.colorScheme.primary
    val emptyBarColor = MaterialTheme.colorScheme.surfaceVariant

    val maxCalories = remember(days) {
        days.maxOfOrNull { it.calories }?.coerceAtLeast(1)?.toFloat()?.times(1.15f) ?: 2000f
    }
    val paddingBottom = if (showDayLabels) 56f else 20f

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged {
                    chartSize = Size(it.width.toFloat(), it.height.toFloat())
                }
                .pointerInput(days) {
                    val paddingLeft = 56f
                    val paddingRight = 24f
                    fun selectAt(x: Float) {
                        val graphWidth = size.width - paddingLeft - paddingRight
                        if (days.isEmpty() || graphWidth <= 0f) return
                        val stepX = graphWidth / days.size
                        val index = ((x - paddingLeft) / stepX)
                            .toInt()
                            .coerceIn(0, days.lastIndex)
                        val day = days[index]
                        onSelectDate(day.date)
                        popupDay = day
                    }
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        selectAt(down.position.x)
                        drag(down.id) { change ->
                            selectAt(change.position.x)
                            change.consume()
                        }
                    }
                }
        ) {
            if (days.isEmpty()) return@Canvas

            val paddingLeft = 56f
            val paddingRight = 24f
            val paddingTop = 28f
            val graphWidth = size.width - paddingLeft - paddingRight
            val graphHeight = size.height - paddingTop - paddingBottom
            val stepX = graphWidth / days.size
            val barWidth = (stepX * 0.55f).coerceIn(8f, 56f)
            val corner = CornerRadius(12f, 12f)
            val barAlpha = 0.88f

            // Y-axis grid (calories)
            val gridCount = 3
            for (i in 0..gridCount) {
                val ratio = i.toFloat() / gridCount
                val y = paddingTop + graphHeight * (1f - ratio)
                drawLine(
                    color = gridColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 2f
                )
                val label = "${(maxCalories * ratio).toInt()}"
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(8f, y - 14f),
                    style = TextStyle(color = textColor, fontSize = 10.sp)
                )
            }

            days.forEachIndexed { idx, day ->
                val centerX = paddingLeft + idx * stepX + stepX / 2
                val isSelected = day.date == selectedDate
                val barHeight = if (day.calories <= 0) {
                    0f
                } else {
                    (day.calories / maxCalories) * graphHeight
                }
                val baseY = paddingTop + graphHeight
                val left = centerX - barWidth / 2

                if (isSelected) {
                    val glowH = barHeight.coerceAtLeast(6f)
                    drawRoundRect(
                        color = selectedHue.copy(alpha = 0.10f),
                        topLeft = Offset(left - 10f, baseY - glowH - 10f),
                        size = Size(barWidth + 20f, glowH + 20f),
                        cornerRadius = CornerRadius(18f, 18f)
                    )
                    drawRoundRect(
                        color = selectedHue.copy(alpha = 0.18f),
                        topLeft = Offset(left - 5f, baseY - glowH - 5f),
                        size = Size(barWidth + 10f, glowH + 10f),
                        cornerRadius = CornerRadius(14f, 14f)
                    )
                }

                if (barHeight <= 0f) {
                    val stub = 6f
                    drawRoundRect(
                        color = emptyBarColor.copy(alpha = barAlpha),
                        topLeft = Offset(left, baseY - stub),
                        size = Size(barWidth, stub),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                } else {
                    val pCal = day.proteinG * 4f
                    val cCal = day.carbsG * 4f
                    val fCal = day.fatsG * 9f
                    val macroSum = (pCal + cCal + fCal).coerceAtLeast(1f)

                    val clip = Path().apply {
                        addRoundRect(
                            RoundRect(
                                left = left,
                                top = baseY - barHeight,
                                right = left + barWidth,
                                bottom = baseY,
                                cornerRadius = corner
                            )
                        )
                    }
                    clipPath(clip) {
                        var currentY = baseY
                        val segments = listOf(
                            (pCal / macroSum) to proteinColor.copy(alpha = barAlpha),
                            (cCal / macroSum) to carbsColor.copy(alpha = barAlpha),
                            (fCal / macroSum) to fatsColor.copy(alpha = barAlpha)
                        )
                        segments.forEach { (frac, color) ->
                            val h = barHeight * frac
                            if (h <= 0f) return@forEach
                            val top = currentY - h
                            drawRect(
                                color = color,
                                topLeft = Offset(left, top),
                                size = Size(barWidth, h)
                            )
                            currentY = top
                        }
                    }
                }

                if (showDayLabels) {
                    val labelLayout = textMeasurer.measure(day.weekdayLabel)
                    drawText(
                        textMeasurer = textMeasurer,
                        text = day.weekdayLabel,
                        topLeft = Offset(
                            centerX - labelLayout.size.width / 2,
                            size.height - paddingBottom + 14f
                        ),
                        style = TextStyle(
                            color = if (isSelected) selectedHue else textColor,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }

        popupDay?.let { day ->
            val index = days.indexOfFirst { it.date == day.date }.coerceAtLeast(0)
            val paddingLeft = 56f
            val paddingRight = 24f
            val graphWidth = (chartSize.width - paddingLeft - paddingRight).coerceAtLeast(1f)
            val stepX = graphWidth / days.size.coerceAtLeast(1)
            val centerX = paddingLeft + index * stepX + stepX / 2
            val popupWidthPx = with(density) { 108.dp.toPx() }
            val x = (centerX - popupWidthPx / 2)
                .coerceIn(8f, (chartSize.width - popupWidthPx - 8f).coerceAtLeast(8f))
            val barH = if (day.calories <= 0 || maxCalories <= 0f) {
                0f
            } else {
                (day.calories / maxCalories) * (chartSize.height - 28f - paddingBottom)
            }
            val y = (28f + (chartSize.height - 28f - paddingBottom) - barH - with(density) { 52.dp.toPx() })
                .coerceAtLeast(4f)

            MacroFloatPopup(
                calories = day.calories,
                proteinG = day.proteinG,
                carbsG = day.carbsG,
                fatsG = day.fatsG,
                proteinColor = proteinColor,
                carbsColor = carbsColor,
                fatsColor = fatsColor,
                dateLabel = day.date.substringAfter("-"),
                onDismiss = { popupDay = null },
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
            )
        }
    }
}

@Composable
private fun MacroFloatPopup(
    calories: Int,
    proteinG: Int,
    carbsG: Int,
    fatsG: Int,
    proteinColor: Color,
    carbsColor: Color,
    fatsColor: Color,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dateLabel: String? = null
) {
    Surface(
        modifier = modifier.clickable(onClick = onDismiss),
        shape = RoundedCornerShape(10.dp),
        tonalElevation = 4.dp,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (dateLabel != null) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
            }
            Text(
                text = "$calories kcal",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroSwatch(proteinColor, "${proteinG}g")
                MacroSwatch(carbsColor, "${carbsG}g")
                MacroSwatch(fatsColor, "${fatsG}g")
            }
        }
    }
}

@Composable
private fun MacroSwatch(color: Color, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(8.dp)
                .height(8.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Generic single-series line chart for a body metric over time.
 * Points are spaced evenly; X labels are omitted. Scrub to see date + trend.
 * Line segments are green/red for improvement vs decline vs the previous reading.
 *
 * @param points chronological (dateLabel, value) pairs
 * @param decreaseIsPositive true when a drop is healthier (e.g. fat); false when a rise is
 *   healthier (e.g. muscle)
 */
@Composable
fun MetricLineChart(
    points: List<Pair<String, Double>>,
    unit: String,
    modifier: Modifier = Modifier,
    decreaseIsPositive: Boolean = true,
    targetValue: Double? = null,
    targetPreferHigher: Boolean = false,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    var selectedIndex by remember(points) { mutableIntStateOf(-1) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    if (points.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No readings yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val chartTarget = targetValue?.takeIf { it.isFinite() && it > 0.0 }
    val values = points.map { it.second }
    val boundsValues = chartTarget?.let { values + it } ?: values
    val rawMin = boundsValues.min()
    val rawMax = boundsValues.max()
    val span = (rawMax - rawMin).takeIf { it > 0.0 } ?: (if (rawMax != 0.0) rawMax * 0.1 else 1.0)
    val minVal = rawMin - span * 0.15
    val maxVal = rawMax + span * 0.15

    val gridLineColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val markerCoreColor = MaterialTheme.colorScheme.surface
    val targetLineColor = MaterialTheme.colorScheme.outline
    val cautionColor = Color(0xFFF59E0B)

    fun targetColor(value: Double): Color {
        val difference = value - (chartTarget ?: return lineColor)
        return when {
            kotlin.math.abs(difference) <= 1.0 -> TrendGreen
            targetPreferHigher && difference > 1.0 -> cautionColor
            !targetPreferHigher && difference < -1.0 -> cautionColor
            else -> TrendRed
        }
    }

    val leftPadPx = with(density) { 44.dp.toPx() }
    val rightPadPx = with(density) { 8.dp.toPx() }
    val topPadPx = with(density) { 12.dp.toPx() }
    val bottomPadPx = with(density) { 8.dp.toPx() }

    fun indexForX(x: Float): Int {
        val count = points.size
        if (count <= 1) return 0
        val plotW = (canvasSize.width - leftPadPx - rightPadPx).coerceAtLeast(1f)
        val t = ((x - leftPadPx) / plotW).coerceIn(0f, 1f)
        return (t * (count - 1)).roundToInt().coerceIn(0, count - 1)
    }

    fun pointOffset(index: Int): Offset {
        val count = points.size
        val plotW = (canvasSize.width - leftPadPx - rightPadPx).coerceAtLeast(1f)
        val plotH = (canvasSize.height - topPadPx - bottomPadPx).coerceAtLeast(1f)
        val range = (maxVal - minVal).coerceAtLeast(0.0001)
        val x = if (count <= 1) {
            leftPadPx + plotW / 2f
        } else {
            leftPadPx + plotW * (index.toFloat() / (count - 1).toFloat())
        }
        val y = topPadPx + plotH *
            (1f - ((points[index].second - minVal) / range).toFloat()).coerceIn(0f, 1f)
        return Offset(x, y)
    }

    Box(
        modifier = modifier
            .onSizeChanged { canvasSize = it }
            .pointerInput(points) {
                detectTapGestures { pos -> selectedIndex = indexForX(pos.x) }
            }
            .pointerInput(points) {
                detectDragGestures(
                    onDragStart = { pos -> selectedIndex = indexForX(pos.x) },
                    onDrag = { change, _ ->
                        selectedIndex = indexForX(change.position.x)
                        change.consume()
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val leftPad = leftPadPx
            val rightPad = rightPadPx
            val topPad = topPadPx
            val bottomPad = bottomPadPx
            val graphWidth = size.width - leftPad - rightPad
            val graphHeight = size.height - topPad - bottomPad
            if (graphWidth <= 0f || graphHeight <= 0f) return@Canvas

            val range = (maxVal - minVal).coerceAtLeast(0.0001)
            val stepX = if (points.size <= 1) 0f else graphWidth / (points.size - 1)

            val gridCount = 4
            for (i in 0..gridCount) {
                val ratio = i.toFloat() / gridCount
                val y = topPad + graphHeight * (1f - ratio)
                val valueLabel = formatMetric(minVal + range * ratio)
                drawLine(
                    color = gridLineColor,
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = valueLabel,
                    topLeft = Offset(10f, y - 20f),
                    style = TextStyle(color = textColor, fontSize = 10.sp),
                )
            }

            chartTarget?.let { target ->
                val targetY = topPad + graphHeight *
                    (1f - ((target - minVal) / range).toFloat()).coerceIn(0f, 1f)
                drawLine(
                    color = targetLineColor,
                    start = Offset(leftPad, targetY),
                    end = Offset(size.width - rightPad, targetY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f),
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = "Target · ${formatMetric(target)} kg",
                    topLeft = Offset(leftPad + 6f, targetY - 28f),
                    style = TextStyle(
                        color = targetLineColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }

            val pts = points.mapIndexed { idx, pair ->
                val x = if (points.size <= 1) leftPad + graphWidth / 2f else leftPad + idx * stepX
                val y = topPad + graphHeight *
                    (1f - ((pair.second - minVal) / range).toFloat()).coerceIn(0f, 1f)
                Offset(x, y)
            }

            val fillPath = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
                lineTo(pts.last().x, topPad + graphHeight)
                lineTo(pts.first().x, topPad + graphHeight)
                close()
            }
            drawPath(
                path = fillPath,
                color = lineColor.copy(alpha = 0.14f),
            )

            val strokeWidth = 8f
            for (i in 1 until pts.size) {
                val segmentColor = if (chartTarget != null) {
                    targetColor(points[i].second)
                } else {
                    metricStepTrend(
                        points[i - 1].second,
                        points[i].second,
                        decreaseIsPositive,
                    )?.color() ?: lineColor
                }
                drawLine(
                    color = segmentColor,
                    start = pts[i - 1],
                    end = pts[i],
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }

            pts.forEachIndexed { idx, point ->
                val isSelected = idx == selectedIndex
                val pointColor = if (chartTarget != null) {
                    targetColor(points[idx].second)
                } else {
                    lineColor
                }
                if (isSelected) {
                    drawLine(
                        color = pointColor.copy(alpha = 0.35f),
                        start = Offset(point.x, topPad),
                        end = Offset(point.x, topPad + graphHeight),
                        strokeWidth = 3f,
                    )
                }
                drawCircle(
                    color = pointColor,
                    radius = if (isSelected) 14f else 8f,
                    center = point
                )
                drawCircle(
                    color = markerCoreColor,
                    radius = if (isSelected) 5f else 3f,
                    center = point,
                )
            }
        }

        if (selectedIndex in points.indices && canvasSize.width > 0) {
            val pair = points[selectedIndex]
            val pos = pointOffset(selectedIndex)
            val prev = points.getOrNull(selectedIndex - 1)
            val delta = prev?.let { pair.second - it.second }
            val trend = prev?.let {
                metricStepTrend(it.second, pair.second, decreaseIsPositive)
            }
            val targetDifference = chartTarget?.let { pair.second - it }
            val targetStatusColor = chartTarget?.let { targetColor(pair.second) }
            val targetStatusText = targetDifference?.let { difference ->
                when {
                    kotlin.math.abs(difference) < 0.05 -> "On target"
                    difference > 0.0 -> "Above target by ${formatMetric(difference)} kg"
                    else -> "Below target by ${formatMetric(-difference)} kg"
                }
            }
            val valueText = "${formatMetric(pair.second)}$unit"
            val bubbleMaxWidth = with(density) { 220.dp.toPx() }
            val bubbleApproxHeight = with(density) { 78.dp.toPx() }
            val x = (pos.x - bubbleMaxWidth / 2f)
                .coerceIn(0f, (canvasSize.width - bubbleMaxWidth).coerceAtLeast(0f))
            val y = (pos.y - bubbleApproxHeight - with(density) { 10.dp.toPx() })
                .coerceAtLeast(0f)

            Surface(
                modifier = Modifier
                    .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                    .padding(horizontal = 4.dp),
                shape = RoundedCornerShape(12.dp),
                tonalElevation = 3.dp,
                shadowElevation = 4.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        pair.first,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        valueText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (targetStatusText != null && targetStatusColor != null) {
                        Text(
                            targetStatusText,
                            style = MaterialTheme.typography.labelMedium,
                            color = targetStatusColor,
                            fontWeight = FontWeight.Medium,
                        )
                    } else if (delta != null && trend != null) {
                        val rose = delta > 0.0
                        // Direction of change (not health judgment — color carries that).
                        val hint = if (rose) "Increased" else "Decreased"
                        val sign = if (delta > 0) "+" else ""
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Icon(
                                imageVector = if (rose) {
                                    Icons.Filled.ArrowDropUp
                                } else {
                                    Icons.Filled.ArrowDropDown
                                },
                                contentDescription = null,
                                tint = trend.color(),
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                "$hint · $sign${formatMetric(delta)}$unit",
                                style = MaterialTheme.typography.labelMedium,
                                color = trend.color(),
                            )
                        }
                    } else if (selectedIndex == 0) {
                        Text(
                            "First reading in range",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private val TrendGreen = Color(0xFF22C55E)
private val TrendRed = Color(0xFFEF4444)

private enum class MetricStepTrend { Improved, Worsened }

private fun MetricStepTrend.color(): Color = when (this) {
    MetricStepTrend.Improved -> TrendGreen
    MetricStepTrend.Worsened -> TrendRed
}

private fun metricStepTrend(
    prev: Double,
    curr: Double,
    decreaseIsPositive: Boolean,
): MetricStepTrend? {
    val delta = curr - prev
    if (kotlin.math.abs(delta) < 1e-4) return null
    val improved = if (delta < 0.0) decreaseIsPositive else !decreaseIsPositive
    return if (improved) MetricStepTrend.Improved else MetricStepTrend.Worsened
}

/** Trims trailing ".0" so whole numbers read cleanly while decimals keep one place. */
private fun formatMetric(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

/** A calendar day rendered in the contribution-style calories-burned heatmap. */
@Immutable
private data class CaloriesHeatmapDay(
    val date: String,
    val calories: Int,
    val level: Int
)

@Immutable
private data class CaloriesHeatmapMonthLabel(
    val weekIndex: Int,
    val label: String
)

@Immutable
private data class CaloriesHeatmapGrid(
    val cells: List<CaloriesHeatmapDay?>,
    val days: List<CaloriesHeatmapDay>,
    val weekCount: Int,
    val monthLabels: List<CaloriesHeatmapMonthLabel>
)

private fun buildCaloriesHeatmapGrid(
    summaries: List<ExerciseDailySummary>,
    rangeStart: String,
    rangeEnd: String
): CaloriesHeatmapGrid {
    if (rangeStart > rangeEnd || rangeStart.length < 7 || rangeEnd.length < 7) {
        return CaloriesHeatmapGrid(emptyList(), emptyList(), 0, emptyList())
    }

    val caloriesByDate = summaries
        .asSequence()
        .filter { it.dateString in rangeStart..rangeEnd }
        .associate { it.dateString to it.totalBurned.coerceAtLeast(0) }
    val positiveValues = caloriesByDate.values.filter { it > 0 }.distinct().sorted()

    fun levelFor(calories: Int): Int {
        if (calories <= 0) return 0
        if (positiveValues.size <= 1) return 4
        val rank = positiveValues.binarySearch(calories).coerceAtLeast(0)
        return 1 + rank * 3 / positiveValues.lastIndex
    }

    val days = buildList {
        var date = rangeStart
        while (date <= rangeEnd) {
            val calories = caloriesByDate[date] ?: 0
            add(CaloriesHeatmapDay(date, calories, levelFor(calories)))
            date = DateUtils.addDays(date, 1)
        }
    }
    val cells = mutableListOf<CaloriesHeatmapDay?>()
    val monthLabels = mutableListOf<CaloriesHeatmapMonthLabel>()
    var month = rangeStart.take(7)
    val endMonth = rangeEnd.take(7)
    while (month <= endMonth) {
        val monthDays = days.filter { it.date.startsWith(month) }
        if (monthDays.isNotEmpty()) {
            monthLabels += CaloriesHeatmapMonthLabel(
                weekIndex = cells.size / 7,
                label = DateUtils.monthLabel(month).substringBefore(" ")
            )
            val leadingCells = Calendar.getInstance().run {
                time = DateUtils.parse(monthDays.first().date)
                get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY
            }
            repeat(leadingCells) { cells += null }
            cells += monthDays
            val trailingCells = (7 - cells.size % 7) % 7
            repeat(trailingCells) { cells += null }
        }
        month = DateUtils.addMonths(month, 1)
    }
    return CaloriesHeatmapGrid(
        cells = cells,
        days = days,
        weekCount = cells.size / 7,
        monthLabels = monthLabels
    )
}

/** GitHub-style contribution calendar with weeks in one horizontally scrollable strip. */
@Composable
fun CaloriesBurnedHeatmap(
    summaries: List<ExerciseDailySummary>,
    rangeStart: String,
    rangeEnd: String,
    modifier: Modifier = Modifier
) {
    val grid = remember(summaries, rangeStart, rangeEnd) {
        buildCaloriesHeatmapGrid(summaries, rangeStart, rangeEnd)
    }
    var selectedDate by remember(rangeStart, rangeEnd) { mutableStateOf<String?>(null) }

    if (grid.weekCount == 0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No calendar days to show", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val colorScheme = MaterialTheme.colorScheme
    val baseColor = colorScheme.surfaceContainerHighest
    val heatColors = listOf(
        androidx.compose.ui.graphics.lerp(baseColor, colorScheme.onSurface, 0.12f),
        androidx.compose.ui.graphics.lerp(baseColor, colorScheme.primary, 0.28f),
        androidx.compose.ui.graphics.lerp(baseColor, colorScheme.primary, 0.48f),
        androidx.compose.ui.graphics.lerp(baseColor, colorScheme.primary, 0.72f),
        colorScheme.primary
    )
    val selectedDay = selectedDate?.let { date -> grid.days.firstOrNull { it.date == date } }
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val cellSize = 11.dp
    val cellGap = 3.dp
    val cellStride = cellSize + cellGap
    val monthLabelHeight = 20.dp
    val canvasWidth = cellStride * grid.weekCount
    val canvasHeight = monthLabelHeight + cellStride * 7
    val cellSizePx = with(density) { cellSize.toPx() }
    val cellStridePx = with(density) { cellStride.toPx() }
    val monthLabelHeightPx = with(density) { monthLabelHeight.toPx() }
    val cornerRadiusPx = with(density) { 2.dp.toPx() }
    val selectedStrokePx = with(density) { 1.5.dp.toPx() }
    val monthLabelStyle = MaterialTheme.typography.labelSmall.copy(
        color = colorScheme.onSurfaceVariant,
        fontSize = 9.sp
    )
    val accessibilityActions = grid.days.map { day ->
        CustomAccessibilityAction(
            label = "${DateUtils.displayDateSubtitle(day.date)}, " +
                "${day.calories} calories burned"
        ) {
            selectedDate = if (selectedDate == day.date) null else day.date
            true
        }
    }

    LaunchedEffect(scrollState.maxValue, rangeEnd) {
        if (scrollState.maxValue > 0) scrollState.scrollTo(scrollState.maxValue)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.width(26.dp)) {
                Spacer(Modifier.height(monthLabelHeight))
                listOf("", "Mon", "", "Wed", "", "Fri", "").forEach { label ->
                    Box(
                        modifier = Modifier
                            .width(26.dp)
                            .height(cellStride),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        if (label.isNotEmpty()) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
            ) {
                Canvas(
                    modifier = Modifier
                        .width(canvasWidth)
                        .height(canvasHeight)
                        .semantics {
                            contentDescription = "Calories burned contribution calendar"
                            stateDescription = selectedDay?.let {
                                "Selected ${DateUtils.displayDateSubtitle(it.date)}, " +
                                    "${it.calories} calories burned"
                            } ?: "No day selected"
                            customActions = accessibilityActions
                        }
                        .pointerInput(grid.cells, selectedDate) {
                            detectTapGestures { tap ->
                                val localY = tap.y - monthLabelHeightPx
                                if (tap.x < 0f || localY < 0f) return@detectTapGestures
                                val week = (tap.x / cellStridePx).toInt()
                                val weekday = (localY / cellStridePx).toInt()
                                if (week !in 0 until grid.weekCount || weekday !in 0..6) {
                                    return@detectTapGestures
                                }
                                grid.cells[week * 7 + weekday]?.let { day ->
                                    selectedDate = if (selectedDate == day.date) null else day.date
                                }
                            }
                        }
                ) {
                    grid.monthLabels.forEach { month ->
                        val measured = textMeasurer.measure(month.label, monthLabelStyle)
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(month.weekIndex * cellStridePx, 0f)
                        )
                    }
                    grid.cells.forEachIndexed { index, day ->
                        day ?: return@forEachIndexed
                        val week = index / 7
                        val weekday = index % 7
                        val topLeft = Offset(
                            x = week * cellStridePx,
                            y = monthLabelHeightPx + weekday * cellStridePx
                        )
                        drawRoundRect(
                            color = heatColors[day.level],
                            topLeft = topLeft,
                            size = Size(cellSizePx, cellSizePx),
                            cornerRadius = CornerRadius(cornerRadiusPx)
                        )
                        if (day.date == selectedDate) {
                            drawRoundRect(
                                color = colorScheme.onSurface,
                                topLeft = topLeft,
                                size = Size(cellSizePx, cellSizePx),
                                cornerRadius = CornerRadius(cornerRadiusPx),
                                style = Stroke(width = selectedStrokePx)
                            )
                        }
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = selectedDay?.let {
                        "${DateUtils.displayDateSubtitle(it.date)}, ${it.calories} calories burned"
                    } ?: "No heatmap day selected"
                },
            shape = RoundedCornerShape(10.dp),
            color = baseColor
        ) {
            if (selectedDay == null) {
                Text(
                    text = "Swipe the calendar and tap a square for details",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
            } else {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = DateUtils.displayDateSubtitle(selectedDay.date),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${selectedDay.calories} kcal",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Less",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
            heatColors.forEach { color ->
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
            Text(
                text = "More",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}
