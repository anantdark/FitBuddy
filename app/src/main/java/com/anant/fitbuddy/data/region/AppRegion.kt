package com.anant.fitbuddy.data.region

/** User-facing dietary/lifestyle region used to pick region-specific AI prompt content. */
enum class AppRegion {
    INDIA, US, EUROPE, LATIN_AMERICA;

    fun displayName(): String = when (this) {
        INDIA -> "India"
        US -> "North America"
        EUROPE -> "Europe"
        LATIN_AMERICA -> "Latin America"
    }

    /** Compact label for tight UI (e.g. Settings segmented control). */
    fun shortDisplayName(): String = when (this) {
        INDIA -> "India"
        US -> "N.Am"
        EUROPE -> "Europe"
        LATIN_AMERICA -> "LatAm"
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
