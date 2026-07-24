package com.anant.fitbuddy.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.anant.fitbuddy.ui.components.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anant.fitbuddy.data.database.ExerciseLog
import com.anant.fitbuddy.data.database.FoodLog
import com.anant.fitbuddy.ui.components.CalorieRing
import com.anant.fitbuddy.ui.components.MacroCarbsColor
import com.anant.fitbuddy.ui.components.MacroFatsColor
import com.anant.fitbuddy.ui.components.MacroProteinColor
import com.anant.fitbuddy.ui.components.PressableCard
import com.anant.fitbuddy.ui.components.WeekDayMacroBar
import com.anant.fitbuddy.ui.components.WeekMacroBarChart
import com.anant.fitbuddy.ui.components.pressable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.anant.fitbuddy.ui.viewmodel.DashboardUiState
import com.anant.fitbuddy.ui.viewmodel.DayLogSnapshot
import com.anant.fitbuddy.util.DateUtils

// Macro accent colors shared with the stacked bar chart legend.
private val ProteinColor = MacroProteinColor
private val CarbsColor = MacroCarbsColor
private val FatsColor = MacroFatsColor

@androidx.compose.runtime.Immutable
private data class LogRowItem(
    val title: String,
    val subtitle: String,
    val calories: Int,
    val timestamp: Long,
    val isFood: Boolean,
    val foodLog: FoodLog? = null,
    val exerciseLog: ExerciseLog? = null
)

