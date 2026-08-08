package com.anant.fitbuddy.data.region

/** User-facing dietary/lifestyle region used to pick region-specific AI prompt content. */
enum class AppRegion {
    INDIA, US, EUROPE;

    fun displayName(): String = when (this) {
        INDIA -> "India"
        US -> "United States"
        EUROPE -> "Europe"
    }

    companion object {
        fun fromStored(value: String?): AppRegion? =
            entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) }
    }
}
