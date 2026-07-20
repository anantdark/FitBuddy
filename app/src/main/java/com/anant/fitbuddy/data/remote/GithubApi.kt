package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.remote.dto.GithubReleaseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface GithubApi {
    /**
     * Lists releases newest-first. Prefer this over `/releases/latest` so F-Droid tags
     * (`vX.Y.Z`, no `-buildN`) never become the in-app update target.
     */
    @GET("repos/anantdark/FitBuddy/releases")
    suspend fun listReleases(
        @Query("per_page") perPage: Int = 30
    ): List<GithubReleaseDto>
}
