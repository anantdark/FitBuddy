package com.anant.fitbuddy.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `text model falls back to vision model when blank`() {
        val settings = AppSettings(
            provider = AiProvider.OPENROUTER,
            openRouterModel = "vision-model",
            openRouterTextModel = ""
        )
        assertEquals("vision-model", settings.textModel)
        assertEquals("vision-model", settings.modelFor(hasImage = false))
        assertEquals("vision-model", settings.modelFor(hasImage = true))
    }

    @Test
    fun `text model used only when no image attached`() {
        val settings = AppSettings(
            provider = AiProvider.OPENROUTER,
            openRouterModel = "vision-model",
            openRouterTextModel = "text-model"
        )
        assertEquals("text-model", settings.modelFor(hasImage = false))
        assertEquals("vision-model", settings.modelFor(hasImage = true))
    }

    @Test
    fun `gemini text model fallback mirrors openrouter`() {
        val settings = AppSettings(
            provider = AiProvider.GEMINI,
            geminiModel = "gemini-2.0-flash",
            geminiTextModel = ""
        )
        assertEquals("gemini-2.0-flash", settings.textModel)
    }

    @Test
    fun `ollama never sends an auth header`() {
        val settings = AppSettings(provider = AiProvider.OLLAMA, ollamaBaseUrl = "http://host:11434")
        assertNull(settings.authHeader)
        assertEquals("http://host:11434/v1/chat/completions", settings.chatUrl)
    }

    @Test
    fun `openrouter auth header only present when key set`() {
        val noKey = AppSettings(provider = AiProvider.OPENROUTER, openRouterApiKey = "")
        assertNull(noKey.authHeader)

        val withKey = AppSettings(provider = AiProvider.OPENROUTER, openRouterApiKey = "sk-123")
        assertEquals("Bearer sk-123", withKey.authHeader)
    }

    @Test
    fun `isConfigured requires both key and model per provider`() {
        assertFalse(
            AppSettings(
                provider = AiProvider.OPENROUTER,
                openRouterApiKey = "",
                openRouterModel = "m"
            ).isConfigured
        )
        assertTrue(
            AppSettings(
                provider = AiProvider.OPENROUTER,
                openRouterApiKeys = listOf("k"),
                openRouterApiKey = "k",
                openRouterModel = "m"
            ).isConfigured
        )
        assertFalse(
            AppSettings(provider = AiProvider.GEMINI, geminiApiKey = "", geminiModel = "m").isConfigured
        )
        assertTrue(
            AppSettings(
                provider = AiProvider.OLLAMA,
                ollamaBaseUrl = "http://h",
                ollamaModel = "llava"
            ).isConfigured
        )
    }

    @Test
    fun `keysFor merges list and active key`() {
        val fromList = AppSettings(openRouterApiKeys = listOf("a", "b"))
        assertEquals(listOf("a", "b"), fromList.keysFor(AiProvider.OPENROUTER))

        val legacy = AppSettings(openRouterApiKey = "solo")
        assertEquals(listOf("solo"), legacy.keysFor(AiProvider.OPENROUTER))
    }

    @Test
    fun `withKey sets active credential for attempt`() {
        val base = AppSettings(
            openRouterApiKeys = listOf("a", "b"),
            openRouterApiKey = "a"
        )
        val attempt = base.withKey(AiProvider.OPENROUTER, "b")
        assertEquals("b", attempt.openRouterApiKey)
        assertEquals("Bearer b", attempt.copy(provider = AiProvider.OPENROUTER).authHeader)
    }

    @Test
    fun `parseApiKeys splits commas newlines and dedupes`() {
        assertEquals(
            listOf("k1", "k2", "k3"),
            parseApiKeys("k1\nk2, k3\n k2 ")
        )
        assertEquals(emptyList<String>(), parseApiKeys("  \n,"))
    }
}
