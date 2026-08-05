package com.anant.fitbuddy.data.settings

import com.anant.fitbuddy.data.remote.dto.ModelCatalogModality
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Auto-failover ladders loaded from [CONFIG_RESOURCE] (`config/failover_ladders.json`).
 *
 * Order: user-selected model first → config ladder ∩ live catalog → remaining catalog
 * ids A–Z. Update the JSON (not this file) when free catalogs change; see
 * `skills/benchmark-free-models`.
 */
object FailoverLadders {

    const val CONFIG_RESOURCE = "/failover_ladders.json"
    const val CONFIG_PATH = "config/failover_ladders.json"

    @JsonClass(generateAdapter = true)
    data class LadderFile(
        @Json(name = "version") val version: Int = 1,
        @Json(name = "updated") val updated: String? = null,
        @Json(name = "source") val source: String? = null,
        @Json(name = "text") val text: Map<String, List<String>> = emptyMap(),
        @Json(name = "photo") val photo: Map<String, List<String>> = emptyMap()
    )

    private val moshi: Moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    private val fileAdapter by lazy { moshi.adapter(LadderFile::class.java) }

    /** Parsed config (lazy; classpath resource from `config/`). */
    val config: LadderFile by lazy { loadConfig() }

    val TEXT: Map<AiProvider, List<String>>
        get() = toProviderMap(config.text)

    val PHOTO: Map<AiProvider, List<String>>
        get() = toProviderMap(config.photo)

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
     * 1. [selected] (if non-blank) — most preferred until the user picks something else
     * 2. Config ladder entries present in [catalogIds] (or full ladder if catalog empty)
     * 3. Remaining catalog ids not on the ladder (stable alphabetical) — end of ladder
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
    fun orderCatalog(
        provider: AiProvider,
        modality: ModelCatalogModality,
        catalogIds: List<String>
    ): List<String> {
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

    private fun toProviderMap(raw: Map<String, List<String>>): Map<AiProvider, List<String>> {
        val out = linkedMapOf<AiProvider, List<String>>()
        for (provider in AiProvider.entries) {
            val ids = raw[provider.name].orEmpty().map { it.trim() }.filter { it.isNotEmpty() }
            if (ids.isNotEmpty()) out[provider] = ids
        }
        return out
    }

    private fun loadConfig(): LadderFile {
        val stream = FailoverLadders::class.java.getResourceAsStream(CONFIG_RESOURCE)
            ?: error(
                "Missing $CONFIG_RESOURCE on classpath. " +
                    "Ensure config/failover_ladders.json is wired via app resources.srcDir."
            )
        val json = stream.bufferedReader().use { it.readText() }
        return fileAdapter.fromJson(json)
            ?: error("Could not parse $CONFIG_PATH")
    }
}
