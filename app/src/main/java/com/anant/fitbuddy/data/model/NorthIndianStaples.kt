package com.anant.fitbuddy.data.model

import com.anant.fitbuddy.data.region.IndiaRegionPack
import com.anant.fitbuddy.data.region.RegionalDish

/**
 * Deprecated: thin facade over [IndiaRegionPack]. Kept so existing call sites (offline
 * simulator, analysis prompt) keep compiling during the multi-region migration; new code
 * should use `RegionPacks.pack(region)` instead.
 */
@Deprecated("Use com.anant.fitbuddy.data.region.RegionPacks.pack(region) instead.")
object NorthIndianStaples {

    data class Staple(
        val name: String,
        val weightG: Int,
        val calories: Int,
        val proteinG: Int,
        val carbsG: Int,
        val fatsG: Int
    )

    private fun RegionalDish.toStaple() = Staple(name, weightG, calories, proteinG, carbsG, fatsG)

    val ROTI = IndiaRegionPack.ROTI.toStaple()
    val PARATHA = IndiaRegionPack.PARATHA.toStaple()
    val NAAN = IndiaRegionPack.NAAN.toStaple()
    val BHATURA = IndiaRegionPack.BHATURA.toStaple()
    val DAL_KATORI = IndiaRegionPack.DAL_KATORI.toStaple()
    val RICE_BOWL = IndiaRegionPack.RICE_BOWL.toStaple()
    val CURD_KATORI = IndiaRegionPack.CURD_KATORI.toStaple()
    val SABZI_DRY = IndiaRegionPack.SABZI_DRY.toStaple()
    val GHEE_TSP = IndiaRegionPack.GHEE_TSP.toStaple()
    val SAMOSA = IndiaRegionPack.SAMOSA.toStaple()
    val CHOLE_KATORI = IndiaRegionPack.CHOLE_KATORI.toStaple()

    /** Compact table injected into the analysis system prompt. */
    fun promptReferenceTable(): String = IndiaRegionPack.promptReferenceTable()
}
