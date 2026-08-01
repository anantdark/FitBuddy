package com.anant.fitbuddy.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import com.anant.fitbuddy.ui.components.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.anant.fitbuddy.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import com.anant.fitbuddy.ui.components.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.ExerciseDailySummary
import com.anant.fitbuddy.data.database.FoodDailySummary
import com.anant.fitbuddy.ui.components.CalorieRing
import com.anant.fitbuddy.ui.components.CustomBarChart
import com.anant.fitbuddy.ui.components.CustomLineChart
import com.anant.fitbuddy.ui.components.CustomStackedBarChart
import com.anant.fitbuddy.ui.components.MacroCarbsColor
import com.anant.fitbuddy.ui.components.MacroFatsColor
import com.anant.fitbuddy.ui.components.MacroProteinColor
import com.anant.fitbuddy.ui.components.MetricLineChart
import com.anant.fitbuddy.ui.viewmodel.ProgressInsightUiState
import com.anant.fitbuddy.util.DateUtils
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

private val ProteinColor = MacroProteinColor
private val CarbsColor = MacroCarbsColor
private val FatsColor = MacroFatsColor

/** Body composition charts always use this many newest readings (not week/month range). */
private const val BODY_COMPOSITION_READING_LIMIT = 15

private data class BodyMetric(
    val label: String,
    val unit: String,
    val extractor: (BodyMeasurement) -> Double?
)

private val BODY_METRICS = listOf(
    BodyMetric("Weight", " kg") { it.weightKg },
    BodyMetric("BMI", "") { it.bmi },
    BodyMetric("Body fat", "%") { it.bodyFatPct },
    BodyMetric("Muscle rate", "%") { it.muscleRatePct },
    BodyMetric("Body water", "%") { it.bodyWaterPct },
    BodyMetric("Muscle mass", " kg") { it.muscleMassKg },
    BodyMetric("Fat mass", " kg") { it.fatMassKg },
    BodyMetric("Bone mass", " kg") { it.boneMassKg },
    BodyMetric("BMR", " kcal") { it.bmr?.toDouble() },
    BodyMetric("Metabolic age", " yrs") { it.metabolicAge?.toDouble() },
    BodyMetric("Visceral fat", "%") { it.visceralFat },
    BodyMetric("Subcutaneous fat", "%") { it.subcutaneousFatPct },
    BodyMetric("Protein mass", " kg") { it.proteinMassKg },
    BodyMetric("Weight without fat", " kg") { it.fatFreeMassKg },
    BodyMetric("Skeletal muscle", " kg") { it.skeletalMuscleMassKg },
    BodyMetric("Water weight", " kg") { it.waterWeightKg }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    weeklyFood: List<FoodDailySummary>,
    monthlyFood: List<FoodDailySummary>,
    weeklyExercise: List<ExerciseDailySummary>,
    monthlyExercise: List<ExerciseDailySummary>,
    measurements: List<BodyMeasurement>,
    targetCalories: Int,
    monthlyEndDate: String,
    realToday: String,
    progressInsightState: ProgressInsightUiState,
    isAiConfigured: Boolean,
    onShiftMonth: (Int) -> Unit,
    onRequestInsight: () -> Unit,
    onOpenChat: () -> Unit,
    onDismissInsight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableIntStateOf(0) } // 0 = Weekly, 1 = Monthly
    val options = listOf("Weekly", "Monthly")

    val foodSummaries = if (selectedRange == 0) weeklyFood else monthlyFood
    val exerciseSummaries = if (selectedRange == 0) weeklyExercise else monthlyExercise
    val isLatestWindow = monthlyEndDate == realToday
    val rangeLabel = remember(monthlyEndDate, isLatestWindow) {
        if (isLatestWindow) "Last 30 days" else DateUtils.rolling30DayLabel(monthlyEndDate)
    }

    progressInsightState.summary?.let {
        ProgressInsightDialog(
            state = progressInsightState,
            onOpenChat = onOpenChat,
            onDismiss = onDismissInsight
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            InsightCard(
                state = progressInsightState,
                isAiConfigured = isAiConfigured,
                onRequestInsight = onRequestInsight
            )
        }

        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedRange == index,
                        onClick = { selectedRange = index },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size)
                    ) {
                        Text(label)
                    }
                }
            }
        }

        if (selectedRange == 1) {
            item {
                MonthRangeNavigator(
                    label = rangeLabel,
                    rangeSubtitle = if (isLatestWindow) {
                        DateUtils.rolling30DayLabel(monthlyEndDate)
                    } else {
                        null
                    },
                    canGoPrev = true,
                    canGoNext = !isLatestWindow,
                    onPrev = { onShiftMonth(-1) },
                    onNext = { onShiftMonth(1) }
                )
            }
        }

        item {
            ChartCard(title = "Net Calories vs Target") {
                CustomLineChart(
                    foodSummaries = foodSummaries,
                    exerciseSummaries = exerciseSummaries,
                    targetCalories = targetCalories,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }
        }

        item { ExerciseCard(exerciseSummaries = exerciseSummaries) }

        item {
            ChartCard(title = "Macronutrient Trend") {
                MacroLegend()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Bar height = calories · color share = P/C/F · scrub to inspect",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                CustomStackedBarChart(
                    foodSummaries = foodSummaries,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }
        }

        // Independent of Weekly/Monthly: always the most recent scale readings.
        item { BodyMetricCard(measurements = measurements) }
    }
}

