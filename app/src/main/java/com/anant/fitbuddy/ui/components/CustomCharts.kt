package com.anant.fitbuddy.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anant.fitbuddy.data.database.ExerciseDailySummary
import com.anant.fitbuddy.data.database.FoodDailySummary
import kotlin.math.max
import kotlin.math.min

@Composable
fun CalorieRing(
    progress: Float, // 0.0f to 1.0f or more if exceeding
    centerText: String,
    subText: String,
    modifier: Modifier = Modifier,
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
 * A highly interactive custom line chart that draws net calories consumed
 * with smooth curves, background grid lines, and interactive tap detection.
 */
@Composable
fun CustomLineChart(
    foodSummaries: List<FoodDailySummary>,
    exerciseSummaries: List<ExerciseDailySummary>,
    targetCalories: Int,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember { mutableStateOf(-1) }

    // Combine into dates and net calorie values: Food - Exercise
    val dataPoints = remember(foodSummaries, exerciseSummaries) {
        val exerciseMap = exerciseSummaries.associate { it.dateString to it.totalBurned }
        foodSummaries.map { f ->
            val burned = exerciseMap[f.dateString] ?: 0
            val net = f.totalCalories - burned
            val label = f.dateString.substringAfter("-") // e.g. "07-17"
            Pair(label, net)
        }.reversed() // chronological order
    }

    if (dataPoints.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No log data available yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxVal = max(targetCalories, dataPoints.maxOf { it.second }).toFloat() * 1.2f
    val minVal = 0f

    val gridLineColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val targetLineColor = MaterialTheme.colorScheme.error
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    // Hoisted out of the Canvas: MaterialTheme is composable-only and cannot be read
    // inside the (non-composable) DrawScope lambda below.
    val tooltipBgColor = MaterialTheme.colorScheme.primaryContainer
    val tooltipTextColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(dataPoints) {
                    detectTapGestures { offset ->
                        val paddingLeft = 120f
                        val paddingRight = 40f
                        val graphWidth = size.width - paddingLeft - paddingRight
                        val stepX = graphWidth / (dataPoints.size - 1).coerceAtLeast(1)

                        val relativeX = offset.x - paddingLeft
                        val index = (relativeX / stepX + 0.5f).toInt()
                        if (index in dataPoints.indices) {
                            selectedIndex = index
                        }
                    }
                }
        ) {
            val paddingLeft = 120f
            val paddingRight = 40f
            val paddingTop = 60f
            val paddingBottom = 80f

            val graphWidth = size.width - paddingLeft - paddingRight
            val graphHeight = size.height - paddingTop - paddingBottom

            val stepX = graphWidth / (dataPoints.size - 1).coerceAtLeast(1)

            // 1. Draw horizontal grid lines & Y-axis labels
            val gridCount = 4
            for (i in 0..gridCount) {
                val ratio = i.toFloat() / gridCount
                val y = paddingTop + graphHeight * (1f - ratio)
                val valueLabel = (minVal + (maxVal - minVal) * ratio).toInt().toString()

                drawLine(
                    color = gridLineColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = valueLabel,
                    topLeft = Offset(10f, y - 20f),
                    style = TextStyle(color = textColor, fontSize = 10.sp)
                )
            }

            // 2. Draw Target Calories baseline
            val targetY = paddingTop + graphHeight * (1f - (targetCalories - minVal) / (maxVal - minVal))
            drawLine(
                color = targetLineColor,
                start = Offset(paddingLeft, targetY),
                end = Offset(size.width - paddingRight, targetY),
                strokeWidth = 3f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = "Target ($targetCalories)",
                topLeft = Offset(paddingLeft + 10f, targetY - 35f),
                style = TextStyle(color = targetLineColor, fontSize = 10.sp)
            )

            // 3. Construct Path for values
            val path = Path()
            val points = dataPoints.mapIndexed { idx, pair ->
                val x = paddingLeft + idx * stepX
                val value = pair.second.coerceAtLeast(0)
                val y = paddingTop + graphHeight * (1f - (value - minVal) / (maxVal - minVal))
                Offset(x, y)
            }

            if (points.isNotEmpty()) {
                path.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val pPrev = points[i - 1]
                    val pCurr = points[i]
                    val controlX1 = pPrev.x + (pCurr.x - pPrev.x) / 2
                    val controlY1 = pPrev.y
                    val controlX2 = pPrev.x + (pCurr.x - pPrev.x) / 2
                    val controlY2 = pCurr.y

                    path.cubicTo(controlX1, controlY1, controlX2, controlY2, pCurr.x, pCurr.y)
                }

                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 8f, cap = StrokeCap.Round)
                )
            }

            // 4. Draw node points and bottom labels
            points.forEachIndexed { idx, point ->
                val isSelected = idx == selectedIndex
                drawCircle(
                    color = if (isSelected) targetLineColor else primaryColor,
                    radius = if (isSelected) 14f else 8f,
                    center = point
                )

                // Date label below
                val dateLabel = dataPoints[idx].first
                val labelWidth = textMeasurer.measure(dateLabel).size.width
                drawText(
                    textMeasurer = textMeasurer,
                    text = dateLabel,
                    topLeft = Offset(point.x - labelWidth / 2, size.height - paddingBottom + 15f),
                    style = TextStyle(color = textColor, fontSize = 10.sp)
                )
            }

            // 5. Draw overlay tooltip
            if (selectedIndex in dataPoints.indices) {
                val point = points[selectedIndex]
                val pair = dataPoints[selectedIndex]
                val tooltipText = "${pair.first}: ${pair.second} kcal"
                val textLayoutResult = textMeasurer.measure(tooltipText)
                val textWidth = textLayoutResult.size.width
                val tooltipX = (point.x - textWidth / 2).coerceIn(paddingLeft, size.width - paddingRight - textWidth)

                drawRect(
                    color = tooltipBgColor,
                    topLeft = Offset(tooltipX - 10f, point.y - 70f),
                    size = Size(textWidth + 20f, 50f)
                )

                drawText(
                    textMeasurer = textMeasurer,
                    text = tooltipText,
                    topLeft = Offset(tooltipX, point.y - 62f),
                    style = TextStyle(color = tooltipTextColor, fontSize = 11.sp)
                )
            }
        }
    }
}

