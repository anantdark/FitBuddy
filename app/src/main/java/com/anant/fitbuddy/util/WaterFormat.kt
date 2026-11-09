package com.anant.fitbuddy.util

import com.anant.fitbuddy.data.database.WaterDrinkKind
import com.anant.fitbuddy.data.database.WaterLog
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Per-drink volumes for the stacked water meter.
 * Stack order (bottom → top / start → leading edge): water → coffee → tea.
 */
data class WaterFillLayers(
    val waterMl: Int = 0,
    val coffeeMl: Int = 0,
    val teaMl: Int = 0,
    val goalMl: Int = 0,
) {
    val totalMl: Int get() = waterMl + coffeeMl + teaMl

    /** Shares of the goal, scaled down together if over goal so the bar stays ≤ 100%. */
    val waterShare: Float
    val coffeeShare: Float
    val teaShare: Float
    val totalShare: Float

    init {
        val goal = goalMl.coerceAtLeast(1).toFloat()
        val rawWater = waterMl.coerceAtLeast(0) / goal
        val rawCoffee = coffeeMl.coerceAtLeast(0) / goal
        val rawTea = teaMl.coerceAtLeast(0) / goal
        val rawTotal = rawWater + rawCoffee + rawTea
        val scale = if (rawTotal > 1f) 1f / rawTotal else 1f
        waterShare = rawWater * scale
        coffeeShare = rawCoffee * scale
        teaShare = rawTea * scale
        totalShare = (waterShare + coffeeShare + teaShare).coerceIn(0f, 1f)
    }

    companion object {
        fun fromLogs(logs: List<WaterLog>, goalMl: Int): WaterFillLayers {
            var water = 0
            var coffee = 0
            var tea = 0
            logs.forEach { log ->
                when (WaterDrinkKind.fromStored(log.drinkKind).logKind) {
                    WaterDrinkKind.WATER, WaterDrinkKind.BOTTLE -> water += log.amountMl
                    WaterDrinkKind.COFFEE -> coffee += log.amountMl
                    WaterDrinkKind.TEA -> tea += log.amountMl
                    WaterDrinkKind.CUSTOM -> water += log.amountMl
                }
            }
            return WaterFillLayers(
                waterMl = water,
                coffeeMl = coffee,
                teaMl = tea,
                goalMl = goalMl,
            )
        }
    }
}

/** Shared ml / L formatting for dashboard + widgets. */
object WaterFormat {

    fun formatMl(ml: Int, units: UnitSystem = UnitSystem.METRIC): String =
        Units.formatVolume(ml, units)

    /** e.g. 1150 → "1.15 L", 2000 → "2 L", 250 → "0.25 L". */
    fun formatLiters(ml: Int, units: UnitSystem = UnitSystem.METRIC): String {
        if (units == UnitSystem.IMPERIAL) return Units.formatVolume(ml, units)
        val liters = ml.coerceAtLeast(0) / 1000.0
        val cents = (liters * 100.0).roundToInt()
        return if (cents % 100 == 0) {
            "${cents / 100} L"
        } else if (cents % 10 == 0) {
            String.format(Locale.US, "%.1f L", cents / 100.0)
        } else {
            String.format(Locale.US, "%.2f L", cents / 100.0)
        }
    }

    fun progress(consumedMl: Int, goalMl: Int): Float =
        if (goalMl <= 0) 0f else (consumedMl.toFloat() / goalMl).coerceIn(0f, 1f)
}
