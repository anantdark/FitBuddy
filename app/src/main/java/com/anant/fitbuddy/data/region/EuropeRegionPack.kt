package com.anant.fitbuddy.data.region

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

    override val analyzeSystemIntro: String = """
        You are FitBuddy, a nutrition and fitness analysis engine optimised for (Western/
        Mediterranean) European home and casual-dining food.
    """.trimIndent()

    override val targetSystemIntro: String = """
        You are FitBuddy, a nutrition and body-composition coach optimised for European
        diets and lifestyles.
    """.trimIndent()

    override val progressSystemIntro: String = """
        You are FitBuddy, a supportive but honest fitness coach optimised for European
        diets and lifestyles.
    """.trimIndent()

    override val analyzePromptPriors: String = """
        Europe food priors (apply when identifying dishes from photos or loose text):
        - Default to Western/Mediterranean European home and casual-dining naming — bread- and
          olive-oil-forward staples, moderate portions vs. US fast food.
        - Mains: pasta dishes, roast/grilled chicken, grilled fish, stews/casseroles, risotto,
          schnitzel, sausages, sandwiches/paninis, pizza (thinner, less topping-heavy than US).
        - Breakfast: bread/rolls with butter/jam/cheese/cold cuts, croissants and other pastries,
          yogurt with muesli, coffee with milk.
        - Sides: boiled/roast potatoes, mixed salad with olive oil dressing, soup as a starter,
          bread as a near-universal side.
        - Cooking fats: when a dish is pan-fried, roasted, or dressed and oil is not stated,
          include "olive oil" as its own ingredient with a typical amount (~1 tbsp / 14 g).
        - Naming: use familiar European dish names in "dish_name". Break multi-course meals
          (starter/soup + main + side) into named components rather than "mixed plate".
    """.trimIndent()

    override val targetCoachNotes: String = """
        Keep macro splits practical for typical European meal patterns (bread- and
        vegetable-forward meals, olive oil as the primary added fat, moderate portions).
    """.trimIndent()

    override val progressFoodGuidance: String = """
        When suggesting food swaps, prefer familiar European options
        (grilled fish or chicken, salad with olive oil, yogurt, wholegrain bread, soup)
        over unfamiliar substitutes.
    """.trimIndent()

    override val foodLogHint: String =
        "e.g. \"pasta with tomato sauce\" or \"2 slices of bread with cheese\""

    override val askPortionHint: String =
        "Know the dish but not grams? Estimates a standard European home serving " +
            "(grams, millilitres, pieces)."

    override val measurementPromptNotes: String = """
        Portion / measurement language (Europe — metric for user text):
        - Prefer grams, millilitres, pieces, slices, and tbsp olive oil. Users may say
          "200 g pasta", "150 ml yogurt", "2 slices bread". Still output weight_g in grams.
        - Countable items without a count (rolls, eggs, slices) → prefer CLARIFICATION_REQUIRED.
        - Modest European restaurant portions vs oversized US fast food — don't inflate.
        - Never treat a leading count as grams ("4 almonds" ≠ 4 g).
    """.trimIndent()

    override val askPortionPromptNotes: String =
        "using typical European metric portion sizes (grams, millilitres, pieces, slices). " +
            "Include olive oil or butter as its own ingredient when the dish is dressed, " +
            "pan-fried, or roasted with fat."

    override val barcodeExample: String = "4006381333931"
}
