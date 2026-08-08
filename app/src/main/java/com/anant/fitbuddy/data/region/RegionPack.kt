package com.anant.fitbuddy.data.region

/**
 * Region-specific content injected into AI prompts (analyze / target / progress) and used by
 * the offline simulator. One implementation per [AppRegion].
 */
interface RegionPack {
    val region: AppRegion
    val displayName: String

    /** Typical staple dishes/portions for this region, with offline-sim match keywords. */
    val staples: List<RegionalDish>

    /** Region-specific food identification priors for the analyze prompt. */
    val analyzePromptPriors: String

    /** Short notes on meal-split / diet style conventions for the target-plan prompt. */
    val targetCoachNotes: String

    /** Food swap guidance for progress-report / progress-chat prompts. */
    val progressFoodGuidance: String

    /** First lines of the analyze system prompt, e.g. "...optimised for ...". */
    val analyzeSystemIntro: String

    /** First lines of the target-plan system prompt. */
    val targetSystemIntro: String

    /** First lines of the progress-report/chat system prompt. */
    val progressSystemIntro: String

    /** Example line under the text-log dialog, e.g. `e.g. "2 rotis with dal tadka"…`. */
    val foodLogHint: String

    /** Helper under "Ask for portion" in the food review sheet. */
    val askPortionHint: String

    /**
     * Prompt block for portion/quantity language in this region (katori vs cups vs g/ml).
     * Injected into the analyze system prompt; macros still reported in grams.
     */
    val measurementPromptNotes: String

    /** Suffix for the "ask for portion" re-analyze user prompt. */
    val askPortionPromptNotes: String

    /** Example barcode digits for the manual barcode entry placeholder. */
    val barcodeExample: String

    /** Compact staple reference table injected into the analysis system prompt. */
    fun promptReferenceTable(): String {
        val lines = staples.joinToString("\n") { d ->
            "        - ${d.name} ~${d.weightG} g → ~${d.calories} kcal / " +
                "P${d.proteinG} C${d.carbsG} F${d.fatsG}"
        }
        return """
        Staple reference (typical serving; adjust to size/count — priors, not hard law):
$lines
        """.trimIndent()
    }
}

object RegionPacks {
    fun pack(region: AppRegion): RegionPack = when (region) {
        AppRegion.INDIA -> IndiaRegionPack
        AppRegion.US -> UsRegionPack
        AppRegion.EUROPE -> EuropeRegionPack
    }

    fun packOrIndia(region: AppRegion?): RegionPack = pack(region ?: AppRegion.INDIA)
}
