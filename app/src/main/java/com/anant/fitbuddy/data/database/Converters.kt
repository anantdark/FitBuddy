package com.anant.fitbuddy.data.database

import androidx.room.TypeConverter
import com.anant.fitbuddy.data.model.LoggedIngredient
import com.anant.fitbuddy.data.model.PresetMealFood
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val ingredientsAdapter = moshi.adapter<List<LoggedIngredient>>(
        Types.newParameterizedType(List::class.java, LoggedIngredient::class.java)
    )

    private val presetMealFoodsAdapter = moshi.adapter<List<PresetMealFood>>(
        Types.newParameterizedType(List::class.java, PresetMealFood::class.java)
    )

    @TypeConverter
    fun ingredientsToJson(ingredients: List<LoggedIngredient>?): String? =
        ingredients?.let { ingredientsAdapter.toJson(it) }

    @TypeConverter
    fun jsonToIngredients(json: String?): List<LoggedIngredient>? =
        json?.let { ingredientsAdapter.fromJson(it) }

    @TypeConverter
    fun presetMealFoodsToJson(foods: List<PresetMealFood>?): String? =
        foods?.let { presetMealFoodsAdapter.toJson(it) }

    @TypeConverter
    fun jsonToPresetMealFoods(json: String?): List<PresetMealFood>? =
        json?.let { presetMealFoodsAdapter.fromJson(it) }
}
