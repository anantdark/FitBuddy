package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.remote.dto.OffProductResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}")
    suspend fun getProduct(@Path("barcode") barcode: String): OffProductResponse
}
