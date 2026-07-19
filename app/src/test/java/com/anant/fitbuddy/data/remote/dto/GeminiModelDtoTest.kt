package com.anant.fitbuddy.data.remote.dto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiModelDtoTest {

    @Test
    fun `isFreeTier accepts flash families and rejects paid pro`() {
        assertTrue(dto("gemini-3.5-flash").isFreeTier)
        assertTrue(dto("gemini-3-flash-preview").isFreeTier)
        assertTrue(dto("gemini-2.5-flash").isFreeTier)
        assertTrue(dto("gemini-flash-latest").isFreeTier)
        assertTrue(dto("gemini-3.1-flash-lite").isFreeTier)
        assertTrue(dto("gemini-2.5-flash-lite").isFreeTier)
        assertTrue(dto("gemini-2.0-flash").isFreeTier)
        assertTrue(dto("gemini-2.0-flash-lite-001").isFreeTier)

        assertFalse(dto("gemini-3.1-pro-preview").isFreeTier)
        assertFalse(dto("gemini-3-pro-preview").isFreeTier)
        assertFalse(dto("gemini-2.5-pro").isFreeTier)
        assertFalse(dto("gemini-pro-latest").isFreeTier)
        assertFalse(dto("gemini-2.5-computer-use-preview-10-2025").isFreeTier)
    }

    @Test
    fun `supportsVision excludes niche omni robotics and image models`() {
        assertTrue(dto("gemini-2.5-flash").supportsVision)
        assertFalse(dto("gemini-2.5-flash-image").supportsVision)
        assertFalse(dto("gemini-omni-flash-preview").supportsVision)
        assertFalse(dto("gemini-robotics-er-1.5-preview").supportsVision)
    }

    @Test
    fun `intelligence rank follows free-tier capability ladder`() {
        val order = listOf(
            "gemini-3.5-flash",
            "gemini-3-flash-preview",
            "gemini-2.5-flash",
            "gemini-flash-latest",
            "gemini-3.1-flash-lite",
            "gemini-2.5-flash-lite",
            "gemini-flash-lite-latest",
            "gemini-2.0-flash",
            "gemini-2.0-flash-lite"
        )
        val ranks = order.map { geminiIntelligenceRank(it) }
        ranks.zipWithNext().forEach { (higher, lower) ->
            assertTrue(higher >= lower)
        }
        assertTrue(geminiIntelligenceRank("gemini-3.5-flash") > geminiIntelligenceRank("gemini-3-flash-preview"))
        assertTrue(geminiIntelligenceRank("gemini-2.5-flash") > geminiIntelligenceRank("gemini-2.5-flash-lite"))
        assertTrue(geminiIntelligenceRank("gemini-2.0-flash") > geminiIntelligenceRank("gemini-2.0-flash-lite"))
        assertTrue(geminiIntelligenceRank("gemini-2.5-pro") > geminiIntelligenceRank("gemini-2.5-flash"))
        assertTrue(geminiIntelligenceRank("gemini-3.5-pro") > geminiIntelligenceRank("gemini-3.5-flash"))
    }

    @Test
    fun `gemma first rank prefers gemma 4 then size then older gemma`() {
        val order = listOf(
            "google/gemma-4-31b-it:free",
            "google/gemma-4-26b-a4b-it:free",
            "google/gemma-3-27b-it:free",
            "gemma-3-4b",
            "meta-llama/llama-3.3-70b-instruct:free"
        )
        val ranks = order.map { gemmaFirstIntelligenceRank(it) }
        ranks.zipWithNext().forEach { (higher, lower) ->
            assertTrue(higher > lower)
        }
        assertTrue(
            gemmaFirstIntelligenceRank("gemma4:31b") >
                gemmaFirstIntelligenceRank("gemma-3-27b")
        )
    }

    private fun dto(id: String) = GeminiModelDto(
        name = "models/$id",
        displayName = id,
        supportedGenerationMethods = listOf("generateContent")
    )
}
