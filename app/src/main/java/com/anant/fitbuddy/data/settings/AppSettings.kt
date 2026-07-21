package com.anant.fitbuddy.data.settings

import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.backup.mongo.MongoUriVault

/** Which LLM backend the app talks to. All use the OpenAI-compatible chat/completions API. */
enum class AiProvider {
    OPENROUTER,
    GEMINI,
    OLLAMA;

    fun displayName(): String = when (this) {
        OPENROUTER -> "OpenRouter"
        GEMINI -> "Gemini"
        OLLAMA -> "Ollama"
    }
}

/**
 * All user-configurable app settings (persisted in DataStore). Endpoint/model/auth are derived
 * from [provider] so the network layer stays provider-agnostic.
 *
 * Each provider may store multiple API keys ([openRouterApiKeys], etc.). [openRouterApiKey] /
 * [geminiApiKey] / [ollamaApiKey] are the **active** key used for the current attempt
 * (first key on load; overridden per-attempt during Auto failover).
 */
data class AppSettings(
    val provider: AiProvider = AiProvider.OPENROUTER,
    val openRouterApiKeys: List<String> = emptyList(),
    val openRouterApiKey: String = "",
    /**
     * Key issued by OpenRouter OAuth PKCE ("Continue with OpenRouter").
     * Tried after all [openRouterApiKeys] during failover.
     */
    val openRouterOAuthKey: String = "",
    val openRouterModel: String = DEFAULT_OPENROUTER_MODEL,
    val openRouterTextModel: String = "",
    val geminiApiKeys: List<String> = emptyList(),
    val geminiApiKey: String = "",
    val geminiModel: String = DEFAULT_GEMINI_MODEL,
    val geminiTextModel: String = "",
    val ollamaBaseUrl: String = DEFAULT_OLLAMA_URL,
    val ollamaModel: String = DEFAULT_OLLAMA_MODEL,
    val ollamaTextModel: String = "",
    /** When true, talk to ollama.com Cloud instead of a local/LAN server. */
    val ollamaUseCloud: Boolean = false,
    val ollamaApiKeys: List<String> = emptyList(),
    val ollamaApiKey: String = "",
    /**
     * When true, failed requests rotate API keys then other models on the same platform.
     * When false, only the selected model is used; API keys still rotate on failure.
     * Never switches platforms automatically.
     */
    val aiAutoFailover: Boolean = true,
    /**
     * When true, model dropdowns include paid OpenRouter / Gemini models (Pro, etc.).
     * Off by default (free-only). Refresh-models reachability probes are skipped while this
     * is on so paid endpoints are never pinged.
     */
    val showPaidModels: Boolean = false,
    /**
     * Last photo / text models Auto successfully used (with [activeAiProvider]).
     * Shown in green in Settings when Auto is on; the preferred dropdown selection is left
     * unchanged so after rate-limit cooldowns expire Auto tries that model first again.
     * Also reset to the preferred provider's models whenever Save AI Settings runs.
     * Empty until the first success for that modality (or a settings save).
     */
    val activeAiProvider: AiProvider? = null,
    val activePhotoModel: String = "",
    val activeTextModel: String = "",
    val dynamicColor: Boolean = true,
    /**
     * When true, FitBuddy checks GitHub Releases for a newer APK shortly after startup
     * (and still allows a manual check in Settings).
     */
    val autoCheckUpdates: Boolean = !BuildConfig.DEBUG && !BuildConfig.IS_FDROID,
    /**
     * Device-local display name (not in BackupData v5 / BackupSettings — survives app updates
     * via DataStore only; not restored from backup).
     */
    val firstName: String = "",
    val lastName: String = "",
    /**
     * Anonymous install id for crash support (Sentry user.id). Generated once, never PII.
     * Share this with the developer when reporting a bug.
     */
    val supportId: String = "",
    /** When false, Sentry does not send crash events (SDK may still be initialized). Off by default on F-Droid. */
    val crashReportingEnabled: Boolean = !BuildConfig.DEBUG && !BuildConfig.IS_FDROID,
    /** Set when the Settings "Created by" easter egg is unlocked. */
    val easterEggDiscovered: Boolean = false,
    /** Daily local notification reminding the user to log meals (AlarmManager; no Play Services). */
    val dailyLogReminderEnabled: Boolean = true,
    val dailyLogReminderHour: Int = DEFAULT_REMINDER_HOUR,
    val dailyLogReminderMinute: Int = DEFAULT_REMINDER_MINUTE,
    /** Persisted after Package-name unlock gesture in About. */
    val developerModeUnlocked: Boolean = false,
    /** Developer: bypass live AI and use the offline simulator (text only). */
    val forceOfflineAiSimulator: Boolean = false,
    /** Developer: show last raw AI JSON after analysis. */
    val showRawAiJson: Boolean = false,
    /** Developer/experimental: prompt prefers CLARIFICATION_REQUIRED when portions are ambiguous. */
    val strictClarification: Boolean = false,
    /** Developer: OkHttp BODY logs even on release builds. */
    val verboseHttpLogging: Boolean = false,
    /**
     * When true, uploads/downloads use the build-baked Atlas URI ([MongoUriVault]) and
     * this install's [supportId]. Off by default for guest installs until the user opts in.
     */
    val cloudBackupEnabled: Boolean = false,
    /**
     * When true (and [cloudBackupEnabled]), upload on app startup if the last successful
     * upload was ≥ 12 hours ago. Manual upload always bypasses the debounce.
     */
    val cloudAutoUploadEnabled: Boolean = true,
    /** Atlas database name (default [DEFAULT_MONGO_DB_NAME]). Overridable in Developer tools. */
    val mongoDbName: String = DEFAULT_MONGO_DB_NAME,
    /** Atlas collection name (default [DEFAULT_MONGO_COLLECTION]). Overridable in Developer tools. */
    val mongoCollectionName: String = DEFAULT_MONGO_COLLECTION,
    /** Epoch ms of the last cloud upload attempt (0 = never). */
    val mongoLastUploadAt: Long = 0L,
    val mongoLastUploadOk: Boolean = false,
    val mongoLastError: String = "",
    /**
     * Epoch ms of the last successful backup (local export or cloud upload).
     * Used to gate auto-install after "Export backup & update" (must be fresh).
     */
    val lastSuccessfulBackupAt: Long = 0L
) {
    /** Trimmed first name for greetings; empty when not set. */
    val displayFirstName: String
        get() = firstName.trim()

    /** True when a first name has been saved (greeting / name prompt gate). */
    val hasUserName: Boolean
        get() = displayFirstName.isNotEmpty()

    /** "First Last" for Sentry heartbeats; blank when no name. */
    val usernameForHeartbeat: String
        get() {
            val first = firstName.trim()
            val last = lastName.trim()
            return when {
                first.isEmpty() -> ""
                last.isEmpty() -> first
                else -> "$first $last"
            }
        }

    /** True when cloud backup is opted in and this build has a vault URI + Support ID. */
    val isMongoBackupConfigured: Boolean
        get() = cloudBackupEnabled &&
            supportId.isNotBlank() &&
            MongoUriVault.isAvailable()

    /**
     * True when a successful backup was recorded within [BACKUP_FRESHNESS_FOR_UPDATE_MS]
     * (used before auto-install after Export backup & update).
     */
    fun hasFreshSuccessfulBackup(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (lastSuccessfulBackupAt <= 0L) return false
        val age = nowMs - lastSuccessfulBackupAt
        return age in 0L..BACKUP_FRESHNESS_FOR_UPDATE_MS
    }

    /** Vision/multimodal model id sent for the active provider (used for photo analysis). */
    val model: String
        get() = when (provider) {
            AiProvider.OPENROUTER -> openRouterModel
            AiProvider.GEMINI -> geminiModel
            AiProvider.OLLAMA -> ollamaModel
        }

    /**
     * Model id used for text-only queries (loose text logs, "recalculate with AI"). Falls back to
     * the vision [model] when no dedicated text model is configured.
     */
    val textModel: String
        get() = when (provider) {
            AiProvider.OPENROUTER -> openRouterTextModel.ifBlank { openRouterModel }
            AiProvider.GEMINI -> geminiTextModel.ifBlank { geminiModel }
            AiProvider.OLLAMA -> ollamaTextModel.ifBlank { ollamaModel }
        }

    /** Picks the vision model when an image is attached, else the (cheaper) text model. */
    fun modelFor(hasImage: Boolean): String = if (hasImage) model else textModel

    /** Effective Ollama host (cloud fixed URL, or the user-supplied local/LAN base). */
    val ollamaEffectiveBaseUrl: String
        get() = if (ollamaUseCloud) {
            OLLAMA_CLOUD_BASE_URL
        } else {
            ollamaBaseUrl.trim().trimEnd('/')
        }

    /** Absolute chat-completions URL for the active provider (used with Retrofit @Url). */
    val chatUrl: String
        get() = when (provider) {
            AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            AiProvider.GEMINI -> "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
            AiProvider.OLLAMA -> ollamaEffectiveBaseUrl + "/v1/chat/completions"
        }

    /** Authorization header value, or null when the provider needs none (local Ollama). */
    val authHeader: String?
        get() = when (provider) {
            AiProvider.OPENROUTER -> activeKey(AiProvider.OPENROUTER).takeIf { it.isNotBlank() }?.let { "Bearer $it" }
            AiProvider.GEMINI -> activeKey(AiProvider.GEMINI).takeIf { it.isNotBlank() }?.let { "Bearer $it" }
            AiProvider.OLLAMA -> if (ollamaUseCloud) {
                activeKey(AiProvider.OLLAMA).takeIf { it.isNotBlank() }?.let { "Bearer $it" }
            } else {
                null
            }
        }

    /** Manual (pasted) keys only — never includes [openRouterOAuthKey]. */
    fun keysFor(p: AiProvider): List<String> = when (p) {
        AiProvider.OPENROUTER -> openRouterApiKeys.ifEmpty {
            listOfNotNull(openRouterApiKey.takeIf { it.isNotBlank() && it != openRouterOAuthKey })
        }
        AiProvider.GEMINI -> geminiApiKeys.ifEmpty {
            listOfNotNull(geminiApiKey.takeIf { it.isNotBlank() })
        }
        AiProvider.OLLAMA -> ollamaApiKeys.ifEmpty {
            listOfNotNull(ollamaApiKey.takeIf { it.isNotBlank() })
        }
    }

    /** OpenRouter request order: pasted keys first, OAuth key last. */
    fun openRouterAttemptKeys(): List<String> {
        val manual = keysFor(AiProvider.OPENROUTER)
        val oauth = openRouterOAuthKey.takeIf { it.isNotBlank() && it !in manual }
        return manual + listOfNotNull(oauth)
    }

    val isOpenRouterOAuthConnected: Boolean
        get() = openRouterOAuthKey.isNotBlank()

    fun activeKey(p: AiProvider): String = when (p) {
        AiProvider.OPENROUTER -> openRouterApiKey.ifBlank {
            openRouterAttemptKeys().firstOrNull().orEmpty()
        }
        AiProvider.GEMINI -> geminiApiKey.ifBlank { geminiApiKeys.firstOrNull().orEmpty() }
        AiProvider.OLLAMA -> ollamaApiKey.ifBlank { ollamaApiKeys.firstOrNull().orEmpty() }
    }

    /** In-memory copy with [key] as the active credential for [p] (failover attempt). */
    fun withKey(p: AiProvider, key: String): AppSettings = when (p) {
        AiProvider.OPENROUTER -> copy(provider = p, openRouterApiKey = key)
        AiProvider.GEMINI -> copy(provider = p, geminiApiKey = key)
        AiProvider.OLLAMA -> copy(provider = p, ollamaApiKey = key)
    }

    /** In-memory copy targeting [p] with both vision and text model ids set to [modelId]. */
    fun withModel(p: AiProvider, modelId: String): AppSettings = when (p) {
        AiProvider.OPENROUTER -> copy(
            provider = p,
            openRouterModel = modelId,
            openRouterTextModel = modelId
        )
        AiProvider.GEMINI -> copy(
            provider = p,
            geminiModel = modelId,
            geminiTextModel = modelId
        )
        AiProvider.OLLAMA -> copy(
            provider = p,
            ollamaModel = modelId,
            ollamaTextModel = modelId
        )
    }

    /** True when [p] has enough config to attempt a live call. */
    fun isProviderConfigured(p: AiProvider): Boolean = when (p) {
        AiProvider.OPENROUTER ->
            openRouterAttemptKeys().isNotEmpty() && openRouterModel.isNotBlank()
        AiProvider.GEMINI -> keysFor(p).isNotEmpty() && geminiModel.isNotBlank()
        AiProvider.OLLAMA -> if (ollamaUseCloud) {
            keysFor(p).isNotEmpty() && ollamaModel.isNotBlank()
        } else {
            ollamaBaseUrl.isNotBlank() && ollamaModel.isNotBlank()
        }
    }

    /** True when the preferred [provider] has enough config to attempt a live call. */
    val isConfigured: Boolean
        get() = isProviderConfigured(provider)

    /** Labels for Settings Auto status (photo and text tracked separately). */
    fun activePhotoModelDisplay(): String {
        val prov = activeAiProvider?.displayName() ?: provider.displayName()
        val id = activePhotoModel.ifBlank { model }.ifBlank { "(none)" }
        return "$prov: $id"
    }

    fun activeTextModelDisplay(): String {
        val prov = activeAiProvider?.displayName() ?: provider.displayName()
        val id = activeTextModel.ifBlank { textModel }.ifBlank { "(none)" }
        return "$prov: $id"
    }

    companion object {
        const val DEFAULT_OPENROUTER_MODEL = "google/gemma-4-31b-it:free"
        const val DEFAULT_GEMINI_MODEL = "gemini-3.5-flash"
        const val DEFAULT_OLLAMA_URL = "http://192.168.1.10:11434"
        const val DEFAULT_OLLAMA_MODEL = "llava"
        const val OLLAMA_CLOUD_BASE_URL = "https://ollama.com"
        const val DEFAULT_REMINDER_HOUR = 20
        const val DEFAULT_REMINDER_MINUTE = 0
        /** Debounce window for startup auto-upload (manual upload bypasses this). */
        const val CLOUD_AUTO_UPLOAD_DEBOUNCE_MS: Long = 12L * 60L * 60L * 1000L
        /** Max age of [lastSuccessfulBackupAt] before auto-install after backup-and-update. */
        const val BACKUP_FRESHNESS_FOR_UPDATE_MS: Long = 5L * 60L * 1000L
        const val DEFAULT_MONGO_DB_NAME = "fitbuddy"
        const val DEFAULT_MONGO_COLLECTION = "fitbuddy_backup"

        /** Build settings from key lists, syncing active key to the first entry. */
        fun withKeys(
            openRouterKeys: List<String> = emptyList(),
            geminiKeys: List<String> = emptyList(),
            ollamaKeys: List<String> = emptyList(),
            base: AppSettings = AppSettings()
        ): AppSettings = base.copy(
            openRouterApiKeys = openRouterKeys,
            openRouterApiKey = openRouterKeys.firstOrNull().orEmpty(),
            geminiApiKeys = geminiKeys,
            geminiApiKey = geminiKeys.firstOrNull().orEmpty(),
            ollamaApiKeys = ollamaKeys,
            ollamaApiKey = ollamaKeys.firstOrNull().orEmpty()
        )
    }
}
