package com.anant.fitbuddy.data.database

/**
 * Fills missing [lastUsedAt] when importing older backups that predate the field.
 * Does not try to recover legacy manual sort order.
 */
object LibraryRecency {

    fun normalizeSavedFoods(foods: List<SavedFood>): List<SavedFood> =
        foods.map { food ->
            if (food.lastUsedAt > 0L) food
            else food.copy(lastUsedAt = food.createdAt)
        }

    fun normalizeMealPresets(presets: List<MealPreset>): List<MealPreset> =
        presets.map { preset ->
            if (preset.lastUsedAt > 0L) preset
            else preset.copy(lastUsedAt = preset.createdAt)
        }
}
