package com.anant.fitbuddy.util

import androidx.compose.runtime.staticCompositionLocalOf
import com.anant.fitbuddy.data.region.AppRegion
import java.util.Locale
import kotlin.math.roundToInt

enum class UnitSystem {
    METRIC, IMPERIAL;

    companion object {
        /** Explicit user choice wins; otherwise North America → US, every other region → metric. */
        fun resolve(stored: String, regionStored: String): UnitSystem {
            runCatching { valueOf(stored) }.getOrNull()?.let { return it }
            return if (AppRegion.fromStored(regionStored) == AppRegion.US) IMPERIAL else METRIC
        }
    }
}

val LocalUnitSystem = staticCompositionLocalOf { UnitSystem.METRIC }

/** Storage is always kg / cm / ml; these convert for display and input only. */
object Units {
    private const val LB_PER_KG = 2.2046226218
    private const val CM_PER_IN = 2.54
    private const val ML_PER_FL_OZ = 29.5735295625

    fun weightUnit(u: UnitSystem) = if (u == UnitSystem.IMPERIAL) "lb" else "kg"
    fun heightUnit(u: UnitSystem) = if (u == UnitSystem.IMPERIAL) "in" else "cm"

    fun kgToDisplay(kg: Double, u: UnitSystem): Double = if (u == UnitSystem.IMPERIAL) kg * LB_PER_KG else kg
    fun displayToKg(v: Double, u: UnitSystem): Double = if (u == UnitSystem.IMPERIAL) v / LB_PER_KG else v
    fun cmToDisplay(cm: Double, u: UnitSystem): Double = if (u == UnitSystem.IMPERIAL) cm / CM_PER_IN else cm
    fun displayToCm(v: Double, u: UnitSystem): Double = if (u == UnitSystem.IMPERIAL) v * CM_PER_IN else v

    /** One-decimal number without trailing ".0", e.g. 70.0 → "70", 154.3 → "154.3". */
    fun trim(value: Double): String {
        val r = (value * 10).roundToInt() / 10.0
        return if (r % 1.0 == 0.0) r.toLong().toString() else String.format(Locale.US, "%.1f", r)
    }

    fun weightText(kg: Double, u: UnitSystem): String = trim(kgToDisplay(kg, u))
    fun weightLabel(kg: Double, u: UnitSystem): String = "${weightText(kg, u)} ${weightUnit(u)}"
    fun heightText(cm: Double, u: UnitSystem): String = trim(cmToDisplay(cm, u))

    fun mlToFlOz(ml: Int): Double = ml / ML_PER_FL_OZ
    fun flOzToMl(oz: Double): Int = (oz * ML_PER_FL_OZ).roundToInt()

    fun formatVolume(ml: Int, u: UnitSystem): String =
        if (u == UnitSystem.IMPERIAL) "${trim(mlToFlOz(ml.coerceAtLeast(0)))} fl oz" else "${ml.coerceAtLeast(0)} ml"
}
