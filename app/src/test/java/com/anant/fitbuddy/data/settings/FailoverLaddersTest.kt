package com.anant.fitbuddy.data.settings

import com.anant.fitbuddy.data.remote.dto.ModelCatalogModality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FailoverLaddersTest {

    @Test
    fun `selected model stays first even if not on ladder`() {
        val order = FailoverLadders.buildAttemptOrder(
            AiProvider.OPENROUTER,
            ModelCatalogModality.TEXT,
            selected = "custom/my-model:free",
            catalogIds = listOf(
                "google/gemma-4-26b-a4b-it:free",
                "inclusionai/ling-3.0-flash:free",
                "custom/my-model:free",
                "zzz-other:free"
            )
        )
        assertEquals("custom/my-model:free", order.first())
        assertEquals(
            listOf(
                "custom/my-model:free",
                "inclusionai/ling-3.0-flash:free",
                "google/gemma-4-26b-a4b-it:free",
                "zzz-other:free"
            ),
            order
        )
    }

    @Test
    fun `empty catalog falls back to full config ladder after selected`() {
        val order = FailoverLadders.buildAttemptOrder(
            AiProvider.OLLAMA,
            ModelCatalogModality.TEXT,
            selected = "minimax-m3",
            catalogIds = emptyList()
        )
        assertEquals("minimax-m3", order.first())
        assertEquals(FailoverLadders.TEXT[AiProvider.OLLAMA], order)
    }

    @Test
    fun `ladder filters to live catalog and appends leftovers A-Z`() {
        val order = FailoverLadders.buildAttemptOrder(
            AiProvider.GEMINI,
            ModelCatalogModality.TEXT,
            selected = "gemini-3.6-flash",
            catalogIds = listOf(
                "gemini-2.5-flash",
                "gemini-flash-lite-latest",
                "gemini-aaa-extra",
                "gemini-3.6-flash"
            )
        )
        assertEquals(
            listOf(
                "gemini-3.6-flash",
                "gemini-flash-lite-latest",
                "gemini-2.5-flash",
                "gemini-aaa-extra"
            ),
            order
        )
    }

    @Test
    fun `orderCatalog puts ladder head before alphabetical remainder`() {
        val ordered = FailoverLadders.orderCatalog(
            AiProvider.OPENROUTER,
            ModelCatalogModality.TEXT,
            listOf("zzz:free", "inclusionai/ling-3.0-flash:free", "aaa:free")
        )
        assertEquals("inclusionai/ling-3.0-flash:free", ordered.first())
        assertEquals(listOf("inclusionai/ling-3.0-flash:free", "aaa:free", "zzz:free"), ordered)
    }

    @Test
    fun `nextBest picks ladder head when preferred missing`() {
        val next = FailoverLadders.nextBest(
            AiProvider.GEMINI,
            ModelCatalogModality.TEXT,
            catalogIds = listOf("gemini-3.1-flash-lite", "gemini-flash-lite-latest"),
            missingId = "gone-model"
        )
        assertEquals("gemini-flash-lite-latest", next)
    }

    @Test
    fun `defaults match ladder heads`() {
        assertEquals(
            FailoverLadders.preferredDefault(AiProvider.OPENROUTER, ModelCatalogModality.TEXT),
            AppSettings.DEFAULT_OPENROUTER_TEXT_MODEL
        )
        assertEquals(
            FailoverLadders.preferredDefault(AiProvider.GEMINI, ModelCatalogModality.TEXT),
            AppSettings.DEFAULT_GEMINI_TEXT_MODEL
        )
        assertEquals(
            FailoverLadders.preferredDefault(AiProvider.OLLAMA, ModelCatalogModality.TEXT),
            AppSettings.DEFAULT_OLLAMA_TEXT_MODEL
        )
        assertEquals(
            FailoverLadders.preferredDefault(AiProvider.OPENROUTER, ModelCatalogModality.PHOTO),
            AppSettings.DEFAULT_OPENROUTER_MODEL
        )
        assertTrue(FailoverLadders.TEXT[AiProvider.OLLAMA]!!.isNotEmpty())
    }
}