/**
 * Custom-drawn Stacked Bar Chart showcasing macronutrient trend breakdowns
 * (Protein, Carbs, Fats distribution stacked in a clean, vertical canvas).
 */
@Composable
fun CustomStackedBarChart(
    foodSummaries: List<FoodDailySummary>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val dataPoints = remember(foodSummaries) {
        foodSummaries.reversed() // Chronological order
    }

    if (dataPoints.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No log data available yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    // Color Palette
    val pColor = Color(0xFFFF7043) // Protein - Orange
    val cColor = Color(0xFF26A69A) // Carbs - Teal
    val fColor = Color(0xFFFFCA28) // Fats - Amber
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    // Hoisted out of the Canvas (see note in CustomLineChart).
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Canvas(modifier = modifier) {
        val paddingLeft = 100f
        val paddingRight = 40f
        val paddingTop = 40f
        val paddingBottom = 80f

        val graphWidth = size.width - paddingLeft - paddingRight
        val graphHeight = size.height - paddingTop - paddingBottom

        val barCount = dataPoints.size
        val barWidth = (graphWidth / barCount * 0.6f).coerceIn(20f, 100f)
        val stepX = graphWidth / barCount.coerceAtLeast(1)

        // Find max total macro grams
        val maxGrams = dataPoints.maxOf { it.totalProtein + it.totalCarbs + it.totalFats }.toFloat().coerceAtLeast(100f) * 1.1f

        // Draw basic background Y metric indicators
        val gridCount = 3
        for (i in 0..gridCount) {
            val ratio = i.toFloat() / gridCount
            val y = paddingTop + graphHeight * (1f - ratio)
            val valLabel = "${(maxGrams * ratio).toInt()}g"

            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(size.width - paddingRight, y),
                strokeWidth = 2f
            )

            drawText(
                textMeasurer = textMeasurer,
                text = valLabel,
                topLeft = Offset(10f, y - 20f),
                style = TextStyle(color = textColor, fontSize = 10.sp)
            )
        }

        // Draw stacked bars
        dataPoints.forEachIndexed { idx, entry ->
            val centerX = paddingLeft + idx * stepX + stepX / 2

            val pG = entry.totalProtein.toFloat()
            val cG = entry.totalCarbs.toFloat()
            val fG = entry.totalFats.toFloat()

            // Scale Heights
            val pHeight = (pG / maxGrams) * graphHeight
            val cHeight = (cG / maxGrams) * graphHeight
            val fHeight = (fG / maxGrams) * graphHeight

            var currentY = paddingTop + graphHeight

            // Draw Protein (Bottom layer)
            if (pHeight > 0) {
                drawRect(
                    color = pColor,
                    topLeft = Offset(centerX - barWidth / 2, currentY - pHeight),
                    size = Size(barWidth, pHeight)
                )
                currentY -= pHeight
            }

            // Draw Carbs (Middle layer)
            if (cHeight > 0) {
                drawRect(
                    color = cColor,
                    topLeft = Offset(centerX - barWidth / 2, currentY - cHeight),
                    size = Size(barWidth, cHeight)
                )
                currentY -= cHeight
            }

            // Draw Fats (Top layer)
            if (fHeight > 0) {
                drawRect(
                    color = fColor,
                    topLeft = Offset(centerX - barWidth / 2, currentY - fHeight),
                    size = Size(barWidth, fHeight)
                )
            }

            // Date label
            val dateLabel = entry.dateString.substringAfter("-")
            val labelLayout = textMeasurer.measure(dateLabel)
            drawText(
                textMeasurer = textMeasurer,
                text = dateLabel,
                topLeft = Offset(centerX - labelLayout.size.width / 2, size.height - paddingBottom + 15f),
                style = TextStyle(color = textColor, fontSize = 10.sp)
            )
        }
    }
}

