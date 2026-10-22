package com.anant.fitbuddy.util

import com.anant.fitbuddy.data.model.IngredientDraft
import com.anant.fitbuddy.data.model.FitnessTrackerResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MacroMathTest {
    @Test
    fun roundMacro_keepsOneDecimal() {
        assertEquals(12.3, 12.34.roundMacro(), 0.0)
        assertEquals(12.4, 12.35.roundMacro(), 0.0)
        assertEquals(0.1 + 0.2, 0.3, 1e-9)
        assertEquals(0.3, (0.1 + 0.2).roundMacro(), 0.0)
    }

    @Test
    fun formatMacro_dropsTrailingZero() {
        assertEquals("12", 12.0.formatMacro())
        assertEquals("12.5", 12.5.formatMacro())
        assertEquals("0", 0.0.formatMacro())
    }

    @Test
    fun sanitizeMacroInput_allowsSingleDecimal() {
        assertEquals("12.5", "12.56".sanitizeMacroInput())
        assertEquals("12.5", "12,5".sanitizeMacroInput())
        assertEquals("12.", "12.".sanitizeMacroInput())
        assertEquals("125", "1a2b5".sanitizeMacroInput())
    }

    @Test
    fun toMacroOrNull_parsesDecimals() {
        assertEquals(7.5, "7.5".toMacroOrNull()!!, 0.0)
        assertEquals(7.0, "7".toMacroOrNull()!!, 0.0)
        assertNull("".toMacroOrNull())
    }

    @Test
    fun ingredientDraft_scalesToOneDecimal() {
        val ing = IngredientDraft.fromAbsolute("oats", 40, 150, 5.3, 27.1, 2.6)
        assertEquals(5.3, ing.protein, 0.0)
        assertEquals(27.1, ing.carbs, 0.0)
        assertEquals(2.6, ing.fats, 0.0)
    }

    @Test
    fun aiResponse_decimalMacrosParse() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val json = """{"status":"SUCCESS","food_analysis":{"dish_name":"x","macros":{"calories":200,"protein_g":12.5,"carbs_g":30,"fats_g":4.25}}}"""
        val r = moshi.adapter(FitnessTrackerResponse::class.java).fromJson(json)!!
        assertEquals(12.5, r.foodAnalysis!!.macros.proteinG, 0.0)
        assertEquals(30.0, r.foodAnalysis!!.macros.carbsG, 0.0)
    }
}
