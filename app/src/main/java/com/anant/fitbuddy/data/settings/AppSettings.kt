package com.anant.fitbuddy.data.settings

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
     * Last photo / text models Auto successfully used (with [activeAiProvider]).
     * Shown in Settings when Auto is on; synced into that provider's model fields so the
     * dropdowns update. Also reset to the preferred provider's models whenever Save AI
     * Settings runs. Empty until the first success for that modality (or a settings save).
     */
    val activeAiProvider: AiProvider? = null,
    val activePhotoModel: String = "",
    val activeTextModel: String = "",
    val dynamicColor: Boolean = true,
    /**
     * When true, FitBuddy checks GitHub Releases for a newer APK shortly after startup
     * (and still allows a manual check in Settings).
     */
    val autoCheckUpdates: Boolean = true,
    /**
     * Anonymous install id for crash support (Sentry user.id). Generated once, never PII.
     * Share this with the developer when reporting a bug.
     */
    val supportId: String = "",
    /** When false, Sentry does not send crash events (SDK may still be initialized). */
    val crashReportingEnabled: Boolean = true,
    /** Set when the Settings "Created by" easter egg is discovered — hides startup credit toast. */
    val easterEggDiscovered: Boolean = false
) {
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

    fun keysFor(p: AiProvider): List<String> = when (p) {
        AiProvider.OPENROUTER -> openRouterApiKeys.ifEmpty {
            listOfNotNull(openRouterApiKey.takeIf { it.isNotBlank() })
        }
        AiProvider.GEMINI -> geminiApiKeys.ifEmpty {
            listOfNotNull(geminiApiKey.takeIf { it.isNotBlank() })
        }
        AiProvider.OLLAMA -> ollamaApiKeys.ifEmpty {
            listOfNotNull(ollamaApiKey.takeIf { it.isNotBlank() })
        }
    }

    fun activeKey(p: AiProvider): String = when (p) {
        AiProvider.OPENROUTER -> openRouterApiKey.ifBlank { openRouterApiKeys.firstOrNull().orEmpty() }
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
        AiProvider.OPENROUTER -> keysFor(p).isNotEmpty() && openRouterModel.isNotBlank()
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
        const val DEFAULT_OPENROUTER_MODEL = "google/gemma-3-27b-it:free"
        const val DEFAULT_GEMINI_MODEL = "gemini-2.5-flash"
        const val DEFAULT_OLLAMA_URL = "http://192.168.1.10:11434"
        const val DEFAULT_OLLAMA_MODEL = "llava"
        const val OLLAMA_CLOUD_BASE_URL = "https://ollama.com"

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
