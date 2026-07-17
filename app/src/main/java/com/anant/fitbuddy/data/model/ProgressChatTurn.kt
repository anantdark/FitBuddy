package com.anant.fitbuddy.data.model

/** One turn in a progress-coach chat (OpenAI-compatible roles). */
data class ProgressChatTurn(
    val role: String,
    val content: String
)
