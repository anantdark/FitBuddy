package com.anant.fitbuddy.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenFoodFactsBarcodeNormalizeTest {

    @Test
    fun `normalizeBarcode keeps digits only`() {
        assertEquals(
            "8901030865422",
            OpenFoodFactsDataSource.normalizeBarcode("8901030865422")
        )
        assertEquals(
            "8901030865422",
            OpenFoodFactsDataSource.normalizeBarcode(" 890-1030-865422 ")
        )
        assertEquals("", OpenFoodFactsDataSource.normalizeBarcode("abc"))
        assertEquals("", OpenFoodFactsDataSource.normalizeBarcode(""))
    }
}
