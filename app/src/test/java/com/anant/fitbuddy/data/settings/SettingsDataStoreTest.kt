package com.anant.fitbuddy.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SettingsDataStoreTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `migrate copies legacy when shared prefs missing`() {
        val legacy = tmp.newFile("legacy.pb").apply { writeText("keys-from-legacy") }
        val shared = File(tmp.root, "shared.pb")

        SettingsDataStore.migrateLegacyIfNeeded(shared, legacy)

        assertTrue(shared.exists())
        assertEquals("keys-from-legacy", shared.readText())
        assertFalse(legacy.exists())
    }

    @Test
    fun `migrate prefers larger legacy over empty shared prefs`() {
        val legacy = tmp.newFile("legacy.pb").apply { writeText("full-settings-payload") }
        val shared = tmp.newFile("shared.pb").apply { writeText("x") }

        SettingsDataStore.migrateLegacyIfNeeded(shared, legacy)

        assertEquals("full-settings-payload", shared.readText())
        assertFalse(legacy.exists())
    }

    @Test
    fun `migrate keeps shared prefs when already larger`() {
        val legacy = tmp.newFile("legacy.pb").apply { writeText("old") }
        val shared = tmp.newFile("shared.pb").apply { writeText("already-migrated-settings") }

        SettingsDataStore.migrateLegacyIfNeeded(shared, legacy)

        assertEquals("already-migrated-settings", shared.readText())
        assertFalse(legacy.exists())
    }

    @Test
    fun `migrate no-ops when legacy missing`() {
        val shared = tmp.newFile("shared.pb").apply { writeText("current") }

        SettingsDataStore.migrateLegacyIfNeeded(shared, File(tmp.root, "missing.pb"))

        assertEquals("current", shared.readText())
    }
}