/**
 * Generic single-series line chart for a body metric over time (weight, body fat %, muscle mass,
 * etc.). Y-axis auto-scales to the data range. Callers pass only non-null readings as
 * (dateLabel, value) pairs in chronological order.
 */
@Composable
fun MetricLineChart(
    points: List<Pair<String, Double>>,
    unit: String,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary
) {
    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember(points) { mutableStateOf(-1) }

    if (points.size < 2) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text(
                if (points.isEmpty()) "No readings yet" else "Add another reading to see a trend",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val values = points.map { it.second }
    val rawMin = values.min()
    val rawMax = values.max()
    // Pad the range by 10% (or a flat amount when all values are equal) so the line isn't clipped.
    val span = (rawMax - rawMin).takeIf { it > 0.0 } ?: (if (rawMax != 0.0) rawMax * 0.1 else 1.0)
    val minVal = rawMin - span * 0.15
    val maxVal = rawMax + span * 0.15

    val gridLineColor = MaterialTheme.colorScheme.outlineVariant
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipBgColor = MaterialTheme.colorScheme.primaryContainer
    val tooltipTextColor = MaterialTheme.colorScheme.onPrimaryContainer

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val paddingLeft = 120f
                        val paddingRight = 40f
                        val graphWidth = size.width - paddingLeft - paddingRight
                        val stepX = graphWidth / (points.size - 1).coerceAtLeast(1)
                        val index = ((offset.x - paddingLeft) / stepX + 0.5f).toInt()
                        if (index in points.indices) selectedIndex = index
                    }
                }
        ) {
            val paddingLeft = 120f
            val paddingRight = 40f
            val paddingTop = 40f
            val paddingBottom = 80f
            val graphWidth = size.width - paddingLeft - paddingRight
            val graphHeight = size.height - paddingTop - paddingBottom
            val stepX = graphWidth / (points.size - 1).coerceAtLeast(1)
            val range = (maxVal - minVal).coerceAtLeast(0.0001)

            val gridCount = 4
            for (i in 0..gridCount) {
                val ratio = i.toFloat() / gridCount
                val y = paddingTop + graphHeight * (1f - ratio)
                val valueLabel = formatMetric(minVal + range * ratio)
                drawLine(
                    color = gridLineColor,
                    start = Offset(paddingLeft, y),
                    end = Offset(size.width - paddingRight, y),
                    strokeWidth = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = valueLabel,
                    topLeft = Offset(10f, y - 20f),
                    style = TextStyle(color = textColor, fontSize = 10.sp)
                )
            }

            val pts = points.mapIndexed { idx, pair ->
                val x = paddingLeft + idx * stepX
                val y = paddingTop + graphHeight * (1f - ((pair.second - minVal) / range).toFloat())
                Offset(x, y)
            }

            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) {
                    val prev = pts[i - 1]
                    val curr = pts[i]
                    val midX = prev.x + (curr.x - prev.x) / 2
                    cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                }
            }
            drawPath(path = path, color = lineColor, style = Stroke(width = 8f, cap = StrokeCap.Round))

            pts.forEachIndexed { idx, point ->
                val isSelected = idx == selectedIndex
                drawCircle(
                    color = lineColor,
                    radius = if (isSelected) 14f else 8f,
                    center = point
                )
                val dateLabel = points[idx].first
                val labelWidth = textMeasurer.measure(dateLabel).size.width
                drawText(
                    textMeasurer = textMeasurer,
                    text = dateLabel,
                    topLeft = Offset(point.x - labelWidth / 2, size.height - paddingBottom + 15f),
                    style = TextStyle(color = textColor, fontSize = 10.sp)
                )
            }

            if (selectedIndex in points.indices) {
                val point = pts[selectedIndex]
                val tooltipText = "${points[selectedIndex].first}: ${formatMetric(points[selectedIndex].second)}$unit"
                val layout = textMeasurer.measure(tooltipText)
                val textWidth = layout.size.width
                val tooltipX = (point.x - textWidth / 2)
                    .coerceIn(paddingLeft, size.width - paddingRight - textWidth)
                drawRect(
                    color = tooltipBgColor,
                    topLeft = Offset(tooltipX - 10f, point.y - 70f),
                    size = Size(textWidth + 20f, 50f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = tooltipText,
                    topLeft = Offset(tooltipX, point.y - 62f),
                    style = TextStyle(color = tooltipTextColor, fontSize = 11.sp)
                )
            }
        }
    }
}