@Composable
private fun MonthRangeNavigator(
    label: String,
    rangeSubtitle: String?,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onPrev, enabled = canGoPrev) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous 30 days",
                tint = if (canGoPrev) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (rangeSubtitle != null) {
                Text(
                    text = rangeSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next 30 days",
                tint = if (canGoNext) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BodyMetricCard(measurements: List<BodyMeasurement>) {
    val pagerState = rememberPagerState(pageCount = { BODY_METRICS.size })
    val scope = rememberCoroutineScope()

    ChartCard(title = "Body Composition") {
        // Tabs stay in sync with the pager: tap to jump straight to a metric, or swipe the chart
        // below to move between metrics one at a time.
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            divider = {}
        ) {
            BODY_METRICS.forEachIndexed { index, metric ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(metric.label) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) { page ->
            val metric = BODY_METRICS[page]
            // Last 15 readings (newest-first from Room), then oldest→newest for the chart.
            // Ignores the Weekly/Monthly toggle above.
            val points = remember(measurements, page) {
                measurements
                    .take(BODY_COMPOSITION_READING_LIMIT)
                    .asReversed()
                    .mapNotNull { m -> metric.extractor(m)?.let { m.dateString.substringAfter("-") to it } }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                if (points.isNotEmpty()) {
                    val latest = points.last().second
                    val first = points.first().second
                    val delta = latest - first
                    Text(
                        text = "Latest ${trim(latest)}${metric.unit}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (points.size >= 2) {
                        val sign = if (delta > 0) "+" else ""
                        Text(
                            text = "$sign${trim(delta)}${metric.unit} over last ${points.size} readings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                MetricLineChart(
                    points = points,
                    unit = metric.unit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(exerciseSummaries: List<ExerciseDailySummary>) {
    val values = remember(exerciseSummaries) {
        exerciseSummaries.asReversed().map { it.dateString.substringAfter("-") to it.totalBurned }
    }
    val totalBurned = exerciseSummaries.sumOf { it.totalBurned }
    val activeDays = exerciseSummaries.count { it.totalBurned > 0 }

    ChartCard(title = "Calories Burned") {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SummaryStat("Total", "$totalBurned")
            SummaryStat("Active days", "$activeDays")
            SummaryStat("Avg/day", if (activeDays > 0) "${totalBurned / activeDays}" else "0")
        }
        Spacer(Modifier.height(12.dp))
        CustomBarChart(
            values = values,
            unit = " kcal",
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun JapanRowingAnimation(boatProgress: Float, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "japan-rowing")

    // Wave offset scrolls continuously
    val waveOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave-offset"
    )

    // Oar angle oscillates (rowing stroke)
    val oarAngle by transition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "oar-angle"
    )

    // Arm angle follows oar
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
    val hatColor  = Color(0xFF5C3D1E)
    val boatColor = Color(0xFF8B4513)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val waterLineY = h * 0.65f
        val waveAmplitude = h * 0.07f

        // Sky
        drawRect(color = Color(0xFFF5EDE0), size = size)

        // Sun — Japan red dot, right side
        val sunRadius = h * 0.22f
        val sunCx = w * 0.87f
        val sunCy = waterLineY - sunRadius * 0.6f
        drawCircle(color = japanRed, radius = sunRadius, center = Offset(sunCx, sunCy))

        // Water body
        drawRect(
            color = waterBlue,
            topLeft = Offset(0f, waterLineY),
            size = androidx.compose.ui.geometry.Size(w, h - waterLineY)
        )

        // Wave pattern (two layers)
        drawWaves(waveOffset, waterLineY, w, h, waveAmplitude, waterDark, alpha = 0.55f)
        drawWaves(waveOffset + 0.5f, waterLineY + waveAmplitude * 0.4f, w, h,
            waveAmplitude * 0.5f, Color.White, alpha = 0.30f)

        // Boat + person
        val boatX = boatProgress * (w + w * 0.3f) - w * 0.15f
        val boatW = w * 0.22f
        val boatH = h * 0.12f
        val boatY = waterLineY - boatH * 0.5f

        drawBoat(boatX, boatY, boatW, boatH, boatColor)
        drawRower(
            cx        = boatX + boatW * 0.38f,
            baseY     = boatY,
            scale     = boatH * 1.5f,
            oarAngle  = oarAngle,
            armAngle  = armAngle,
            skinColor = skinColor,
            robeColor = robeColor,
            hatColor  = hatColor
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

    val hipY      = baseY - unit * 1.2f
    val shoulderY = hipY  - unit * 2.5f
    val headY     = shoulderY - unit * 1.8f

    // Body
    drawLine(color = robeColor, start = Offset(cx, hipY), end = Offset(cx, shoulderY),
        strokeWidth = unit * 2.2f, cap = StrokeCap.Round)

    // Head
    drawCircle(color = skinColor, radius = unit * 1.0f, center = Offset(cx, headY))

    // Kasa hat
    val hatPath = Path().apply {
        moveTo(cx,             headY - unit * 2.2f)
        lineTo(cx - unit * 2.2f, headY - unit * 0.2f)
        lineTo(cx + unit * 2.2f, headY - unit * 0.2f)
        close()
    }
    drawPath(hatPath, color = hatColor)
    drawLine(color = Color(0xFF3A2000),
        start = Offset(cx - unit * 2.2f, headY - unit * 0.2f),
        end   = Offset(cx + unit * 2.2f, headY - unit * 0.2f),
        strokeWidth = unit * 0.5f)

    // Arm
    val armRad = Math.toRadians(armAngle.toDouble()).toFloat()
    val armLen = unit * 2.5f
    val elbowX = cx + armLen * sin(armRad)
    val elbowY = shoulderY + armLen * (1 - 0.3f * sin(armRad))
    drawLine(color = skinColor, start = Offset(cx, shoulderY),
        end = Offset(elbowX, elbowY), strokeWidth = unit * 0.9f, cap = StrokeCap.Round)

    // Oar
    val oarRad = Math.toRadians(oarAngle.toDouble()).toFloat()
    val oarLen = unit * 5.5f
    val oarEndX = elbowX - oarLen * sin(oarRad)
    val oarEndY = elbowY + oarLen * 0.6f
    drawLine(color = Color(0xFF8B6914), start = Offset(elbowX, elbowY),
        end = Offset(oarEndX, oarEndY), strokeWidth = unit * 0.6f, cap = StrokeCap.Round)
    drawCircle(color = Color(0xFFA0783C), radius = unit * 0.8f,
        center = Offset(oarEndX, oarEndY))
}

private val INSIGHT_CAPTIONS = listOf(
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
private fun InsightCard(
    state: ProgressInsightUiState,
    isAiConfigured: Boolean,
    onRequestInsight: () -> Unit
) {
    // Hoist boatProgress so we can drive caption changes off it
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

    // Advance caption every 8 seconds
    var captionIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(8_000L)
            captionIndex = (captionIndex + 1) % INSIGHT_CAPTIONS.size
        }
    }

    ChartCard(title = "AI Progress Coach") {
        Text(
            "Generate an insight from your charts, then ask follow-up questions in a dedicated chat.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = isAiConfigured && !state.isLoading && !state.isChatLoading,
            onClick = onRequestInsight,
            contentPadding = if (state.isLoading) PaddingValues(0.dp)
                             else PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    contentAlignment = Alignment.Center
                ) {
                    JapanRowingAnimation(
                        boatProgress = boatProgress,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = INSIGHT_CAPTIONS[captionIndex],
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
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Generate insight")
            }
        }
        if (!isAiConfigured) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Connect an AI provider in Settings to enable insights.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProgressInsightDialog(
    state: ProgressInsightUiState,
    onOpenChat: () -> Unit,
    onDismiss: () -> Unit
) {
    val maxBodyHeight = (LocalConfiguration.current.screenHeightDp * 0.55f).dp
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null) },
        title = { Text("Progress insight") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = maxBodyHeight)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                state.bodyScore?.let { BodyScoreGauge(it) }
                Text(state.summary.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                if (state.recommendations.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Recommendations",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        state.recommendations.forEach { rec ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("• ", style = MaterialTheme.typography.bodyMedium)
                                Text(rec, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenChat) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    if (state.chatMessages.size > 1) "Continue chat" else "Ask follow-up"
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun BodyScoreGauge(score: Int) {
    val clamped = score.coerceIn(0, 100)
    val color = when {
        clamped >= 75 -> MaterialTheme.colorScheme.primary
        clamped >= 50 -> Color(0xFFFFCA28)
        else -> MaterialTheme.colorScheme.error
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        CalorieRing(
            progress = clamped / 100f,
            centerText = "$clamped",
            subText = "/ 100",
            modifier = Modifier.size(88.dp),
            strokeWidth = 14f,
            progressColor = color
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                "Body Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                when {
                    clamped >= 75 -> "Great alignment with your goal"
                    clamped >= 50 -> "Making progress, room to improve"
                    else -> "Trend needs attention"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun MacroLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendDot("Protein", ProteinColor)
        LegendDot("Carbs", CarbsColor)
        LegendDot("Fats", FatsColor)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Trims trailing ".0" so whole numbers read cleanly while decimals keep one place. */
private fun trim(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
