package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.model.ScannedProduct
import kotlin.math.roundToInt

class OpenFoodFactsDataSource(
    private val api: OpenFoodFactsApi
) {
    suspend fun lookupBarcode(barcode: String): ScannedProduct {
        val code = normalizeBarcode(barcode)
        require(code.isNotBlank()) { "Invalid barcode" }

        val http = api.getProduct(code, OpenFoodFactsApi.FIELDS)
        // OFF returns HTTP 404 (JSON body) for unknown codes. Using Response<> avoids Retrofit's
        // bare "HTTP 404" HttpException so we can say which barcode failed.
        val response = http.body()
        if (!http.isSuccessful || response == null || response.status != 1 || response.product == null) {
            throw IllegalStateException("No Open Food Facts product for barcode $code")
        }

        val product = response.product
        val name = listOfNotNull(
            product.productName?.trim()?.takeIf { it.isNotBlank() },
            product.brands?.trim()?.takeIf { it.isNotBlank() }
        ).distinct().joinToString(" · ").ifBlank { "Packaged food" }

        val servingGrams = parseServingGrams(product.servingSize)
        val nutriments = product.nutriments
            ?: throw IllegalStateException("No nutrition data for barcode $code")

        val calories = pickNutrient(nutriments.energyKcalServing, nutriments.energyKcal100g, servingGrams)
            ?: throw IllegalStateException("No calorie info for barcode $code")
        val protein = pickNutrient(nutriments.proteinsServing, nutriments.proteins100g, servingGrams) ?: 0.0
        val carbs = pickNutrient(nutriments.carbsServing, nutriments.carbs100g, servingGrams) ?: 0.0
        val fats = pickNutrient(nutriments.fatServing, nutriments.fat100g, servingGrams) ?: 0.0

        return ScannedProduct(
            barcode = code,
            name = name,
            calories = calories.roundToInt().coerceAtLeast(0),
            proteinG = protein.roundToInt().coerceAtLeast(0),
            carbsG = carbs.roundToInt().coerceAtLeast(0),
            fatsG = fats.roundToInt().coerceAtLeast(0),
            servingGrams = servingGrams
        )
    }

    private fun pickNutrient(perServing: Double?, per100g: Double?, servingGrams: Int?): Double? {
        perServing?.takeIf { it > 0 }?.let { return it }
        val per100 = per100g?.takeIf { it > 0 } ?: return null
        val grams = servingGrams?.takeIf { it > 0 } ?: 100
        return per100 * grams / 100.0
    }

    private fun parseServingGrams(servingSize: String?): Int? {
        if (servingSize.isNullOrBlank()) return null
        val match = Regex("(\\d+(?:\\.\\d+)?)\\s*g", RegexOption.IGNORE_CASE).find(servingSize)
        return match?.groupValues?.get(1)?.toDoubleOrNull()?.roundToInt()?.takeIf { it > 0 }
    }

    companion object {
        fun normalizeBarcode(raw: String): String = raw.filter { it.isDigit() }
    }
}
