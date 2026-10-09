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

/** Hard-coded donors for Developer → Testing preview / thank-you (enough rows to scroll). */
object DemoDonors {
    /** Bundled portrait for offline demo cards (`app/src/main/assets/donors/`). */
    private const val PHILAUTIAN_ASSET =
        "file:///android_asset/donors/philautian.jpg"

    val all: List<DonorEntry> = listOf(
        DonorEntry(
            hash = "146a345312644c125e4ae4aae2ceec64fe10e2da99f451bf8d12e656d4ca460b",
            name = "Philautian",
            photoUrl = PHILAUTIAN_ASSET,
            linkUrl = "https://www.instagram.com/philautian_art/",
            tags = listOf("og"),
            donations = listOf(DonationRecord(amountUsd = 1.0, at = "2026-10-09")),
        ),
        DonorEntry(
            hash = "demo-hash-01",
            name = "Ada Lovelace",
            photoUrl = "https://avatars.githubusercontent.com/u/9919?s=128",
            linkUrl = "https://github.com/torvalds",
            tags = listOf("og"),
            donations = listOf(DonationRecord(amountUsd = 12.0, at = "2026-01-05")),
        ),
        DonorEntry(
            hash = "demo-hash-02",
            name = "Grace Hopper",
            linkUrl = "https://www.instagram.com/instagram/",
            tags = listOf("og"),
            donations = listOf(DonationRecord(amountUsd = 4.0, at = "2026-01-06")),
        ),
        DonorEntry(
            hash = "demo-hash-03",
            name = "Alan Turing",
            photoUrl = "https://avatars.githubusercontent.com/u/2?s=128",
            linkUrl = "https://en.wikipedia.org/wiki/Alan_Turing",
            tags = listOf("og"),
            donations = listOf(DonationRecord(amountUsd = 7.0, at = "2026-01-07")),
        ),
        DonorEntry(
            hash = "demo-hash-04",
            name = "Katherine Johnson",
            tags = listOf("og"),
            donations = listOf(DonationRecord(amountUsd = 2.0, at = "2026-01-08")),
        ),
        DonorEntry(
            hash = "demo-hash-05",
            name = "Claude Shannon",
            linkUrl = "https://github.com/github",
            tags = listOf("og"),
            donations = listOf(DonationRecord(amountUsd = 15.0, at = "2026-02-01")),
        ),
        DonorEntry(
            hash = "demo-hash-06",
            name = "Margaret Hamilton",
            photoUrl = "https://avatars.githubusercontent.com/u/3?s=128",
            donations = listOf(DonationRecord(amountUsd = 6.0, at = "2026-02-10")),
        ),
        DonorEntry(
            hash = "demo-hash-07",
            name = "Tim Berners-Lee",
            linkUrl = "https://www.w3.org/",
            donations = listOf(
                DonationRecord(amountUsd = 2.0, at = "2026-02-11"),
                DonationRecord(amountUsd = 2.5, at = "2026-03-01"),
            ),
        ),
        DonorEntry(
            hash = "demo-hash-08",
            name = "Radia Perlman",
            donations = listOf(DonationRecord(amountUsd = 11.0, at = "2026-03-05")),
        ),
        DonorEntry(
            hash = "demo-hash-09",
            name = "Dennis Ritchie",
            photoUrl = "https://avatars.githubusercontent.com/u/4?s=128",
            linkUrl = "https://github.com/golang",
            donations = listOf(DonationRecord(amountUsd = 3.5, at = "2026-03-08")),
        ),
        DonorEntry(
            hash = "demo-hash-10",
            name = "Barbara Liskov",
            linkUrl = "https://www.instagram.com/meta/",
            donations = listOf(DonationRecord(amountUsd = 8.0, at = "2026-03-12")),
        ),
        DonorEntry(
            hash = "demo-hash-only-no-thank-you",
            donations = listOf(DonationRecord(amountUsd = 5.0, at = "2026-03-12")),
        ),
    ).mapIndexed { index, donor -> donor.copy(rosterIndex = index) }

    val forThankYou: List<DonorEntry>
        get() = all.filter { it.hasDisplayInfo }

    val forGallery: List<DonorEntry>
        get() = all.filter { it.hasDisplayInfo }
}
