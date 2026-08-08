package com.anant.fitbuddy.data.region

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

/**
 * Best-effort, offline region detection from locale/SIM/network country codes and time zone.
 * Used only to seed a sensible default; the user can always override in Settings.
 */
object RegionDetector {

    private val EUROPEAN_COUNTRIES = setOf(
        "GB", "IE", "DE", "FR", "IT", "ES", "NL", "BE", "AT", "CH",
        "SE", "NO", "DK", "FI", "PL", "PT", "GR", "CZ", "RO", "HU",
        "SK", "SI", "HR", "BG", "LT", "LV", "EE", "LU", "IS", "MT", "CY"
    )

    private val US_TIME_ZONES = setOf(
        "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
        "America/Phoenix", "America/Anchorage", "America/Honolulu", "America/Detroit",
        "America/Boise", "America/Juneau", "America/Adak"
    )
    private val US_TIME_ZONE_PREFIXES = setOf("America/Indiana/", "America/Kentucky/", "America/North_Dakota/")

    fun detect(
        localeCountry: String?,
        simCountry: String?,
        networkCountry: String?,
        timeZoneId: String?
    ): AppRegion {
        val codes = listOfNotNull(localeCountry, simCountry, networkCountry)
            .map { it.trim().uppercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }

        if (codes.any { it == "US" }) return AppRegion.US
        if (codes.any { it in EUROPEAN_COUNTRIES }) return AppRegion.EUROPE

        val tz = timeZoneId?.trim().orEmpty()
        if (isUsTimeZone(tz)) return AppRegion.US
        if (tz.startsWith("Europe/")) return AppRegion.EUROPE

        return AppRegion.INDIA
    }

    private fun isUsTimeZone(tz: String): Boolean =
        tz in US_TIME_ZONES || US_TIME_ZONE_PREFIXES.any { tz.startsWith(it) }

    fun detectFromDevice(context: Context): AppRegion {
        val localeCountry = Locale.getDefault().country
        var simCountry: String? = null
        var networkCountry: String? = null
        try {
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            simCountry = tm?.simCountryIso
            networkCountry = tm?.networkCountryIso
        } catch (_: SecurityException) {
            // Missing READ_PHONE_STATE (or similar) on some OEMs/policies — fall back silently.
        }
        val timeZoneId = java.util.TimeZone.getDefault().id
        return detect(localeCountry, simCountry, networkCountry, timeZoneId)
    }
}
