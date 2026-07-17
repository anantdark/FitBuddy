package com.anant.fitbuddy.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OffProductResponse(
    @Json(name = "status") val status: Int = 0,
    @Json(name = "product") val product: OffProduct? = null
)

@JsonClass(generateAdapter = true)
data class OffProduct(
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "brands") val brands: String? = null,
    @Json(name = "serving_size") val servingSize: String? = null,
    @Json(name = "nutriments") val nutriments: OffNutriments? = null
)

@JsonClass(generateAdapter = true)
data class OffNutriments(
    @Json(name = "energy-kcal_serving") val energyKcalServing: Double? = null,
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double? = null,
    @Json(name = "proteins_serving") val proteinsServing: Double? = null,
    @Json(name = "proteins_100g") val proteins100g: Double? = null,
    @Json(name = "carbohydrates_serving") val carbsServing: Double? = null,
    @Json(name = "carbohydrates_100g") val carbs100g: Double? = null,
    @Json(name = "fat_serving") val fatServing: Double? = null,
    @Json(name = "fat_100g") val fat100g: Double? = null
)
