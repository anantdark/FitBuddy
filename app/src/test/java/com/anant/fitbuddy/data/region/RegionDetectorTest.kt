package com.anant.fitbuddy.data.region

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionDetectorTest {

    @Test
    fun `US country code wins regardless of time zone`() {
        val region = RegionDetector.detect(
            localeCountry = "US",
            simCountry = null,
            networkCountry = null,
            timeZoneId = "Asia/Kolkata"
        )
        assertEquals(AppRegion.US, region)
    }

    @Test
    fun `European country code detected from sim`() {
        val region = RegionDetector.detect(
            localeCountry = null,
            simCountry = "de",
            networkCountry = null,
            timeZoneId = null
        )
        assertEquals(AppRegion.EUROPE, region)
    }

    @Test
    fun `network country overrides missing locale and sim`() {
        val region = RegionDetector.detect(
            localeCountry = null,
            simCountry = null,
            networkCountry = "GB",
            timeZoneId = null
        )
        assertEquals(AppRegion.EUROPE, region)
    }

    @Test
    fun `falls back to US time zone when no country codes present`() {
        val region = RegionDetector.detect(
            localeCountry = null,
            simCountry = null,
            networkCountry = null,
            timeZoneId = "America/Chicago"
        )
        assertEquals(AppRegion.US, region)
    }

    @Test
    fun `falls back to Europe time zone when no country codes present`() {
        val region = RegionDetector.detect(
            localeCountry = null,
            simCountry = null,
            networkCountry = null,
            timeZoneId = "Europe/Berlin"
        )
        assertEquals(AppRegion.EUROPE, region)
    }

    @Test
    fun `defaults to India when nothing matches`() {
        val region = RegionDetector.detect(
            localeCountry = "IN",
            simCountry = null,
            networkCountry = null,
            timeZoneId = "Asia/Kolkata"
        )
        assertEquals(AppRegion.INDIA, region)
    }

    @Test
    fun `unrelated country and time zone still defaults to India`() {
        val region = RegionDetector.detect(
            localeCountry = "JP",
            simCountry = null,
            networkCountry = null,
            timeZoneId = "Asia/Tokyo"
        )
        assertEquals(AppRegion.INDIA, region)
    }

    @Test
    fun `Indiana time zone counts as US`() {
        val region = RegionDetector.detect(
            localeCountry = null,
            simCountry = null,
            networkCountry = null,
            timeZoneId = "America/Indiana/Indianapolis"
        )
        assertEquals(AppRegion.US, region)
    }

    @Test
    fun `Canada country code maps to US pack`() {
        val region = RegionDetector.detect(
            localeCountry = "CA",
            simCountry = null,
            networkCountry = null,
            timeZoneId = "America/Toronto"
        )
        assertEquals(AppRegion.US, region)
    }

    @Test
    fun `Mexico country code maps to Latin America`() {
        val region = RegionDetector.detect(
            localeCountry = "MX",
            simCountry = null,
            networkCountry = null,
            timeZoneId = null
        )
        assertEquals(AppRegion.LATIN_AMERICA, region)
    }

    @Test
    fun `Chile and Uruguay country codes map to Latin America`() {
        assertEquals(
            AppRegion.LATIN_AMERICA,
            RegionDetector.detect("CL", null, null, null)
        )
        assertEquals(
            AppRegion.LATIN_AMERICA,
            RegionDetector.detect("UY", null, null, null)
        )
    }

    @Test
    fun `Latin America time zone used when no country codes present`() {
        val region = RegionDetector.detect(
            localeCountry = null,
            simCountry = null,
            networkCountry = null,
            timeZoneId = "America/Santiago"
        )
        assertEquals(AppRegion.LATIN_AMERICA, region)
    }

    @Test
    fun `Canadian time zone maps to US when no country codes present`() {
        val region = RegionDetector.detect(
            localeCountry = null,
            simCountry = null,
            networkCountry = null,
            timeZoneId = "America/Vancouver"
        )
        assertEquals(AppRegion.US, region)
    }

    @Test
    fun `member country codes cover NA Europe LatAm and not India`() {
        assertEquals(listOf("CA", "US"), RegionDetector.memberCountryCodes(AppRegion.US))
        assertTrue(
            RegionDetector.memberCountryCodes(AppRegion.EUROPE).containsAll(listOf("DE", "FR", "GB"))
        )
        assertTrue(
            RegionDetector.memberCountryCodes(AppRegion.LATIN_AMERICA)
                .containsAll(listOf("MX", "CL", "UY", "BR"))
        )
        assertTrue(RegionDetector.memberCountryCodes(AppRegion.INDIA).isEmpty())
        assertTrue(AppRegion.US.cyclesMemberFlags())
        assertFalse(AppRegion.INDIA.cyclesMemberFlags())
    }

    @Test
    fun `AppRegion fromStored is case insensitive`() {
        assertEquals(AppRegion.US, AppRegion.fromStored("us"))
        assertEquals(AppRegion.US, AppRegion.fromStored("NORTH_AMERICA"))
        assertEquals(AppRegion.EUROPE, AppRegion.fromStored(" Europe "))
        assertEquals(AppRegion.LATIN_AMERICA, AppRegion.fromStored("latin_america"))
        assertEquals(null, AppRegion.fromStored("mars"))
    }
}