/**
 * Home dashboard: original calorie ring + macros + today's log.
 * Tapping the header card opens [WeekHistoryScreen] (press scale + ripple).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardHomeScreen(
    realToday: String,
    weekSnapshots: Map<String, DayLogSnapshot>,
    profileState: DashboardUiState,
    isAnalyzing: Boolean,
    analyzingModel: String? = null,
    onOpenWeekHistory: () -> Unit,
    onEditFood: (FoodLog) -> Unit,
    onDeleteFood: (FoodLog) -> Unit,
    onCloneFood: (FoodLog) -> Unit,
    onViewExercise: (ExerciseLog) -> Unit,
    onDeleteExercise: (ExerciseLog) -> Unit,
    onCloneExercise: (ExerciseLog) -> Unit,
    modifier: Modifier = Modifier
) {
    val snap = weekSnapshots[realToday] ?: DayLogSnapshot()
    val todayState = profileState.copy(
        consumedCalories = snap.consumedCalories,
        burnedCalories = snap.burnedCalories,
        consumedProtein = snap.consumedProtein,
        consumedCarbs = snap.consumedCarbs,
        consumedFats = snap.consumedFats
    )
    val logItems = rememberCombinedLogs(snap.foodLogs, snap.exerciseLogs)
    var actionItem by remember { mutableStateOf<LogRowItem?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CalorieHeaderCard(
                state = todayState,
                onClick = onOpenWeekHistory
            )
        }

        if (isAnalyzing) {
            item { AnalyzingBanner(analyzingModel) }
        }

        item { MacroRow(todayState) }

        item {
            Text(
                text = "Today's Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (logItems.isEmpty()) {
            item { EmptyLogState() }
        } else {
            items(
                logItems,
                key = { "${it.isFood}-${it.timestamp}-${it.title}-${it.foodLog?.id}-${it.exerciseLog?.id}" }
            ) { item ->
                LogRow(item, onClick = { actionItem = item })
            }
        }
    }

    actionItem?.let { item ->
        LogActionSheet(
            item = item,
            showClone = true,
            onEdit = {
                item.foodLog?.let(onEditFood)
                actionItem = null
            },
            onViewExercise = {
                item.exerciseLog?.let(onViewExercise)
                actionItem = null
            },
            onClone = {
                if (item.isFood) item.foodLog?.let(onCloneFood)
                else item.exerciseLog?.let(onCloneExercise)
                actionItem = null
            },
            onDelete = {
                if (item.isFood) item.foodLog?.let(onDeleteFood)
                else item.exerciseLog?.let(onDeleteExercise)
                actionItem = null
            },
            onDismiss = { actionItem = null }
        )
    }
}

@Composable
private fun CalorieHeaderCard(
    state: DashboardUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PressableCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (state.isOverTarget) "Over target" else "Calories remaining",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            CalorieRing(
                progress = state.calorieProgress,
                centerText = state.remainingCalories.toString(),
                subText = "kcal left",
                modifier = Modifier.size(200.dp),
                progressColor = if (state.isOverTarget) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeaderStat("Eaten", state.consumedCalories)
                HeaderStat("Burned", state.burnedCalories)
                HeaderStat("Target", state.targetCalories)
            }
        }
    }
}

@Composable
private fun MacroRow(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MacroCard(
            label = "Protein",
            remaining = state.remainingProtein,
            consumed = state.consumedProtein,
            target = state.targetProtein,
            color = ProteinColor,
            modifier = Modifier.weight(1f)
        )
        MacroCard(
            label = "Carbs",
            remaining = state.remainingCarbs,
            consumed = state.consumedCarbs,
            target = state.targetCarbs,
            color = CarbsColor,
            modifier = Modifier.weight(1f)
        )
        MacroCard(
            label = "Fats",
            remaining = state.remainingFats,
            consumed = state.consumedFats,
            target = state.targetFats,
            color = FatsColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MacroCard(
    label: String,
    remaining: Int,
    consumed: Int,
    target: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target <= 0) 0f else (consumed.toFloat() / target).coerceIn(0f, 1f)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${remaining}g",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "left",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

/** Week history: stacked calorie bars (P/C/F) + selected day's log entries. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekHistoryScreen(
    weekDates: List<String>,
    selectedDate: String,
    realToday: String,
    weekSnapshots: Map<String, DayLogSnapshot>,
    profileState: DashboardUiState,
    isAnalyzing: Boolean,
    analyzingModel: String? = null,
    onSelectDate: (String) -> Unit,
    onShiftWeek: (Int) -> Unit,
    onEditFood: (FoodLog) -> Unit,
    onDeleteFood: (FoodLog) -> Unit,
    onCloneFood: (FoodLog) -> Unit,
    onViewExercise: (ExerciseLog) -> Unit,
    onDeleteExercise: (ExerciseLog) -> Unit,
    onCloneExercise: (ExerciseLog) -> Unit,
    modifier: Modifier = Modifier
) {
    val dates = weekDates.ifEmpty { DateUtils.rollingWeekDates(realToday) }
    val selectedIndex = dates.indexOf(selectedDate).coerceAtLeast(0)
    val snap = weekSnapshots[selectedDate] ?: DayLogSnapshot()
    val logItems = rememberCombinedLogs(snap.foodLogs, snap.exerciseLogs)
    var actionItem by remember(selectedDate) { mutableStateOf<LogRowItem?>(null) }

    val isCurrentWeek = dates.lastOrNull() == realToday
    val weekLabel = remember(dates, isCurrentWeek) {
        if (isCurrentWeek) "This week" else DateUtils.weekRangeLabel(dates)
    }
    val canGoNextWeek = dates.lastOrNull() != null && dates.last() < realToday

    val barDays = remember(dates, weekSnapshots) {
        dates.map { date ->
            val day = weekSnapshots[date] ?: DayLogSnapshot()
            WeekDayMacroBar(
                date = date,
                weekdayLabel = DateUtils.shortWeekday(date).take(3),
                calories = day.consumedCalories,
                proteinG = day.consumedProtein,
                carbsG = day.consumedCarbs,
                fatsG = day.consumedFats
            )
        }
    }

    val dayLabel = DateUtils.dayTitle(selectedDate, realToday)
    val logTitle = when (dayLabel) {
        "Today" -> "Today's Log"
        "Yesterday" -> "Yesterday's Log"
        else -> "Log · $dayLabel"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            WeekRangeNavigator(
                label = weekLabel,
                rangeSubtitle = if (isCurrentWeek) DateUtils.weekRangeLabel(dates) else null,
                canGoPrev = true,
                canGoNext = canGoNextWeek,
                onPrev = { onShiftWeek(-1) },
                onNext = { onShiftWeek(1) }
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isCurrentWeek) {
                            "Calories this week"
                        } else {
                            "Calories · $weekLabel"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Tap a bar for macros · selects that day's log",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    WeekMacroLegend()
                    Spacer(Modifier.height(8.dp))
                    WeekMacroBarChart(
                        days = barDays,
                        selectedDate = selectedDate,
                        onSelectDate = onSelectDate,
                        proteinColor = ProteinColor,
                        carbsColor = CarbsColor,
                        fatsColor = FatsColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                }
            }
        }

        item {
            WeekDateNavigator(
                date = selectedDate,
                canGoPrev = selectedIndex > 0,
                canGoNext = selectedIndex < dates.lastIndex,
                onPrev = { dates.getOrNull(selectedIndex - 1)?.let(onSelectDate) },
                onNext = { dates.getOrNull(selectedIndex + 1)?.let(onSelectDate) }
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                HeaderStat("Eaten", snap.consumedCalories)
                HeaderStat("Burned", snap.burnedCalories)
                HeaderStat("Target", profileState.targetCalories)
            }
        }

        if (isAnalyzing) {
            item { AnalyzingBanner(analyzingModel) }
        }

        item {
            Text(
                text = logTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (logItems.isEmpty()) {
            item { EmptyLogState() }
        } else {
            items(
                logItems,
                key = { "${it.isFood}-${it.timestamp}-${it.title}-${it.foodLog?.id}-${it.exerciseLog?.id}" }
            ) { item ->
                LogRow(item, onClick = { actionItem = item })
            }
        }
    }

    actionItem?.let { item ->
        LogActionSheet(
            item = item,
            showClone = true,
            onEdit = {
                item.foodLog?.let(onEditFood)
                actionItem = null
            },
            onViewExercise = {
                item.exerciseLog?.let(onViewExercise)
                actionItem = null
            },
            onClone = {
                if (item.isFood) item.foodLog?.let(onCloneFood)
                else item.exerciseLog?.let(onCloneExercise)
                actionItem = null
            },
            onDelete = {
                if (item.isFood) item.foodLog?.let(onDeleteFood)
                else item.exerciseLog?.let(onDeleteExercise)
                actionItem = null
            },
            onDismiss = { actionItem = null }
        )
    }
}

@Composable
private fun WeekMacroLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendDot("Protein", ProteinColor)
        LegendDot("Carbs", CarbsColor)
        LegendDot("Fats", FatsColor)
    }
}

@Composable
private fun LegendDot(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = CircleShape,
            color = color
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WeekRangeNavigator(
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
                contentDescription = "Previous week",
                tint = if (canGoPrev) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                contentDescription = "Next week",
                tint = if (canGoNext) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}

@Composable
private fun WeekDateNavigator(
    date: String,
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
                contentDescription = "Previous day",
                tint = if (canGoPrev) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = DateUtils.displayDateCompact(date),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next day",
                tint = if (canGoNext) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogActionSheet(
    item: LogRowItem,
    showClone: Boolean,
    onEdit: () -> Unit,
    onViewExercise: () -> Unit,
    onClone: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            if (item.isFood) {
                ActionRow(Icons.Filled.Edit, "Edit meal / foods", onEdit)
            } else {
                ActionRow(Icons.Filled.FitnessCenter, "View / edit exercises", onViewExercise)
            }
            if (showClone) {
                ActionRow(Icons.Filled.ContentCopy, "Clone to today", onClone)
            }
            ActionRow(
                icon = Icons.Filled.DeleteOutline,
                label = "Remove from log",
                onClick = onDelete,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
private fun HeaderStat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LogRow(item: LogRowItem, onClick: () -> Unit) {
    PressableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val accent = if (item.isFood) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.tertiary
            }
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (item.isFood) {
                            Icons.Filled.Restaurant
                        } else {
                            Icons.Filled.LocalFireDepartment
                        },
                        contentDescription = null,
                        tint = accent
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (item.isFood) "+${item.calories}" else "-${item.calories}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}

/**
 * Three plasma orbs chasing each other along interlocked Lissajous paths.
 * Driven by raw frame-clock milliseconds via [withInfiniteAnimationFrameMillis] so the
 * time value grows monotonically — the animation is perfectly seamless with no reset jump.
 * Incommensurable frequency ratios (irrational multiples) mean the pattern never closes
 * back to exactly the same state, so it always looks like it's mid-motion.
 */
