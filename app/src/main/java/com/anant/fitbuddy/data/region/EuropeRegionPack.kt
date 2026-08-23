package com.anant.fitbuddy.data.region

import com.anant.fitbuddy.data.prompts.PromptCatalog

/** Europe region pack: Mediterranean/Western European home and casual-dining commons. */
object EuropeRegionPack : RegionPack {

    override val region: AppRegion = AppRegion.EUROPE
    override val displayName: String = "Europe"

    val BREAD_ROLL = RegionalDish("Bread roll", 60, 165, 6, 30, 2, listOf("bread roll", "bread", "baguette"))
    val CHEESE_PORTION = RegionalDish("Cheese portion", 30, 110, 7, 1, 9, listOf("cheese"))
    val PASTA_BOWL = RegionalDish("Pasta bowl w/ sauce", 300, 440, 15, 65, 12, listOf("pasta", "spaghetti"))
    val CROISSANT = RegionalDish("Croissant", 60, 230, 5, 26, 12, listOf("croissant"))
    val YOGURT = RegionalDish("Yogurt pot", 150, 130, 8, 12, 5, listOf("yogurt", "yoghurt"))
    val SALAD = RegionalDish("Salad w/ olive oil dressing", 250, 220, 6, 12, 17, listOf("salad"))
    val GRILLED_FISH = RegionalDish("Grilled fish fillet", 150, 220, 32, 0, 9, listOf("grilled fish", "fish"))
    val ROAST_CHICKEN = RegionalDish("Roast chicken portion", 170, 300, 34, 0, 17, listOf("roast chicken", "chicken"))
    val POTATOES = RegionalDish("Boiled/roast potatoes", 200, 220, 4, 44, 4, listOf("potatoes", "potato"))
    val SOUP_BOWL = RegionalDish("Soup bowl", 300, 180, 6, 20, 8, listOf("soup"))
    val SANDWICH = RegionalDish("Sandwich/baguette", 200, 380, 16, 46, 14, listOf("sandwich", "panini"))
    val OLIVE_OIL_TBSP = RegionalDish("Olive oil (1 tbsp)", 14, 120, 0, 0, 14, listOf("olive oil"))

    override val staples: List<RegionalDish> = listOf(
        BREAD_ROLL, CHEESE_PORTION, PASTA_BOWL, CROISSANT, YOGURT, SALAD,
        GRILLED_FISH, ROAST_CHICKEN, POTATOES, SOUP_BOWL, SANDWICH, OLIVE_OIL_TBSP
    )

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
        "e.g. \"pasta with tomato sauce\" or \"2 slices of bread with cheese\""

    override val askPortionHint: String =
        "Know the dish but not grams? Estimates a standard European home serving " +
            "(grams, millilitres, pieces)."

    override val barcodeExample: String = "4006381333931"
}
