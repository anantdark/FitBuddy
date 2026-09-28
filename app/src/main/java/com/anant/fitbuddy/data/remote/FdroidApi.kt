package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.remote.dto.FdroidPackageDto
import retrofit2.http.GET
import retrofit2.http.Path

interface FdroidApi {
    @GET("api/v1/packages/{packageName}")
    suspend fun getPackage(
        @Path("packageName") packageName: String
    ): FdroidPackageDto
}
