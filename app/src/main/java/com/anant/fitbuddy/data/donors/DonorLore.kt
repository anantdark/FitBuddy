package com.anant.fitbuddy.data.donors

/**
 * Ten lore lines per supporter tier. The card back shows exactly one, picked
 * deterministically from the donor's **current** primary card style so a new
 * medal (tier change) swaps the pool — and thus the line — without reshuffling
 * on every open of the same tier.
 */
object DonorLore {

    private val Og = listOf(
        "Present at the dawn of FitBuddy — when the first meal log was still a dare.",
        "Helped light the first campfire. Every free feature still warms by that spark.",
        "Signed the imaginary founding scroll. Chai stains optional, conviction required.",
        "One of the earliest believers. The roadmap still nods in their direction.",
        "Held the door open while FitBuddy walked into the world. Never asked for a throne.",
        "Origin-circle energy: quiet, stubborn, and allergic to paywalls.",
        "Their tip landed before the hype. That's how legends get a seat up front.",
        "Witness to version zero vibes. Still cheering like it's day one.",
        "Carved their name into the cornerstone — metaphorically, and with excellent macros.",
        "OG status isn't bought twice. It was earned when the app was still finding its voice.",
    )

    private val Generous = listOf(
        "A warm tip that keeps the kettle on and the commits landing.",
        "Proof that kindness scales: one chai at a time, features unlock for everyone.",
        "Shows up with generosity and zero guilt trips. FitBuddy's favorite kind of hero.",
        "Turned spare change into open-source momentum. The community felt it.",
        "A soft boost with hard impact — servers hum a little happier.",
        "Believes free fitness tracking should stay free. Backs that belief.",
        "Their support smells like fresh chai and merged pull requests.",
        "Didn't need a parade. Just sent fuel and let the app cook.",
        "The kind of tip that makes late-night bugfixes feel worth it.",
        "Generosity logged. Gratitude returned — forever, and in public.",
    )

    private val Legendary = listOf(
        "Rare aura detected. Roadmaps subtly rearrange themselves in respect.",
        "When they tip, the changelog grows a little braver.",
        "Legendary backing: the sort that turns \"someday\" into \"shipped\".",
        "Walks softly, funds loudly. FitBuddy's lore books took notes.",
        "A presence you feel in the release notes before you see the name.",
        "Elevated the floor for everyone else. That's legendary manners.",
        "Their support has main-character energy without the ego DLC.",
        "Rumored to make CI pass on the first try. Unconfirmed. Respected.",
        "Left a mark in the supporter vault that still faintly glows cyan.",
        "Not loud. Not flashy. Just legendary — and deeply appreciated.",
    )

    private val Godlike = listOf(
        "Mythic backing. The kind that bends roadmaps and steadies the night builds.",
        "When gods tip, open source answers. FitBuddy heard them clearly.",
        "A seal of belief so strong the paywall never stood a chance.",
        "Godlike generosity: features arrive faster, guilt stays at zero.",
        "Whispers say the coffee machine bows. The changelog definitely does.",
        "Rewrote the odds for a free app. Mortals call that godlike.",
        "Their name in the vault reads like a final boss — on our side.",
        "Funded the impossible quietly. The app got louder with gratitude.",
        "A tip that felt like armor for every unpaid evening of craft.",
        "Ascended the supporter ladder and still kept the vibe humble. Iconic.",
    )

    private val Standard = listOf(
        "A valued FitBuddy supporter keeping the app free and open.",
        "Part of the circle that chooses community over paywalls.",
        "Logged kindness the same way others log protein — consistently.",
        "Their support is a quiet yes to home kitchens and honest tracking.",
        "Standing with FitBuddy so everyone can keep logging without guilt.",
        "A tip, a thank-you, a little more runway for the next feature.",
        "Proof the community shows up for tools that respect them.",
        "Helping keep the lights on and the macros honest.",
        "One of the folks who make \"free and open\" actually sustainable.",
        "Appreciated deeply — on the dashboard and behind the scenes.",
    )

    fun poolFor(style: DonorCardStyle): List<String> = when (style) {
        DonorCardStyle.OG -> Og
        DonorCardStyle.GENEROUS -> Generous
        DonorCardStyle.LEGENDARY -> Legendary
        DonorCardStyle.GODLIKE -> Godlike
        DonorCardStyle.STANDARD -> Standard
    }

    /**
     * Stable pick for [donor] at their current [style]. Changing style (new medal tier)
     * changes the pool and the selected line.
     */
    fun forDonor(donor: DonorEntry, style: DonorCardStyle = DonorBadgeRules.cardStyle(donor)): String {
        val pool = poolFor(style)
        if (pool.isEmpty()) return Standard.first()
        val seed = stableSeed(donor.normalizedHash, style.name)
        val index = (seed % pool.size.toLong()).toInt().let { if (it < 0) it + pool.size else it }
        return pool[index]
    }

    private fun stableSeed(hash: String, styleName: String): Long {
        var h = 0xcbf29ce484222325uL
        val key = "$hash|$styleName"
        for (ch in key) {
            h = h xor ch.code.toULong()
            h *= 0x100000001b3uL
        }
        return (h and Long.MAX_VALUE.toULong()).toLong()
    }

    /** Supporter number from JSON array order: first entry = 1001. */
    fun supporterNumber(indexInFile: Int): Int = 1001 + indexInFile.coerceAtLeast(0)

    fun supporterNumber(file: DonorsFile, donor: DonorEntry): Int? {
        val index = file.donors.indexOfFirst { it.normalizedHash == donor.normalizedHash }
        if (index < 0) return null
        return supporterNumber(index)
    }

    fun supporterNumber(list: List<DonorEntry>, donor: DonorEntry): Int? {
        val index = list.indexOfFirst { it.normalizedHash == donor.normalizedHash }
        if (index < 0) return null
        return supporterNumber(index)
    }
}
