package com.anant.fitbuddy.data.region

import com.anant.fitbuddy.data.prompts.PromptCatalog

/** North America (US + Canada) region pack: home/street food, typical portions. */
object UsRegionPack : RegionPack {

    override val region: AppRegion = AppRegion.US
    override val displayName: String = "North America"

    val SANDWICH = RegionalDish("Deli sandwich", 230, 420, 20, 45, 16, listOf("sandwich", "sub", "hoagie"))
    val BURGER = RegionalDish("Cheeseburger", 220, 550, 27, 40, 30, listOf("burger", "cheeseburger", "hamburger"))
    val PIZZA_SLICE = RegionalDish("Pizza slice", 115, 285, 12, 34, 11, listOf("pizza"))
    val OATMEAL_BOWL = RegionalDish("Oatmeal bowl", 240, 210, 7, 37, 4, listOf("oatmeal", "porridge"))
    val GRILLED_CHICKEN = RegionalDish("Grilled chicken breast", 170, 280, 52, 0, 6, listOf("grilled chicken", "chicken breast"))
    val SALAD_BOWL = RegionalDish("Salad bowl w/ dressing", 300, 320, 12, 18, 22, listOf("salad"))
    val SCRAMBLED_EGGS = RegionalDish("Scrambled eggs (2)", 120, 220, 14, 2, 16, listOf("scrambled eggs", "eggs"))
    val BAGEL = RegionalDish("Bagel w/ cream cheese", 130, 350, 12, 55, 10, listOf("bagel"))
    val FRENCH_FRIES = RegionalDish("French fries (medium)", 115, 365, 4, 48, 17, listOf("french fries", "fries"))
    val PASTA_BOWL = RegionalDish("Pasta bowl w/ sauce", 300, 460, 16, 68, 12, listOf("pasta", "spaghetti"))
    val YOGURT_CUP = RegionalDish("Yogurt cup", 170, 150, 12, 18, 4, listOf("yogurt", "greek yogurt"))
    val PEANUT_BUTTER_TBSP = RegionalDish("Peanut butter (1 tbsp)", 16, 95, 4, 3, 8, listOf("peanut butter", "pb"))

    override val staples: List<RegionalDish> = listOf(
        SANDWICH, BURGER, PIZZA_SLICE, OATMEAL_BOWL, GRILLED_CHICKEN, SALAD_BOWL,
        SCRAMBLED_EGGS, BAGEL, FRENCH_FRIES, PASTA_BOWL, YOGURT_CUP, PEANUT_BUTTER_TBSP
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
        "e.g. \"cheeseburger with fries\" or \"2 scrambled eggs and toast\""

    override val askPortionHint: String =
        "Know the dish but not grams? Estimates a standard North American home/restaurant " +
            "serving (cups, ounces, slices)."

    override val barcodeExample: String = "0012345678905"
}
