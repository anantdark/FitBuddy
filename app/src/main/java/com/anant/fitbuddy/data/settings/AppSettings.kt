package com.anant.fitbuddy.data.settings

/** Which LLM backend the app talks to. All use the OpenAI-compatible chat/completions API. */
enum class AiProvider {
    OPENROUTER,
    GEMINI,
    OLLAMA
}

/**
 * All user-configurable app settings (persisted in DataStore). Endpoint/model/auth are derived
 * from [provider] so the network layer stays provider-agnostic.
 */
data class AppSettings(
    val provider: AiProvider = AiProvider.OPENROUTER,
    val openRouterApiKey: String = "",
    val openRouterModel: String = DEFAULT_OPENROUTER_MODEL,
    val openRouterTextModel: String = "",
    val geminiApiKey: String = "",
    val geminiModel: String = DEFAULT_GEMINI_MODEL,
    val geminiTextModel: String = "",
    val ollamaBaseUrl: String = DEFAULT_OLLAMA_URL,
    val ollamaModel: String = DEFAULT_OLLAMA_MODEL,
    val ollamaTextModel: String = "",
    val dynamicColor: Boolean = true,
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

    /** Absolute chat-completions URL for the active provider (used with Retrofit @Url). */
    val chatUrl: String
        get() = when (provider) {
            AiProvider.OPENROUTER -> "https://openrouter.ai/api/v1/chat/completions"
            AiProvider.GEMINI -> "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
            AiProvider.OLLAMA -> ollamaBaseUrl.trim().trimEnd('/') + "/v1/chat/completions"
        }

    /** Authorization header value, or null when the provider needs none (local Ollama). */
    val authHeader: String?
        get() = when (provider) {
            AiProvider.OPENROUTER -> openRouterApiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
            AiProvider.GEMINI -> geminiApiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
            AiProvider.OLLAMA -> null
        }

    /** True when the active provider has enough config to attempt a live call. */
    val isConfigured: Boolean
        get() = when (provider) {
            AiProvider.OPENROUTER -> openRouterApiKey.isNotBlank() && openRouterModel.isNotBlank()
            AiProvider.GEMINI -> geminiApiKey.isNotBlank() && geminiModel.isNotBlank()
            AiProvider.OLLAMA -> ollamaBaseUrl.isNotBlank() && ollamaModel.isNotBlank()
        }

    companion object {
        const val DEFAULT_OPENROUTER_MODEL = "google/gemini-2.0-flash-001"
        const val DEFAULT_GEMINI_MODEL = "gemini-2.0-flash"
        const val DEFAULT_OLLAMA_URL = "http://192.168.1.10:11434"
        const val DEFAULT_OLLAMA_MODEL = "llava"
    }
}
