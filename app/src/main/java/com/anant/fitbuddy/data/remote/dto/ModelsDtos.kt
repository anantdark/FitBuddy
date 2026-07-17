package com.anant.fitbuddy.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response of GET /models on OpenRouter (and OpenAI-compatible gateways). */
@JsonClass(generateAdapter = true)
data class ModelsResponse(
    @Json(name = "data") val data: List<ModelDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ModelDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String? = null,
    @Json(name = "architecture") val architecture: ArchitectureDto? = null,
    @Json(name = "pricing") val pricing: PricingDto? = null
) {
    /** Vision-capable if it accepts image input (new schema) or legacy modality mentions image. */
    val supportsVision: Boolean
        get() {
            val modalities = architecture?.inputModalities
            if (modalities != null) return modalities.any { it.equals("image", ignoreCase = true) }
            return architecture?.modality?.contains("image", ignoreCase = true) == true
        }

    /** Free if both prompt and completion token prices are zero. */
    val isFree: Boolean
        get() {
            val prompt = pricing?.prompt?.toDoubleOrNull() ?: return false
            val completion = pricing?.completion?.toDoubleOrNull() ?: return false
            return prompt == 0.0 && completion == 0.0
        }
}

@JsonClass(generateAdapter = true)
data class ArchitectureDto(
    @Json(name = "input_modalities") val inputModalities: List<String>? = null,
    @Json(name = "modality") val modality: String? = null
)

@JsonClass(generateAdapter = true)
data class PricingDto(
    @Json(name = "prompt") val prompt: String? = null,
    @Json(name = "completion") val completion: String? = null
)

/** Response of GET /v1beta/models on the Gemini (Generative Language) API. */
@JsonClass(generateAdapter = true)
data class GeminiModelsResponse(
    @Json(name = "models") val models: List<GeminiModelDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GeminiModelDto(
    @Json(name = "name") val name: String,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "supportedGenerationMethods") val supportedGenerationMethods: List<String>? = null
) {
    /** Bare model id used by the OpenAI-compat endpoint (strip the "models/" resource prefix). */
    val modelId: String get() = name.removePrefix("models/")

    /** Usable for chat: supports content generation (excludes embedding/token-only models). */
    val supportsGenerateContent: Boolean
        get() = supportedGenerationMethods?.any { it.equals("generateContent", ignoreCase = true) } == true

    /**
     * Vision-capable heuristic for meal analysis. The list endpoint exposes no modality flag,
     * but all current generateContent Gemini flash/pro models are multimodal input. Exclude:
     * - deprecated text-only Gemini 1.0 pro,
     * - image-generation / TTS / non-Gemini models (e.g. gemini-2.5-flash-image, imagen, veo,
     *   tts) which accept text but can't return the JSON analysis we need.
     */
    val supportsVision: Boolean
        get() {
            if (!supportsGenerateContent) return false
            val id = modelId.lowercase()
            if (!id.startsWith("gemini")) return false
            // Output-generation models, and non-chat classifiers, not vision-analysis chat models.
            val isNonAnalysis = listOf(
                "image", "imagen", "veo", "tts", "audio", "embedding",
                "safety", "guard", "shield"
            ).any { id.contains(it) }
            if (isNonAnalysis) return false
            // gemini-1.0-pro / gemini-pro (1.0) are text-only; the -vision variant is fine.
            val isLegacyTextOnly = (id == "gemini-pro" || id.startsWith("gemini-1.0")) &&
                !id.contains("vision")
            return !isLegacyTextOnly
        }
}
