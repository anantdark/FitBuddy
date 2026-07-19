package com.anant.fitbuddy.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * OpenAI/OpenRouter-compatible "chat completions" request. The whole conversation is sent
 * as a single multimodal user message: a text part (system instructions + user-state context +
 * the user's loose log) plus an optional image part for food photos.
 *
 * Moshi omits null fields by default, so [ContentPart.text] / [ContentPart.imageUrl] simply
 * disappear from the wire when unused.
 */
@JsonClass(generateAdapter = true)
data class ChatRequest(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<ChatMessage>,
    @Json(name = "response_format") val responseFormat: ResponseFormat? = ResponseFormat(),
    @Json(name = "temperature") val temperature: Double = 0.2
)

@JsonClass(generateAdapter = true)
data class ChatMessage(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: List<ContentPart>
)

@JsonClass(generateAdapter = true)
data class ContentPart(
    @Json(name = "type") val type: String,
    @Json(name = "text") val text: String? = null,
    @Json(name = "image_url") val imageUrl: ImageUrl? = null
)

@JsonClass(generateAdapter = true)
data class ImageUrl(
    @Json(name = "url") val url: String
)

/** Forces the model to answer with a raw JSON object matching the FitBuddy schema. */
@JsonClass(generateAdapter = true)
data class ResponseFormat(
    @Json(name = "type") val type: String = "json_object"
)

/**
 * Fallback request shape for text-only completions, used only as a retry when a model's backend
 * rejects the standard OpenAI-style array `content` (see [ChatMessage]) with HTTP 400 — some
 * free/community-hosted, non-vision models only accept a plain string `content` for a message.
 */
@JsonClass(generateAdapter = true)
data class ChatRequestPlain(
    @Json(name = "model") val model: String,
    @Json(name = "messages") val messages: List<ChatMessagePlain>,
    @Json(name = "temperature") val temperature: Double = 0.2,
    /** Optional; used for cheap reachability probes (Refresh models). */
    @Json(name = "max_tokens") val maxTokens: Int? = null
)

@JsonClass(generateAdapter = true)
data class ChatMessagePlain(
    @Json(name = "role") val role: String,
    @Json(name = "content") val content: String
)

/**
 * [error] covers gateways (e.g. OpenRouter) that report an upstream/provider failure with
 * HTTP 200 and an `{"error": {...}}` body instead of an HTTP error status or a "choices" array.
 */
@JsonClass(generateAdapter = true)
data class ChatResponse(
    @Json(name = "choices") val choices: List<Choice> = emptyList(),
    @Json(name = "error") val error: ChatErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class ChatErrorDto(
    @Json(name = "message") val message: String? = null,
    @Json(name = "code") val code: Int? = null
)

/**
 * [error] covers providers that fail mid-generation for a single choice (e.g. an upstream
 * timeout) with HTTP 200 overall but a per-choice error and a null message content.
 */
@JsonClass(generateAdapter = true)
data class Choice(
    @Json(name = "message") val message: ResponseMessage?,
    @Json(name = "error") val error: ChatErrorDto? = null
)

@JsonClass(generateAdapter = true)
data class ResponseMessage(
    @Json(name = "role") val role: String?,
    @Json(name = "content") val content: String?
)
