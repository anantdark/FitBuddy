package com.anant.fitbuddy.data.model

import com.squareup.moshi.JsonClass

/** A photo log saved for later: photos live on disk as [photoFiles], analysed only when run. */
@JsonClass(generateAdapter = true)
data class QueuedPhotoLog(
    val id: String,
    val createdAtEpochMs: Long,
    val note: String = "",
    val photoFiles: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
internal data class QueuedPhotoIndex(val items: List<QueuedPhotoLog> = emptyList())
