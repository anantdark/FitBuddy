package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.remote.dto.GithubReleaseDto
import retrofit2.http.GET

interface GithubApi {
    @GET("repos/anantdark/FitBuddy/releases/latest")
    suspend fun getLatestRelease(): GithubReleaseDto
}
