package com.anant.fitbuddy.data.remote.dto

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenRouterAuthKeyRequest(
    val code: String,
    val code_verifier: String,
    val code_challenge_method: String = "S256"
)

@JsonClass(generateAdapter = true)
data class OpenRouterAuthErrorBody(
    val message: String? = null,
    val code: Int? = null
)

@JsonClass(generateAdapter = true)
data class OpenRouterAuthKeyResponse(
    val key: String? = null,
    val error: OpenRouterAuthErrorBody? = null
)
