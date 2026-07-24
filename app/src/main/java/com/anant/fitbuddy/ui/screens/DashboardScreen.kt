package com.anant.fitbuddy.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.abs
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
    // Single flipped label — only one card can be flipped at a time.
    // Tapping the same card again flips it back.
    var flippedLabel by remember { mutableStateOf<String?>(null) }

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
            isFlipped = flippedLabel == "Protein",
            onTap = { flippedLabel = if (flippedLabel == "Protein") null else "Protein" },
            modifier = Modifier.weight(1f)
        )
        MacroCard(
            label = "Carbs",
            remaining = state.remainingCarbs,
            consumed = state.consumedCarbs,
            target = state.targetCarbs,
            color = CarbsColor,
            isFlipped = flippedLabel == "Carbs",
            onTap = { flippedLabel = if (flippedLabel == "Carbs") null else "Carbs" },
            modifier = Modifier.weight(1f)
        )
        MacroCard(
            label = "Fats",
            remaining = state.remainingFats,
            consumed = state.consumedFats,
            target = state.targetFats,
            color = FatsColor,
            isFlipped = flippedLabel == "Fats",
            onTap = { flippedLabel = if (flippedLabel == "Fats") null else "Fats" },
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
    isFlipped: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (target <= 0) 0f else (consumed.toFloat() / target).coerceIn(0f, 1f)

    // Animate Y-rotation: 0° = front, 180° = back
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "macro-flip-$label"
    )

    // When rotation passes 90° we show the back face (with horizontal mirror correction)
    val showBack = rotation > 90f

    Card(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density   // perspective depth
            },
        colors = CardDefaults.cardColors(
            containerColor = if (showBack)
                color.copy(alpha = 0.15f)
            else
                MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onTap
    ) {
        if (!showBack) {
            // ── FRONT: remaining + progress bar ──
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
        } else {
            // ── BACK: consumed amount (mirror the X axis so text reads correctly) ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { rotationY = 180f }   // un-mirror text
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = color
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${consumed}g",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "eaten",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                // Filled progress arc showing how much of target is consumed
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "of ${target}g",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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

@Composable
private fun AnalyzingBanner(modelId: String? = null) {
    var scaledTime by remember { mutableStateOf(0.0) }
    var lastFrame  by remember { mutableLongStateOf(0L) }
    var speedMultiplier by remember { mutableFloatStateOf(1f) }

    LaunchedEffect(Unit) {
        while (true) {
            withInfiniteAnimationFrameMillis { now ->
                val delta = if (lastFrame == 0L) 0L else (now - lastFrame)
                lastFrame = now
                scaledTime = accumulateScaledTime(scaledTime, delta, speedMultiplier)
            }
        }
    }

    // Cycling caption — advances every 1000 ms regardless of scaledTime
    var captionIndex by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(8_000L)
            captionIndex = (captionIndex + 1) % analyzingCaptions.size
        }
    }

    val amber        = Color(0xFFFFC107)
    val sunGlowColor = Color(0xFFFFE082)

    val planets   = remember { solarSystemPlanets(MacroProteinColor, MacroCarbsColor, MacroFatsColor) }
    val firstLine = analyzingLabel(modelId)

    // ── Stars: 80 dots with stronger twinkle ─────────────────────────────────────────────
    data class Star(val x: Float, val y: Float, val baseAlpha: Float,
                    val freq: Double, val phase: Double, val radius: Float)
    val stars = remember {
        val rng = kotlin.random.Random(0xA57A_2024)
        List(80) {
            Star(
                x         = rng.nextFloat(),
                y         = rng.nextFloat(),
                baseAlpha = 0.25f + rng.nextFloat() * 0.70f,   // wider alpha range
                freq      = 0.0008 + rng.nextDouble() * 0.003,  // 3× faster twinkle
                phase     = rng.nextDouble() * 6.283,
                radius    = 0.5f + rng.nextFloat() * 1.8f       // more size variety
            )
        }
    }

    // ── Shooting stars: random direction, speed, lifetime, completely independent ────────
    data class ShootingStar(
        val startX: Float, val startY: Float,   // 0..1 normalised start pos
        val dx: Float, val dy: Float,            // normalised direction (per period)
        val periodMs: Double,                    // full-travel duration
        val offsetMs: Double,                    // time offset so they fire at different times
        val alpha: Float,                        // max brightness
        val length: Float                        // tail length (0..1 of canvas width)
    )
    val shootingStars = remember {
        val rng = kotlin.random.Random(0xB33F_2025)
        List(6) {
            // Random angle — any direction, not just left-to-right
            val angle = rng.nextDouble() * 2.0 * Math.PI
            ShootingStar(
                startX   = rng.nextFloat(),
                startY   = rng.nextFloat(),
                dx       = cos(angle).toFloat(),
                dy       = sin(angle).toFloat(),
                periodMs = 1800.0 + rng.nextDouble() * 3200.0,  // 1.8–5 s cycle
                offsetMs = rng.nextDouble() * 5000.0,            // staggered start
                alpha    = 0.55f + rng.nextFloat() * 0.45f,
                length   = 0.12f + rng.nextFloat() * 0.22f
            )
        }
    }

    val tailSteps     = 55
    val tailStepMs    = 14.0
    val sunTailSteps  = 40
    val sunTailStepMs = 20.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape  = RoundedCornerShape(12.dp),
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
                                speedMultiplier = 3f
                                try { tryAwaitRelease() } finally { speedMultiplier = 1f }
                            }
                        )
                    }
            ) {
                val W     = size.width
                val H     = size.height
                val cy    = H / 2f
                val halfW = W / 2f
                val halfH = H / 2f
                val now   = scaledTime

                // ── Stars ──────────────────────────────────────────────────────────────────
                stars.forEach { s ->
                    // Full sine swing → very visible twinkle
                    val twinkle = sin(now * s.freq + s.phase).toFloat()
                    val a = (s.baseAlpha + twinkle * s.baseAlpha * 0.9f).coerceIn(0.02f, 1.0f)
                    drawCircle(
                        color  = Color.White.copy(alpha = a),
                        radius = s.radius,
                        center = Offset(s.x * W, s.y * H)
                    )
                }

                // ── Shooting stars (fully random direction / timing) ───────────────────────
                shootingStars.forEach { ss ->
                    // t in [0,1] within its own period; active for first 15% of period
                    val t = (((now + ss.offsetMs) % ss.periodMs) / ss.periodMs).toFloat()
                    if (t < 0.15f) {
                        val progress = t / 0.15f          // 0→1 during active window
                        val headX = (ss.startX + ss.dx * ss.length * progress) * W
                        val headY = (ss.startY + ss.dy * ss.length * H / W * progress) * H
                        val tailX = (ss.startX + ss.dx * ss.length * (progress - 0.4f).coerceAtLeast(0f)) * W
                        val tailY = (ss.startY + ss.dy * ss.length * H / W * (progress - 0.4f).coerceAtLeast(0f)) * H
                        // Fade in then out
                        val a = ss.alpha * (1f - abs(progress - 0.5f) * 2f)
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = a)),
                                start  = Offset(tailX, tailY),
                                end    = Offset(headX, headY)
                            ),
                            start       = Offset(tailX, tailY),
                            end         = Offset(headX, headY),
                            strokeWidth = 1.5.dp.toPx(),
                            cap         = StrokeCap.Round
                        )
                    }
                }

                // ── Sun position ──────────────────────────────────────────────────────────
                val sx     = sunX(now, W)
                val sunPos = Offset(sx, cy)

                // ── Sun cloud trail — gradient line fading left to right ─────────────────
                val sunTrailPts = mutableListOf<Offset>()
                for (step in sunTailSteps downTo 1) {
                    val past = now - step * sunTailStepMs
                    val psx  = sunX(past, W)
                    if (psx > sx) continue
                    sunTrailPts.add(Offset(psx, cy))
                }
                sunTrailPts.add(Offset(sx, cy))

                val sunTrailW = 6.dp.toPx()
                for (i in 0 until sunTrailPts.size - 1) {
                    val p0 = sunTrailPts[i]
                    val p1 = sunTrailPts[i + 1]
                    val a0 = (i.toFloat() / sunTrailPts.size) * 0.45f
                    val a1 = ((i + 1).toFloat() / sunTrailPts.size) * 0.45f
                    val w  = sunTrailW * (i.toFloat() / sunTrailPts.size)
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(sunGlowColor.copy(alpha = a0), sunGlowColor.copy(alpha = a1)),
                            start = p0, end = p1
                        ),
                        start = p0, end = p1,
                        strokeWidth = w.coerceAtLeast(1.dp.toPx()),
                        cap = StrokeCap.Round
                    )
                }

                // ── Planet states + draw order ─────────────────────────────────────────────
                val states = planets.map { spec ->
                    spec to helicalPoint(spec, now, cy, halfW, halfH, W)
                }
                val (backPlanets, frontPlanets) = orderByDepth(states)

                fun drawPlanet(spec: PlanetSpec, pos: Triple<Float, Float, Float>) {
                    val (hx, hy, hz) = pos

                    // Smooth gradient trail — collect valid past positions then draw segments
                    // Each segment fades from transparent (tail end) to the planet's color (head).
                    val trailPts = mutableListOf<Pair<Offset, Float>>()  // (position, alpha)
                    for (step in tailSteps downTo 1) {
                        val past = now - step * tailStepMs
                        if (sunX(past, W) > sx) continue          // skip pre-wrap ghosts
                        val (px, py, pz) = helicalPoint(spec, past, cy, halfW, halfH, W)
                        val f     = step.toFloat() / tailSteps    // 1 = tail end, 0 = near head
                        val alpha = (1f - f) * 0.55f * depthAlpha(pz)
                        trailPts.add(Pair(Offset(px, py), alpha))
                    }
                    // Add head position as the final (brightest) point
                    trailPts.add(Pair(Offset(hx, hy), 0.55f * depthAlpha(hz)))

                    // Draw gradient segments along the collected trail
                    val baseWidth = 4.dp.toPx() * depthScale(hz)
                    for (i in 0 until trailPts.size - 1) {
                        val (p0, a0) = trailPts[i]
                        val (p1, a1) = trailPts[i + 1]
                        val segWidth = baseWidth * (i.toFloat() / trailPts.size)
                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    spec.color.copy(alpha = a0),
                                    spec.color.copy(alpha = a1)
                                ),
                                start = p0,
                                end   = p1
                            ),
                            start       = p0,
                            end         = p1,
                            strokeWidth = segWidth.coerceAtLeast(1.5.dp.toPx()),
                            cap         = StrokeCap.Round
                        )
                    }

                    // Planet head — radial glow + solid bright core
                    val scale = depthScale(hz)
                    val alpha = depthAlpha(hz)
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(spec.color.copy(alpha = 0.45f * alpha), Color.Transparent),
                            center = Offset(hx, hy), radius = 8.dp.toPx() * scale
                        ),
                        radius = 8.dp.toPx() * scale, center = Offset(hx, hy)
                    )
                    drawCircle(
                        color  = spec.color.copy(alpha = 0.98f * alpha),
                        radius = 3.dp.toPx() * scale,
                        center = Offset(hx, hy)
                    )
                }

                backPlanets.forEach { (spec, pos) -> drawPlanet(spec, pos) }

                // ── Sun ───────────────────────────────────────────────────────────────────
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(amber.copy(alpha = 0.22f), Color.Transparent),
                        center = sunPos, radius = 20.dp.toPx()
                    ), radius = 20.dp.toPx(), center = sunPos
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(amber.copy(alpha = 0.60f), Color.Transparent),
                        center = sunPos, radius = 9.dp.toPx()
                    ), radius = 9.dp.toPx(), center = sunPos
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFBE7), amber),
                        center = sunPos, radius = 4.5.dp.toPx()
                    ), radius = 4.5.dp.toPx(), center = sunPos
                )

                frontPlanets.forEach { (spec, pos) -> drawPlanet(spec, pos) }
            }

            // ── Label: overlaid bottom-left, fills to end so long model ids aren't cropped ──
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, end = 12.dp, bottom = 8.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text     = firstLine,
                    style    = MaterialTheme.typography.labelSmall.copy(
                        fontFamily   = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        fontWeight   = FontWeight.Bold
                    ),
                    color    = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text  = analyzingCaptions[captionIndex],
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily    = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    ),
                    color    = Color.White.copy(alpha = 0.55f),
                    maxLines = 1
                )
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
