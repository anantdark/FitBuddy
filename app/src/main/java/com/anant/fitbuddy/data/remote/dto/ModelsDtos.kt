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

/**
 * Higher = smarter. Free-tier ordering aligned with Google AI Studio capability tiers:
 * 3.5 Flash → 3 Flash (preview) → 2.5 Flash / flash-latest →
 * 3.1 Flash-Lite → 2.5 Flash-Lite / flash-lite-latest →
 * 2.0 Flash → 2.0 Flash-Lite.
 * Stable preferred over preview within the same family.
 */
fun geminiIntelligenceRank(modelId: String): Int {
    val id = modelId.lowercase().removePrefix("models/")
    val lite = isGeminiFlashLite(id)
    val preview = "preview" in id || "exp" in id

    val base = when {
        // Group 2 — modern Flash
        geminiHasVersion(id, major = 3, minor = 5) && !lite -> 400
        geminiHasVersion(id, major = 3, minor = null) && !lite &&
            !geminiHasVersion(id, major = 3, minor = 1) &&
            !geminiHasVersion(id, major = 3, minor = 5) -> 390
        isGeminiFlashLatestAlias(id) || (geminiHasVersion(id, major = 2, minor = 5) && !lite) -> 370

        // Group 3 — Flash-Lite
        geminiHasVersion(id, major = 3, minor = 1) && lite -> 340
        isGeminiFlashLiteLatestAlias(id) || (geminiHasVersion(id, major = 2, minor = 5) && lite) -> 320

        // Group 4 — 2.0 legacy
        geminiHasVersion(id, major = 2, minor = 0) && !lite -> 280
        geminiHasVersion(id, major = 2, minor = 0) && lite -> 260

        // Older free flash if still listed
        geminiHasVersion(id, major = 1, minor = 5) && !lite -> 200
        geminiHasVersion(id, major = 1, minor = 5) && lite -> 180

        lite -> 140
        "flash" in id -> 160
        else -> 50
    }
    return base + if (preview) -5 else 0
}

private fun isGeminiFlashLite(id: String): Boolean =
    "flash-lite" in id || "flashlite" in id || "flash_lite" in id

/** Alias that points at the current stable 2.5 Flash family. */
private fun isGeminiFlashLatestAlias(id: String): Boolean =
    id == "gemini-flash-latest" || id.endsWith("/flash-latest") ||
        (id.contains("flash-latest") && !isGeminiFlashLite(id))

/** Alias that points at the current stable 2.5 Flash-Lite family. */
private fun isGeminiFlashLiteLatestAlias(id: String): Boolean =
    id == "gemini-flash-lite-latest" || id.contains("flash-lite-latest") ||
        id.contains("flashlite-latest")

/**
 * True when [id] encodes Gemini version [major].[minor] (or [major] alone when [minor] is null).
 * `gemini-3-flash` matches major=3 minor=null; `gemini-3.5-flash` matches major=3 minor=5.
 */
private fun geminiHasVersion(id: String, major: Int, minor: Int?): Boolean {
    if (minor != null) {
        return Regex("""(?:^|[^0-9])$major\.$minor(?:[^0-9]|$)""").containsMatchIn(id)
    }
    // Bare major (e.g. gemini-3-flash) — not 3.1 / 3.5
    return Regex("""(?:^|[^0-9])$major(?:-|_|\.|$)""").containsMatchIn(id) &&
        !Regex("""(?:^|[^0-9])$major\.\d""").containsMatchIn(id)
}

/**
 * Higher = smarter for OpenRouter / Ollama catalogs. Gemma family is boosted to the top;
 * within a family, larger parameter sizes rank higher (27b > 12b > 4b > 2b).
 */
fun gemmaFirstIntelligenceRank(modelId: String): Int {
    val id = modelId.lowercase()
    val gemmaBoost = if ("gemma" in id) 1_000 else 0
    val sizeB = Regex("""(\d+(?:\.\d+)?)\s*b\b""").find(id)
        ?.groupValues?.get(1)
        ?.toDoubleOrNull()
        ?: 0.0
    return gemmaBoost + (sizeB * 10).toInt()
}
