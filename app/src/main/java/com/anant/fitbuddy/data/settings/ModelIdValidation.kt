package com.anant.fitbuddy.data.settings

/**
 * Whether [modelId] is a plausible id for [provider]. Prevents cross-contamination
 * (e.g. Gemini Studio ids like `gemini-3-flash-preview` stored as OpenRouter selections).
 *
 * Ollama (local + Cloud) accepts bare names including Cloud Gemini tags
 * (`gemini-3-flash-preview`, `…:cloud`) — those are valid on ollama.com, not Studio.
 */
fun isPlausibleModelIdFor(provider: AiProvider, modelId: String): Boolean {
    val id = modelId.trim()
    if (id.isEmpty()) return false
    val lower = id.lowercase().removePrefix("models/")
    return when (provider) {
        AiProvider.GEMINI -> lower.startsWith("gemini")
        AiProvider.OPENROUTER -> {
            // OpenRouter ids are org/model (optionally :free). Bare Gemini Studio ids are not.
            if (lower.startsWith("gemini-") || lower == "gemini") return false
            true
        }
        AiProvider.OLLAMA -> true
    }
}

/** Returns [modelId] if valid for [provider], otherwise [fallback]. */
fun sanitizeModelIdFor(
    provider: AiProvider,
    modelId: String?,
    fallback: String
): String {
    val id = modelId?.trim().orEmpty()
    return if (isPlausibleModelIdFor(provider, id)) id else fallback
}