/** Trims trailing ".0" so whole numbers read cleanly while decimals keep one place. */
private fun formatMetric(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

/**
 * Simple vertical bar chart for a single integer series over time (e.g. daily calories burned).
 * Values are passed as (dateLabel, value) pairs in chronological order.
 */
@Composable
fun CustomBarChart(
    values: List<Pair<String, Int>>,
    unit: String,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.tertiary
) {
    val textMeasurer = rememberTextMeasurer()
    var selectedIndex by remember(values) { mutableStateOf(-1) }

    if (values.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No exercise logged yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val tooltipBgColor = MaterialTheme.colorScheme.tertiaryContainer
    val tooltipTextColor = MaterialTheme.colorScheme.onTertiaryContainer

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(values) {
                    detectTapGestures { offset ->
                        val paddingLeft = 100f
                        val paddingRight = 40f
                        val graphWidth = size.width - paddingLeft - paddingRight
                        val stepX = graphWidth / values.size.coerceAtLeast(1)
                        val index = ((offset.x - paddingLeft) / stepX).toInt()
                        if (index in values.indices) selectedIndex = index
                    }
                }
        ) {
            val paddingLeft = 100f
            val paddingRight = 40f
            val paddingTop = 40f
            val paddingBottom = 80f
            val graphWidth = size.width - paddingLeft - paddingRight
            val graphHeight = size.height - paddingTop - paddingBottom
            val barCount = values.size
            val barWidth = (graphWidth / barCount * 0.6f).coerceIn(16f, 100f)
            val stepX = graphWidth / barCount.coerceAtLeast(1)
            val maxVal = values.maxOf { it.second }.toFloat().coerceAtLeast(1f) * 1.1f

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
                drawText(
                    textMeasurer = textMeasurer,
                    text = (maxVal * ratio).toInt().toString(),
                    topLeft = Offset(10f, y - 20f),
                    style = TextStyle(color = textColor, fontSize = 10.sp)
                )
            }

            values.forEachIndexed { idx, entry ->
                val centerX = paddingLeft + idx * stepX + stepX / 2
                val barHeight = (entry.second / maxVal) * graphHeight
                val top = paddingTop + graphHeight - barHeight
                val isSelected = idx == selectedIndex
                drawRect(
                    color = if (isSelected) tooltipBgColor else barColor,
                    topLeft = Offset(centerX - barWidth / 2, top),
                    size = Size(barWidth, barHeight)
                )
                val dateLabel = entry.first
                val labelLayout = textMeasurer.measure(dateLabel)
                drawText(
                    textMeasurer = textMeasurer,
                    text = dateLabel,
                    topLeft = Offset(centerX - labelLayout.size.width / 2, size.height - paddingBottom + 15f),
                    style = TextStyle(color = textColor, fontSize = 10.sp)
                )
            }

            if (selectedIndex in values.indices) {
                val entry = values[selectedIndex]
                val centerX = paddingLeft + selectedIndex * stepX + stepX / 2
                val barHeight = (entry.second / maxVal) * graphHeight
                val top = paddingTop + graphHeight - barHeight
                val tooltipText = "${entry.first}: ${entry.second}$unit"
                val layout = textMeasurer.measure(tooltipText)
                val textWidth = layout.size.width
                val tooltipX = (centerX - textWidth / 2)
                    .coerceIn(paddingLeft, size.width - paddingRight - textWidth)
                val tooltipY = min(top - 60f, graphHeight)
                drawRect(
                    color = tooltipBgColor,
                    topLeft = Offset(tooltipX - 10f, tooltipY),
                    size = Size(textWidth + 20f, 44f)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = tooltipText,
                    topLeft = Offset(tooltipX, tooltipY + 12f),
                    style = TextStyle(color = tooltipTextColor, fontSize = 11.sp)
                )
            }
        }
    }
}
