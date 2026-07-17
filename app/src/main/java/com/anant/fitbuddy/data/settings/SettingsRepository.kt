package com.anant.fitbuddy.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anant.fitbuddy.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/** Reads/writes [AppSettings] via DataStore. First-run defaults seed from BuildConfig (local.properties). */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        AppSettings(
            provider = runCatching { AiProvider.valueOf(prefs[KEY_PROVIDER] ?: "") }
                .getOrDefault(AiProvider.OPENROUTER),
            openRouterApiKey = prefs[KEY_OR_KEY] ?: BuildConfig.OPENROUTER_API_KEY,
            openRouterModel = prefs[KEY_OR_MODEL]
                ?: BuildConfig.AI_MODEL.ifBlank { AppSettings.DEFAULT_OPENROUTER_MODEL },
            openRouterTextModel = prefs[KEY_OR_TEXT_MODEL] ?: "",
            geminiApiKey = prefs[KEY_GEMINI_KEY] ?: "",
            geminiModel = prefs[KEY_GEMINI_MODEL] ?: AppSettings.DEFAULT_GEMINI_MODEL,
            geminiTextModel = prefs[KEY_GEMINI_TEXT_MODEL] ?: "",
            ollamaBaseUrl = prefs[KEY_OLLAMA_URL] ?: AppSettings.DEFAULT_OLLAMA_URL,
            ollamaModel = prefs[KEY_OLLAMA_MODEL] ?: AppSettings.DEFAULT_OLLAMA_MODEL,
            ollamaTextModel = prefs[KEY_OLLAMA_TEXT_MODEL] ?: "",
            dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: true,
            easterEggDiscovered = prefs[KEY_EASTER_EGG] ?: false
        )
    }

    suspend fun save(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_PROVIDER] = settings.provider.name
            prefs[KEY_OR_KEY] = settings.openRouterApiKey
            prefs[KEY_OR_MODEL] = settings.openRouterModel
            prefs[KEY_OR_TEXT_MODEL] = settings.openRouterTextModel
            prefs[KEY_GEMINI_KEY] = settings.geminiApiKey
            prefs[KEY_GEMINI_MODEL] = settings.geminiModel
            prefs[KEY_GEMINI_TEXT_MODEL] = settings.geminiTextModel
            prefs[KEY_OLLAMA_URL] = settings.ollamaBaseUrl
            prefs[KEY_OLLAMA_MODEL] = settings.ollamaModel
            prefs[KEY_OLLAMA_TEXT_MODEL] = settings.ollamaTextModel
            prefs[KEY_DYNAMIC_COLOR] = settings.dynamicColor
            prefs[KEY_EASTER_EGG] = settings.easterEggDiscovered
        }
    }

    private companion object {
        val KEY_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_OR_KEY = stringPreferencesKey("openrouter_api_key")
        val KEY_OR_MODEL = stringPreferencesKey("openrouter_model")
        val KEY_OR_TEXT_MODEL = stringPreferencesKey("openrouter_text_model")
        val KEY_GEMINI_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val KEY_GEMINI_TEXT_MODEL = stringPreferencesKey("gemini_text_model")
        val KEY_OLLAMA_URL = stringPreferencesKey("ollama_base_url")
        val KEY_OLLAMA_MODEL = stringPreferencesKey("ollama_model")
        val KEY_OLLAMA_TEXT_MODEL = stringPreferencesKey("ollama_text_model")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_EASTER_EGG = booleanPreferencesKey("easter_egg_discovered")
    }
}
