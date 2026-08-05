package com.anant.fitbuddy.data.settings

import com.anant.fitbuddy.data.remote.dto.ModelCatalogModality

/**
 * Hardcoded Auto-failover ladders from the 2026-08 free-model benchmark
 * (`tools/benchmark/CATALOG_REVIEW.md`).
 *
 * Failover order = user-selected model first, then this ladder (intersected with
 * the live catalog when available), then any remaining catalog ids. Catalog
 * “intelligence” ranking must not reorder this list.
 *
 * Re-run `skills/benchmark-free-models` when free catalogs change.
 */
object FailoverLadders {

    /** Source note for Settings / agents. */
    const val BENCHMARK_DOC = "tools/benchmark/CATALOG_REVIEW.md"

    /**
     * Balanced text ladders (champion → fast/accurate backups).
     * OpenRouter / Gemini / Ollama free tiers from the approved review.
     */
    val TEXT: Map<AiProvider, List<String>> = mapOf(
        AiProvider.OPENROUTER to listOf(
            "inclusionai/ling-3.0-flash:free",
            "google/gemma-4-26b-a4b-it:free",
            "nvidia/nemotron-3-super-120b-a12b:free",
            "poolside/laguna-xs-2.1:free",
            "nvidia/nemotron-3-ultra-550b-a55b:free",
            "nvidia/nemotron-3-nano-30b-a3b:free"
        ),
        AiProvider.GEMINI to listOf(
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-3.6-flash",
            "gemini-3-flash-preview",
            "gemini-flash-lite-latest",
            "gemini-flash-latest",
            "gemini-2.5-flash"
        ),
        AiProvider.OLLAMA to listOf(
            "minimax-m3",
            "gemma4:31b",
            "nemotron-3-nano:30b",
            "gpt-oss:120b",
            "nemotron-3-ultra",
            "nemotron-3-super",
            "gpt-oss:20b"
        ),
        // Paid curated — keep OpenAI catalog order as the ladder.
        AiProvider.OPENAI to listOf(
            "gpt-4o-mini",
            "gpt-4o",
            "gpt-4.1-mini",
            "gpt-4.1"
        )
    )

    /**
     * Photo / vision ladders: Flash (non-lite) first on Gemini; multimodal
     * cousins of the text champions elsewhere. Not separately meal-benchmarked.
     */
    val PHOTO: Map<AiProvider, List<String>> = mapOf(
        AiProvider.OPENROUTER to listOf(
            "google/gemma-4-26b-a4b-it:free",
            "nvidia/nemotron-3-super-120b-a12b:free",
            "nvidia/nemotron-3-nano-30b-a3b:free",
            "nvidia/nemotron-3-ultra-550b-a55b:free",
            "inclusionai/ling-3.0-flash:free"
        ),
        AiProvider.GEMINI to listOf(
            "gemini-flash-latest",
            "gemini-3.6-flash",
            "gemini-3.5-flash",
            "gemini-3-flash-preview",
            "gemini-2.5-flash",
            "gemini-3.5-flash-lite",
            "gemini-3.1-flash-lite",
            "gemini-flash-lite-latest"
        ),
        AiProvider.OLLAMA to listOf(
            "gemma4:31b",
            "minimax-m3",
            "nemotron-3-nano:30b",
            "gpt-oss:120b",
            "nemotron-3-ultra",
            "nemotron-3-super"
        ),
        AiProvider.OPENAI to listOf(
            "gpt-4o-mini",
            "gpt-4o",
            "gpt-4.1-mini",
            "gpt-4.1"
        )
    )

    fun ladder(provider: AiProvider, modality: ModelCatalogModality): List<String> =
        when (modality) {
            ModelCatalogModality.TEXT -> TEXT[provider].orEmpty()
            ModelCatalogModality.PHOTO -> PHOTO[provider].orEmpty()
        }

    /** Built-in default preferred id (ladder head). */
    fun preferredDefault(provider: AiProvider, modality: ModelCatalogModality): String =
        ladder(provider, modality).firstOrNull().orEmpty()

    /**
     * Auto-failover attempt order:
     * 1. [selected] (if non-blank)
     * 2. Hardcoded ladder entries that appear in [catalogIds] (or full ladder if catalog empty)
     * 3. Remaining catalog ids not already listed (stable alphabetical)
     */
    fun buildAttemptOrder(
        provider: AiProvider,
        modality: ModelCatalogModality,
        selected: String,
        catalogIds: List<String>
    ): List<String> {
        val ladder = ladder(provider, modality)
        val catalogSet = catalogIds.toSet()
        val useCatalogFilter = catalogSet.isNotEmpty()
        val fromLadder = if (useCatalogFilter) {
            ladder.filter { it in catalogSet }
        } else {
            ladder
        }
        val remainder = if (useCatalogFilter) {
            catalogIds.filter { id -> id !in fromLadder && id != selected }.sorted()
        } else {
            emptyList()
        }
        return buildList {
            if (selected.isNotBlank()) add(selected)
            fromLadder.filter { it != selected }.forEach { add(it) }
            remainder.forEach { add(it) }
        }.distinct()
    }

    /**
     * Dropdown order: ladder models first (in ladder order), then the rest A–Z.
     * Only ids present in [catalogIds] are returned.
     */
    fun orderCatalog(provider: AiProvider, modality: ModelCatalogModality, catalogIds: List<String>): List<String> {
        if (catalogIds.isEmpty()) return emptyList()
        val ladder = ladder(provider, modality)
        val set = catalogIds.toSet()
        val head = ladder.filter { it in set }
        val tail = catalogIds.filter { it !in head }.sorted()
        return head + tail
    }

    /** Next ladder (or catalog) pick when [missingId] is gone from the live list. */
    fun nextBest(
        provider: AiProvider,
        modality: ModelCatalogModality,
        catalogIds: List<String>,
        missingId: String
    ): String? {
        val ordered = orderCatalog(provider, modality, catalogIds)
        return ordered.firstOrNull { it != missingId } ?: ordered.firstOrNull()
    }
}
