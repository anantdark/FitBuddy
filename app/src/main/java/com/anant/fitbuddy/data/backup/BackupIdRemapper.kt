package com.anant.fitbuddy.data.backup

import com.anant.fitbuddy.data.database.MealFood
import com.anant.fitbuddy.data.database.MealPreset
import com.anant.fitbuddy.data.model.PresetMealFood

/** Pure ID remaps used when re-inserting auto-generated Room rows on import. */
internal object BackupIdRemapper {

    fun idMap(oldIds: List<Int>, newIds: List<Long>): Map<Int, Int> =
        oldIds.zip(newIds.map { it.toInt() }).toMap()

    fun remapMealPreset(preset: MealPreset, savedFoodIdMap: Map<Int, Int>): MealPreset =
        preset.copy(
            id = 0,
            foods = preset.foods?.map { remapPresetMealFood(it, savedFoodIdMap) }
        )

    fun remapPresetMealFood(
        food: PresetMealFood,
        savedFoodIdMap: Map<Int, Int>
    ): PresetMealFood = food.copy(
        savedFoodId = food.savedFoodId?.let { savedFoodIdMap[it] }
    )

    fun remapMealFood(
        food: MealFood,
        mealLogIdMap: Map<Int, Int>,
        savedFoodIdMap: Map<Int, Int>
    ): MealFood? {
        val newMealId = mealLogIdMap[food.mealLogId] ?: return null
        return food.copy(
            id = 0,
            mealLogId = newMealId,
            presetId = food.presetId?.let { savedFoodIdMap[it] }
        )
    }
}
