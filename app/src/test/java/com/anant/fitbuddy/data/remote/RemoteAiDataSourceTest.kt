package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.remote.dto.ArchitectureDto
import com.anant.fitbuddy.data.remote.dto.ChatErrorDto
import com.anant.fitbuddy.data.remote.dto.ChatRequest
import com.anant.fitbuddy.data.remote.dto.ChatRequestPlain
import com.anant.fitbuddy.data.remote.dto.ChatResponse
import com.anant.fitbuddy.data.remote.dto.Choice
import com.anant.fitbuddy.data.remote.dto.GeminiModelDto
import com.anant.fitbuddy.data.remote.dto.GeminiModelsResponse
import com.anant.fitbuddy.data.remote.dto.ModelDto
import com.anant.fitbuddy.data.remote.dto.ModelsResponse
import com.anant.fitbuddy.data.remote.dto.PricingDto
import com.anant.fitbuddy.data.remote.dto.ResponseMessage
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Records every call and serves canned [ChatResponse]s in order; no network involved. */
private class FakeAiApi(
    private val responses: MutableList<ChatResponse> = mutableListOf(),
    private val modelsResponse: ModelsResponse = ModelsResponse(),
    private val geminiModelsResponse: GeminiModelsResponse = GeminiModelsResponse()
) : AiApi {
    var callCount = 0
        private set

    override suspend fun chatCompletion(url: String, authorization: String?, request: ChatRequest): ChatResponse {
        callCount++
        check(responses.isNotEmpty()) { "no fake response queued for call #$callCount" }
        return responses.removeAt(0)
    }

    override suspend fun chatCompletionPlain(url: String, authorization: String?, request: ChatRequestPlain): ChatResponse {
        callCount++
        check(responses.isNotEmpty()) { "no fake response queued for call #$callCount" }
        return responses.removeAt(0)
    }

    override suspend fun listModels(url: String, authorization: String?) = modelsResponse

    override suspend fun listGeminiModels(url: String) = geminiModelsResponse
}

/**
 * Regression coverage for the flaky-free-model bugs hit in manual testing: HTTP-200-with-error
 * bodies (top-level and per-choice), non-JSON replies from router aliases / non-chat models, and
 * the dropdown filters that should keep those models out in the first place.
 */
class RemoteAiDataSourceTest {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val settings = AppSettings(provider = AiProvider.OPENROUTER, openRouterApiKey = "k", openRouterModel = "m")

    private fun chatResponse(
        content: String?,
        choiceError: ChatErrorDto? = null,
        topLevelError: ChatErrorDto? = null
    ) = ChatResponse(
        choices = listOf(Choice(message = ResponseMessage(role = "assistant", content = content), error = choiceError)),
        error = topLevelError
    )

    @Test
    fun `analyze parses valid JSON response`() = runTest {
        val json = """{"status":"SUCCESS","clarification_message":null,"food_analysis":{"dish_name":"Idli","macros":{"calories":100,"protein_g":2,"carbs_g":20,"fats_g":1},"ingredients":null},"exercise_analysis":null}"""
        val api = FakeAiApi(mutableListOf(chatResponse(json)))
        val source = RemoteAiDataSource(api, moshi)

        val result = source.analyze(settings, "idli", "{}", null)

        assertEquals("SUCCESS", result.status)
        assertEquals("Idli", result.foodAnalysis?.dishName)
    }

    @Test
    fun `analyze throws when no choices and no error present`() = runTest {
        val api = FakeAiApi(mutableListOf(ChatResponse(choices = emptyList())))
        val source = RemoteAiDataSource(api, moshi)

        val error = runCatching { source.analyze(settings, "idli", "{}", null) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertEquals("Empty response from AI service", error!!.message)
    }

    @Test
    fun `analyze surfaces non-retryable per-choice upstream error immediately`() = runTest {
        val api = FakeAiApi(mutableListOf(
            chatResponse(content = null, choiceError = ChatErrorDto(message = "content policy violation", code = 400))
        ))
        val source = RemoteAiDataSource(api, moshi)

        val error = runCatching { source.analyze(settings, "idli", "{}", null) }.exceptionOrNull()

        assertEquals(1, api.callCount)
        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message!!.contains("400"))
        assertTrue(error.message!!.contains("content policy violation"))
    }

