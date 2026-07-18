package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.remote.dto.OffProductResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}")
    suspend fun getProduct(
        @Path("barcode") barcode: String,
        @Query("fields") fields: String = FIELDS
    ): Response<OffProductResponse>

    companion object {
        /** Only the nutriment keys we map — keeps payloads small and avoids CDN quirks. */
        const val FIELDS =
            "code,product_name,brands,serving_size,nutriments"
    }
}
