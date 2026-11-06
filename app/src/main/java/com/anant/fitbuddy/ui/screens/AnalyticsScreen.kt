package com.anant.fitbuddy.ui.screens

import com.anant.fitbuddy.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import com.anant.fitbuddy.ui.components.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.anant.fitbuddy.ui.theme.isMakoStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.ExerciseDailySummary
import com.anant.fitbuddy.data.database.FoodDailySummary
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.components.CalorieRing
import com.anant.fitbuddy.ui.components.CaloriesBurnedHeatmap
import com.anant.fitbuddy.ui.components.CustomLineChart
import com.anant.fitbuddy.ui.components.MetricLineChart
import com.anant.fitbuddy.ui.components.calorieTargetPrefersSurplus
import com.anant.fitbuddy.ui.loading.LoadingAnimationHost
import com.anant.fitbuddy.ui.loading.LoadingAnimationSlot
import com.anant.fitbuddy.ui.viewmodel.ProgressInsightUiState
import com.anant.fitbuddy.util.DateUtils
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** Body composition charts always use this many newest readings (not week/month range). */
private const val BODY_COMPOSITION_READING_LIMIT = 15

private data class BodyMetric(
    @param:androidx.annotation.StringRes val labelRes: Int,
    val unit: String,
    /** True when a lower value is healthier (fat, weight, etc.). */
    val decreaseIsPositive: Boolean,
    val extractor: (BodyMeasurement) -> Double?,
)

