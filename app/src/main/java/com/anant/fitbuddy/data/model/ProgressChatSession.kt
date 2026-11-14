package com.anant.fitbuddy.data.model

import com.squareup.moshi.JsonClass

/** One persisted progress-coach chat message. */
@JsonClass(generateAdapter = true)
data class ProgressChatSessionMessage(
    val role: String,
    val text: String
)

/**
 * Disk snapshot of a kept progress-coach chat. Valid until the UTC calendar day after
 * [lastMessageAtEpochMs] (cleared at that midnight or later).
 */
@JsonClass(generateAdapter = true)
data class ProgressChatSession(
    val messages: List<ProgressChatSessionMessage> = emptyList(),
    val metricsContextFull: String = "",
    val summary: String? = null,
    val recommendations: List<String> = emptyList(),
    val bodyScore: Int? = null,
    val lastMessageAtEpochMs: Long = 0L
)
