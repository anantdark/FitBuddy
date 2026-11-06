package com.anant.fitbuddy.ui.screens

import com.anant.fitbuddy.R
import com.anant.fitbuddy.ui.loading.animations.analyzingCaptionRes
import com.anant.fitbuddy.ui.loading.animations.formatAnalyzingLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Property tests for label helpers in BannerAnimationMath.kt.
 *
 * Feature: analyzing-solar-system-animation
 */
class LabelPropertiesTest {

    // -----------------------------------------------------------------------
    // Property 8: Label formatting for all model-id inputs
    // Feature: analyzing-solar-system-animation, Property 8: Label formatting for all model-id inputs
    // Validates: Requirements 6.1, 6.2
    // -----------------------------------------------------------------------

    /**
     * For every non-null, non-blank id the label must be exactly "ANALYZING {id}" with no
     * embedded newline.
     */
    @Test
    fun `Property 8 - non-blank id yields ANALYZING space id with no newline`() {
        val rng = Random(seed = 42L)
        fun randomNonBlankId(): String {
            val length = rng.nextInt(1, 30)
            val chars = CharArray(length) {
                rng.nextInt(33, 127).toChar()
            }
            return String(chars)
        }

        repeat(100) {
            val id = randomNonBlankId()
            val label = formatAnalyzingLabel(id)
            assertEquals("ANALYZING $id", label)
            assertFalse("Label must not contain newline for id='$id'", label.contains('\n'))
        }
    }

    /**
     * A null model-id must produce exactly "ANALYZING" (no trailing space or newline).
     */
    @Test
    fun `Property 8 - null id yields ANALYZING`() {
        repeat(100) {
            val label = formatAnalyzingLabel(null)
            assertEquals("ANALYZING", label)
        }
    }

    /**
     * A blank (empty or whitespace-only) model-id must produce exactly "ANALYZING".
     */
    @Test
    fun `Property 8 - blank or whitespace-only id yields ANALYZING`() {
        val rng = Random(seed = 99L)
        val fixedBlanks = listOf("", " ", "\t", "\n", "   ", "\t \n")
        repeat(100) { i ->
            val id: String = when {
                i < fixedBlanks.size -> fixedBlanks[i]
                else -> {
                    val wsChars = charArrayOf(' ', '\t', '\n', '\r')
                    val len = rng.nextInt(1, 20)
                    String(CharArray(len) { wsChars[rng.nextInt(wsChars.size)] })
                }
            }
            val label = formatAnalyzingLabel(id)
            assertEquals("Expected 'ANALYZING' for blank id='${id.replace("\n", "\\n")}'", "ANALYZING", label)
        }
    }

    // -----------------------------------------------------------------------
    // Property 9: Micro-caption membership
    // Feature: analyzing-solar-system-animation, Property 9: Micro-caption membership
    // Validates: Requirements 6.3
    // -----------------------------------------------------------------------

    private val expectedCaptionRes: List<Int> = listOf(
        R.string.loading_reading_plate,
        R.string.loading_counting_macros,
        R.string.loading_weighing_portions,
        R.string.loading_estimating_calories,
        R.string.loading_crunching_numbers,
    )

    /**
     * Every random pick from [analyzingCaptionRes] must be a member of the fixed five-caption set.
     */
    @Test
    fun `Property 9 - random caption selection always returns a member of the fixed five-caption set`() {
        val rng = Random(seed = 7L)
        repeat(100) {
            val picked = analyzingCaptionRes.random(rng)
            assertTrue(
                "Picked caption res '$picked' is not in the expected set",
                expectedCaptionRes.contains(picked)
            )
        }
    }

    /**
     * The [analyzingCaptionRes] list itself must contain exactly the five expected captions
     * (no more, no less) — guards against accidental additions/removals.
     */
    @Test
    fun `Property 9 - analyzingCaptionRes contains exactly the five fixed captions`() {
        assertEquals(5, analyzingCaptionRes.size)
        assertEquals(expectedCaptionRes, analyzingCaptionRes)
    }
}
