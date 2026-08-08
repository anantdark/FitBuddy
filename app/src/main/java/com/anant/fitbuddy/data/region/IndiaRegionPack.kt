package com.anant.fitbuddy.data.region

/**
 * Indian region pack. Content is still North-Indian-dish-heavy (that's the app's original,
 * best-tuned prior set) but copy is broadened from "North Indian" to "Indian" throughout.
 */
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

    override val analyzeSystemIntro: String = """
        You are FitBuddy, a nutrition and fitness analysis engine optimised for Indian
        (Hindi belt / Punjabi / Delhi-NCR / UP / Haryana / Rajasthan, plus common South/West/East
        dishes) home and street food.
    """.trimIndent()

    override val targetSystemIntro: String = """
        You are FitBuddy, a nutrition and body-composition coach optimised for Indian
        diets and lifestyles.
    """.trimIndent()

    override val progressSystemIntro: String = """
        You are FitBuddy, a supportive but honest fitness coach optimised for Indian
        diets and lifestyles.
    """.trimIndent()

    override val analyzePromptPriors: String = """
        Indian food priors (apply when identifying dishes from photos or Hinglish text):
        - Default to North Indian names unless clear South/West/East markers are present
          (idli, dosa, sambar, coconut chutney, medu vada, appam, fish curry Kerala-style,
          misal, dhokla, momos with clear Tibetan plating, etc.).
        - Flatbreads: chapati / phulka / roti, tawa/stuffed paratha (aloo, gobi, paneer, mooli),
          laccha paratha, naan, kulcha, bhatura — NOT dosa/uttapam unless those are obvious.
        - Dals & curries: dal tadka/fry, dal makhani, rajma, chole/chana masala, kadhi pakora,
          paneer butter masala / palak paneer / kadhai paneer, butter chicken, egg bhurji,
          keema — prefer these over generic "curry" or sambar when cues match.
        - Sabzi / sides: aloo gobi, bhindi, baingan bharta, mixed veg, raita, salad, pickle, papad.
        - Staples & combos users type loosely: "roti sabzi", "dal chawal", "2 parantha",
          "chole bhature", "rajma chawal", "paneer bhurji", "aloo paratha with curd".
        - Street / snack: samosa, pakora, aloo tikki, golgappe/pani puri, chaat, pav bhaji,
          chole kulche, jalebi, lassi, chai.
        - Cooking fats: when the dish is tadka, fried, buttery, or makhani and oil/ghee is not
          stated, include "ghee" or "oil" as its OWN ingredient with a typical home amount
          (often ~1 tsp / 5 g). Do not invent coconut oil unless the dish implies it.
        - Naming: use familiar Indian dish names in "dish_name" (Hinglish ok). Break
          thalis and combos into named components (roti, dal, sabzi, rice, raita, bhatura)
          rather than "mixed plate" — especially chole bhature, rajma chawal, dal chawal.
    """.trimIndent()

    override val targetCoachNotes: String = """
        Keep splits practical for Indian meals (roti/dal/sabzi, rice/dal/sabzi, curd, paneer).
    """.trimIndent()

    override val progressFoodGuidance: String = """
        When suggesting food swaps, prefer familiar Indian options
        (dal, roti, sabzi, dahi, paneer, chole, rajma) over unfamiliar Western substitutes.
    """.trimIndent()

    override val foodLogHint: String =
        "e.g. \"2 rotis with dal tadka\" or \"aloo paratha with curd\""

    override val askPortionHint: String =
        "Know the dish but not grams? Estimates a standard home serving " +
            "(roti counts, katori volumes)."

    override val measurementPromptNotes: String = """
        Portion / measurement language (India):
        - Prefer household units users type: roti/paratha/naan counts, katori/bowl for dal/sabzi/
          curd/rice, "ek plate", tsp/tbsp ghee or oil. Still output weight_g in grams.
        - Countable staples without a count → CLARIFICATION_REQUIRED (how many rotis/parathas/…).
        - "ek katori" / "1 bowl" / "do roti" / Hinglish counts (ek, do, teen…) map to one unit prior.
        - Never treat a leading count as grams ("4 almonds" ≠ 4 g).
    """.trimIndent()

    override val askPortionPromptNotes: String =
        "using typical Indian portion sizes (roti counts, katori volumes, rice bowls). " +
            "Include cooking fat (ghee/oil) as its own ingredient when the dish is tadka, fried, or buttery."

    override val barcodeExample: String = "8901030865422"
}
