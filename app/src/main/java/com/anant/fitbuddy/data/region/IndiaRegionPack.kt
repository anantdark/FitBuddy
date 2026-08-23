package com.anant.fitbuddy.data.region

import com.anant.fitbuddy.data.prompts.PromptCatalog

/** India region pack: home and street-food priors, staples, and portion language. */
object IndiaRegionPack : RegionPack {

    override val region: AppRegion = AppRegion.INDIA
    override val displayName: String = "India"

    val ROTI = RegionalDish("Wheat roti/phulka", 35, 105, 3, 20, 2, listOf("roti", "chapati", "phulka"))
    val PARATHA = RegionalDish("Stuffed paratha", 100, 290, 7, 36, 12, listOf("paratha", "parantha", "parotta"))
    val NAAN = RegionalDish("Naan", 100, 300, 9, 50, 7, listOf("naan"))
    val BHATURA = RegionalDish("Bhatura", 80, 280, 6, 36, 12, listOf("bhatura", "bhature"))
    val DAL_KATORI = RegionalDish("Dal (1 katori)", 160, 150, 9, 22, 4, listOf("dal", "daal"))
    val RICE_BOWL = RegionalDish("Cooked rice bowl", 175, 200, 4, 44, 1, listOf("rice", "chawal"))
    val CURD_KATORI = RegionalDish("Curd/raita katori", 120, 75, 4, 6, 4, listOf("curd", "raita", "dahi", "yogurt"))
    val SABZI_DRY = RegionalDish("Dry sabzi katori", 120, 140, 4, 14, 8, listOf("sabzi", "sabji"))
    val GHEE_TSP = RegionalDish("Ghee/oil (1 tsp)", 5, 45, 0, 0, 5, listOf("ghee", "oil"))
    val SAMOSA = RegionalDish("Samosa (1)", 100, 260, 4, 32, 12, listOf("samosa"))
    val CHOLE_KATORI = RegionalDish("Chole (1 katori)", 160, 220, 10, 28, 8, listOf("chole", "chana masala", "chhole"))

    override val staples: List<RegionalDish> = listOf(
        ROTI, PARATHA, NAAN, BHATURA, DAL_KATORI, CHOLE_KATORI, RICE_BOWL, CURD_KATORI, SABZI_DRY, SAMOSA, GHEE_TSP
    )

    override fun promptReferenceTable(): String = """
        Staple reference (typical home serving; adjust to size/count — priors, not hard law):
        - 1 roti/chapati/phulka ~${ROTI.weightG} g → ~${ROTI.calories} kcal / P${ROTI.proteinG} C${ROTI.carbsG} F${ROTI.fatsG}
        - 1 stuffed paratha ~${PARATHA.weightG} g → ~${PARATHA.calories} kcal / P${PARATHA.proteinG} C${PARATHA.carbsG} F${PARATHA.fatsG}
        - 1 naan ~${NAAN.weightG} g → ~${NAAN.calories} kcal / P${NAAN.proteinG} C${NAAN.carbsG} F${NAAN.fatsG}
        - 1 bhatura ~${BHATURA.weightG} g → ~${BHATURA.calories} kcal / P${BHATURA.proteinG} C${BHATURA.carbsG} F${BHATURA.fatsG}
        - 1 katori dal ~${DAL_KATORI.weightG} g → ~${DAL_KATORI.calories} kcal / P${DAL_KATORI.proteinG} C${DAL_KATORI.carbsG} F${DAL_KATORI.fatsG}
        - 1 katori chole ~${CHOLE_KATORI.weightG} g → ~${CHOLE_KATORI.calories} kcal / P${CHOLE_KATORI.proteinG} C${CHOLE_KATORI.carbsG} F${CHOLE_KATORI.fatsG}
        - cooked rice bowl ~${RICE_BOWL.weightG} g → ~${RICE_BOWL.calories} kcal / P${RICE_BOWL.proteinG} C${RICE_BOWL.carbsG} F${RICE_BOWL.fatsG}
        - curd/raita katori ~${CURD_KATORI.weightG} g → ~${CURD_KATORI.calories} kcal / P${CURD_KATORI.proteinG} C${CURD_KATORI.carbsG} F${CURD_KATORI.fatsG}
        - dry sabzi katori ~${SABZI_DRY.weightG} g → ~${SABZI_DRY.calories} kcal / P${SABZI_DRY.proteinG} C${SABZI_DRY.carbsG} F${SABZI_DRY.fatsG}
        - 1 samosa ~${SAMOSA.weightG} g → ~${SAMOSA.calories} kcal / P${SAMOSA.proteinG} C${SAMOSA.carbsG} F${SAMOSA.fatsG}
        - cooking fat: 1 tsp ghee/oil ~${GHEE_TSP.weightG} g → ~${GHEE_TSP.calories} kcal (all fat)
    """.trimIndent()

    override val analyzeSystemIntro: String
        get() = PromptCatalog.regionText(region, "analyze_system_intro")
    override val targetSystemIntro: String
        get() = PromptCatalog.regionText(region, "target_system_intro")
    override val progressSystemIntro: String
        get() = PromptCatalog.regionText(region, "progress_system_intro")
    override val analyzePromptPriors: String
        get() = PromptCatalog.regionText(region, "analyze_prompt_priors")
    override val targetCoachNotes: String
        get() = PromptCatalog.regionText(region, "target_coach_notes")
    override val progressFoodGuidance: String
        get() = PromptCatalog.regionText(region, "progress_food_guidance")
    override val measurementPromptNotes: String
        get() = PromptCatalog.regionText(region, "measurement_prompt_notes")
    override val askPortionPromptNotes: String
        get() = PromptCatalog.regionText(region, "ask_portion_prompt_notes")

    override val foodLogHint: String =
        "e.g. \"2 rotis with dal tadka\" or \"aloo paratha with curd\""

    override val askPortionHint: String =
        "Know the dish but not grams? Estimates a standard home serving " +
            "(roti counts, katori volumes)."

    override val barcodeExample: String = "8901030865422"
}
