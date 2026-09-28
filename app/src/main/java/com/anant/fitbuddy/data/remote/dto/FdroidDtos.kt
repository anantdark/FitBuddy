package com.anant.fitbuddy.data.remote.dto

import com.squareup.moshi.JsonClass

/** Response from `GET https://f-droid.org/api/v1/packages/{packageName}`. */
@JsonClass(generateAdapter = true)
data class FdroidPackageDto(
    val packageName: String = "",
    val suggestedVersionCode: Int = 0,
    val packages: List<FdroidPackageVersionDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class FdroidPackageVersionDto(
    val versionName: String = "",
    val versionCode: Int = 0,
)
