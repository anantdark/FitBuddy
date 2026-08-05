package com.anant.fitbuddy.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/** Response of GET /models on OpenRouter (and OpenAI-compatible gateways). */
@JsonClass(generateAdapter = true)
data class ModelsResponse(
    @Json(name = "data") val data: List<ModelDto> = emptyList(),
    /**
     * llama.cpp (and Ollama's native listing) return a parallel `models` array alongside the
     * OpenAI-compat `data` array. Unlike `data`, it carries capability flags — the only reliable
     * modality signal these self-hosted servers expose (the `data` entries have none).
     */
    @Json(name = "models") val models: List<NativeModelDto> = emptyList()
) {
    /** Model ids the host advertises as multimodal/vision (empty when it reports no capabilities). */
    val visionCapableIds: Set<String>
        get() = models
            .filter { it.isMultimodal }
            .flatMap { listOfNotNull(it.model, it.name) }
            .toSet()
}

/** Native (non-OpenAI) model entry from llama.cpp / Ollama, used only for its capability flags. */
@JsonClass(generateAdapter = true)
data class NativeModelDto(
    @Json(name = "name") val name: String? = null,
    @Json(name = "model") val model: String? = null,
    @Json(name = "capabilities") val capabilities: List<String>? = null
) {
    /** True when the server tags this model as image-capable. */
    val isMultimodal: Boolean
        get() = capabilities?.any {
            it.equals("multimodal", ignoreCase = true) || it.equals("vision", ignoreCase = true)
        } == true
}

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
     * but current generateContent Gemini flash models accept image input. Exclude:
     * - deprecated text-only Gemini 1.0 pro,
     * - image-generation / TTS / video / agent / robotics models which can't return the JSON
     *   analysis we need (e.g. flash-image, imagen, veo, omni, computer-use, robotics-ER).
     */
    val supportsVision: Boolean
        get() {
            if (!supportsGenerateContent) return false
            val id = modelId.lowercase()
            if (!id.startsWith("gemini")) return false
            val isNonAnalysis = listOf(
                "image", "imagen", "veo", "tts", "audio", "embedding",
                "safety", "guard", "shield",
                "omni", "computer-use", "computer_use", "robotics"
            ).any { id.contains(it) }
            if (isNonAnalysis) return false
            val isLegacyTextOnly = (id == "gemini-pro" || id.startsWith("gemini-1.0")) &&
                !id.contains("vision")
            return !isLegacyTextOnly
        }

    /**
     * Free-tier heuristic (Gemini list API has no pricing field). Matches Google AI Studio
     * policy: Pro / Ultra / computer-use / robotics are paid-only; Flash and Flash-Lite
     * (including `-latest` aliases and free previews) are free. Niche omni models are free
     * during preview but unsuitable for meal JSON — excluded via [supportsVision].
     */
    val isFreeTier: Boolean
        get() {
            val id = modelId.lowercase()
            if ("pro" in id || "ultra" in id) return false
            if ("computer-use" in id || "computer_use" in id || "robotics" in id) return false
            return "flash" in id
        }
}

enum class ModelCatalogModality { PHOTO, TEXT }

