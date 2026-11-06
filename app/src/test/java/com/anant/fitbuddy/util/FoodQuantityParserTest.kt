package com.anant.fitbuddy.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FoodQuantityParserTest {

    @Test
    fun `parseSegments extracts leading count`() {
        val segments = FoodQuantityParser.parseSegments("4 almonds")
        assertEquals(1, segments.size)
        assertEquals(4.0, segments[0].quantity, 0.0)
        assertEquals("almonds", segments[0].name)
    }

    @Test
    fun `parseSegments extracts decimal count`() {
        val segments = FoodQuantityParser.parseSegments("1.5 bananas")
        assertEquals(1, segments.size)
        assertEquals(1.5, segments[0].quantity, 0.0)
        assertEquals("bananas", segments[0].name)
    }

    @Test
    fun `parseSegments splits compound logs`() {
        val segments = FoodQuantityParser.parseSegments("2 rotis and dal")
        assertEquals(1, segments.size)
        assertEquals(2.0, segments[0].quantity, 0.0)
        assertEquals("rotis", segments[0].name)
    }

    @Test
    fun `quantityForIngredient matches plural ingredient names`() {
        val qty = FoodQuantityParser.quantityForIngredient(
            userText = "4 almonds",
            ingredientName = "Almond",
            aiQuantity = 1.0
        )
        assertEquals(4.0, qty, 0.0)
    }

    @Test
    fun `quantityForIngredient prefers explicit AI quantity`() {
        val qty = FoodQuantityParser.quantityForIngredient(
            userText = "4 almonds",
            ingredientName = "Almond",
            aiQuantity = 6.0
        )
        assertEquals(6.0, qty, 0.0)
    }

    @Test
    fun `quantityForIngredient returns 1 when no match`() {
        val qty = FoodQuantityParser.quantityForIngredient(
            userText = "bowl of dal",
            ingredientName = "Dal",
            aiQuantity = 1.0
        )
        assertEquals(1.0, qty, 0.0)
    }

    @Test
    fun `parseSegments understands hinglish counts`() {
        val segments = FoodQuantityParser.parseSegments("do roti")
        assertEquals(1, segments.size)
        assertEquals(2.0, segments[0].quantity, 0.0)
        assertEquals("roti", segments[0].name)
    }

    @Test
    fun `parseSegments understands teen paratha`() {
        val segments = FoodQuantityParser.parseSegments("teen paratha with curd")
        assertEquals(1, segments.size)
        assertEquals(3.0, segments[0].quantity, 0.0)
        assertEquals("paratha", segments[0].name)
    }

    @Test
    fun `parseSegments understands katori of dal`() {
        val segments = FoodQuantityParser.parseSegments("ek katori of dal")
        assertEquals(1, segments.size)
        assertEquals(1.0, segments[0].quantity, 0.0)
        assertEquals("dal", segments[0].name)
    }

    @Test
    fun `quantityForIngredient matches hinglish roti`() {
        val qty = FoodQuantityParser.quantityForIngredient(
            userText = "char roti aur dal",
            ingredientName = "Wheat roti",
            aiQuantity = 1.0
        )
        assertEquals(4.0, qty, 0.0)
    }
}
