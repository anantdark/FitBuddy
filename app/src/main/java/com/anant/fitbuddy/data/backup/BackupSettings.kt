package com.anant.fitbuddy.data.backup

import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.squareup.moshi.JsonClass

/**
 * Serializable slice of [AppSettings] for backup JSON. Includes AI keys so a restore can
 * pick up on another device without re-entering Settings.
 *
 * Intentionally omitted (ephemeral / device-local / build-baked):
 * - model rate-limit cooldowns
 * - [AppSettings.mongoLastUploadAt] / [AppSettings.mongoLastUploadOk] / [AppSettings.mongoLastError]
 * - [AppSettings.lastSuccessfulBackupAt]
 * - Legacy cloud-backup URI fields (never in backup JSON on F-Droid)
 * - Heartbeat day / in-flight OAuth PKCE verifier
 *
 * Active API key strings ([AppSettings.openRouterApiKey] etc.) are derived from the key lists
 * on restore via [AppSettings.withKeys].
 *
 * When adding a user-facing [AppSettings] field, add it here + [from] + [toAppSettings],
 * and extend [com.anant.fitbuddy.data.backup.BackupSettingsTest].
 */
@JsonClass(generateAdapter = true)
data class BackupSettings(
    val provider: String = AiProvider.OPENROUTER.name,
    val openRouterApiKeys: List<String> = emptyList(),
    val openRouterOAuthKey: String = "",
    val openRouterModel: String = AppSettings.DEFAULT_OPENROUTER_MODEL,
    val openRouterTextModel: String = "",
    val geminiApiKeys: List<String> = emptyList(),
    val geminiModel: String = AppSettings.DEFAULT_GEMINI_MODEL,
    val geminiTextModel: String = "",
    val ollamaBaseUrl: String = AppSettings.DEFAULT_OLLAMA_URL,
    val ollamaModel: String = AppSettings.DEFAULT_OLLAMA_MODEL,
    val ollamaTextModel: String = "",
    val ollamaUseCloud: Boolean = false,
    val ollamaApiKeys: List<String> = emptyList(),
    val aiAutoFailover: Boolean = true,
    val showPaidModels: Boolean = false,
    val activeAiProvider: String? = null,
    val activePhotoModel: String = "",
    val activeTextModel: String = "",
    val dynamicColor: Boolean = true,
    val autoCheckUpdates: Boolean = false,
    val supportId: String = "",
    val crashReportingEnabled: Boolean = false,
    val easterEggDiscovered: Boolean = false,
    val dailyLogReminderEnabled: Boolean = true,
    val dailyLogReminderHour: Int = AppSettings.DEFAULT_REMINDER_HOUR,
    val dailyLogReminderMinute: Int = AppSettings.DEFAULT_REMINDER_MINUTE,
    val developerModeUnlocked: Boolean = false,
    val forceOfflineAiSimulator: Boolean = false,
    val showRawAiJson: Boolean = false,
    val strictClarification: Boolean = false,
    val verboseHttpLogging: Boolean = false,
    val cloudBackupEnabled: Boolean = false,
    val cloudAutoUploadEnabled: Boolean = true,
    val mongoDbName: String = AppSettings.DEFAULT_MONGO_DB_NAME,
    val mongoCollectionName: String = AppSettings.DEFAULT_MONGO_COLLECTION,
    /** @deprecated Ignored on import — Atlas URI is build-baked. Kept for old JSON compatibility. */
    val mongoDbUri: String = "",
) {
    fun toAppSettings(): AppSettings {
        val provider = runCatching { AiProvider.valueOf(provider) }.getOrDefault(AiProvider.OPENROUTER)
        val activeProvider = activeAiProvider?.let {
            runCatching { AiProvider.valueOf(it) }.getOrNull()
        }
        return AppSettings.withKeys(
            openRouterKeys = openRouterApiKeys,
            geminiKeys = geminiApiKeys,
            ollamaKeys = ollamaApiKeys,
            base = AppSettings(
                provider = provider,
                openRouterOAuthKey = openRouterOAuthKey,
                openRouterModel = openRouterModel,
                openRouterTextModel = openRouterTextModel,
                geminiModel = geminiModel,
                geminiTextModel = geminiTextModel,
                ollamaBaseUrl = ollamaBaseUrl,
                ollamaModel = ollamaModel,
                ollamaTextModel = ollamaTextModel,
                ollamaUseCloud = ollamaUseCloud,
                aiAutoFailover = aiAutoFailover,
                showPaidModels = showPaidModels,
                activeAiProvider = activeProvider,
                activePhotoModel = activePhotoModel,
                activeTextModel = activeTextModel,
                dynamicColor = dynamicColor,
                autoCheckUpdates = autoCheckUpdates,
                supportId = supportId,
                crashReportingEnabled = crashReportingEnabled,
                easterEggDiscovered = easterEggDiscovered,
                dailyLogReminderEnabled = dailyLogReminderEnabled,
                dailyLogReminderHour = dailyLogReminderHour.coerceIn(0, 23),
                dailyLogReminderMinute = dailyLogReminderMinute.coerceIn(0, 59),
                developerModeUnlocked = developerModeUnlocked,
                forceOfflineAiSimulator = forceOfflineAiSimulator,
                showRawAiJson = showRawAiJson,
                strictClarification = strictClarification,
                verboseHttpLogging = verboseHttpLogging,
                cloudBackupEnabled = cloudBackupEnabled,
                cloudAutoUploadEnabled = cloudAutoUploadEnabled,
                mongoDbName = mongoDbName.ifBlank { AppSettings.DEFAULT_MONGO_DB_NAME },
                mongoCollectionName = mongoCollectionName.ifBlank {
                    AppSettings.DEFAULT_MONGO_COLLECTION
                }
            )
        )
    }

    companion object {
        fun from(settings: AppSettings): BackupSettings = BackupSettings(
            provider = settings.provider.name,
            openRouterApiKeys = settings.keysFor(AiProvider.OPENROUTER),
            openRouterOAuthKey = settings.openRouterOAuthKey,
            openRouterModel = settings.openRouterModel,
            openRouterTextModel = settings.openRouterTextModel,
            geminiApiKeys = settings.keysFor(AiProvider.GEMINI),
            geminiModel = settings.geminiModel,
            geminiTextModel = settings.geminiTextModel,
            ollamaBaseUrl = settings.ollamaBaseUrl,
            ollamaModel = settings.ollamaModel,
            ollamaTextModel = settings.ollamaTextModel,
            ollamaUseCloud = settings.ollamaUseCloud,
            ollamaApiKeys = settings.keysFor(AiProvider.OLLAMA),
            aiAutoFailover = settings.aiAutoFailover,
            showPaidModels = settings.showPaidModels,
            activeAiProvider = settings.activeAiProvider?.name,
            activePhotoModel = settings.activePhotoModel,
            activeTextModel = settings.activeTextModel,
            dynamicColor = settings.dynamicColor,
            autoCheckUpdates = settings.autoCheckUpdates,
            supportId = settings.supportId,
            crashReportingEnabled = settings.crashReportingEnabled,
            easterEggDiscovered = settings.easterEggDiscovered,
            dailyLogReminderEnabled = settings.dailyLogReminderEnabled,
            dailyLogReminderHour = settings.dailyLogReminderHour,
            dailyLogReminderMinute = settings.dailyLogReminderMinute,
            developerModeUnlocked = settings.developerModeUnlocked,
            forceOfflineAiSimulator = settings.forceOfflineAiSimulator,
            showRawAiJson = settings.showRawAiJson,
            strictClarification = settings.strictClarification,
            verboseHttpLogging = settings.verboseHttpLogging,
            cloudBackupEnabled = settings.cloudBackupEnabled,
            cloudAutoUploadEnabled = settings.cloudAutoUploadEnabled,
            mongoDbName = settings.mongoDbName.ifBlank { AppSettings.DEFAULT_MONGO_DB_NAME },
            mongoCollectionName = settings.mongoCollectionName.ifBlank {
                AppSettings.DEFAULT_MONGO_COLLECTION
            },
            mongoDbUri = ""
        )
    }
}
