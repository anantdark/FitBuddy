package com.anant.fitbuddy.data.region

/** United States region pack: American home/street food, typical restaurant/home portions. */
object UsRegionPack : RegionPack {

    override val region: AppRegion = AppRegion.US
    override val displayName: String = "United States"

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

    override val analyzeSystemIntro: String = """
        You are FitBuddy, a nutrition and fitness analysis engine optimised for United States
        home and restaurant/takeout food (diner, fast-casual, deli, and home-cooked staples).
    """.trimIndent()

    override val targetSystemIntro: String = """
        You are FitBuddy, a nutrition and body-composition coach optimised for United States
        diets and lifestyles.
    """.trimIndent()

    override val progressSystemIntro: String = """
        You are FitBuddy, a supportive but honest fitness coach optimised for United States
        diets and lifestyles.
    """.trimIndent()

    override val analyzePromptPriors: String = """
        US food priors (apply when identifying dishes from photos or loose text):
        - Default to American home/fast-casual/diner naming and portion cues (plates, containers,
          takeout boxes) rather than metric-bowl framing.
        - Mains: burgers/cheeseburgers, sandwiches/subs/hoagies, pizza (by slice or whole), pasta
          bowls, grilled/fried chicken, steak, tacos/burritos, mac and cheese, chili.
        - Breakfast: scrambled/fried eggs, bacon, pancakes/waffles, oatmeal, bagels, cereal,
          breakfast burritos, toast with peanut butter or butter.
        - Sides / snacks: French fries, chips, side salad, coleslaw, dinner rolls, chips & salsa.
        - Drinks/extras that carry real calories: soda, sweetened coffee drinks, protein shakes,
          ranch/BBQ/other dipping sauces — call these out as separate ingredients when mentioned.
        - Portion cues: fast-food sizes (small/medium/large), "footlong" sub, "personal pan" pizza,
          "family size" — use these to scale weight/calories realistically.
        - Naming: use familiar American dish names in "dish_name". Break combo meals (e.g. burger
          + fries + soda) into named components rather than "mixed plate".
    """.trimIndent()

    override val targetCoachNotes: String = """
        Keep macro splits practical for typical US meal patterns (protein + starch + veg at
        dinner, sandwich/salad lunches, higher-protein breakfasts like eggs or Greek yogurt).
    """.trimIndent()

    override val progressFoodGuidance: String = """
        When suggesting food swaps, prefer familiar American options
        (grilled chicken, salad, Greek yogurt, oatmeal, whole-grain bread) over unfamiliar
        substitutes, and flag high-calorie fast-food/soda patterns when the data shows them.
    """.trimIndent()

    override val foodLogHint: String =
        "e.g. \"cheeseburger with fries\" or \"2 scrambled eggs and toast\""

    override val askPortionHint: String =
        "Know the dish but not grams? Estimates a standard US home/restaurant serving " +
            "(cups, ounces, slices)."

    override val measurementPromptNotes: String = """
        Portion / measurement language (United States — US customary for user text):
        - Prefer cups, fl oz, oz by weight, tbsp/tsp, slices, pieces, and fast-food sizes
          (small/medium/large). Still output weight_g in grams (convert cups/oz → g).
        - Countable items without a count (slices of pizza, eggs, cookies) → prefer
          CLARIFICATION_REQUIRED asking how many / which size.
        - "1 cup oatmeal", "8 oz steak", "2 tbsp peanut butter", "medium fries" are normal.
        - Never treat a leading count as grams ("4 almonds" ≠ 4 g).
    """.trimIndent()

    override val askPortionPromptNotes: String =
        "using typical United States portion sizes (cups, ounces, slices, tbsp). " +
            "Include cooking oil/butter/dressing as its own ingredient when the dish is fried, " +
            "buttered, or dressed."

    override val barcodeExample: String = "0012345678905"
}
