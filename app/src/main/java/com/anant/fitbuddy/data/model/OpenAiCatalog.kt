package com.anant.fitbuddy.data.model

/**
 * Curated OpenAI model metadata for the dedicated [com.anant.fitbuddy.data.settings.AiProvider.OPENAI]
 * provider. Shared by the network layer (merged with the account's live `/v1/models` list) and
 * Settings (shown as a client-side fallback so the dropdown is never empty when automatic
 * model-list refresh is off for paid endpoints).
 */
object OpenAiCatalog {
    /** Official OpenAI API base URL (fixed — the provider is no longer detected via URL sniffing). */
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