@Composable
private fun AnalyzingBanner(modelId: String? = null) {
    // Monotonically growing frame clock — never resets, no loop seam.
    var frameMillis by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { frameMillis = it }
        }
    }

    val primary   = MaterialTheme.colorScheme.primary
    val tertiary  = MaterialTheme.colorScheme.tertiary
    val secondary = MaterialTheme.colorScheme.secondary

    // Orb definitions: (x-freq-hz, y-freq-hz, phase-offset-turns)
    // Irrational ratios → Lissajous figure that never closes.
    val orbs = remember {
        listOf(
            Triple(0.143, 0.202, 0.00),  // ≈1 : √2
            Triple(0.247, 0.143, 0.33),  // ≈√3 : 1
            Triple(0.143, 0.320, 0.67),  // ≈1 : √5
        )
    }
    val orbColors = listOf(primary, tertiary, secondary)
    val tailSteps = 10
    // Tail step is 28 ms back in time — derived from raw millis, so also seamless.
    val tailStepMs = 28L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Canvas(modifier = Modifier.size(44.dp)) {
                val cx = size.width  / 2f
                val cy = size.height / 2f
                val rx = cx * 0.84f
                val ry = cy * 0.84f

                fun orbPos(freqX: Double, freqY: Double, phase: Double, ms: Long): Offset {
                    val tSec = ms / 1000.0
                    val ox = rx * sin(2.0 * PI * freqX * tSec + phase * 2.0 * PI).toFloat()
                    val oy = ry * cos(2.0 * PI * freqY * tSec + phase * 2.0 * PI * 0.7).toFloat()
                    return Offset(cx + ox, cy + oy)
                }

                orbs.forEachIndexed { i, (fx, fy, ph) ->
                    val color = orbColors[i]

                    // Comet tail — past positions at evenly spaced ms offsets
                    for (step in tailSteps downTo 1) {
                        val pastMs = frameMillis - step * tailStepMs
                        val pos = orbPos(fx, fy, ph, pastMs)
                        val fraction = step.toFloat() / tailSteps
                        val tailAlpha = (1f - fraction) * 0.38f
                        val tailRadius = 3.2.dp.toPx() * (1f - fraction * 0.55f)
                        drawCircle(
                            color = color.copy(alpha = tailAlpha),
                            radius = tailRadius,
                            center = pos
                        )
                    }

                    // Head glow + solid core
                    val headPos = orbPos(fx, fy, ph, frameMillis)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.28f), Color.Transparent),
                            center = headPos,
                            radius = 8.dp.toPx()
                        ),
                        radius = 8.dp.toPx(),
                        center = headPos
                    )
                    drawCircle(
                        color = color.copy(alpha = 0.92f),
                        radius = 3.2.dp.toPx(),
                        center = headPos
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "ANALYZING",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                if (!modelId.isNullOrBlank()) {
                    Text(
                        text = modelId,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLogState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No entries yet",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap Log to add food or a workout.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun rememberCombinedLogs(
    foodLogs: List<FoodLog>,
    exerciseLogs: List<ExerciseLog>
): List<LogRowItem> = remember(foodLogs, exerciseLogs) {
    val food = foodLogs.map {
        LogRowItem(
            title = it.dishName,
            subtitle = "${it.proteinG}p · ${it.carbsG}c · ${it.fatsG}f",
            calories = it.calories,
            timestamp = it.timestamp,
            isFood = true,
            foodLog = it
        )
    }
    val exercise = exerciseLogs.map {
        LogRowItem(
            title = it.activityName,
            subtitle = "${it.durationMinutes} min",
            calories = it.caloriesBurned,
            timestamp = it.timestamp,
            isFood = false,
            exerciseLog = it
        )
    }
    (food + exercise).sortedByDescending { it.timestamp }
}
