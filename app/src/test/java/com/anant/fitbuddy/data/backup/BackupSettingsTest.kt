package com.anant.fitbuddy.data.backup

import com.anant.fitbuddy.data.remote.NetworkModule
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSettingsTest {

    @Test
    fun roundTrip_preservesAiKeysAndReminder() {
        val original = AppSettings.withKeys(
            openRouterKeys = listOf("or-key-1", "or-key-2"),
            geminiKeys = listOf("gem-key"),
            base = AppSettings(
                provider = AiProvider.GEMINI,
                geminiModel = "gemini-2.5-flash",
                dailyLogReminderEnabled = false,
                dailyLogReminderHour = 21,
                dailyLogReminderMinute = 30,
                supportId = "support-xyz"
            )
        )
        val restored = BackupSettings.from(original).toAppSettings()
        assertEquals(AiProvider.GEMINI, restored.provider)
        assertEquals(listOf("or-key-1", "or-key-2"), restored.openRouterApiKeys)
        assertEquals(listOf("gem-key"), restored.geminiApiKeys)
        assertEquals("gem-key", restored.geminiApiKey)
        assertEquals(false, restored.dailyLogReminderEnabled)
        assertEquals(21, restored.dailyLogReminderHour)
        assertEquals(30, restored.dailyLogReminderMinute)
        assertEquals("support-xyz", restored.supportId)
    }

    @Test
    fun backupData_moshiRoundTrip_includesSettingsAndExercisePresets() {
        val moshi = NetworkModule.moshi
        val adapter = moshi.adapter(BackupData::class.java)
        val data = BackupData(
            exportedAt = 123L,
            exercisePresets = emptyList(),
            settings = BackupSettings(
                provider = "OLLAMA",
                ollamaBaseUrl = "http://10.0.0.2:11434",
                ollamaModel = "llava",
                ollamaApiKeys = listOf("cloud-key")
            )
        )
        val parsed = adapter.fromJson(adapter.toJson(data))!!
        assertEquals(BackupData.CURRENT_VERSION, parsed.version)
        val settings = requireNotNull(parsed.settings)
        assertEquals("OLLAMA", settings.provider)
        assertEquals("http://10.0.0.2:11434", settings.ollamaBaseUrl)
        assertEquals(listOf("cloud-key"), settings.ollamaApiKeys)
        assertTrue(parsed.exercisePresets.isEmpty())
    }

    @Test
    fun legacyBackup_missingSettingsAndExercisePresets_defaultsCleanly() {
        val json = """
            {
              "version": 4,
              "exportedAt": 1,
              "foodLogs": [],
              "mealFoods": [],
              "savedFoods": [],
              "mealPresets": []
            }
        """.trimIndent()
        val parsed = NetworkModule.moshi.adapter(BackupData::class.java).fromJson(json)!!
        assertEquals(4, parsed.version)
        assertEquals(null, parsed.settings)
        assertTrue(parsed.exercisePresets.isEmpty())
    }
}