private val BODY_METRICS = listOf(
    BodyMetric(R.string.onboarding_weight, " kg", decreaseIsPositive = true) { it.weightKg },
    BodyMetric(R.string.analytics_body_fat, "%", decreaseIsPositive = true) { it.bodyFatPct },
    BodyMetric(R.string.analytics_bmr, " kcal", decreaseIsPositive = false) { it.bmr?.toDouble() },
    BodyMetric(R.string.analytics_muscle_mass, " kg", decreaseIsPositive = false) { it.muscleMassKg },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    weeklyFood: List<FoodDailySummary>,
    monthlyFood: List<FoodDailySummary>,
    weeklyExercise: List<ExerciseDailySummary>,
    monthlyExercise: List<ExerciseDailySummary>,
    sixMonthExercise: List<ExerciseDailySummary>,
    measurements: List<BodyMeasurement>,
    targetCaloriesForDate: (String) -> Int,
    targetWeightKg: Double? = null,
    /** Profile goal: LOSE_WEIGHT | GAIN_MUSCLE | RECOMP | AUTO */
    goal: String = "RECOMP",
    monthlyEndDate: String,
    realToday: String,
    progressInsightState: ProgressInsightUiState,
    isAiConfigured: Boolean,
    animationChoice: String = AppSettings.LOADING_ANIM_RANDOM,
    forceShowAnimation: Boolean = false,
    onShiftMonth: (Int) -> Unit,
    onRequestInsight: () -> Unit,
    onOpenChat: () -> Unit,
    onDismissInsight: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRange by remember { mutableIntStateOf(0) } // 0 = Weekly, 1 = Monthly
    val options = listOf(stringResource(R.string.analytics_weekly), stringResource(R.string.analytics_monthly))
    val preferSurplus = remember(goal) { calorieTargetPrefersSurplus(goal) }

    val foodSummaries = if (selectedRange == 0) weeklyFood else monthlyFood
    val isLatestWindow = monthlyEndDate == realToday
    val last30DaysLabel = stringResource(R.string.analytics_last_30_days)
    val rangeLabel = remember(monthlyEndDate, isLatestWindow, last30DaysLabel) {
        if (isLatestWindow) last30DaysLabel else DateUtils.rolling30DayLabel(monthlyEndDate)
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
                animationChoice = animationChoice,
                forceShowAnimation = forceShowAnimation,
                onRequestInsight = onRequestInsight
            )
        }

        item {
            val segmentBase = if (isMakoStyle()) {
                RoundedCornerShape(0.dp)
            } else {
                SegmentedButtonDefaults.baseShape
            }
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = selectedRange == index,
                        onClick = { selectedRange = index },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size,
                            baseShape = segmentBase
                        )
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
            ChartCard(title = stringResource(R.string.analytics_calories_eaten_vs_target)) {
                CustomLineChart(
                    foodSummaries = foodSummaries,
                    targetCaloriesForDate = targetCaloriesForDate,
                    preferSurplus = preferSurplus,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }
        }

        item {
            val (exerciseRangeStart, exerciseRangeEnd) =
                DateUtils.rollingSixMonthBounds(realToday)
            ExerciseCard(
                exerciseSummaries = sixMonthExercise,
                rangeStart = exerciseRangeStart,
                rangeEnd = exerciseRangeEnd
            )
        }

        // Independent of Weekly/Monthly: always the most recent scale readings.
        item {
            BodyMetricCard(
                measurements = measurements,
                targetWeightKg = targetWeightKg,
                goal = goal
            )
        }
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
                contentDescription = stringResource(R.string.analytics_previous_30_days),
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
                contentDescription = stringResource(R.string.analytics_next_30_days),
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
private fun BodyMetricCard(
    measurements: List<BodyMeasurement>,
    targetWeightKg: Double?,
    goal: String
) {
    val pagerState = rememberPagerState(pageCount = { BODY_METRICS.size })
    val scope = rememberCoroutineScope()
    val weightTarget = targetWeightKg?.takeIf { it.isFinite() && it > 0.0 }
    val preferHigherWeight = remember(goal) { calorieTargetPrefersSurplus(goal) }

    ChartCard(title = stringResource(R.string.analytics_body_composition)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) { page ->
            val metric = BODY_METRICS[page]
            // Last 15 readings (newest-first from Room), then oldest→newest for the chart.
            // Ignores the Weekly/Monthly toggle above.
            val points = remember(measurements, page) {
                measurements
                    .take(BODY_COMPOSITION_READING_LIMIT)
                    .asReversed()
                    .mapNotNull { m ->
                        metric.extractor(m)?.let { value ->
                            m.dateString to value
                        }
                    }
            }

            MetricLineChart(
                points = points,
                unit = metric.unit,
                decreaseIsPositive = metric.decreaseIsPositive,
                targetValue = if (metric.labelRes == R.string.onboarding_weight) weightTarget else null,
                targetPreferHigher = preferHigherWeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
            )
        }

        val currentMetric = BODY_METRICS[pagerState.currentPage]
        val currentPoints = remember(measurements, pagerState.currentPage) {
            measurements
                .take(BODY_COMPOSITION_READING_LIMIT)
                .asReversed()
                .mapNotNull { m ->
                    currentMetric.extractor(m)?.let { value ->
                        m.dateString to value
                    }
                }
        }
        if (currentPoints.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            val latest = currentPoints.last().second
            val first = currentPoints.first().second
            val delta = latest - first
            Text(
                text = stringResource(R.string.analytics_latest, trim(latest), currentMetric.unit),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (currentPoints.size >= 2) {
                val sign = if (delta > 0) "+" else ""
                Text(
                    text = "$sign${trim(delta)}${currentMetric.unit} over last ${currentPoints.size} readings",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (currentMetric.labelRes == R.string.onboarding_weight && weightTarget != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (preferHigherWeight) {
                    stringResource(R.string.analytics_within_1_kg_green_above_1_kg)
                } else {
                    stringResource(R.string.analytics_within_1_kg_green_below_1_kg)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(12.dp))

        // Metric switches under the graph (tap or swipe the chart above).
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
                    text = { Text(stringResource(metric.labelRes)) }
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exerciseSummaries: List<ExerciseDailySummary>,
    rangeStart: String,
    rangeEnd: String
) {
    val visibleSummaries = remember(exerciseSummaries, rangeStart, rangeEnd) {
        exerciseSummaries.filter { it.dateString in rangeStart..rangeEnd }
    }
    val totalBurned = visibleSummaries.sumOf { it.totalBurned }
    val activeDays = visibleSummaries.count { it.totalBurned > 0 }

    ChartCard(title = stringResource(R.string.analytics_calories_burned_last_6_months)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            SummaryStat(stringResource(R.string.analytics_total), "$totalBurned")
            SummaryStat(stringResource(R.string.analytics_active_days), "$activeDays")
            SummaryStat(stringResource(R.string.analytics_avg_day), if (activeDays > 0) "${totalBurned / activeDays}" else "0")
        }
        Spacer(Modifier.height(12.dp))
        CaloriesBurnedHeatmap(
            summaries = visibleSummaries,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            modifier = Modifier.fillMaxWidth()
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
    animationChoice: String,
    forceShowAnimation: Boolean = false,
    onRequestInsight: () -> Unit
) {
    ChartCard(title = stringResource(R.string.analytics_ai_progress_coach)) {
        Text(
            stringResource(R.string.analytics_generate_an_insight_from_your_charts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        state.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        val insightLoading = state.isLoading || forceShowAnimation
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = isAiConfigured && !insightLoading && !state.isChatLoading,
            onClick = onRequestInsight,
            // Keep primary colors while loading — disabled greys out the spinner caption.
            colors = if (insightLoading) {
                ButtonDefaults.buttonColors(
                    disabledContainerColor = MaterialTheme.colorScheme.primary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                ButtonDefaults.buttonColors()
            },
            contentPadding = if (insightLoading) PaddingValues(0.dp)
                             else PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            if (insightLoading) {
                LoadingAnimationHost(
                    slot = LoadingAnimationSlot.INSIGHT,
                    animationChoice = animationChoice,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                )
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.analytics_generate_insight))
            }
        }
        if (!isAiConfigured) {
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.analytics_connect_an_ai_provider_in_settings),
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
        title = { Text(stringResource(R.string.analytics_progress_insight)) },
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
                            stringResource(R.string.analytics_recommendations),
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
                    if (state.chatMessages.size > 1) "Continue chat" else stringResource(R.string.analytics_ask_follow_up)
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.donate_close)) }
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
                stringResource(R.string.analytics_body_score),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                when {
                    clamped >= 75 -> stringResource(R.string.analytics_great_alignment_with_your_goal)
                    clamped >= 50 -> stringResource(R.string.analytics_making_progress_room_to_improve)
                    else -> stringResource(R.string.analytics_trend_needs_attention)
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

/** Trims trailing ".0" so whole numbers read cleanly while decimals keep one place. */
private fun trim(value: Double): String {
    val rounded = (value * 10).toInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}
