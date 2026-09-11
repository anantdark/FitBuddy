package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.model.FitnessTrackerResponse
import com.anant.fitbuddy.data.model.ModelOption
import com.anant.fitbuddy.data.model.OpenAiCatalog
import com.anant.fitbuddy.data.model.CustomExerciseResponse
import com.anant.fitbuddy.data.model.ParsedWorkoutResponse
import com.anant.fitbuddy.data.model.ProgressChatTurn
import com.anant.fitbuddy.data.model.ProgressInsightResponse
import com.anant.fitbuddy.data.model.TargetPlanResponse
import com.anant.fitbuddy.data.model.WorkoutCaloriesResponse
import com.anant.fitbuddy.data.model.WorkoutNameResponse
import com.anant.fitbuddy.data.model.normalized
import com.anant.fitbuddy.data.prompts.PromptCatalog
import com.anant.fitbuddy.data.region.AppRegion
import com.anant.fitbuddy.data.region.RegionPack
import com.anant.fitbuddy.data.region.RegionPacks
import com.anant.fitbuddy.data.remote.dto.ChatErrorDto
import com.anant.fitbuddy.data.remote.dto.ChatMessage
import com.anant.fitbuddy.data.remote.dto.ChatMessagePlain
import com.anant.fitbuddy.data.remote.dto.ChatRequest
import com.anant.fitbuddy.data.remote.dto.ChatRequestPlain
import com.anant.fitbuddy.data.remote.dto.ChatResponse
import com.anant.fitbuddy.data.remote.dto.ContentPart
import com.anant.fitbuddy.data.remote.dto.ImageUrl
import com.anant.fitbuddy.data.remote.dto.ModelCatalogModality
import com.anant.fitbuddy.data.remote.dto.ModelDto
import com.anant.fitbuddy.data.remote.dto.ResponseFormat
import com.anant.fitbuddy.data.remote.dto.ResponseMessage
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.util.DiagnosticLogger
import com.anant.fitbuddy.data.settings.FailoverLadders
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.Moshi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.Buffer
import retrofit2.HttpException
import java.io.IOException

/**
 * Talks to the multimodal LLM. Responsible for prompt assembly, image attachment and
 * parsing the assistant's raw JSON string into a strongly-typed [FitnessTrackerResponse].
 */
