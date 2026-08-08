package com.anant.fitbuddy.data.region

import org.junit.Assert.assertEquals
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
    fun `AppRegion fromStored is case insensitive`() {
        assertEquals(AppRegion.US, AppRegion.fromStored("us"))
        assertEquals(AppRegion.EUROPE, AppRegion.fromStored(" Europe "))
        assertEquals(null, AppRegion.fromStored("mars"))
    }
}
