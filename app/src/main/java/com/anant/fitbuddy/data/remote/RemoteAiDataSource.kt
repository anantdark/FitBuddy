package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.model.FitnessTrackerResponse
import com.anant.fitbuddy.data.model.ModelOption
import com.anant.fitbuddy.data.model.CustomExerciseResponse
import com.anant.fitbuddy.data.model.NorthIndianStaples
import com.anant.fitbuddy.data.model.ParsedWorkoutResponse
import com.anant.fitbuddy.data.model.ProgressChatTurn
import com.anant.fitbuddy.data.model.ProgressInsightResponse
import com.anant.fitbuddy.data.model.TargetPlanResponse
import com.anant.fitbuddy.data.model.WorkoutCaloriesResponse
import com.anant.fitbuddy.data.remote.dto.ChatErrorDto
import com.anant.fitbuddy.data.remote.dto.ChatMessage
import com.anant.fitbuddy.data.remote.dto.ChatMessagePlain
import com.anant.fitbuddy.data.remote.dto.ChatRequest
import com.anant.fitbuddy.data.remote.dto.ChatRequestPlain
import com.anant.fitbuddy.data.remote.dto.ChatResponse
import com.anant.fitbuddy.data.remote.dto.ContentPart
import com.anant.fitbuddy.data.remote.dto.ImageUrl
import com.anant.fitbuddy.data.remote.dto.ModelDto
import com.anant.fitbuddy.data.remote.dto.ResponseFormat
import com.anant.fitbuddy.data.remote.dto.gemmaFirstIntelligenceRank
import com.anant.fitbuddy.data.remote.dto.geminiIntelligenceRank
import com.anant.fitbuddy.data.settings.AppSettings
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
        val promptText = buildPrompt(
            userStateContextJson,
            userText,
            imageDataUrl != null,
            forceEstimate,
            strictClarification = settings.developerModeUnlocked && settings.strictClarification
        )
        val cleanJson = completeToJson(settings, promptText, imageDataUrl)
        return parseJson(responseAdapter, cleanJson)
    }

    /**
     * Recommends a goal + daily calorie/macro targets from the user's profile + body composition.
     * Text-only completion (no image), reusing the same retry/parse pipeline as [analyze].
     */
    suspend fun designTargets(
        settings: AppSettings,
        contextJson: String
    ): TargetPlanResponse {
        val json = completeToJson(settings, buildTargetPrompt(contextJson), null)
        return parseJson(targetPlanAdapter, json)
    }

    /** Summarises progress trends and returns actionable recommendations (text-only completion). */
    suspend fun summarizeProgress(
        settings: AppSettings,
        compressedMetrics: String
    ): ProgressInsightResponse {
        val json = completeToJson(settings, buildProgressPrompt(compressedMetrics), null)
        return parseJson(progressInsightAdapter, json)
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
            add(ChatMessagePlain(role = "system", content = buildProgressChatSystemPrompt(contextJson)))
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
        val json = completeToJson(settings, buildWorkoutCaloriesPrompt(contextJson), null)
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
        val json = completeToJson(settings, buildClassifyExercisePrompt(rawName, knownExerciseNames), null)
        return parseJson(customExerciseAdapter, json)
    }

    /** Parses a free-text workout description into structured exercises (sets/reps/weight). */
    suspend fun parseWorkoutDescription(
        settings: AppSettings,
        description: String,
        knownExerciseNames: List<String>
    ): ParsedWorkoutResponse {
        val json = completeToJson(settings, buildParseWorkoutPrompt(description, knownExerciseNames), null)
        return parseJson(parsedWorkoutAdapter, json)
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
        imageDataUrl: String?
    ): String {
        val hasImage = imageDataUrl != null
        val response = try {
            sendChat(settings, promptText, imageDataUrl, includeResponseFormat = true)
        } catch (e: AiBadRequestException) {
            if (hasImage) throw IllegalStateException(e.message, e.cause)
            runCatching { sendChat(settings, promptText, null, includeResponseFormat = false) }
                .getOrElse {
                    runCatching { sendChatPlain(settings, promptText) }
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
        includeResponseFormat: Boolean
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
            responseFormat = if (includeResponseFormat) ResponseFormat() else null
        )
        return chatWithRetry { api.chatCompletion(settings.chatUrl, settings.authHeader, request) }
    }

    private suspend fun sendChatPlain(settings: AppSettings, promptText: String): ChatResponse {
        val request = ChatRequestPlain(
            model = settings.modelFor(false),
            messages = listOf(ChatMessagePlain(role = "user", content = promptText))
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
        (response.error ?: response.choices.firstOrNull()?.error)?.let { throw upstreamError(it) }
        return response.choices.firstOrNull()?.message?.content?.trim()
            ?: throw IllegalStateException("Empty response from AI service")
    }

    private fun extractJson(response: ChatResponse): String {
        (response.error ?: response.choices.firstOrNull()?.error)?.let { err ->
            throw upstreamError(err)
        }
        val rawContent = response.choices.firstOrNull()?.message?.content
            ?: throw IllegalStateException("Empty response from AI service")

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
                    delay(backoffFor(e, attempt))
                    attempt++
                    continue
                }
                val friendly = friendlyHttpMessage(e)
                if (e.code() == 400) throw AiBadRequestException(friendly, e)
                throw IllegalStateException(friendly, e)
            } catch (e: IOException) {
                throw IllegalStateException("Network error: ${e.message ?: "check your connection"}", e)
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
     * or free+paid when [includePaid]), Gemma-first by generation then size. Auth is optional
     * for this endpoint (used if a key is present).
     */
    suspend fun fetchFreeVisionModels(
        apiKey: String,
        includePaid: Boolean = false
    ): List<ModelOption> {
        val auth = apiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
        val response = api.listModels(OPENROUTER_MODELS_URL, auth)
        return response.data
            .filter { (includePaid || it.isFree) && it.supportsVision && !isUnsuitableModel(it) }
            .map { ModelOption(id = it.id, displayName = it.name ?: it.id) }
            .sortedByDescending { gemmaFirstIntelligenceRank(it.id) }
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
     * [includePaid]), Gemma-first by generation then size. Used for the text-query dropdown.
     */
    suspend fun fetchFreeModels(
        apiKey: String,
        includePaid: Boolean = false
    ): List<ModelOption> {
        val auth = apiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
        val response = api.listModels(OPENROUTER_MODELS_URL, auth)
        return response.data
            .filter { (includePaid || it.isFree) && !isUnsuitableModel(it) }
            .map { ModelOption(id = it.id, displayName = it.name ?: it.id) }
            .sortedByDescending { gemmaFirstIntelligenceRank(it.id) }
    }

    /**
     * Gemini chat models for the text-query dropdown, ordered by estimated intelligence
     * (smartest first). Free Flash by default; Pro/Ultra included when [includePaid].
     * List API has no pricing field — see [GeminiModelDto.isFreeTier].
     */
    suspend fun fetchGeminiTextModels(
        apiKey: String,
        includePaid: Boolean = false
    ): List<ModelOption> =
        fetchGeminiModels(apiKey, includePaid)

    /**
     * Vision Gemini models for the photo dropdown / in-request model failover,
     * ordered by estimated intelligence (smartest first).
     */
    suspend fun fetchGeminiVisionModels(
        apiKey: String,
        includePaid: Boolean = false
    ): List<ModelOption> =
        fetchGeminiModels(apiKey, includePaid)

    private suspend fun fetchGeminiModels(
        apiKey: String,
        includePaid: Boolean
    ): List<ModelOption> {
        require(apiKey.isNotBlank()) { "A Gemini API key is required to list models" }
        val url = "$GEMINI_MODELS_URL?key=$apiKey&pageSize=200"
        val response = api.listGeminiModels(url)
        return response.models
            .filter { it.supportsVision && (includePaid || it.isFreeTier) }
            .map { ModelOption(id = it.modelId, displayName = it.displayName ?: it.modelId) }
            .sortedByDescending { geminiIntelligenceRank(it.id) }
    }

    /**
     * Lists models from an Ollama host (local/LAN or ollama.com Cloud) via OpenAI-compat
     * `GET /v1/models`. [apiKey] is required for Cloud; omit for local.
     */
    suspend fun fetchOllamaModels(baseUrl: String, apiKey: String = ""): List<ModelOption> {
        val base = baseUrl.trim().trimEnd('/')
        require(base.isNotBlank()) { "Ollama server URL is required to list models" }
        val auth = apiKey.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
        val response = api.listModels("$base/v1/models", auth)
        return response.data
            .map { ModelOption(id = it.id, displayName = it.name ?: it.id) }
            .sortedByDescending { gemmaFirstIntelligenceRank(it.id) }
    }

    /** Vision-capable Ollama models (name heuristic — `/v1/models` has no modality flag). */
    suspend fun fetchOllamaVisionModels(baseUrl: String, apiKey: String = ""): List<ModelOption> =
        fetchOllamaModels(baseUrl, apiKey).filter { isLikelyOllamaVisionModel(it.id) }

    /** Text/chat Ollama models (full catalog from the host). */
    suspend fun fetchOllamaTextModels(baseUrl: String, apiKey: String = ""): List<ModelOption> =
        fetchOllamaModels(baseUrl, apiKey)

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

    /** Some models wrap JSON in ```json ... ``` fences; strip them before parsing. */
    private fun stripCodeFences(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```")) {
            text = text.removePrefix("```json").removePrefix("```").trim()
            text = text.removeSuffix("```").trim()
        }
        return text
    }

    private fun buildPrompt(
        userStateContextJson: String,
        userText: String,
        hasImage: Boolean,
        forceEstimate: Boolean,
        strictClarification: Boolean = false
    ): String = """
        You are FitBuddy, a nutrition and fitness analysis engine optimised for North Indian
        (Hindi belt / Punjabi / Delhi-NCR / UP / Haryana / Rajasthan) home and street food.
        Analyse the user's input and respond with a SINGLE JSON object and nothing else. Do not
        include markdown fences or commentary.

        Decide the "status":
        - "SUCCESS": the input clearly shows/describes food or a meal and you can confidently
          estimate macros.
        - "EXERCISE_LOGGED": the input describes a physical activity/workout.
        - "CLARIFICATION_REQUIRED": it IS food/exercise but too vague (missing quantity, portion,
          or which item) to estimate accurately. Ask a short, specific question in
          "clarification_message".
        - "NOT_IDENTIFIED": there is NO identifiable food or physical activity. Use this when an
          attached image contains no food (e.g. a person, object, scenery, screenshot, document),
          or the text is unrelated to food/exercise. Put a short reason in "clarification_message"
          (e.g. "No food detected in the image."). Do NOT guess or invent a dish in this case.

        North Indian food priors (apply when identifying dishes from photos or Hinglish text):
        - Default to North Indian names unless clear South/West/East markers are present
          (idli, dosa, sambar, coconut chutney, medu vada, appam, fish curry Kerala-style,
          misal, dhokla, momos with clear Tibetan plating, etc.).
        - Flatbreads: chapati / phulka / roti, tawa/stuffed paratha (aloo, gobi, paneer, mooli),
          laccha paratha, naan, kulcha, bhatura — NOT dosa/uttapam unless those are obvious.
        - Dals & curries: dal tadka/fry, dal makhani, rajma, chole/chana masala, kadhi pakora,
          paneer butter masala / palak paneer / kadhai paneer, butter chicken, egg bhurji,
          keema — prefer these over generic "curry" or sambar when cues match.
        - Sabzi / sides: aloo gobi, bhindi, baingan bharta, mixed veg, raita, salad, pickle, papad.
        - Staples & combos users type loosely: "roti sabzi", "dal chawal", "2 parantha",
          "chole bhature", "rajma chawal", "paneer bhurji", "aloo paratha with curd".
        - Street / snack: samosa, pakora, aloo tikki, golgappe/pani puri, chaat, pav bhaji,
          chole kulche, jalebi, lassi, chai.
        - Cooking fats: when the dish is tadka, fried, buttery, or makhani and oil/ghee is not
          stated, include "ghee" or "oil" as its OWN ingredient with a typical home amount
          (often ~1 tsp / 5 g). Do not invent coconut oil unless the dish implies it.
        - Naming: use familiar North Indian dish names in "dish_name" (Hinglish ok). Break
          thalis and combos into named components (roti, dal, sabzi, rice, raita, bhatura)
          rather than "mixed plate" — especially chole bhature, rajma chawal, dal chawal.

        ${NorthIndianStaples.promptReferenceTable()}

        Clarification for countable staples (text only, no clear photo portions):
        - If the user mentions roti/paratha/naan/bhatura/samosa/eggs without a count
          (e.g. "roti sabzi", "paratha with curd"), return CLARIFICATION_REQUIRED asking how many,
          unless ${if (hasImage) "the attached photo makes portion size obvious" else "a count is stated"}.
        ${if (strictClarification) """
        STRICT CLARIFICATION MODE (on): when any portion is ambiguous, prefer
        CLARIFICATION_REQUIRED over guessing. Ask one short, specific question.
        """.trimIndent() else ""}

        Accuracy rules (important):
        - Only report food you actually see in the image / that is explicitly described. Never
          default to a generic "mixed plate" or invent items to fill the schema.
        - If unsure whether the image contains food, prefer "NOT_IDENTIFIED" over guessing.
        - Base portion sizes on visible cues; keep macros internally consistent.
        - Still recognise non-North Indian food correctly when clearly present — priors only
          break ties and guide ambiguous Indian plates.

        For food, break the dish into its component ingredients. For EACH ingredient, estimate its
        assumed weight in grams and its macros AT THAT WEIGHT. The dish-level "macros" MUST equal
        the sum of the ingredient macros.

        Ingredient quantity rules (critical for loose text):
        - When the user states a count of discrete items ("4 almonds", "2 rotis", "3 eggs",
          "do roti", "teen paratha"), set "quantity" to that count and "weight_g" to the TOTAL
          grams for all units combined (not grams per unit). Example: "4 almonds" -> quantity 4,
          weight_g ~5-6, macros for all 4 almonds combined.
        - For bulk or continuous portions (rice, dal, sabzi, milk, curd), use quantity 1 and
          weight_g as the portion weight in grams. "ek katori" / "1 bowl" → one katori/bowl prior.
        - Never treat a leading count in the user's text as grams (e.g. "4 almonds" must NOT become
          1 almond at 4 g).

        Respond using EXACTLY this schema. Set "food_analysis" to null OUTRIGHT (not an object with
        null fields inside) when status is not SUCCESS, and "exercise_analysis" to null OUTRIGHT
        when status is not EXERCISE_LOGGED:
        {
          "status": "SUCCESS | CLARIFICATION_REQUIRED | EXERCISE_LOGGED | NOT_IDENTIFIED",
          "clarification_message": "String text or null",
          "food_analysis": {
            "dish_name": "String",
            "macros": { "calories": Int, "protein_g": Int, "carbs_g": Int, "fats_g": Int },
            "ingredients": [
              { "name": "String", "quantity": Int, "weight_g": Int, "calories": Int, "protein_g": Int, "carbs_g": Int, "fats_g": Int }
            ]
          },
          "exercise_analysis": {
            "activity_detected": "String",
            "calories_burned": Int,
            "duration_minutes": Int
          }
        }

        Use the user's current state only to personalise exercise burn (body weight, age, sex)
        and remaining-calorie awareness. Do NOT inflate or deflate food-macro estimates to match
        daily targets — report realistic dish macros from what is seen/described. Current user
        state (JSON):
        $userStateContextJson

        ${if (hasImage) "An image is attached. First check whether it actually contains food; if it does not, return NOT_IDENTIFIED." else ""}

        ${if (forceEstimate) "The user is correcting a previously analysed meal by renaming the dish. Treat the input as the definitive dish name and ALWAYS return status \"SUCCESS\" with your best-effort estimate for a standard single serving, recomputing the full ingredient breakdown and macros. Do NOT return CLARIFICATION_REQUIRED or NOT_IDENTIFIED for this correction." else ""}

        User input: "$userText"
    """.trimIndent()

    private fun buildTargetPrompt(contextJson: String): String = """
        You are FitBuddy, a nutrition and body-composition coach optimised for North Indian
        diets and lifestyles. Using the user's profile and latest body-composition data below,
        decide the single most appropriate goal and design a daily nutrition plan for it.

        Choose "recommended_goal" as EXACTLY one of:
        - "LOSE_WEIGHT": high body fat / cutting is the priority.
        - "GAIN_MUSCLE": lean already or clearly under-muscled; a lean bulk is best.
        - "RECOMP": simultaneously lose fat and gain muscle (typical for moderate body fat).
        If the user's profile specifies a goal other than "AUTO", respect it unless the data makes it
        clearly unsafe, and design the plan for that goal.

        IMPORTANT — how this app applies "daily_target_calories": the app tracks a NET calorie
        balance for each day, computed as (calories eaten) MINUS (calories burned via logged
        exercise). The user's remaining allowance = daily_target_calories - net calories, which
        means every exercise session automatically credits its estimated burn back onto that day's
        eating allowance (a 1:1 "eat back your exercise calories" model). Design
        "daily_target_calories" as the appropriate REST-DAY baseline intake for the goal — do NOT
        inflate it further to pre-account for exercise, since the app already adds that back
        automatically per session. Because exercise-calorie estimates are frequently overestimated,
        keep the baseline honestly aligned with the goal (do not be overly generous) so that
        crediting exercise back can't silently erase a fat-loss deficit or blow past a lean-bulk
        surplus; mention this "eat-back" mechanic briefly in the rationale if the user logs
        exercise often, per "avg_daily_calories_burned_recent" in the data below.

        Personalise from the ACTUAL fields present in the JSON — never invent missing demographics
        or scale metrics. Required anchors when available: age, sex, height_cm, current_weight_kg,
        activity_level, stated_goal. When "latest_measurement" is present, prefer its fields over
        guesses: bmr, bmi, body_fat_pct, muscle_mass_kg / skeletal_muscle_mass_kg /
        fat_free_mass_kg, fat_mass_kg, visceral_fat, metabolic_age, etc. Example: an older
        FEMALE with LOSE_WEIGHT needs a lower calorie budget and about 1 g/kg protein — do NOT
        assign young-male bodybuilding macros just because "protein is good".

        Design realistic daily targets:
        - Calories: sex-specific BMR (prefer measured bmr from latest_measurement; else
          Mifflin–St Jeor using age, sex, height_cm, current_weight_kg). Scale by activity_level
          to a rest-day TDEE. Then adjust for the goal with modest, sustainable deltas:
          LOSE_WEIGHT ~300–500 kcal below TDEE (smaller deficit for older adults / lower BMR /
          smaller women; floor ~1200 kcal women / ~1500 kcal men unless the person is notably
          small); RECOMP near maintenance; GAIN_MUSCLE ~200–300 kcal above TDEE. Prefer
          conservative numbers. This is the REST-DAY baseline, not exercise-inclusive.
        - Protein (g per kg CURRENT bodyweight) — match goal; modulate with age/sex/body comp;
          never default to bodybuilding-high protein:
            · LOSE_WEIGHT: ~1.0 g/kg. Fine for muscle preservation in general fat loss. Older
              adults and many women do not need more than this for weight loss; do not push
              1.5–2 g/kg on a cut unless stated_goal is RECOMP/GAIN_MUSCLE.
            · RECOMP or GAIN_MUSCLE: 1.5–2.0 g/kg (high protein). Use the LOWER end when heavier,
              older, female, calorie budget is tight, or body_fat_pct is high; UPPER end only when
              lean, actively strength-training, and younger/mid-adult. Cap at ~2.0 g/kg — never
              invent 2.2+ g/kg.
          If muscle_mass_kg or fat_free_mass_kg is logged and protein-at-bodyweight looks
          extreme for a high-BF person on LOSE_WEIGHT, stay near 1.0 g/kg bodyweight (do not
          secretly switch to 2 g/kg "per lean mass" for fat-loss goals).
        - Fats ~0.8–1.0 g/kg or ~20–30% of calories (do not starve fats for older women). Fill
          remaining calories with carbs. Macros must sum roughly to the calorie target
          (protein/carbs 4 kcal/g, fats 9 kcal/g). Keep splits practical for North Indian meals
          (roti/dal/sabzi, dal-chawal, paratha, paneer/chole/rajma — not Western templates).
          Round to whole grams.
        - "rationale": 2–4 sentences citing age, sex, weight/height, goal, and any used
          body-comp metrics (bf%, muscle, BMR/BMI), plus the protein g/kg you applied.

        Respond with a SINGLE JSON object and nothing else (no markdown fences, no commentary),
        EXACTLY this schema:
        {
          "recommended_goal": "LOSE_WEIGHT | GAIN_MUSCLE | RECOMP",
          "daily_target_calories": Int,
          "target_protein_g": Int,
          "target_carbs_g": Int,
          "target_fats_g": Int,
          "rationale": "String"
        }

        User data (JSON):
        $contextJson
    """.trimIndent()

    private fun buildProgressPrompt(compressedMetrics: String): String = """
        You are FitBuddy, a supportive but honest fitness coach optimised for North Indian
        diets and lifestyles. Analyse the user's progress data below (body-composition trend over
        time, calories consumed vs. burned, macro adherence, and exercise) and produce a concise
        report. When suggesting food swaps, prefer familiar North Indian options
        (dal, roti, sabzi, dahi, paneer, chole, rajma) over unfamiliar Western substitutes.

        The data is a COMPRESSED snapshot (oldest→newest):
        - Header may include age, sex, height_cm, weight_kg, activity, goal, and macro targets
        - BODY30: past-month day-level scale readings (date|kg|bf%|muscle_kg|visceral|bmr|bmi)
        - BODY_PRIOR: older months as concise averages (month|n|avg_kg|start/end weights + end comps)
        - NUT30: past-month daily nutrition (date|calories_in|calories_burned|net|protein|carbs|fats)
        - NUT_PRIOR: older months as averages (month|days|avg_in|avg_burn|avg_net|macros|ex_days)
        - EX30 / EX_PRIOR: past-month daily burn vs older month averages/totals
        - "targets" kcal is a REST-DAY baseline compared against NET calories (in - burn) each day.

        IMPORTANT — this app credits exercise calories back onto the day's eating allowance
        (NET calories = consumed - burned is compared against the rest-day target). On exercise
        days, check whether intake rose to match burn. If the user regularly eats back most/all of
        their exercise calories while the weight trend isn't matching their stated goal (e.g. still
        gaining despite a LOSE_WEIGHT goal), call this out explicitly — exercise-burn estimates
        usually run high, so recommend eating back only part of it rather than the full amount.
        Don't assume this is happening if the data doesn't support it.

        Focus on: direction of weight/body-fat/muscle trends, whether intake and training align with
        their goal, and 2-4 specific, actionable recommendations grounded in THEIR age, sex, and
        body-comp data when present. Be encouraging and realistic; do NOT invent data that isn't
        present. If there is too little data, say so and suggest logging more consistently.
        Protein advice: ~1 g/kg bodyweight is enough for LOSE_WEIGHT / maintenance (especially
        older adults and many women — do not tell them to "stuff protein"); reserve 1.5–2 g/kg
        only for RECOMP or GAIN_MUSCLE. Never push 2+ g/kg as a default.

        Also compute "body_score": an integer 0-100 holistic body-composition score (higher is
        better) reflecting how well the user's CURRENT body composition and its recent trend align
        with their stated goal. Weigh body fat %, muscle mass, visceral fat, and trend direction
        relative to age/sex norms when those fields exist. Set "body_score" to null (not a guess)
        if BODY30 / body_measurements has fewer than 2 readings or lacks enough fields to judge.

        Respond with a SINGLE JSON object and nothing else (no markdown fences, no commentary),
        EXACTLY this schema:
        {
          "summary": "String (2-4 sentences)",
          "recommendations": ["String", "String"],
          "body_score": Int or null
        }

        User progress data (compressed):
        $compressedMetrics
    """.trimIndent()

    private fun buildProgressChatSystemPrompt(contextJson: String): String = """
        You are FitBuddy, a supportive but honest fitness coach optimised for North Indian
        diets and lifestyles. The user is on the Progress screen reviewing charts (weekly +
        monthly calories, macros, exercise burn, and body-composition trends). Food advice should
        default to North Indian staples (roti, dal, sabzi, dahi, rice, paneer) when relevant.

        Below is the FULL progress dataset (JSON). Treat it as authoritative — do not invent numbers
        not present here. Use profile fields (age, sex, height_cm, weight_kg, activity_level, goal)
        plus day-level "body_measurements", "nutrition_daily", and "exercise_daily" for the past
        ~30 days, and concise calendar-month averages in "body_prior_months",
        "nutrition_prior_months", and "exercise_prior_months" for older history. When scale metrics
        exist (bmi, body_fat_pct, muscle_mass_kg, bmr, visceral_fat, etc.), use them to personalise
        advice.

        --- DATA START ---
        $contextJson
        --- DATA END ---

        App calorie model (critical):
        - "target_calories_rest_day_baseline" is compared against NET calories each day.
        - NET = calories eaten minus calories burned via logged exercise.
        - Exercise automatically credits back onto that day's eating allowance (1:1 eat-back).
        - On exercise days, check whether intake rose to match burn; exercise estimates often run
          high, so eating back all of it can stall fat loss.

        Macro guidance if asked: personalise to age, sex, goal, and body composition. ~1 g/kg
        protein is enough for fat loss / maintenance (do not push high protein on older women
        cutting weight); 1.5–2 g/kg only for recomp / muscle-gain goals. Never default to 2+ g/kg.

        You already opened the conversation with an initial progress insight (in chat history).
        Continue naturally: answer follow-ups about trends, graphs, body composition, macros,
        training, and goal alignment. Reference specific dates/values from the data when useful.
        Keep replies concise (2-5 sentences) unless the user asks for detail. Be encouraging and
        realistic. If data is sparse, say so and suggest consistent logging.
    """.trimIndent()

    private fun buildWorkoutCaloriesPrompt(contextJson: String): String = """
        You are FitBuddy, an exercise-physiology estimator. Estimate the energy expenditure
        of the logged workout session below, personalised to the user's body factors (weight_kg,
        age, sex, activity_level) using standard MET (metabolic equivalent) methodology. Prefer
        measured fields from the JSON when present; do not invent missing demographics.

        Guidance:
        - Use each exercise's equipment/type, sets, reps and weight to judge intensity (heavier
          load and lower reps generally means more MET for the working sets, but rest between sets
          lowers the SESSION-average MET compared to continuous cardio).
        - Resistance-training sessions (dumbbell/barbell/bench/machine) typically average
          MET 3-6 across the whole session once rest is included; bodyweight circuits and cardio
          machines can run higher.
        - "duration_minutes": if the user already provided one in the data, use it unless it's
          clearly unrealistic for the number of exercises/sets logged; otherwise estimate a
          realistic total including rest between sets.
        - "calories_burned": kcal = MET * weight_kg * (duration_minutes / 60), personalised to the
          provided body weight (heavier users burn more for the same activity). Sex/age mainly
          inform intensity judgement when the session description is ambiguous — do not invent
          a different formula.
        - "intensity_note": ONE short phrase (e.g. "Moderate strength session").
        - Be conservative: exercise calorie estimates often run high; prefer the lower plausible
          MET when uncertain.

        Respond with a SINGLE JSON object and nothing else (no markdown fences, no commentary),
        EXACTLY this schema:
        {
          "calories_burned": Int,
          "duration_minutes": Int,
          "intensity_note": "String"
        }

        Workout + user data (JSON):
        $contextJson
    """.trimIndent()

    private fun buildClassifyExercisePrompt(rawName: String, knownExerciseNames: List<String>): String {
        val knownList = knownExerciseNames.joinToString("\n") { "- $it" }
        return """
            You are FitBuddy, a gym-exercise normaliser. The user typed a custom exercise
            name. Map it to a clean canonical exercise name and the best equipment tag.

            Rules:
            - If the typed name clearly matches (or is a synonym/abbreviation of) an exercise in
              the known list below, use that EXACT canonical name from the list.
            - Otherwise invent a concise Title Case name (no equipment prefix unless it disambiguates).
            - Do NOT create near-duplicates of the known list (e.g. "Dumbbell Bench Press" when
              "Bench Press" already exists — use "Bench Press").
            - "equipment" must be EXACTLY one of:
              Dumbbell, Bench, Barbell, Bodyweight, Machine, Cardio, Other

            Known exercises:
            $knownList

            Respond with a SINGLE JSON object and nothing else (no markdown fences, no commentary),
            EXACTLY this schema:
            {
              "canonical_name": "String",
              "equipment": "Dumbbell | Bench | Barbell | Bodyweight | Machine | Cardio | Other"
            }

            User typed: "$rawName"
        """.trimIndent()
    }

    private fun buildParseWorkoutPrompt(description: String, knownExerciseNames: List<String>): String {
        val knownList = knownExerciseNames.joinToString("\n") { "- $it" }
        return """
            You are FitBuddy, a workout-log parser. The user describes one or more exercises
            in natural language (possibly including sets, reps, and weight). Extract EVERY exercise
            mentioned and return them as structured rows.

            Parsing rules:
            - Accept formats like "4x8 bench press", "3 sets of 12 lateral raises", "goblet squat
              4×10 @ 20kg", "30 min run 5 km", bullet lists, or comma-separated items.
            - Map each exercise to the closest name from the known list below when possible (use the
              EXACT spelling from that list). Otherwise use a concise Title Case name.
            - For strength/resistance: default to 3 sets × 10 reps when sets/reps aren't specified.
            - For cardio (run, jog, bike, row, etc.): use equipment "Cardio", set sets/reps to 1,
              populate "duration_minutes" and "distance_km" when the user mentions time/distance.
            - "weight_kg": only when the user gives a load in kg (or lb converted to kg); else null.
            - "equipment" must be EXACTLY one of:
              Dumbbell, Bench, Barbell, Bodyweight, Machine, Cardio, Other
            - Do NOT duplicate the same exercise twice unless the user explicitly logged it twice.

            Known exercises:
            $knownList

            Respond with a SINGLE JSON object and nothing else (no markdown fences, no commentary),
            EXACTLY this schema:
            {
              "exercises": [
                {
                  "name": "String",
                  "equipment": "Dumbbell | Bench | Barbell | Bodyweight | Machine | Cardio | Other",
                  "sets": Int,
                  "reps": Int,
                  "weight_kg": Double or null,
                  "duration_minutes": Int or null,
                  "distance_km": Double or null
                }
              ]
            }

            User description:
            $description
        """.trimIndent()
    }
}