class RemoteAiDataSource(
    private val api: AiApi,
    moshi: Moshi
) {
    private val responseAdapter = moshi.adapter(FitnessTrackerResponse::class.java)
    private val targetPlanAdapter = moshi.adapter(TargetPlanResponse::class.java)
    private val progressInsightAdapter = moshi.adapter(ProgressInsightResponse::class.java)
    private val workoutCaloriesAdapter = moshi.adapter(WorkoutCaloriesResponse::class.java)
    private val customExerciseAdapter = moshi.adapter(CustomExerciseResponse::class.java)
    private val parsedWorkoutAdapter = moshi.adapter(ParsedWorkoutResponse::class.java)
    private val workoutNameAdapter = moshi.adapter(WorkoutNameResponse::class.java)

    /** Last assistant JSON string from [completeToJson] (developer tooling). */
    @Volatile
    var lastRawJson: String? = null
        private set

    private companion object {
        const val OPENROUTER_MODELS_URL = "https://openrouter.ai/api/v1/models"
        const val GEMINI_MODELS_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val MAX_RETRIES = 2
        const val BASE_BACKOFF_MS = 1_500L
        const val MAX_BACKOFF_MS = 8_000L
        /** Parallel chat probes when filtering the Refresh-models list. */
        const val MODEL_PROBE_CONCURRENCY = 6
    }

    suspend fun analyze(
        settings: AppSettings,
        userText: String,
        userStateContextJson: String,
        imageDataUrl: String?,
        forceEstimate: Boolean = false
    ): FitnessTrackerResponse {
        val pack = regionPack(settings)
        val promptText = PromptCatalog.analyzePrompt(
            userStateContextJson = userStateContextJson,
            userText = userText,
            hasImage = imageDataUrl != null,
            forceEstimate = forceEstimate,
            strictClarification = settings.developerModeUnlocked && settings.strictClarification,
            pack = pack
        )
        val cleanJson = completeToJson(settings, promptText, imageDataUrl)
        return parseJson(responseAdapter, cleanJson)
    }

    /**
     * Optionally asks AI to explain the authoritative on-device target plan. Numeric output from
     * the model is ignored by the repository.
     */
    suspend fun designTargets(
        settings: AppSettings,
        contextJson: String
    ): TargetPlanResponse {
        // temperature=0: target math should be deterministic across identical inputs
        val json = completeToJson(
            settings,
            PromptCatalog.targetPrompt(contextJson, regionPack(settings)),
            imageDataUrl = null,
            temperature = 0.0
        )
        return parseJson(targetPlanAdapter, json)
    }

    /** Summarises progress trends and returns actionable recommendations (text-only completion). */
    suspend fun summarizeProgress(
        settings: AppSettings,
        compressedMetrics: String
    ): ProgressInsightResponse {
        val json = completeToJson(
            settings,
            PromptCatalog.progressPrompt(compressedMetrics, regionPack(settings)),
            null
        )
        return parseJson(progressInsightAdapter, json).normalized()
    }

    /**
     * Multi-turn progress coach chat. [contextJson] is the full progress payload injected once
     * as the system prompt; [history] is the visible conversation (seeded with the initial insight).
     */
    suspend fun chatProgressCoach(
        settings: AppSettings,
        contextJson: String,
        history: List<ProgressChatTurn>
    ): String {
        val messages = buildList {
            add(
                ChatMessagePlain(
                    role = "system",
                    content = PromptCatalog.progressChatSystemPrompt(
                        contextJson,
                        regionPack(settings)
                    )
                )
            )
            history.forEach { add(ChatMessagePlain(role = it.role, content = it.content)) }
        }
        val request = ChatRequestPlain(
            model = settings.textModel,
            messages = messages,
            temperature = 0.5
        )
        return extractPlainContent(
            chatWithRetry { api.chatCompletionPlain(settings.chatUrl, settings.authHeader, request) }
        )
    }

    /**
     * Estimates calories burned for a logged workout session (exercises + sets/reps/weight)
     * personalised to the user's body factors. Text-only completion.
     */
    suspend fun estimateWorkoutCalories(
        settings: AppSettings,
        contextJson: String
    ): WorkoutCaloriesResponse {
        val json = completeToJson(settings, PromptCatalog.workoutCaloriesPrompt(contextJson), null)
        return parseJson(workoutCaloriesAdapter, json)
    }

    /**
     * Normalises a user-typed exercise name to a canonical label + equipment tag for the picker.
     * Text-only completion.
     */
    suspend fun classifyExercise(
        settings: AppSettings,
        rawName: String,
        knownExerciseNames: List<String>
    ): CustomExerciseResponse {
        val json = completeToJson(
            settings,
            PromptCatalog.classifyExercisePrompt(rawName, knownExerciseNames),
            null
        )
        return parseJson(customExerciseAdapter, json)
    }

    /** Parses a free-text workout description into structured exercises (sets/reps/weight). */
    suspend fun parseWorkoutDescription(
        settings: AppSettings,
        description: String,
        knownExerciseNames: List<String>
    ): ParsedWorkoutResponse {
        val json = completeToJson(
            settings,
            PromptCatalog.parseWorkoutPrompt(description, knownExerciseNames),
            null
        )
        return parseJson(parsedWorkoutAdapter, json)
    }

    /** Suggests a short session name based on the exercises in the workout. */
    suspend fun suggestWorkoutName(
        settings: AppSettings,
        exerciseNames: List<String>
    ): WorkoutNameResponse {
        val json = completeToJson(settings, PromptCatalog.workoutNamePrompt(exerciseNames), null)
        return parseJson(workoutNameAdapter, json)
    }

    /**
     * Sends a single multimodal user message and returns the assistant's raw JSON string (code
     * fences stripped). Shared by every JSON-producing call (meal analysis, target design,
     * progress insight).
     *
     * Some free/community-hosted models reject the request outright with HTTP 400 — either
     * because that backend doesn't support the "response_format" param, or (for non-vision
     * models) because it doesn't accept the OpenAI-style array `content` shape for a plain-text
     * message. For text-only calls (no image), a 400 triggers up to two narrower retries before
     * giving up: first without `response_format`, then with plain-string `content` as well.
     */
    private suspend fun completeToJson(
        settings: AppSettings,
        promptText: String,
        imageDataUrl: String?,
        temperature: Double = 0.2
    ): String {
        val hasImage = imageDataUrl != null
        val response = try {
            sendChat(
                settings,
                promptText,
                imageDataUrl,
                includeResponseFormat = true,
                temperature = temperature
            )
        } catch (e: AiBadRequestException) {
            if (hasImage) throw IllegalStateException(e.message, e.cause)
            runCatching {
                sendChat(
                    settings,
                    promptText,
                    null,
                    includeResponseFormat = false,
                    temperature = temperature
                )
            }
                .getOrElse {
                    runCatching { sendChatPlain(settings, promptText, temperature) }
                        .getOrElse { throw IllegalStateException(e.message, e.cause) }
                }
        }
        val json = extractJson(response)
        lastRawJson = json
        return json
    }

    private suspend fun sendChat(
        settings: AppSettings,
        promptText: String,
        imageDataUrl: String?,
        includeResponseFormat: Boolean,
        temperature: Double = 0.2
    ): ChatResponse {
        val hasImage = imageDataUrl != null
        val contentParts = buildList {
            add(ContentPart(type = "text", text = promptText))
            if (imageDataUrl != null) {
                add(ContentPart(type = "image_url", imageUrl = ImageUrl(imageDataUrl)))
            }
        }
        val request = ChatRequest(
            model = settings.modelFor(hasImage),
            messages = listOf(ChatMessage(role = "user", content = contentParts)),
            responseFormat = if (includeResponseFormat) ResponseFormat() else null,
            temperature = temperature,
            enableThinking = disableThinkingForModel(settings.modelFor(hasImage))
        )
        return chatWithRetry { api.chatCompletion(settings.chatUrl, settings.authHeader, request) }
    }

    private suspend fun sendChatPlain(
        settings: AppSettings,
        promptText: String,
        temperature: Double = 0.2
    ): ChatResponse {
        val request = ChatRequestPlain(
            model = settings.modelFor(false),
            messages = listOf(ChatMessagePlain(role = "user", content = promptText)),
            temperature = temperature,
            enableThinking = disableThinkingForModel(settings.modelFor(false))
        )
        return chatWithRetry { api.chatCompletionPlain(settings.chatUrl, settings.authHeader, request) }
    }

    /**
     * Some gateways (e.g. OpenRouter) report upstream/provider failures with HTTP 200 and an
     * "error" body instead of an HTTP error status — either response-level, or per-choice (e.g.
     * a reasoning model that burns its time budget and gets cut off mid-generation, leaving
     * message.content null).
     */
    private fun extractPlainContent(response: ChatResponse): String {
        gatewayError(response)?.let { throw it }
        return effectiveMessageContent(response.choices.firstOrNull()?.message)?.trim()
            ?: throw emptyResponseError()
    }

    private fun extractJson(response: ChatResponse): String {
        gatewayError(response)?.let { throw it }
        val rawContent = effectiveMessageContent(response.choices.firstOrNull()?.message)
            ?: throw emptyResponseError()

        val cleanJson = stripCodeFences(rawContent)
        if (!looksLikeJson(cleanJson)) {
            // Some OpenRouter model ids are routers/aliases (e.g. "openrouter/free") that load
            // balance across ANY free model on the platform, including non-chat ones (safety
            // classifiers, text-only models) that never return our JSON schema.
            throw IllegalStateException(
                "The selected model replied with plain text instead of JSON, so it can't be used " +
                    "here. If you're using a router alias like \"openrouter/free\", " +
                    "pick a specific model from the dropdown in Settings instead."
            )
        }
        return cleanJson
    }

    private fun looksLikeJson(text: String): Boolean =
        text.trim().let { it.startsWith("{") || it.startsWith("[") }

    /** Marks an HTTP 400 specifically, so [completeToJson] knows it's worth a narrower retry. */
    private class AiBadRequestException(message: String, cause: Throwable) : Exception(message, cause)

    private fun gatewayError(response: ChatResponse): IllegalStateException? {
        (response.error ?: response.choices.firstOrNull()?.error)?.let { return upstreamError(it) }
        if (response.choices.isEmpty() && !response.message.isNullOrBlank()) {
            return IllegalStateException(
                "AI provider error: ${response.message.trim()}. Check your API key, model id, and base URL in Settings."
            )
        }
        return null
    }

    /** Prefer normal assistant text; fall back to Qwen/DeepSeek `reasoning_content` when `content` is blank. */
    private fun effectiveMessageContent(message: ResponseMessage?): String? {
        if (message == null) return null
        return message.content?.takeIf { it.isNotBlank() }
            ?: message.reasoningContent?.takeIf { it.isNotBlank() }
    }

    private fun emptyResponseError(): IllegalStateException = IllegalStateException(
        "Empty response from AI service. If you're using a Qwen or DeepSeek thinking model on a " +
            "custom gateway, try a non-thinking chat model or ask your host to disable thinking " +
            "for non-streaming requests."
    )

    /**
     * Thinking models on OpenAI-compatible gateways often return an empty `content` for
     * FitBuddy's non-streaming JSON calls unless thinking is disabled.
     */
    private fun disableThinkingForModel(modelId: String): Boolean? {
        if (!isLikelyThinkingModel(modelId)) return null
        return false
    }

    private fun isLikelyThinkingModel(modelId: String): Boolean {
        val id = modelId.lowercase()
        return id.startsWith("qwen3") ||
            id.contains("deepseek") ||
            id.contains("-thinking") ||
            id.contains("reasoner")
    }

    private fun upstreamError(err: ChatErrorDto): IllegalStateException {
        val code = err.code?.let { " (HTTP $it)" } ?: ""
        return IllegalStateException(
            "AI provider error$code: ${err.message ?: "unknown error"}. Try again or pick a different model in Settings."
        )
    }

    /**
     * Strict parse first; models occasionally emit near-JSON (trailing commas, stray tokens) that
     * Moshi rejects strictly, so fall back to a lenient [JsonReader] before giving up. Any failure
     * is rethrown as a friendly, user-facing message instead of the raw Moshi exception text.
     */
    private fun <T> parseJson(adapter: JsonAdapter<T>, json: String): T {
        runCatching { adapter.fromJson(json) }
            .getOrNull()
            ?.let { return it }

        return try {
            val reader = JsonReader.of(Buffer().writeUtf8(json)).apply { isLenient = true }
            adapter.fromJson(reader) ?: throw errorParsing()
        } catch (e: JsonEncodingException) {
            throw errorParsing(e)
        } catch (e: JsonDataException) {
            throw errorParsing(e)
        } catch (e: IOException) {
            throw errorParsing(e)
        }
    }

    private fun errorParsing(cause: Throwable? = null) = IllegalStateException(
        "The AI returned a response that couldn't be understood. Please try again.", cause
    )

    /**
     * Runs [call], retrying transient failures with (Retry-After-aware) backoff — both thrown
     * HTTP 429s and gateways that report a transient upstream error (e.g. HTTP 500) inside a 200
     * response body — and rethrowing hard failures as clean, user-facing messages. HTTP 400 is
     * raised as [AiBadRequestException] so callers can decide whether a narrower retry applies.
     */
    private suspend fun chatWithRetry(call: suspend () -> ChatResponse): ChatResponse {
        var attempt = 0
        while (true) {
            try {
                val response = call()
                val errorCode = response.error?.code ?: response.choices.firstOrNull()?.error?.code
                if (errorCode != null && errorCode >= 500 && attempt < MAX_RETRIES) {
                    delay(BASE_BACKOFF_MS * (1L shl attempt))
                    attempt++
                    continue
                }
                return response
            } catch (e: HttpException) {
                if (e.code() == 429 && attempt < MAX_RETRIES) {
                    DiagnosticLogger.log(
                        "http",
                        "retry_429",
                        mapOf("attempt" to attempt.toString(), "code" to "429")
                    )
                    delay(backoffFor(e, attempt))
                    attempt++
                    continue
                }
                val friendly = friendlyHttpMessage(e)
                DiagnosticLogger.log(
                    "http",
                    "error",
                    mapOf(
                        "code" to e.code().toString(),
                        "message" to friendly
                    )
                )
                if (e.code() == 400) throw AiBadRequestException(friendly, e)
                throw IllegalStateException(friendly, e)
            } catch (e: IOException) {
                val msg = "Network error: ${e.message ?: "check your connection"}"
                DiagnosticLogger.log(
                    "http",
                    "network_error",
                    mapOf("message" to msg, "cause" to e.javaClass.simpleName)
                )
                throw IllegalStateException(msg, e)
            }
        }
    }

    /** Prefer the server's Retry-After header (seconds); else exponential backoff, capped. */
    private fun backoffFor(e: HttpException, attempt: Int): Long {
        val retryAfterSec = e.response()?.headers()?.get("Retry-After")?.toLongOrNull()
        val fromHeader = retryAfterSec?.times(1000)
        val exponential = BASE_BACKOFF_MS * (1L shl attempt)
        return (fromHeader ?: exponential).coerceAtMost(MAX_BACKOFF_MS)
    }

    private fun friendlyHttpMessage(e: HttpException): String = when (e.code()) {
        429 -> "Rate limited (HTTP 429). The model is busy or you've hit the free-tier quota. " +
            "Wait a minute and retry, or pick a different model in Settings."
        401, 403 -> "Authentication failed (HTTP ${e.code()}). Check your API key in Settings."
        404 -> "Model not found (HTTP 404). Check the model id in Settings."
        400 -> {
            val detail = extractErrorDetail(e)
            "The model rejected the request (HTTP 400)${detail?.let { ": $it" } ?: ""}. " +
                "Some free models don't support this app's request format — if this persists, " +
                "pick a different model in Settings."
        }
        else -> "AI request failed (HTTP ${e.code()}). ${e.message()}"
    }

    /** Best-effort extraction of a human-readable message from the error response body. */
    private fun extractErrorDetail(e: HttpException): String? = runCatching {
        val body = e.response()?.errorBody()?.string() ?: return null
        Regex("\"message\"\\s*:\\s*\"([^\"]{1,200})\"").find(body)?.groupValues?.get(1)
            ?: body.take(200)
    }.getOrNull()?.takeIf { it.isNotBlank() }

    /**
     * Fetches the OpenRouter model catalog and returns vision-capable models (free by default,
     * or free+paid when [includePaid]), ordered by [FailoverLadders] then A–Z.
     */
    suspend fun fetchFreeVisionModels(
        apiKey: String,
        includePaid: Boolean = false
    ): List<ModelOption> {
        val auth = apiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
        val response = api.listModels(OPENROUTER_MODELS_URL, auth)
        val mapped = response.data
            .filter { (includePaid || it.isFree) && it.supportsVision && !isUnsuitableModel(it) }
            .map { ModelOption(id = it.id, displayName = it.name ?: it.id) }
        return orderByFailoverLadder(AiProvider.OPENROUTER, ModelCatalogModality.PHOTO, mapped)
    }

    /**
     * Excludes models that can't be used for our chat-JSON meal analysis:
     * - OpenRouter router/meta aliases (e.g. "openrouter/auto", "openrouter/free") that load
     *   balance across ANY backend model, including unsuitable ones.
     * - Content-safety / moderation / guard classifiers (e.g. "nvidia/nemotron-3.5-content-safety",
     *   "meta-llama/llama-guard-4-12b", "google/shieldgemma-*") which reply with a verdict label,
     *   not the requested JSON schema.
     * - Embedding/rerank models, which aren't chat-completion models at all.
     * - Media-generation models (music/video/image/speech) that report an "image" input modality
     *   (so they slip past the vision filter, e.g. "google/lyria-3-clip-preview" — a music
     *   generator, not a vision-chat model) or would otherwise appear in the free-text list.
     * Checked against both id and display name since not every provider encodes it the same way.
     */
    private fun isUnsuitableModel(model: ModelDto): Boolean {
        val id = model.id.lowercase()
        if (id.startsWith("openrouter/")) return true
        val haystack = "$id ${model.name?.lowercase().orEmpty()}"
        val badTokens = listOf(
            "safety", "moderation", "moderate", "guard", "shield", "toxic", "nsfw",
            "jailbreak", "rerank", "embed",
            "lyria", "veo", "imagen", "suno", "tts", "whisper", "dall-e", "stable-diffusion"
        )
        return badTokens.any { haystack.contains(it) }
    }

    /**
     * Fetches the OpenRouter catalog and returns chat models (free by default, or free+paid when
     * [includePaid]), ordered by [FailoverLadders] then A–Z.
     */
    suspend fun fetchFreeModels(
        apiKey: String,
        includePaid: Boolean = false
    ): List<ModelOption> {
        val auth = apiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
        val response = api.listModels(OPENROUTER_MODELS_URL, auth)
        val mapped = response.data
            .filter { (includePaid || it.isFree) && !isUnsuitableModel(it) }
            .map { ModelOption(id = it.id, displayName = it.name ?: it.id) }
        return orderByFailoverLadder(AiProvider.OPENROUTER, ModelCatalogModality.TEXT, mapped)
    }

    /**
     * Gemini chat models for the text-query dropdown.
     * Free Flash tier by default; all vision-capable models when [includePaid].
     * Ordered by [FailoverLadders]. List API has no pricing field — see [GeminiModelDto.isFreeTier].
     */
    suspend fun fetchGeminiTextModels(
        apiKey: String,
        includePaid: Boolean = false
    ): List<ModelOption> =
        fetchGeminiModels(apiKey, includePaid, ModelCatalogModality.TEXT)

    /**
     * Vision Gemini models for the photo dropdown / in-request model failover.
     * Ordered by [FailoverLadders].
     */
    suspend fun fetchGeminiVisionModels(
        apiKey: String,
        includePaid: Boolean = false
    ): List<ModelOption> =
        fetchGeminiModels(apiKey, includePaid, ModelCatalogModality.PHOTO)

    private suspend fun fetchGeminiModels(
        apiKey: String,
        includePaid: Boolean,
        modality: ModelCatalogModality
    ): List<ModelOption> {
        require(apiKey.isNotBlank()) { "A Gemini API key is required to list models" }
        val url = "$GEMINI_MODELS_URL?key=$apiKey&pageSize=200"
        val response = api.listGeminiModels(url)
        val mapped = response.models
            .filter { it.supportsVision && (includePaid || it.isFreeTier) }
            .map { ModelOption(id = it.modelId, displayName = it.displayName ?: it.modelId) }
        return orderByFailoverLadder(AiProvider.GEMINI, modality, mapped)
    }

    /**
     * Lists models from an Ollama host (local/LAN or ollama.com Cloud) via OpenAI-compat
     * `GET /v1/models`. [apiKey] is required for Cloud; omit for local.
     */
    suspend fun fetchOllamaModels(
        baseUrl: String,
        apiKey: String = "",
        ladderProvider: AiProvider = AiProvider.OLLAMA,
    ): List<ModelOption> {
        val base = AppSettings.normalizeOpenAiCompatBaseUrl(baseUrl)
        require(base.isNotBlank()) { "Server URL is required to list models" }
        val auth = apiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
        val response = api.listModels("$base/v1/models", auth)
        val mapped = response.data
            .map { ModelOption(id = it.id, displayName = it.name ?: it.id) }
        return orderByFailoverLadder(ladderProvider, ModelCatalogModality.TEXT, mapped)
    }

    /**
     * Vision-capable models from an Ollama/llama.cpp host. Prefers the server-reported
     * `capabilities` (llama.cpp tags multimodal models in the `/v1/models` `models` array);
     * falls back to a name heuristic for hosts that expose no capability flags.
     */
    suspend fun fetchOllamaVisionModels(
        baseUrl: String,
        apiKey: String = "",
        ladderProvider: AiProvider = AiProvider.OLLAMA,
    ): List<ModelOption> {
        val base = AppSettings.normalizeOpenAiCompatBaseUrl(baseUrl)
        require(base.isNotBlank()) { "Server URL is required to list models" }
        val auth = apiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
        val response = api.listModels("$base/v1/models", auth)
        val visionIds = response.visionCapableIds
        val mapped = response.data
            .filter { it.id in visionIds || isLikelyOllamaVisionModel(it.id) }
            .map { ModelOption(id = it.id, displayName = it.name ?: it.id) }
        return orderByFailoverLadder(ladderProvider, ModelCatalogModality.PHOTO, mapped)
    }

    /** Text/chat models from an OpenAI-compatible host (full catalog). */
    suspend fun fetchOllamaTextModels(
        baseUrl: String,
        apiKey: String = "",
        ladderProvider: AiProvider = AiProvider.OLLAMA,
    ): List<ModelOption> =
        fetchOllamaModels(baseUrl, apiKey, ladderProvider)

    /**
     * Vision-capable models from the official OpenAI API. The account's live `/v1/models`
     * list is filtered to vision-capable chat models and merged with a curated default set
     * so the dropdown always offers at least GPT-4o even if the listing is empty/unreachable.
     */
    suspend fun fetchOpenAiVisionModels(apiKey: String): List<ModelOption> {
        val live = runCatching { fetchOllamaModels(OpenAiCatalog.HOST_URL, apiKey) }
            .getOrDefault(emptyList())
            .filter { isLikelyOpenAiVisionModel(it.id) }
        return orderByFailoverLadder(
            AiProvider.OPENAI,
            ModelCatalogModality.PHOTO,
            mergeModels(OpenAiCatalog.VISION_MODELS, live)
        )
    }

    /**
     * Text/chat models from the official OpenAI API. The noisy live catalog (embeddings,
     * audio, image, tts, moderation) is filtered to chat models and merged with a curated
     * default set.
     */
    suspend fun fetchOpenAiTextModels(apiKey: String): List<ModelOption> {
        val live = runCatching { fetchOllamaModels(OpenAiCatalog.HOST_URL, apiKey) }
            .getOrDefault(emptyList())
            .filter { isLikelyOpenAiChatModel(it.id) }
        return orderByFailoverLadder(
            AiProvider.OPENAI,
            ModelCatalogModality.TEXT,
            mergeModels(OpenAiCatalog.TEXT_MODELS, live)
        )
    }

    /**
     * Keeps models whose chat endpoint answers HTTP 200 or 429 (reachable / rate-limited).
     * Other statuses and network failures are dropped. Used by the Settings Refresh button;
     * preserves [models] order. Probes run with limited parallelism.
     */
    suspend fun filterReachableModels(
        models: List<ModelOption>,
        chatUrl: String,
        authHeader: String?
    ): List<ModelOption> {
        if (models.isEmpty()) return models
        val semaphore = Semaphore(MODEL_PROBE_CONCURRENCY)
        return coroutineScope {
            models.map { option ->
                async {
                    semaphore.withPermit {
                        if (isModelReachable(chatUrl, authHeader, option.id)) option else null
                    }
                }
            }.mapNotNull { it.await() }
        }
    }

    /** True when a minimal chat completion returns HTTP 200 or 429. */
    private suspend fun isModelReachable(
        chatUrl: String,
        authHeader: String?,
        modelId: String
    ): Boolean {
        val request = ChatRequestPlain(
            model = modelId,
            messages = listOf(ChatMessagePlain(role = "user", content = "ping")),
            temperature = 0.0,
            maxTokens = 1
        )
        return try {
            val response = api.probeChatCompletion(chatUrl, authHeader, request)
            try {
                val code = response.code()
                code == 200 || code == 429
            } finally {
                response.body()?.close()
                response.errorBody()?.close()
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Ollama's list endpoint does not expose input modalities. Match common multimodal model
     * name tokens used for meal-photo analysis.
     */
    private fun isLikelyOllamaVisionModel(modelId: String): Boolean {
        val id = modelId.lowercase()
        val tokens = listOf(
            "llava", "bakllava", "vision", "moondream", "minicpm-v", "minicpm_v",
            "qwen2-vl", "qwen2.5-vl", "qwen2_5_vl", "qwen3-vl", "qwen3.5-vl",
            "llama3.2-vision", "llama-3.2-vision", "gemma3", "gemma4", "gemini"
        )
        return tokens.any { id.contains(it) }
    }

    /** Non-chat OpenAI model families to hide from the dropdowns. */
    private fun isNonChatOpenAiModel(id: String): Boolean {
        val m = id.lowercase()
        return listOf(
            "audio", "realtime", "transcribe", "tts", "embedding",
            "whisper", "moderation", "image", "dall-e", "search", "codex"
        ).any { m.contains(it) }
    }

    /** Vision-capable OpenAI chat models (GPT-4o / 4.1 / 4-turbo / 4-vision / o-series). */
    private fun isLikelyOpenAiVisionModel(id: String): Boolean {
        if (isNonChatOpenAiModel(id)) return false
        val m = id.lowercase()
        return m.startsWith("gpt-4o") || m.startsWith("chatgpt-4o") ||
            m.startsWith("gpt-4.1") || m.startsWith("gpt-4-turbo") ||
            m.startsWith("gpt-4-vision") || m.startsWith("gpt-5") ||
            m.startsWith("o1") || m.startsWith("o3") || m.startsWith("o4")
    }

    /** Any OpenAI text/chat model (superset of vision) for the text-model dropdown. */
    private fun isLikelyOpenAiChatModel(id: String): Boolean {
        if (isNonChatOpenAiModel(id)) return false
        val m = id.lowercase()
        return m.startsWith("gpt-") || m.startsWith("chatgpt") ||
            m.startsWith("o1") || m.startsWith("o3") || m.startsWith("o4")
    }

    /** [preferred] first (curated display names win), then any [extra] not already present. */
    private fun mergeModels(
        preferred: List<ModelOption>,
        extra: List<ModelOption>
    ): List<ModelOption> = (preferred + extra).distinctBy { it.id }

    /** Ladder head first, then remaining catalog ids A–Z — never intelligence-rank. */
    private fun orderByFailoverLadder(
        provider: AiProvider,
        modality: ModelCatalogModality,
        models: List<ModelOption>
    ): List<ModelOption> {
        if (models.isEmpty()) return models
        val byId = models.associateBy { it.id }
        return FailoverLadders.orderCatalog(provider, modality, models.map { it.id })
            .mapNotNull { byId[it] }
    }

    /** Some models wrap JSON in ```json ... ``` fences; strip them before parsing. */
    private fun stripCodeFences(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.removePrefix("```json").removePrefix("```").trim()
            text = text.removeSuffix("```").trim()
        }
        return text
    }

    private fun regionPack(settings: AppSettings): RegionPack =
        RegionPacks.packOrIndia(AppRegion.fromStored(settings.region))
}
