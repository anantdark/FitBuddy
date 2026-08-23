package com.anant.fitbuddy.data.model

/**
 * Curated OpenAI model metadata used when [com.anant.fitbuddy.data.settings.AiProvider.CUSTOM]
 * points at the official OpenAI API. Merged with the account's live `/v1/models` list and shown
 * as a client-side fallback so the dropdown is never empty before Refresh.
 */
object OpenAiCatalog {
    /** Official OpenAI API base URL (same default as [com.anant.fitbuddy.data.settings.AppSettings.DEFAULT_CUSTOM_BASE_URL]). */
    const val HOST_URL = "https://api.openai.com"

    /** Vision-capable defaults (photo analysis). */
    val VISION_MODELS: List<ModelOption> = listOf(
        ModelOption(id = "gpt-4o", displayName = "GPT-4o"),
        ModelOption(id = "gpt-4o-mini", displayName = "GPT-4o mini"),
        ModelOption(id = "gpt-4.1", displayName = "GPT-4.1"),
        ModelOption(id = "gpt-4.1-mini", displayName = "GPT-4.1 mini")
    )

    /** Chat defaults (typed logs / recalculation). */
    val TEXT_MODELS: List<ModelOption> = listOf(
        ModelOption(id = "gpt-4o-mini", displayName = "GPT-4o mini"),
        ModelOption(id = "gpt-4o", displayName = "GPT-4o"),
        ModelOption(id = "gpt-4.1-mini", displayName = "GPT-4.1 mini"),
        ModelOption(id = "gpt-4.1", displayName = "GPT-4.1")
    )
}
