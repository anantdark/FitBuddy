package com.anant.fitbuddy.data.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelIdValidationTest {

    @Test
    fun `rejects bare gemini studio ids on openrouter`() {
        assertFalse(isPlausibleModelIdFor(AiProvider.OPENROUTER, "gemini-3-flash-preview"))
        assertFalse(isPlausibleModelIdFor(AiProvider.OPENROUTER, "gemini-2.5-flash"))
        assertTrue(isPlausibleModelIdFor(AiProvider.OPENROUTER, "google/gemma-3-27b-it:free"))
        assertTrue(isPlausibleModelIdFor(AiProvider.OPENROUTER, "google/gemini-2.0-flash-001:free"))
    }

    @Test
    fun `gemini accepts studio ids`() {
        assertTrue(isPlausibleModelIdFor(AiProvider.GEMINI, "gemini-3-flash-preview"))
        assertFalse(isPlausibleModelIdFor(AiProvider.GEMINI, "google/gemma-3-27b-it:free"))
    }

    @Test
    fun `ollama accepts cloud gemini tags that openrouter rejects`() {
        assertTrue(isPlausibleModelIdFor(AiProvider.OLLAMA, "gemini-3-flash-preview"))
        assertTrue(isPlausibleModelIdFor(AiProvider.OLLAMA, "gemini-3-flash-preview:cloud"))
        assertTrue(isPlausibleModelIdFor(AiProvider.OLLAMA, "llava"))
        assertTrue(isPlausibleModelIdFor(AiProvider.OLLAMA, "gemma3:27b"))
        assertFalse(isPlausibleModelIdFor(AiProvider.OLLAMA, ""))
    }
}
