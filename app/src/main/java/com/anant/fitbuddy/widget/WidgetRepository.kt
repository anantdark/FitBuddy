package com.anant.fitbuddy.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.anant.fitbuddy.FitBuddyApp
import com.anant.fitbuddy.MainActivity
import com.anant.fitbuddy.data.database.WaterDrinkKind
import com.anant.fitbuddy.ui.viewmodel.DashboardUiState
import com.anant.fitbuddy.util.DateUtils
import com.anant.fitbuddy.util.WaterFillLayers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

object WidgetRepository {

    suspend fun load(context: Context): WidgetData {
        val app = context.applicationContext as? FitBuddyApp ?: return WidgetData()
        val settings = app.settingsRepository.settings.first()
        val today = DateUtils.today(settings.dayChangeHour)
        val repo = app.repository
        val profile = repo.activeProfile.first()
        val targets = profile?.targetsForDate(today)
        val totals = repo.getFoodTotalsToday(today).first()
        val burned = repo.getExerciseBurnToday(today).first() ?: 0
        val meals = repo.mealPresets.first()
        val foods = repo.savedFoods.first()
        val chips = buildList {
            meals.take(4).forEach { meal ->
                add(
                    WidgetPresetChip(
                        id = meal.id,
                        name = meal.name,
                        calories = meal.calories,
                        kind = MainActivity.PRESET_KIND_MEAL,
                    )
                )
            }
            if (size < 4) {
                foods.take(4 - size).forEach { food ->
                    add(
                        WidgetPresetChip(
                            id = food.id,
                            name = food.name,
                            calories = food.calories,
                            kind = MainActivity.PRESET_KIND_FOOD,
                        )
                    )
                }
            }
        }
        val layers = if (settings.waterTrackingEnabled) {
            WaterFillLayers.fromLogs(
                logs = repo.getWaterLogsForDate(today).first(),
                goalMl = settings.waterGoalMl,
            )
        } else {
            WaterFillLayers(goalMl = settings.waterGoalMl)
        }
        return WidgetData(
            consumedCalories = totals.totalCalories,
            burnedCalories = burned,
            targetCalories = targets?.kcal ?: DashboardUiState.DEFAULT_TARGET_CALORIES,
            presets = chips,
            waterTrackingEnabled = settings.waterTrackingEnabled,
            waterConsumedMl = layers.totalMl,
            waterGoalMl = settings.waterGoalMl,
            waterOnlyMl = layers.waterMl,
            coffeeMl = layers.coffeeMl,
            teaMl = layers.teaMl,
            waterQuickAddMl = settings.quickAddMl(WaterDrinkKind.WATER),
            bottleQuickAddMl = settings.quickAddMl(WaterDrinkKind.BOTTLE),
        )
    }
}

/**
 * Glance reuses a live session on update(), recomposing with whatever data it captured.
 * Bumping this version makes every widget reload its data inside composition instead.
 */
internal object WidgetRefresh {
    val version = MutableStateFlow(0L)
    fun bump() = version.update { it + 1 }
}

@Composable
internal fun rememberWidgetData(context: Context, initial: WidgetData): WidgetData {
    val version by WidgetRefresh.version.collectAsState()
    return produceState(initial, version) { value = WidgetRepository.load(context) }.value
}
