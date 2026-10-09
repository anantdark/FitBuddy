package com.anant.fitbuddy.data.donors

/**
 * Public badge / card-style labels. Amount thresholds live only here for derivation —
 * never surface amounts or threshold copy in the UI.
 */
enum class MoneyBadge {
    GENEROUS,
    LEGENDARY,
    GODLIKE,
}

/** Visual theme for the trading card (one primary style per person). */
enum class DonorCardStyle {
    /** Tag `og` and no money badge yet. */
    OG,
    GENEROUS,
    LEGENDARY,
    GODLIKE,
    /** Named supporter with neither OG nor a money badge. */
    STANDARD,
}

/**
 * A medal shown on the card. Money badges are exclusive (highest only);
 * tag badges (e.g. OG) stack alongside.
 */
sealed class SupporterBadge {
    abstract val id: String
    abstract val title: String
    abstract val blurb: String

    data object Og : SupporterBadge() {
        override val id: String = "og"
        override val title: String = "OG Supporter"
        override val blurb: String =
            "Among the first to believe in FitBuddy. Forever part of the origin story."
    }

    data object Generous : SupporterBadge() {
        override val id: String = "generous"
        override val title: String = "Generous Supporter"
        override val blurb: String =
            "A warm boost that keeps the chai (and the commits) flowing."
    }

    data object Legendary : SupporterBadge() {
        override val id: String = "legendary"
        override val title: String = "Legendary Supporter"
        override val blurb: String =
            "Rare energy. Features land faster because of people like this."
    }

    data object Godlike : SupporterBadge() {
        override val id: String = "godlike"
        override val title: String = "Godlike Supporter"
        override val blurb: String =
            "Mythic backing. The kind of support that bends roadmaps."
    }

    data class Custom(val tag: String) : SupporterBadge() {
        override val id: String = tag.trim().lowercase()
        override val title: String =
            tag.trim().replace('_', ' ').replace('-', ' ')
                .split(' ')
                .filter { it.isNotEmpty() }
                .joinToString(" ") { part ->
                    part.replaceFirstChar { ch -> ch.titlecase() }
                }
                .ifBlank { "Supporter" }
        override val blurb: String =
            "A special recognition in the FitBuddy supporter series."
    }
}

object DonorBadgeRules {
    /** Lifetime USD → exclusive money badge (null if below the first rung). */
    fun moneyBadge(lifetimeUsd: Double): MoneyBadge? = when {
        lifetimeUsd > 10.0 -> MoneyBadge.GODLIKE
        lifetimeUsd > 5.0 -> MoneyBadge.LEGENDARY
        lifetimeUsd > 3.0 -> MoneyBadge.GENEROUS
        else -> null
    }

    fun lifetimeUsd(donor: DonorEntry): Double =
        donor.donations.sumOf { it.amountUsd.coerceAtLeast(0.0) }

    fun moneyBadge(donor: DonorEntry): MoneyBadge? =
        moneyBadge(lifetimeUsd(donor))

    /**
     * Medals to show: highest money badge (if any) + tag badges (`og` and future tags).
     * Unknown tags become [SupporterBadge.Custom].
     */
    fun badges(donor: DonorEntry): List<SupporterBadge> {
        val out = mutableListOf<SupporterBadge>()
        when (moneyBadge(donor)) {
            MoneyBadge.GODLIKE -> out += SupporterBadge.Godlike
            MoneyBadge.LEGENDARY -> out += SupporterBadge.Legendary
            MoneyBadge.GENEROUS -> out += SupporterBadge.Generous
            null -> Unit
        }
        val reserved = setOf("generous", "legendary", "godlike")
        for (tag in donor.normalizedTags.distinct()) {
            when (tag) {
                "og" -> out += SupporterBadge.Og
                in reserved -> Unit // amount tiers are never driven by tags
                else -> out += SupporterBadge.Custom(tag)
            }
        }
        return out
    }

    /**
     * Card chrome: money tier wins; otherwise OG tag; otherwise standard.
     */
    fun cardStyle(donor: DonorEntry): DonorCardStyle = when (moneyBadge(donor)) {
        MoneyBadge.GODLIKE -> DonorCardStyle.GODLIKE
        MoneyBadge.LEGENDARY -> DonorCardStyle.LEGENDARY
        MoneyBadge.GENEROUS -> DonorCardStyle.GENEROUS
        null -> if (donor.hasTag("og")) DonorCardStyle.OG else DonorCardStyle.STANDARD
    }

    fun latestDonationDate(donor: DonorEntry): String? =
        donor.donations.map { it.normalizedAt }.filter { it.isNotEmpty() }.maxOrNull()

    fun latestDonationDate(file: DonorsFile): String? =
        file.donors.mapNotNull { latestDonationDate(it) }.maxOrNull()

    /** True when [donor] has at least one donation strictly after [lastSeenDate]. */
    fun hasDonationAfter(donor: DonorEntry, lastSeenDate: String?): Boolean {
        val floor = lastSeenDate?.trim().orEmpty()
        return donor.donations.any { rec ->
            val at = rec.normalizedAt
            at.isNotEmpty() && (floor.isEmpty() || at > floor)
        }
    }
}
