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

    private fun dto(id: String) = GeminiModelDto(
        name = "models/$id",
        displayName = id,
        supportedGenerationMethods = listOf("generateContent")
    )
}
