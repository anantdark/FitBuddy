package com.anant.fitbuddy.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File

/**
 * Settings DataStore lives under [shared_prefs] so Android Auto Backup can read it.
 *
 * The default [preferencesDataStore] path (`files/datastore/`) is often mode `700`, which
 * causes the backup agent to silently skip the file. Room DBs still restore, so users get
 * their profile/logs back but AI keys look "missing" and onboarding shows Reconnect AI.
 */
internal object SettingsDataStore {
    const val FILE_NAME = "app_settings.preferences_pb"

    fun sharedPrefsFile(context: Context): File {
        val dir = File(context.applicationContext.filesDir.parentFile, "shared_prefs")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, FILE_NAME)
    }

    fun legacyFile(context: Context): File =
        File(context.applicationContext.filesDir, "datastore/$FILE_NAME")

    /**
     * Copies the legacy `files/datastore` file into `shared_prefs` when needed so upgrades
     * and old Auto Backup restores keep AI credentials.
     */
    fun migrateLegacyIfNeeded(context: Context) {
        migrateLegacyIfNeeded(
            sharedPrefsFile = sharedPrefsFile(context),
            legacyFile = legacyFile(context),
        )
    }

    /** Pure file move for unit tests. */
    fun migrateLegacyIfNeeded(sharedPrefsFile: File, legacyFile: File) {
        if (!legacyFile.exists()) return
        val preferLegacy = !sharedPrefsFile.exists() ||
            (legacyFile.length() > sharedPrefsFile.length() && legacyFile.length() > 0L)
        if (preferLegacy) {
            sharedPrefsFile.parentFile?.mkdirs()
            legacyFile.copyTo(sharedPrefsFile, overwrite = true)
        }
        runCatching { legacyFile.delete() }
    }

    fun create(context: Context): DataStore<Preferences> {
        val appContext = context.applicationContext
        migrateLegacyIfNeeded(appContext)
        return PreferenceDataStoreFactory.create(
            produceFile = { sharedPrefsFile(appContext) },
        )
    }
}
