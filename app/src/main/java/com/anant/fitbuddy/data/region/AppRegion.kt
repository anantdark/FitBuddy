package com.anant.fitbuddy.data.region

import androidx.annotation.StringRes
import com.anant.fitbuddy.R

/** User-facing dietary/lifestyle region used to pick region-specific AI prompt content. */
enum class AppRegion {
    INDIA, US, EUROPE, LATIN_AMERICA;

    @StringRes
    fun displayNameRes(): Int = when (this) {
        INDIA -> R.string.region_india
        US -> R.string.region_north_america
        EUROPE -> R.string.region_europe
        LATIN_AMERICA -> R.string.region_latin_america
    }

    /** Compact label for tight UI (e.g. Settings segmented control). */
    @StringRes
    fun shortDisplayNameRes(): Int = when (this) {
        INDIA -> R.string.region_india
        US -> R.string.region_n_am
        EUROPE -> R.string.region_europe
        LATIN_AMERICA -> R.string.region_latam
    }

    /**
     * True only when this pack maps to a single polity with an official flag we draw in Canvas.
     * Multi-country packs cycle member-country flag emoji instead ([cyclesMemberFlags]).
     */
    fun hasOfficialFlag(): Boolean = this == INDIA

    /** True when the picker should cycle official flags of member countries. */
    fun cyclesMemberFlags(): Boolean = memberCountryCodes().isNotEmpty()

    /** ISO codes for countries included in this pack (empty for India). */
    fun memberCountryCodes(): List<String> = RegionDetector.memberCountryCodes(this)

    companion object {
        /**
         * Stored value is the enum name (`US`, `INDIA`, …). `NORTH_AMERICA` is accepted as an
         * alias for [US] (US + Canada diet pack).
         */
        fun fromStored(value: String?): AppRegion? {
            val key = value?.trim()?.uppercase()?.replace(' ', '_') ?: return null
            if (key == "NORTH_AMERICA") return US
            return entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
        }
    }
}
