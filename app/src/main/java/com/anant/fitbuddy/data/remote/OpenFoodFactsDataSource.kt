package com.anant.fitbuddy.data.remote

import com.anant.fitbuddy.data.model.ScannedProduct
import com.anant.fitbuddy.data.remote.dto.OffNutriments
import kotlin.math.roundToInt

class OpenFoodFactsDataSource(
    private val api: OpenFoodFactsApi
) {
    suspend fun lookupBarcode(barcode: String): ScannedProduct {
        val response = api.getProduct(barcode.trim())
        if (response.status != 1 || response.product == null) {
            throw IllegalStateException("Product not found for barcode $barcode")
        }
        val product = response.product
        val name = listOfNotNull(
            product.productName?.trim()?.takeIf { it.isNotBlank() },
            product.brands?.trim()?.takeIf { it.isNotBlank() }
        ).distinct().joinToString(" · ").ifBlank { "Packaged food" }

        val servingGrams = parseServingGrams(product.servingSize)
        val nutriments = product.nutriments ?: throw IllegalStateException("No nutrition data on this product")

        val calories = pickNutrient(nutriments.energyKcalServing, nutriments.energyKcal100g, servingGrams)
            ?: throw IllegalStateException("No calorie info on this product")
        val protein = pickNutrient(nutriments.proteinsServing, nutriments.proteins100g, servingGrams) ?: 0.0
        val carbs = pickNutrient(nutriments.carbsServing, nutriments.carbs100g, servingGrams) ?: 0.0
        val fats = pickNutrient(nutriments.fatServing, nutriments.fat100g, servingGrams) ?: 0.0

        return ScannedProduct(
            barcode = barcode.trim(),
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
}