    @Test
    fun `analyze retries transient per-choice error then surfaces it after exhausting retries`() = runTest {
        // Reproduces the gpt-oss-20b bug: reasoning model burns its time budget, provider cuts it
        // off (per-choice error, HTTP 502), message.content stays null.
        val timeout = ChatErrorDto(message = "request exceeded 120s deadline", code = 502)
        val api = FakeAiApi(mutableListOf(
            chatResponse(content = null, choiceError = timeout),
            chatResponse(content = null, choiceError = timeout),
            chatResponse(content = null, choiceError = timeout)
        ))
        val source = RemoteAiDataSource(api, moshi)

        val error = runCatching { source.analyze(settings, "idli", "{}", null) }.exceptionOrNull()

        assertEquals(3, api.callCount) // initial attempt + 2 retries
        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message!!.contains("502"))
        assertTrue(error.message!!.contains("120s deadline"))
    }

    @Test
    fun `analyze recovers when a retry succeeds after a top-level 200-body error`() = runTest {
        // Reproduces OpenRouter reporting a provider failure with HTTP 200 + {"error":{...}}.
        val json = """{"status":"SUCCESS","clarification_message":null,"food_analysis":{"dish_name":"Idli","macros":{"calories":100,"protein_g":2,"carbs_g":20,"fats_g":1},"ingredients":null},"exercise_analysis":null}"""
        val api = FakeAiApi(mutableListOf(
            chatResponse(content = null, topLevelError = ChatErrorDto(message = "Internal Server Error", code = 500)),
            chatResponse(content = json)
        ))
        val source = RemoteAiDataSource(api, moshi)

        val result = source.analyze(settings, "idli", "{}", null)

        assertEquals(2, api.callCount)
        assertEquals("SUCCESS", result.status)
    }

    @Test
    fun `analyze rejects plain-text content from router alias or non-chat models`() = runTest {
        // Reproduces "openrouter/free" load-balancing onto a content-safety classifier that
        // replies with a plain-text verdict ("User Safety: safe") instead of JSON.
        val api = FakeAiApi(mutableListOf(chatResponse(content = "User Safety: safe")))
        val source = RemoteAiDataSource(api, moshi)

        val error = runCatching { source.analyze(settings, "idli", "{}", null) }.exceptionOrNull()

        assertTrue(error is IllegalStateException)
        assertTrue(error!!.message!!.contains("plain text"))
    }

    @Test
    fun `fetchFreeVisionModels excludes router aliases, guard and content-safety classifiers`() = runTest {
        fun freeVision(id: String, name: String) = ModelDto(
            id = id,
            name = name,
            architecture = ArchitectureDto(inputModalities = listOf("text", "image")),
            pricing = PricingDto(prompt = "0", completion = "0")
        )
        val visionModel = freeVision("google/gemma-4-26b-a4b-it:free", "Gemma 4 26B")
        val router = freeVision("openrouter/free", "Free router")
        val safetyClassifier = freeVision("nvidia/nemotron-3.5-content-safety:free", "Nemotron Content Safety")
        val guardModel = freeVision("meta-llama/llama-guard-4-12b", "Llama Guard 4 12B")
        // Reproduces a real catalog entry: Lyria (music generation) reports an "image" input
        // modality (likely for cover-art prompts) and slips past the naive vision filter.
        val musicGenModel = freeVision("google/lyria-3-clip-preview", "Google: Lyria 3 Clip Preview")
        val api = FakeAiApi(modelsResponse = ModelsResponse(
            listOf(visionModel, router, safetyClassifier, guardModel, musicGenModel)
        ))
        val source = RemoteAiDataSource(api, moshi)

        val result = source.fetchFreeVisionModels("key")

        assertEquals(listOf("google/gemma-4-26b-a4b-it:free"), result.map { it.id })
    }

    @Test
    fun `fetchFreeModels excludes router aliases and content-safety classifiers`() = runTest {
        val textModel = ModelDto(
            id = "meta-llama/llama-3.3-70b-instruct:free",
            name = "Llama 3.3 70B",
            pricing = PricingDto(prompt = "0", completion = "0")
        )
        val router = ModelDto(id = "openrouter/free", name = "Free router", pricing = PricingDto("0", "0"))
        val safetyClassifier = ModelDto(
            id = "nvidia/nemotron-3.5-content-safety:free",
            name = "Nemotron Content Safety",
            pricing = PricingDto("0", "0")
        )
        val api = FakeAiApi(modelsResponse = ModelsResponse(listOf(textModel, router, safetyClassifier)))
        val source = RemoteAiDataSource(api, moshi)

        val result = source.fetchFreeModels("key")

        assertEquals(listOf("meta-llama/llama-3.3-70b-instruct:free"), result.map { it.id })
    }

    @Test
    fun `fetchGeminiVisionModels excludes non-gemini and image-generation models`() = runTest {
        val chat = GeminiModelDto(
            name = "models/gemini-2.0-flash",
            displayName = "Gemini 2.0 Flash",
            supportedGenerationMethods = listOf("generateContent")
        )
        val imageGen = GeminiModelDto(
            name = "models/gemini-2.5-flash-image",
            displayName = "Gemini 2.5 Flash Image",
            supportedGenerationMethods = listOf("generateContent")
        )
        val api = FakeAiApi(geminiModelsResponse = GeminiModelsResponse(listOf(chat, imageGen)))
        val source = RemoteAiDataSource(api, moshi)

        val result = source.fetchGeminiVisionModels("key")

        assertEquals(listOf("gemini-2.0-flash"), result.map { it.id })
    }
}
