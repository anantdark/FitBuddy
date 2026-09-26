package com.anant.fitbuddy.data.donors

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DonorsFile(
    val updatedAt: String = "",
    val donors: List<DonorEntry> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class DonorEntry(
    val hash: String = "",
    val name: String? = null,
    val photoUrl: String? = null,
    /** Optional profile / website URL; tapping the donor row opens it. */
    val linkUrl: String? = null,
) {
    val displayName: String?
        get() = name?.trim()?.takeIf { it.isNotEmpty() }

    val hasDisplayInfo: Boolean
        get() = displayName != null

    val normalizedHash: String
        get() = hash.trim().lowercase()

    val profileLink: String?
        get() = linkUrl?.trim()?.takeIf { it.isNotEmpty() }
}

/** Hard-coded donors for Developer → Testing preview / thank-you. */
object DemoDonors {
    val all: List<DonorEntry> = listOf(
        DonorEntry(
            hash = "demo-hash-named-with-photo",
            name = "Ada Lovelace",
            photoUrl = "https://avatars.githubusercontent.com/u/9919?s=128",
            linkUrl = "https://en.wikipedia.org/wiki/Ada_Lovelace",
        ),
        DonorEntry(
            hash = "demo-hash-named-no-photo",
            name = "Grace Hopper",
            linkUrl = "https://en.wikipedia.org/wiki/Grace_Hopper",
        ),
        DonorEntry(
            hash = "demo-hash-only-no-thank-you",
        ),
    )

    val forThankYou: List<DonorEntry>
        get() = all.filter { it.hasDisplayInfo }
}
