package com.anant.fitbuddy.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.database.ExerciseLog
import com.anant.fitbuddy.data.database.FoodLog
import com.anant.fitbuddy.ui.components.CalorieRing
import com.anant.fitbuddy.ui.viewmodel.DashboardUiState

// Macro accent colors shared with the stacked bar chart legend.
private val ProteinColor = Color(0xFFFF7043)
private val CarbsColor = Color(0xFF26A69A)
private val FatsColor = Color(0xFFFFCA28)

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    foodLogs: List<FoodLog>,
    exerciseLogs: List<ExerciseLog>,
    isAnalyzing: Boolean,
    onEditFood: (FoodLog) -> Unit,
    onDeleteFood: (FoodLog) -> Unit,
    onViewExercise: (ExerciseLog) -> Unit,
    onDeleteExercise: (ExerciseLog) -> Unit,
    modifier: Modifier = Modifier
) {
    val logItems = rememberCombinedLogs(foodLogs, exerciseLogs)

    // Log entry the user tapped; drives the edit/remove action sheet.
    var actionItem by remember { mutableStateOf<LogRowItem?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { CalorieHeaderCard(state) }

        if (isAnalyzing) {
            item { AnalyzingBanner() }
        }

        item {
            MacroRow(state)
        }

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
            items(logItems, key = { "${it.isFood}-${it.timestamp}-${it.title}" }) { item ->
                LogRow(item, onClick = { actionItem = item })
            }
        }
    }

    actionItem?.let { item ->
        LogActionSheet(
            item = item,
            onEdit = {
                item.foodLog?.let(onEditFood)
                actionItem = null
            },
            onViewExercise = {
                item.exerciseLog?.let(onViewExercise)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogActionSheet(
    item: LogRowItem,
    onEdit: () -> Unit,
    onViewExercise: () -> Unit,
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
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
private fun CalorieHeaderCard(state: DashboardUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
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

@Composable
private fun LogRow(item: LogRowItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
private fun AnalyzingBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp
            )
            Text(
                text = "Analyzing with FitBuddy AI…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
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
