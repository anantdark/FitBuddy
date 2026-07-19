package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.remote.dto.ChatRequest
import com.anant.fitbuddy.data.remote.dto.ChatRequestPlain
import com.anant.fitbuddy.data.remote.dto.ChatResponse
import com.anant.fitbuddy.data.remote.dto.GeminiModelsResponse
import com.anant.fitbuddy.data.remote.dto.ModelsResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Retrofit interface for the LLM endpoint. Any OpenAI-compatible gateway works
 * (OpenRouter, Ollama's /v1 endpoint, Gemini compat layer, self-hosted proxy, etc.).
 *
 * The full URL is passed per-call via [Url] so the provider/host can change at runtime,
 * and the auth header is nullable (Retrofit omits it) for keyless backends like local Ollama.
 */
interface AiApi {

    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body request: ChatRequest
    ): ChatResponse

    /** Plain-string-content fallback; see [ChatRequestPlain]. */
    @POST
    suspend fun chatCompletionPlain(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body request: ChatRequestPlain
    ): ChatResponse

    /**
     * Lightweight chat probe that returns the raw HTTP response (no throw on 4xx/5xx).
     * Used by Refresh models to keep only endpoints that answer 200 or 429.
     */
    @POST
    suspend fun probeChatCompletion(
        @Url url: String,
        @Header("Authorization") authorization: String?,
        @Body request: ChatRequestPlain
    ): Response<ResponseBody>

    @GET
    suspend fun listModels(
        @Url url: String,
        @Header("Authorization") authorization: String?
    ): ModelsResponse

    /** Gemini list-models uses an `?key=` query param (baked into [url]) instead of a header. */
    @GET
    suspend fun listGeminiModels(
        @Url url: String
    ): GeminiModelsResponse
}
