package com.anant.fitbuddy.data.region

import com.anant.fitbuddy.data.prompts.PromptCatalog

/**
 * Latin America region pack: Mexican, Andean, Southern Cone, and broader LatAm home /
 * street-food commons (covers Mexico, Chile, Uruguay, and neighbouring diets).
 */
object LatinAmericaRegionPack : RegionPack {

    override val region: AppRegion = AppRegion.LATIN_AMERICA
    override val displayName: String = "Latin America"

    val CORN_TORTILLA = RegionalDish(
        "Corn tortilla", 30, 65, 2, 13, 1, listOf("tortilla", "tortillas")
    )
    val TACO = RegionalDish(
        "Taco (filled)", 110, 210, 12, 18, 9, listOf("taco", "tacos")
    )
    val RICE_BEANS = RegionalDish(
        "Rice and beans", 280, 320, 12, 55, 5, listOf("rice and beans", "arroz y frijoles", "feijoada")
    )
    val EMPANADA = RegionalDish(
        "Empanada", 100, 260, 9, 24, 14, listOf("empanada", "empanadas")
    )
    val AREPA = RegionalDish(
        "Arepa (filled)", 150, 280, 11, 32, 11, listOf("arepa", "arepas")
    )
    val CEVICHE = RegionalDish(
        "Ceviche portion", 200, 180, 22, 10, 4, listOf("ceviche")
    )
    val ASADO = RegionalDish(
        "Grilled meat (asado)", 180, 380, 36, 0, 26, listOf("asado", "churrasco", "carne asada")
    )
    val AVOCADO = RegionalDish(
        "Avocado / guacamole", 100, 160, 2, 9, 15, listOf("avocado", "guacamole", "aguacate")
    )
    val PLANTAIN = RegionalDish(
        "Fried plantain", 120, 250, 1, 40, 10, listOf("plantain", "plátano", "tostones", "maduro")
    )
    val BLACK_BEANS = RegionalDish(
        "Black beans (bowl)", 200, 230, 14, 38, 2, listOf("black beans", "frijoles negros", "frijoles")
    )
    val AREPAS_CHEESE = RegionalDish(
        "Cheese pupusa / quesadilla", 140, 320, 14, 28, 15,
        listOf("pupusa", "quesadilla", "sincronizada")
    )
    val OIL_TBSP = RegionalDish(
        "Cooking oil (1 tbsp)", 14, 120, 0, 0, 14, listOf("oil", "aceite", "manteca")
    )

    override val staples: List<RegionalDish> = listOf(
        CORN_TORTILLA, TACO, RICE_BEANS, EMPANADA, AREPA, CEVICHE,
        ASADO, AVOCADO, PLANTAIN, BLACK_BEANS, AREPAS_CHEESE, OIL_TBSP
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
        "e.g. \"2 tacos al pastor\" or \"arroz con pollo with beans\""

    override val askPortionHint: String =
        "Know the dish but not grams? Estimates a standard LatAm home/street serving " +
            "(piezas, tazas, platos)."

    /** Mexico GS1 prefix example. */
    override val barcodeExample: String = "7501031311309"
}
