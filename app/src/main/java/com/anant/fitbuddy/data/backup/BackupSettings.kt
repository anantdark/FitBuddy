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
 * - Atlas connection URI (build-baked via MongoUriVault — never in backup JSON)
 * - Sentry heartbeat day, last-known version code, in-flight OAuth PKCE verifier
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
    val openAiApiKeys: List<String> = emptyList(),
    val openAiModel: String = AppSettings.DEFAULT_OPENAI_MODEL,
    val openAiTextModel: String = "",
    // Per-provider auto-failover flags (default true).
    val aiAutoFailoverOpenRouter: Boolean = true,
    val aiAutoFailoverGemini: Boolean = true,
    val aiAutoFailoverOllama: Boolean = true,
    val aiAutoFailoverOpenAi: Boolean = true,
    // Per-provider show-paid-models flags (default false; OpenAI always treated as paid in-app).
    val showPaidModelsOpenRouter: Boolean = false,
    val showPaidModelsGemini: Boolean = false,
    val showPaidModelsOllama: Boolean = false,
    val showPaidModelsOpenAi: Boolean = true,
    val activeAiProvider: String? = null,
    val activePhotoModel: String = "",
    val activeTextModel: String = "",
    val dynamicColor: Boolean = true,
    /** Empty = absent; restore falls back to [loadingAnimationChoice] / [animationsEnabled]. */
    val analyzingAnimationChoice: String = "",
    /** Empty = absent; restore falls back to [loadingAnimationChoice] / [animationsEnabled]. */
    val insightAnimationChoice: String = "",
    /** Legacy shared choice; used when per-slot fields are blank. */
    val loadingAnimationChoice: String = "",
    /** Legacy boolean; used only when all choice fields are blank. */
    val animationsEnabled: Boolean = true,
    val autoCheckUpdates: Boolean = !BuildConfig.DEBUG && !BuildConfig.IS_FDROID,
    val supportId: String = "",
    val crashReportingEnabled: Boolean = !BuildConfig.DEBUG && !BuildConfig.IS_FDROID,
    val easterEggDiscovered: Boolean = false,
    val dailyLogReminderEnabled: Boolean = true,
    val dailyLogReminderHour: Int = AppSettings.DEFAULT_REMINDER_HOUR,
    val dailyLogReminderMinute: Int = AppSettings.DEFAULT_REMINDER_MINUTE,
    val dayChangeHour: Int = AppSettings.DEFAULT_DAY_CHANGE_HOUR,
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
    private fun resolvedSlotChoice(slotStored: String, analyzingSlot: Boolean): String {
        if (slotStored.isNotBlank()) return slotStored
        val shared = loadingAnimationChoice.takeIf { it.isNotBlank() }
        if (shared != null) {
            return when (shared) {
                AppSettings.LOADING_ANIM_OFF,
                AppSettings.LOADING_ANIM_RANDOM -> shared
                "solar_system" -> if (analyzingSlot) shared else AppSettings.LOADING_ANIM_RANDOM
                "japan_rowing" -> if (analyzingSlot) AppSettings.LOADING_ANIM_RANDOM else shared
                else -> AppSettings.LOADING_ANIM_RANDOM
            }
        }
        return if (!animationsEnabled) {
            AppSettings.LOADING_ANIM_OFF
        } else {
            AppSettings.LOADING_ANIM_RANDOM
        }
    }

    fun toAppSettings(): AppSettings {
        val provider = runCatching { AiProvider.valueOf(provider) }.getOrDefault(AiProvider.OPENROUTER)
        val activeProvider = activeAiProvider?.let {
            runCatching { AiProvider.valueOf(it) }.getOrNull()
        }
        return AppSettings.withKeys(
            openRouterKeys = openRouterApiKeys,
            geminiKeys = geminiApiKeys,
            ollamaKeys = ollamaApiKeys,
            openAiKeys = openAiApiKeys,
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
                openAiModel = openAiModel,
                openAiTextModel = openAiTextModel,
                aiAutoFailoverByProvider = mapOf(
                    AiProvider.OPENROUTER to aiAutoFailoverOpenRouter,
                    AiProvider.GEMINI to aiAutoFailoverGemini,
                    AiProvider.OLLAMA to aiAutoFailoverOllama,
                    AiProvider.OPENAI to aiAutoFailoverOpenAi,
                ),
                showPaidModelsByProvider = mapOf(
                    AiProvider.OPENROUTER to showPaidModelsOpenRouter,
                    AiProvider.GEMINI to showPaidModelsGemini,
                    AiProvider.OLLAMA to showPaidModelsOllama,
                    AiProvider.OPENAI to true, // OpenAI always treated as paid
                ),
                activeAiProvider = activeProvider,
                activePhotoModel = activePhotoModel,
                activeTextModel = activeTextModel,
                dynamicColor = dynamicColor,
                analyzingAnimationChoice = resolvedSlotChoice(analyzingAnimationChoice, analyzingSlot = true),
                insightAnimationChoice = resolvedSlotChoice(insightAnimationChoice, analyzingSlot = false),
                animationsEnabled = run {
                    val analyzing = resolvedSlotChoice(analyzingAnimationChoice, analyzingSlot = true)
                    val insight = resolvedSlotChoice(insightAnimationChoice, analyzingSlot = false)
                    analyzing != AppSettings.LOADING_ANIM_OFF ||
                        insight != AppSettings.LOADING_ANIM_OFF
                },
                // Never trust a restored value here: a backup made on a github build (or from
                // before the distribution flavor split) would otherwise silently re-enable the
                // GitHub auto-updater on an F-Droid install, which owns updates for that build.
                autoCheckUpdates = autoCheckUpdates && !BuildConfig.IS_FDROID,
                supportId = supportId,
                crashReportingEnabled = crashReportingEnabled,
                easterEggDiscovered = easterEggDiscovered,
                dailyLogReminderEnabled = dailyLogReminderEnabled,
                dailyLogReminderHour = dailyLogReminderHour.coerceIn(0, 23),
                dailyLogReminderMinute = dailyLogReminderMinute.coerceIn(0, 59),
                dayChangeHour = dayChangeHour.coerceIn(0, 23),
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
            openAiApiKeys = settings.keysFor(AiProvider.OPENAI),
            openAiModel = settings.openAiModel,
            openAiTextModel = settings.openAiTextModel,
            aiAutoFailoverOpenRouter = settings.autoFailoverFor(AiProvider.OPENROUTER),
            aiAutoFailoverGemini = settings.autoFailoverFor(AiProvider.GEMINI),
            aiAutoFailoverOllama = settings.autoFailoverFor(AiProvider.OLLAMA),
            aiAutoFailoverOpenAi = settings.autoFailoverFor(AiProvider.OPENAI),
            showPaidModelsOpenRouter = settings.showPaidFor(AiProvider.OPENROUTER),
            showPaidModelsGemini = settings.showPaidFor(AiProvider.GEMINI),
            showPaidModelsOllama = settings.showPaidFor(AiProvider.OLLAMA),
            showPaidModelsOpenAi = true, // OpenAI always treated as paid
            activeAiProvider = settings.activeAiProvider?.name,
            activePhotoModel = settings.activePhotoModel,
            activeTextModel = settings.activeTextModel,
            dynamicColor = settings.dynamicColor,
            analyzingAnimationChoice = settings.analyzingAnimationChoice,
            insightAnimationChoice = settings.insightAnimationChoice,
            loadingAnimationChoice = "",
            animationsEnabled = settings.animationsEnabled,
            autoCheckUpdates = settings.autoCheckUpdates,
            supportId = settings.supportId,
            crashReportingEnabled = settings.crashReportingEnabled,
            easterEggDiscovered = settings.easterEggDiscovered,
            dailyLogReminderEnabled = settings.dailyLogReminderEnabled,
            dailyLogReminderHour = settings.dailyLogReminderHour,
            dailyLogReminderMinute = settings.dailyLogReminderMinute,
            dayChangeHour = settings.dayChangeHour,
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
