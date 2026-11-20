package com.anant.fitbuddy.data.donors

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DonorsFile(
    val updatedAt: String = "",
    val donors: List<DonorEntry> = emptyList(),
) {
    /** Assign sequential roster indices (JSON order) for supporter numbers. */
    fun withRosterIndices(): DonorsFile =
        copy(donors = donors.mapIndexed { index, donor -> donor.copy(rosterIndex = index) })
}

@JsonClass(generateAdapter = true)
data class DonationRecord(
    /** Optional stable id; date (`at`) drives thank-you visibility. */
    val id: String = "",
    /** USD amount for backend tier math only — never shown in UI. */
    val amountUsd: Double = 0.0,
    /** ISO date `yyyy-MM-dd`. */
    val at: String = "",
) {
    val normalizedAt: String
        get() = at.trim()
}

@JsonClass(generateAdapter = true)
data class DonorEntry(
    val hash: String = "",
    val name: String? = null,
    val photoUrl: String? = null,
    /** Optional profile / website URL; opened from the card flip-side panel only. */
    val linkUrl: String? = null,
    /**
     * Non-amount badges (e.g. `"og"`). Amount tiers are derived from [donations], not tags.
     */
    val tags: List<String> = emptyList(),
    val donations: List<DonationRecord> = emptyList(),
    /** 0-based index in `donors.json` (not serialized). Supporter No. = 1001 + index. */
    @Json(ignore = true) val rosterIndex: Int = -1,
) {
    val displayName: String?
        get() = name?.trim()?.takeIf { it.isNotEmpty() }

    val hasDisplayInfo: Boolean
        get() = displayName != null

    val normalizedHash: String
        get() = hash.trim().lowercase()

    val profileLink: String?
        get() = linkUrl?.trim()?.takeIf { it.isNotEmpty() }

    val normalizedTags: List<String>
        get() = tags.map { it.trim().lowercase() }.filter { it.isNotEmpty() }

    fun hasTag(tag: String): Boolean =
        tag.trim().lowercase() in normalizedTags
}
