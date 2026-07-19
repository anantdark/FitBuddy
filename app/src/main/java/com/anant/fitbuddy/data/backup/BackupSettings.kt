package com.anant.fitbuddy.data.backup

import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.squareup.moshi.JsonClass

/**
 * Serializable slice of [AppSettings] for backup JSON. Includes AI keys so a restore can
 * pick up on another device without re-entering Settings. Model cooldowns are omitted
 * (ephemeral rate-limit state).
 */
@JsonClass(generateAdapter = true)
data class BackupSettings(
    val provider: String = AiProvider.OPENROUTER.name,
    val openRouterApiKeys: List<String> = emptyList(),
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
    val activeAiProvider: String? = null,
    val activePhotoModel: String = "",
    val activeTextModel: String = "",
    val dynamicColor: Boolean = true,
    val autoCheckUpdates: Boolean = true,
    val supportId: String = "",
    val crashReportingEnabled: Boolean = true,
    val easterEggDiscovered: Boolean = false,
    val dailyLogReminderEnabled: Boolean = true,
    val dailyLogReminderHour: Int = AppSettings.DEFAULT_REMINDER_HOUR,
    val dailyLogReminderMinute: Int = AppSettings.DEFAULT_REMINDER_MINUTE,
    val developerModeUnlocked: Boolean = false,
    val forceOfflineAiSimulator: Boolean = false,
    val showRawAiJson: Boolean = false,
    val strictClarification: Boolean = false,
    val verboseHttpLogging: Boolean = false
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
                openRouterModel = openRouterModel,
                openRouterTextModel = openRouterTextModel,
                geminiModel = geminiModel,
                geminiTextModel = geminiTextModel,
                ollamaBaseUrl = ollamaBaseUrl,
                ollamaModel = ollamaModel,
                ollamaTextModel = ollamaTextModel,
                ollamaUseCloud = ollamaUseCloud,
                aiAutoFailover = aiAutoFailover,
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
                verboseHttpLogging = verboseHttpLogging
            )
        )
    }

    companion object {
        fun from(settings: AppSettings): BackupSettings = BackupSettings(
            provider = settings.provider.name,
            openRouterApiKeys = settings.keysFor(AiProvider.OPENROUTER),
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
            verboseHttpLogging = settings.verboseHttpLogging
        )
    }
}
