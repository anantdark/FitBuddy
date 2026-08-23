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

    /** US + Canada → North America diet pack. */
    private val NORTH_AMERICA_COUNTRIES = setOf("US", "CA")

    private val LATIN_AMERICA_COUNTRIES = setOf(
        // Mexico + Central America
        "MX", "GT", "BZ", "HN", "SV", "NI", "CR", "PA",
        // Caribbean (Spanish/Portuguese-speaking focus)
        "CU", "DO", "PR",
        // South America
        "CO", "VE", "EC", "PE", "BO", "CL", "AR", "UY", "PY", "BR", "GY", "SR"
    )

    /**
     * ISO 3166-1 alpha-2 codes covered by [region], sorted by English display name.
     * Empty for India (single-country pack with its own Tiranga draw).
     */
    fun memberCountryCodes(region: AppRegion): List<String> {
        val codes = when (region) {
            AppRegion.INDIA -> emptySet()
            AppRegion.US -> NORTH_AMERICA_COUNTRIES
            AppRegion.EUROPE -> EUROPEAN_COUNTRIES
            AppRegion.LATIN_AMERICA -> LATIN_AMERICA_COUNTRIES
        }
        return codes.sortedBy { isoDisplayName(it) }
    }

    fun isoDisplayName(iso2: String): String {
        val iso = iso2.trim().uppercase(Locale.ROOT)
        return Locale.Builder()
            .setRegion(iso)
            .build()
            .getDisplayCountry(Locale.ENGLISH)
            .ifBlank { iso }
    }

    private val US_TIME_ZONES = setOf(
        "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
        "America/Phoenix", "America/Anchorage", "America/Honolulu", "America/Detroit",
        "America/Boise", "America/Juneau", "America/Adak",
        // Canada
        "America/Toronto", "America/Vancouver", "America/Edmonton", "America/Winnipeg",
        "America/Halifax", "America/St_Johns", "America/Regina", "America/Whitehorse",
        "America/Yellowknife", "America/Iqaluit", "America/Moncton", "America/Goose_Bay"
    )
    private val US_TIME_ZONE_PREFIXES = setOf(
        "America/Indiana/", "America/Kentucky/", "America/North_Dakota/"
    )

    private val LATIN_AMERICA_TIME_ZONES = setOf(
        "America/Mexico_City", "America/Cancun", "America/Merida", "America/Monterrey",
        "America/Mazatlan", "America/Chihuahua", "America/Hermosillo", "America/Tijuana",
        "America/Bahia_Banderas", "America/Guatemala", "America/Belize",
        "America/Tegucigalpa", "America/El_Salvador", "America/Managua",
        "America/Costa_Rica", "America/Panama", "America/Havana", "America/Santo_Domingo",
        "America/Puerto_Rico", "America/Bogota", "America/Caracas", "America/Guayaquil",
        "America/Lima", "America/La_Paz", "America/Santiago", "America/Punta_Arenas",
        "America/Argentina/Buenos_Aires", "America/Argentina/Cordoba",
        "America/Argentina/Mendoza", "America/Montevideo", "America/Asuncion",
        "America/Sao_Paulo", "America/Manaus", "America/Belem", "America/Fortaleza",
        "America/Recife", "America/Bahia", "America/Cuiaba", "America/Campo_Grande",
        "America/Porto_Velho", "America/Rio_Branco", "America/Guyana", "America/Paramaribo"
    )
    private val LATIN_AMERICA_TIME_ZONE_PREFIXES = setOf(
        "America/Argentina/", "America/Mexico_"
    )

    fun detect(
        localeCountry: String?,
        simCountry: String?,
        networkCountry: String?,
        timeZoneId: String?
    ): AppRegion {
        val codes = listOfNotNull(localeCountry, simCountry, networkCountry)
            .map { it.trim().uppercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }

        if (codes.any { it in NORTH_AMERICA_COUNTRIES }) return AppRegion.US
        if (codes.any { it in EUROPEAN_COUNTRIES }) return AppRegion.EUROPE
        if (codes.any { it in LATIN_AMERICA_COUNTRIES }) return AppRegion.LATIN_AMERICA

        val tz = timeZoneId?.trim().orEmpty()
        if (isNorthAmericaTimeZone(tz)) return AppRegion.US
        if (tz.startsWith("Europe/")) return AppRegion.EUROPE
        if (isLatinAmericaTimeZone(tz)) return AppRegion.LATIN_AMERICA

        return AppRegion.INDIA
    }

    private fun isNorthAmericaTimeZone(tz: String): Boolean =
        tz in US_TIME_ZONES || US_TIME_ZONE_PREFIXES.any { tz.startsWith(it) }

    private fun isLatinAmericaTimeZone(tz: String): Boolean =
        tz in LATIN_AMERICA_TIME_ZONES ||
            LATIN_AMERICA_TIME_ZONE_PREFIXES.any { tz.startsWith(it) }

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
