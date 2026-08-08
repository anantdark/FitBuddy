package com.anant.fitbuddy.data.region

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Locks in that selecting / switching [com.anant.fitbuddy.data.settings.AppSettings.region]
 * resolves a distinct pack for prompts, UI hints, and staples — with no cross-pack merge.
 *
 * Production resolve sites: RemoteAiDataSource.regionPack, MainScreen remember(settings.region),
 * MainViewModel.askForPortion, FitnessRepository offline analyze branch.
 */
class RegionPackSwitchTest {

    @Test
    fun packLookup_mapsEachStoredRegionToDistinctPack() {
        assertEquals(IndiaRegionPack, RegionPacks.pack(AppRegion.INDIA))
        assertEquals(UsRegionPack, RegionPacks.pack(AppRegion.US))
        assertEquals(EuropeRegionPack, RegionPacks.pack(AppRegion.EUROPE))

        assertEquals(IndiaRegionPack, packForStored("INDIA"))
        assertEquals(UsRegionPack, packForStored("US"))
        assertEquals(EuropeRegionPack, packForStored("europe")) // case-insensitive
        // Unset / invalid → India fallback (same as RemoteAiDataSource.regionPack)
        assertEquals(IndiaRegionPack, packForStored(""))
        assertEquals(IndiaRegionPack, packForStored(null))
        assertEquals(IndiaRegionPack, packForStored("ATLANTIS"))
    }

    @Test
    fun packs_haveDistinctPromptAndHintConfig() {
        val packs = AppRegion.entries.map { RegionPacks.pack(it) }

        assertDistinct(packs.map { it.analyzeSystemIntro })
        assertDistinct(packs.map { it.analyzePromptPriors })
        assertDistinct(packs.map { it.promptReferenceTable() })
        assertDistinct(packs.map { it.targetSystemIntro })
        assertDistinct(packs.map { it.targetCoachNotes })
        assertDistinct(packs.map { it.progressSystemIntro })
        assertDistinct(packs.map { it.progressFoodGuidance })
        assertDistinct(packs.map { it.measurementPromptNotes })
        assertDistinct(packs.map { it.askPortionPromptNotes })
        assertDistinct(packs.map { it.foodLogHint })
        assertDistinct(packs.map { it.askPortionHint })
        assertDistinct(packs.map { it.barcodeExample })

        packs.forEach { pack ->
            assertTrue(pack.displayName, pack.analyzeSystemIntro.isNotBlank())
            assertTrue(pack.displayName, pack.analyzePromptPriors.isNotBlank())
            assertTrue(pack.displayName, pack.promptReferenceTable().isNotBlank())
            assertTrue(pack.displayName, pack.measurementPromptNotes.isNotBlank())
            assertTrue(pack.displayName, pack.staples.isNotEmpty())
        }

        assertTrue(IndiaRegionPack.analyzeSystemIntro.contains("Indian", ignoreCase = true))
        assertTrue(UsRegionPack.analyzeSystemIntro.contains("United States", ignoreCase = true))
        assertTrue(EuropeRegionPack.analyzeSystemIntro.contains("European", ignoreCase = true))

        assertTrue(IndiaRegionPack.measurementPromptNotes.contains("katori", ignoreCase = true))
        assertTrue(UsRegionPack.measurementPromptNotes.contains("cup", ignoreCase = true))
        assertTrue(EuropeRegionPack.measurementPromptNotes.contains("gram", ignoreCase = true))
    }

    @Test
    fun packs_doNotShareStapleInventories() {
        val indiaNames = IndiaRegionPack.staples.map { it.name }.toSet()
        val usNames = UsRegionPack.staples.map { it.name }.toSet()
        val euNames = EuropeRegionPack.staples.map { it.name }.toSet()

        assertTrue(indiaNames.any { it.contains("roti", ignoreCase = true) })
        assertFalse(usNames.any { it.contains("roti", ignoreCase = true) })
        assertFalse(euNames.any { it.contains("roti", ignoreCase = true) })

        assertTrue(usNames.any { it.contains("burger", ignoreCase = true) })
        assertFalse(indiaNames.any { it.contains("burger", ignoreCase = true) })

        assertTrue(euNames.any { it.contains("croissant", ignoreCase = true) })
        assertFalse(indiaNames.any { it.contains("croissant", ignoreCase = true) })
    }

    @Test
    fun switchingStoredRegion_changesResolvedHintsAndStaples() {
        val before = packForStored("INDIA")
        val after = packForStored("US")

        assertNotEquals(before.foodLogHint, after.foodLogHint)
        assertNotEquals(before.barcodeExample, after.barcodeExample)
        assertNotEquals(before.staples.map { it.name }, after.staples.map { it.name })
        assertNotEquals(before.analyzePromptPriors, after.analyzePromptPriors)

        val europe = packForStored("EUROPE")
        assertNotEquals(after.analyzeSystemIntro, europe.analyzeSystemIntro)
        assertNotEquals(after.progressFoodGuidance, europe.progressFoodGuidance)
    }

    @Test
    fun stapleKeywordMatch_followsActivePackOnly() {
        // Mirrors FitnessRepository.simulateFromRegionPack keyword routing.
        assertEquals(UsRegionPack.BURGER, stapleHit("cheeseburger with fries", AppRegion.US))
        assertEquals(EuropeRegionPack.CROISSANT, stapleHit("croissant", AppRegion.EUROPE))
        assertEquals(IndiaRegionPack.ROTI, stapleHit("2 rotis with dal", AppRegion.INDIA))

        // Same word resolves from the active pack — no global merge.
        val usSalad = stapleHit("salad", AppRegion.US)
        val euSalad = stapleHit("salad", AppRegion.EUROPE)
        assertEquals(UsRegionPack.SALAD_BOWL, usSalad)
        assertEquals(EuropeRegionPack.SALAD, euSalad)
        assertNotEquals(usSalad!!.calories, euSalad!!.calories)

        // India staples must not win while US is active.
        assertEquals(null, stapleHit("roti sabzi", AppRegion.US))
        assertEquals(null, stapleHit("cheeseburger", AppRegion.INDIA))
    }

    private fun stapleHit(input: String, region: AppRegion): RegionalDish? {
        val query = input.lowercase()
        return RegionPacks.pack(region).staples.firstOrNull { dish ->
            dish.keywords.any { kw -> kw in query }
        }
    }

    private fun packForStored(value: String?): RegionPack =
        RegionPacks.packOrIndia(AppRegion.fromStored(value))

    private fun assertDistinct(values: List<String>) {
        assertEquals("expected distinct values, got $values", values.size, values.toSet().size)
    }
}
