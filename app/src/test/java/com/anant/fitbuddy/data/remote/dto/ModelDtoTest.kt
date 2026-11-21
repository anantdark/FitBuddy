package com.anant.fitbuddy.data.remote.dto

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelDtoTest {

    @Test
    fun `isFree when both prompt and completion are zero`() {
        assertTrue(
            model(
                pricing = PricingDto(prompt = "0", completion = "0")
            ).isFree
        )
        assertTrue(
            model(
                pricing = PricingDto(prompt = "0.0", completion = "0.00")
            ).isFree
        )
    }

    @Test
    fun `isFree false when pricing missing or non-zero`() {
        assertFalse(model(pricing = null).isFree)
        assertFalse(
            model(
                pricing = PricingDto(prompt = "0.000001", completion = "0")
            ).isFree
        )
        assertFalse(
            model(
                pricing = PricingDto(prompt = "0", completion = "1")
            ).isFree
        )
        assertFalse(
            model(
                pricing = PricingDto(prompt = "free", completion = "0")
            ).isFree
        )
    }

    @Test
    fun `supportsVision from input_modalities or legacy modality string`() {
        assertTrue(
            model(
                architecture = ArchitectureDto(inputModalities = listOf("text", "image"))
            ).supportsVision
        )
        assertTrue(
            model(
                architecture = ArchitectureDto(modality = "text+image")
            ).supportsVision
        )
        assertFalse(
            model(
                architecture = ArchitectureDto(inputModalities = listOf("text"))
            ).supportsVision
        )
        assertFalse(model(architecture = null).supportsVision)
    }

    private fun model(
        pricing: PricingDto? = null,
        architecture: ArchitectureDto? = null
    ) = ModelDto(
        id = "test/model",
        name = "Test",
        architecture = architecture,
        pricing = pricing
    )
}
