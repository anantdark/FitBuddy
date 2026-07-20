package com.anant.fitbuddy.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GithubReleaseDto(
    @Json(name = "tag_name") val tagName: String,
    val name: String,
    val body: String?,
    @Json(name = "html_url") val htmlUrl: String,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAssetDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GithubAssetDto(
    val name: String,
    @Json(name = "browser_download_url") val downloadUrl: String
)
