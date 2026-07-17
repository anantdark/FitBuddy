package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.ExerciseDailySummary
import com.anant.fitbuddy.data.database.FoodDailySummary
import com.anant.fitbuddy.ui.components.CalorieRing
import com.anant.fitbuddy.ui.components.CustomBarChart
import com.anant.fitbuddy.ui.components.CustomLineChart
import com.anant.fitbuddy.ui.components.CustomStackedBarChart
import com.anant.fitbuddy.ui.components.MetricLineChart
import com.anant.fitbuddy.ui.viewmodel.ProgressInsightUiState
import kotlinx.coroutines.launch

private val ProteinColor = Color(0xFFFF7043)
private val CarbsColor = Color(0xFF26A69A)
private val FatsColor = Color(0xFFFFCA28)

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
    progressInsightState: ProgressInsightUiState,
    isAiConfigured: Boolean,
    onRequestInsight: () -> Unit,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableIntStateOf(0) } // 0 = Weekly, 1 = Monthly
    val options = listOf("Weekly", "Monthly")

    val foodSummaries = if (selectedRange == 0) weeklyFood else monthlyFood
    val exerciseSummaries = if (selectedRange == 0) weeklyExercise else monthlyExercise

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        item { BodyMetricCard(measurements = measurements) }

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

        item {
            ChartCard(title = "Macronutrient Trend") {
                MacroLegend()
                Spacer(Modifier.height(8.dp))
                CustomStackedBarChart(
                    foodSummaries = foodSummaries,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }
        }

        item { ExerciseCard(exerciseSummaries = exerciseSummaries) }

        item {
            InsightCard(
                state = progressInsightState,
                isAiConfigured = isAiConfigured,
                onRequestInsight = onRequestInsight,
                onOpenChat = onOpenChat
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
            // Oldest-to-newest, dropping readings that don't have this metric set.
            val points = remember(measurements, page) {
                measurements
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
                            text = "$sign${trim(delta)}${metric.unit} since first reading",
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
private fun InsightCard(
    state: ProgressInsightUiState,
    isAiConfigured: Boolean,
    onRequestInsight: () -> Unit,
    onOpenChat: () -> Unit
) {
    val hasInsight = state.summary != null

    ChartCard(title = "AI Progress Coach") {
        if (hasInsight) {
            state.bodyScore?.let { score ->
                BodyScoreGauge(score)
                Spacer(Modifier.height(12.dp))
            }
            Text(state.summary!!, style = MaterialTheme.typography.bodyMedium)
            if (state.recommendations.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Recommendations",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                state.recommendations.forEach { rec ->
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        Text("• ", style = MaterialTheme.typography.bodyMedium)
                        Text(rec, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = isAiConfigured && !state.isLoading && !state.isChatLoading,
                onClick = onOpenChat
            ) {
                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(
                    if (state.chatMessages.size > 1) "Continue conversation" else "Ask follow-up questions"
                )
            }
            Text(
                "Opens a dedicated chat — only a summary is sent for insights; full data is used when you ask questions.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(12.dp))
        } else {
            Text(
                "Generate an insight from your charts, then ask follow-up questions in a dedicated chat.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
        }

        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = isAiConfigured && !state.isLoading && !state.isChatLoading,
            onClick = onRequestInsight
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
            }
            Spacer(Modifier.size(8.dp))
            Text(
                when {
                    state.isLoading -> "Analysing…"
                    hasInsight -> "Refresh insight"
                    else -> "Generate insight"
                }
            )
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
