package com.anant.fitbuddy.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodQuantityParserTest {

    @Test
    fun `parseSegments extracts leading count`() {
        val segments = FoodQuantityParser.parseSegments("4 almonds")
        assertEquals(1, segments.size)
        assertEquals(4, segments[0].quantity)
        assertEquals("almonds", segments[0].name)
    }

    @Test
    fun `parseSegments splits compound logs`() {
        val segments = FoodQuantityParser.parseSegments("2 rotis and dal")
        assertEquals(1, segments.size)
        assertEquals(2, segments[0].quantity)
        assertEquals("rotis", segments[0].name)
    }

    @Test
    fun `quantityForIngredient matches plural ingredient names`() {
        val qty = FoodQuantityParser.quantityForIngredient(
            userText = "4 almonds",
            ingredientName = "Almond",
            aiQuantity = 1
        )
        assertEquals(4, qty)
    }

    @Test
    fun `quantityForIngredient prefers explicit AI quantity`() {
        val qty = FoodQuantityParser.quantityForIngredient(
            userText = "4 almonds",
            ingredientName = "Almond",
            aiQuantity = 6
        )
        assertEquals(6, qty)
    }

    @Test
    fun `quantityForIngredient returns 1 when no match`() {
        val qty = FoodQuantityParser.quantityForIngredient(
            userText = "bowl of dal",
            ingredientName = "Dal",
            aiQuantity = 1
        )
        assertEquals(1, qty)
    }

    @Test
    fun `parseSegments understands hinglish counts`() {
        val segments = FoodQuantityParser.parseSegments("do roti")
        assertEquals(1, segments.size)
        assertEquals(2, segments[0].quantity)
        assertEquals("roti", segments[0].name)
    }

    @Test
    fun `parseSegments understands teen paratha`() {
        val segments = FoodQuantityParser.parseSegments("teen paratha with curd")
        assertEquals(1, segments.size)
        assertEquals(3, segments[0].quantity)
        assertEquals("paratha", segments[0].name)
    }

    @Test
    fun `parseSegments understands katori of dal`() {
        val segments = FoodQuantityParser.parseSegments("ek katori of dal")
        assertEquals(1, segments.size)
        assertEquals(1, segments[0].quantity)
        assertEquals("dal", segments[0].name)
    }

    @Test
    fun `quantityForIngredient matches hinglish roti`() {
        val qty = FoodQuantityParser.quantityForIngredient(
            userText = "char roti aur dal",
            ingredientName = "Wheat roti",
            aiQuantity = 1
        )
        assertEquals(4, qty)
    }
}
