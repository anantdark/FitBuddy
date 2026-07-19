package com.anant.fitbuddy.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anant.fitbuddy.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/** Reads/writes [AppSettings] via DataStore. First-run defaults seed from BuildConfig (local.properties). */
class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val orKeys = parseApiKeys(
            prefs[KEY_OR_KEY] ?: BuildConfig.OPENROUTER_API_KEY.takeIf { it.isNotBlank() }
        )
        val geminiKeys = parseApiKeys(prefs[KEY_GEMINI_KEY])
        val ollamaKeys = parseApiKeys(prefs[KEY_OLLAMA_API_KEY])
        AppSettings(
            provider = runCatching { AiProvider.valueOf(prefs[KEY_PROVIDER] ?: "") }
                .getOrDefault(AiProvider.OPENROUTER),
            openRouterApiKeys = orKeys,
            openRouterApiKey = orKeys.firstOrNull().orEmpty(),
            openRouterOAuthKey = prefs[KEY_OR_OAUTH_KEY].orEmpty(),
            openRouterModel = sanitizeModelIdFor(
                AiProvider.OPENROUTER,
                prefs[KEY_OR_MODEL] ?: BuildConfig.AI_MODEL.ifBlank { AppSettings.DEFAULT_OPENROUTER_MODEL },
                AppSettings.DEFAULT_OPENROUTER_MODEL
            ),
            openRouterTextModel = sanitizeModelIdFor(
                AiProvider.OPENROUTER,
                prefs[KEY_OR_TEXT_MODEL] ?: "",
                ""
            ),
            geminiApiKeys = geminiKeys,
            geminiApiKey = geminiKeys.firstOrNull().orEmpty(),
            geminiModel = sanitizeModelIdFor(
                AiProvider.GEMINI,
                prefs[KEY_GEMINI_MODEL] ?: AppSettings.DEFAULT_GEMINI_MODEL,
                AppSettings.DEFAULT_GEMINI_MODEL
            ),
            geminiTextModel = sanitizeModelIdFor(
                AiProvider.GEMINI,
                prefs[KEY_GEMINI_TEXT_MODEL] ?: "",
                ""
            ),
            ollamaBaseUrl = prefs[KEY_OLLAMA_URL] ?: AppSettings.DEFAULT_OLLAMA_URL,
            ollamaModel = sanitizeModelIdFor(
                AiProvider.OLLAMA,
                prefs[KEY_OLLAMA_MODEL] ?: AppSettings.DEFAULT_OLLAMA_MODEL,
                AppSettings.DEFAULT_OLLAMA_MODEL
            ),
            ollamaTextModel = sanitizeModelIdFor(
                AiProvider.OLLAMA,
                prefs[KEY_OLLAMA_TEXT_MODEL] ?: "",
                ""
            ),
            ollamaUseCloud = prefs[KEY_OLLAMA_USE_CLOUD] ?: false,
            ollamaApiKeys = ollamaKeys,
            ollamaApiKey = ollamaKeys.firstOrNull().orEmpty(),
            aiAutoFailover = prefs[KEY_AI_AUTO_FAILOVER] ?: true,
            showPaidModels = prefs[KEY_SHOW_PAID_MODELS] ?: false,
            activeAiProvider = prefs[KEY_ACTIVE_AI_PROVIDER]?.let {
                runCatching { AiProvider.valueOf(it) }.getOrNull()
            },
            activePhotoModel = run {
                val raw = prefs[KEY_ACTIVE_PHOTO_MODEL] ?: prefs[KEY_ACTIVE_AI_MODEL] ?: ""
                val prov = prefs[KEY_ACTIVE_AI_PROVIDER]?.let {
                    runCatching { AiProvider.valueOf(it) }.getOrNull()
                }
                if (prov != null && raw.isNotBlank() && !isPlausibleModelIdFor(prov, raw)) "" else raw
            },
            activeTextModel = run {
                val raw = prefs[KEY_ACTIVE_TEXT_MODEL] ?: ""
                val prov = prefs[KEY_ACTIVE_AI_PROVIDER]?.let {
                    runCatching { AiProvider.valueOf(it) }.getOrNull()
                }
                if (prov != null && raw.isNotBlank() && !isPlausibleModelIdFor(prov, raw)) "" else raw
            },
            dynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: true,
            autoCheckUpdates = prefs[KEY_AUTO_CHECK_UPDATES] ?: !BuildConfig.DEBUG,
            supportId = prefs[KEY_SUPPORT_ID].orEmpty(),
            crashReportingEnabled = prefs[KEY_CRASH_REPORTING] ?: !BuildConfig.DEBUG,
            easterEggDiscovered = prefs[KEY_EASTER_EGG] ?: false,
            dailyLogReminderEnabled = prefs[KEY_DAILY_LOG_REMINDER] ?: true,
            dailyLogReminderHour = (prefs[KEY_DAILY_LOG_REMINDER_HOUR]
                ?: AppSettings.DEFAULT_REMINDER_HOUR).coerceIn(0, 23),
            dailyLogReminderMinute = (prefs[KEY_DAILY_LOG_REMINDER_MINUTE]
                ?: AppSettings.DEFAULT_REMINDER_MINUTE).coerceIn(0, 59),
            developerModeUnlocked = prefs[KEY_DEVELOPER_UNLOCKED] ?: false,
            forceOfflineAiSimulator = prefs[KEY_FORCE_OFFLINE_AI] ?: false,
            showRawAiJson = prefs[KEY_SHOW_RAW_AI_JSON] ?: false,
            strictClarification = prefs[KEY_STRICT_CLARIFICATION] ?: false,
            verboseHttpLogging = prefs[KEY_VERBOSE_HTTP] ?: false,
            cloudBackupEnabled = prefs[KEY_CLOUD_BACKUP_ENABLED] ?: false,
            cloudAutoUploadEnabled = prefs[KEY_CLOUD_AUTO_UPLOAD] ?: true,
            mongoDbName = prefs[KEY_MONGO_DB_NAME]?.ifBlank { null }
                ?: AppSettings.DEFAULT_MONGO_DB_NAME,
            mongoCollectionName = prefs[KEY_MONGO_COLLECTION]?.ifBlank { null }
                ?: AppSettings.DEFAULT_MONGO_COLLECTION,
            mongoLastUploadAt = prefs[KEY_MONGO_LAST_UPLOAD_AT] ?: 0L,
            mongoLastUploadOk = prefs[KEY_MONGO_LAST_UPLOAD_OK] ?: false,
            mongoLastError = prefs[KEY_MONGO_LAST_ERROR].orEmpty()
        )
    }

    /** Ensures a stable anonymous support id exists; returns it. */
    suspend fun ensureSupportId(): String {
        val existing = dataStore.data.first()[KEY_SUPPORT_ID].orEmpty()
        if (existing.isNotBlank()) return existing
        val id = java.util.UUID.randomUUID().toString()
        dataStore.edit { prefs -> prefs[KEY_SUPPORT_ID] = id }
        return id
    }

    /** Replaces the Support ID (crash reporting + cloud backup document key). */
    suspend fun regenerateSupportId(): String {
        val id = java.util.UUID.randomUUID().toString()
        dataStore.edit { prefs -> prefs[KEY_SUPPORT_ID] = id }
        return id
    }

    /** Forces [supportId] when non-blank (e.g. after cloud restore). */
    suspend fun setSupportId(supportId: String) {
        val id = supportId.trim()
        if (id.isBlank()) return
        dataStore.edit { prefs -> prefs[KEY_SUPPORT_ID] = id }
    }

    /** UTC calendar day `yyyy-MM-dd` of the last Sentry heartbeat, or null. */
    suspend fun lastHeartbeatUtcDay(): String? =
        dataStore.data.first()[KEY_LAST_HEARTBEAT_DAY]

    suspend fun markHeartbeatSent(utcDay: String) {
        dataStore.edit { prefs -> prefs[KEY_LAST_HEARTBEAT_DAY] = utcDay }
    }

    /** Active model cooldowns (expired entries already pruned). Survives process death. */
    suspend fun modelCooldowns(): Map<String, Long> {
        val prefs = dataStore.data.first()
        return decodeModelCooldowns(prefs[KEY_MODEL_COOLDOWNS])
    }

    /**
     * Marks [modelId] on [provider] as unavailable until the next UTC midnight.
     * No-op if [error] is not a rate/quota limit. Survives app restarts; after UTC day
     * rollover, Auto failover tries the highest models again.
     */
    suspend fun markModelCooldown(
        provider: AiProvider,
        modelId: String,
        error: Throwable,
        nowEpochMs: Long = System.currentTimeMillis()
    ) {
        if (modelId.isBlank() || !ModelCooldownPolicy.isRateLimitError(error)) return
        val until = ModelCooldownPolicy.cooldownUntilEpochMs(nowEpochMs)
        dataStore.edit { prefs ->
            val current = decodeModelCooldowns(prefs[KEY_MODEL_COOLDOWNS], nowEpochMs).toMutableMap()
            val key = ModelCooldown.keyOf(provider, modelId)
            val existing = current[key] ?: 0L
            current[key] = maxOf(existing, until)
            prefs[KEY_MODEL_COOLDOWNS] = encodeModelCooldowns(current)
        }
    }

    /** Clears all persisted model rate-limit cooldowns (developer tooling). */
    suspend fun clearModelCooldowns() {
        dataStore.edit { prefs -> prefs.remove(KEY_MODEL_COOLDOWNS) }
    }

    /**
     * Records the model last used successfully for photo or text (green “active” lines in
     * Settings). Does not change the preferred dropdown selection — after rate-limit
     * cooldowns expire, Auto tries that preferred model first again.
     * Ignores ids that don't belong to [provider] (e.g. Gemini Studio ids on OpenRouter).
     */
    suspend fun setActiveAiModel(provider: AiProvider, modelId: String, forPhoto: Boolean) {
        if (modelId.isBlank() || !isPlausibleModelIdFor(provider, modelId)) return
        dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_AI_PROVIDER] = provider.name
            if (forPhoto) {
                prefs[KEY_ACTIVE_PHOTO_MODEL] = modelId
            } else {
                prefs[KEY_ACTIVE_TEXT_MODEL] = modelId
            }
        }
    }

    suspend fun save(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_PROVIDER] = settings.provider.name
            prefs[KEY_OR_KEY] = joinApiKeys(settings.keysFor(AiProvider.OPENROUTER))
            prefs[KEY_OR_OAUTH_KEY] = settings.openRouterOAuthKey
            prefs[KEY_OR_MODEL] = settings.openRouterModel
            prefs[KEY_OR_TEXT_MODEL] = settings.openRouterTextModel
            prefs[KEY_GEMINI_KEY] = joinApiKeys(settings.keysFor(AiProvider.GEMINI))
            prefs[KEY_GEMINI_MODEL] = settings.geminiModel
            prefs[KEY_GEMINI_TEXT_MODEL] = settings.geminiTextModel
            prefs[KEY_OLLAMA_URL] = settings.ollamaBaseUrl
            prefs[KEY_OLLAMA_MODEL] = settings.ollamaModel
            prefs[KEY_OLLAMA_TEXT_MODEL] = settings.ollamaTextModel
            prefs[KEY_OLLAMA_USE_CLOUD] = settings.ollamaUseCloud
            prefs[KEY_OLLAMA_API_KEY] = joinApiKeys(settings.keysFor(AiProvider.OLLAMA))
            prefs[KEY_AI_AUTO_FAILOVER] = settings.aiAutoFailover
            prefs[KEY_SHOW_PAID_MODELS] = settings.showPaidModels
            prefs[KEY_DYNAMIC_COLOR] = settings.dynamicColor
            prefs[KEY_AUTO_CHECK_UPDATES] = settings.autoCheckUpdates
            prefs[KEY_CRASH_REPORTING] = settings.crashReportingEnabled
            if (settings.supportId.isNotBlank()) {
                prefs[KEY_SUPPORT_ID] = settings.supportId
            }
            prefs[KEY_EASTER_EGG] = settings.easterEggDiscovered
            prefs[KEY_DAILY_LOG_REMINDER] = settings.dailyLogReminderEnabled
            prefs[KEY_DAILY_LOG_REMINDER_HOUR] = settings.dailyLogReminderHour.coerceIn(0, 23)
            prefs[KEY_DAILY_LOG_REMINDER_MINUTE] = settings.dailyLogReminderMinute.coerceIn(0, 59)
            prefs[KEY_DEVELOPER_UNLOCKED] = settings.developerModeUnlocked
            prefs[KEY_FORCE_OFFLINE_AI] = settings.forceOfflineAiSimulator
            prefs[KEY_SHOW_RAW_AI_JSON] = settings.showRawAiJson
            prefs[KEY_STRICT_CLARIFICATION] = settings.strictClarification
            prefs[KEY_VERBOSE_HTTP] = settings.verboseHttpLogging
            prefs[KEY_CLOUD_BACKUP_ENABLED] = settings.cloudBackupEnabled
            prefs[KEY_CLOUD_AUTO_UPLOAD] = settings.cloudAutoUploadEnabled
            prefs[KEY_MONGO_DB_NAME] = settings.mongoDbName.ifBlank {
                AppSettings.DEFAULT_MONGO_DB_NAME
            }
            prefs[KEY_MONGO_COLLECTION] = settings.mongoCollectionName.ifBlank {
                AppSettings.DEFAULT_MONGO_COLLECTION
            }
            // Cooldowns stay until UTC midnight; AI calls still update active after success.
            // Save always resets Auto selection to the preferred provider's current models
            // (covers platform change, Local↔Cloud, and dropdown edits).
            prefs[KEY_ACTIVE_AI_PROVIDER] = settings.provider.name
            val photo = settings.model
            if (photo.isNotBlank() && isPlausibleModelIdFor(settings.provider, photo)) {
                prefs[KEY_ACTIVE_PHOTO_MODEL] = photo
            } else {
                prefs.remove(KEY_ACTIVE_PHOTO_MODEL)
            }
            val textRaw = when (settings.provider) {
                AiProvider.OPENROUTER -> settings.openRouterTextModel
                AiProvider.GEMINI -> settings.geminiTextModel
                AiProvider.OLLAMA -> settings.ollamaTextModel
            }.trim()
            val text = textRaw.ifBlank { settings.textModel }
            if (text.isNotBlank() && isPlausibleModelIdFor(settings.provider, text)) {
                prefs[KEY_ACTIVE_TEXT_MODEL] = text
            } else {
                prefs.remove(KEY_ACTIVE_TEXT_MODEL)
            }
        }
    }

    /** Records the outcome of a MongoDB cloud upload without touching AI active-model keys. */
    suspend fun setMongoUploadStatus(ok: Boolean, error: String = "", at: Long = System.currentTimeMillis()) {
        dataStore.edit { prefs ->
            prefs[KEY_MONGO_LAST_UPLOAD_AT] = at
            prefs[KEY_MONGO_LAST_UPLOAD_OK] = ok
            prefs[KEY_MONGO_LAST_ERROR] = error.take(500)
        }
    }

    /** Persists an OAuth-issued OpenRouter key without touching manual API keys. */
    suspend fun setOpenRouterOAuthKey(key: String) {
        dataStore.edit { prefs ->
            prefs[KEY_OR_OAUTH_KEY] = key
            prefs[KEY_PROVIDER] = AiProvider.OPENROUTER.name
        }
    }

    suspend fun clearOpenRouterOAuthKey() {
        dataStore.edit { prefs -> prefs[KEY_OR_OAUTH_KEY] = "" }
    }

    /** PKCE verifier for an in-flight OpenRouter OAuth — survives process death in the Custom Tab. */
    suspend fun setPendingOpenRouterPkceVerifier(verifier: String) {
        dataStore.edit { prefs -> prefs[KEY_OR_OAUTH_PKCE_VERIFIER] = verifier }
    }

    /** Returns and clears any pending PKCE verifier. */
    suspend fun takePendingOpenRouterPkceVerifier(): String? {
        val prefs = dataStore.data.first()
        val verifier = prefs[KEY_OR_OAUTH_PKCE_VERIFIER]?.takeIf { it.isNotBlank() } ?: return null
        dataStore.edit { it.remove(KEY_OR_OAUTH_PKCE_VERIFIER) }
        return verifier
    }

    private companion object {
        val KEY_PROVIDER = stringPreferencesKey("ai_provider")
        val KEY_OR_KEY = stringPreferencesKey("openrouter_api_key")
        val KEY_OR_OAUTH_KEY = stringPreferencesKey("openrouter_oauth_key")
        val KEY_OR_OAUTH_PKCE_VERIFIER = stringPreferencesKey("openrouter_oauth_pkce_verifier")
        val KEY_OR_MODEL = stringPreferencesKey("openrouter_model")
        val KEY_OR_TEXT_MODEL = stringPreferencesKey("openrouter_text_model")
        val KEY_GEMINI_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val KEY_GEMINI_TEXT_MODEL = stringPreferencesKey("gemini_text_model")
        val KEY_OLLAMA_URL = stringPreferencesKey("ollama_base_url")
        val KEY_OLLAMA_MODEL = stringPreferencesKey("ollama_model")
        val KEY_OLLAMA_TEXT_MODEL = stringPreferencesKey("ollama_text_model")
        val KEY_OLLAMA_USE_CLOUD = booleanPreferencesKey("ollama_use_cloud")
        val KEY_OLLAMA_API_KEY = stringPreferencesKey("ollama_api_key")
        val KEY_AI_AUTO_FAILOVER = booleanPreferencesKey("ai_auto_failover")
        val KEY_SHOW_PAID_MODELS = booleanPreferencesKey("show_paid_models")
        val KEY_ACTIVE_AI_PROVIDER = stringPreferencesKey("active_ai_provider")
        val KEY_ACTIVE_AI_MODEL = stringPreferencesKey("active_ai_model") // legacy
        val KEY_ACTIVE_PHOTO_MODEL = stringPreferencesKey("active_photo_model")
        val KEY_ACTIVE_TEXT_MODEL = stringPreferencesKey("active_text_model")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val KEY_SUPPORT_ID = stringPreferencesKey("support_id")
        val KEY_CRASH_REPORTING = booleanPreferencesKey("crash_reporting_enabled")
        val KEY_LAST_HEARTBEAT_DAY = stringPreferencesKey("sentry_last_heartbeat_utc_day")
        val KEY_EASTER_EGG = booleanPreferencesKey("easter_egg_discovered")
        val KEY_MODEL_COOLDOWNS = stringPreferencesKey("ai_model_cooldowns")
        val KEY_DAILY_LOG_REMINDER = booleanPreferencesKey("daily_log_reminder_enabled")
        val KEY_DAILY_LOG_REMINDER_HOUR = intPreferencesKey("daily_log_reminder_hour")
        val KEY_DAILY_LOG_REMINDER_MINUTE = intPreferencesKey("daily_log_reminder_minute")
        val KEY_DEVELOPER_UNLOCKED = booleanPreferencesKey("developer_mode_unlocked")
        val KEY_FORCE_OFFLINE_AI = booleanPreferencesKey("force_offline_ai_simulator")
        val KEY_SHOW_RAW_AI_JSON = booleanPreferencesKey("show_raw_ai_json")
        val KEY_STRICT_CLARIFICATION = booleanPreferencesKey("strict_clarification")
        val KEY_VERBOSE_HTTP = booleanPreferencesKey("verbose_http_logging")
        val KEY_CLOUD_BACKUP_ENABLED = booleanPreferencesKey("cloud_backup_enabled")
        val KEY_CLOUD_AUTO_UPLOAD = booleanPreferencesKey("cloud_auto_upload_enabled")
        val KEY_MONGO_DB_NAME = stringPreferencesKey("mongo_db_name")
        val KEY_MONGO_COLLECTION = stringPreferencesKey("mongo_collection_name")
        val KEY_MONGO_LAST_UPLOAD_AT = longPreferencesKey("mongo_last_upload_at")
        val KEY_MONGO_LAST_UPLOAD_OK = booleanPreferencesKey("mongo_last_upload_ok")
        val KEY_MONGO_LAST_ERROR = stringPreferencesKey("mongo_last_error")
    }
}
