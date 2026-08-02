package com.anant.fitbuddy.ui.screens

import com.anant.fitbuddy.ui.loading.animations.analyzingCaptions
import com.anant.fitbuddy.ui.loading.animations.analyzingLabel
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
        // Build a helper that produces random non-blank strings (at least one non-whitespace char).
        fun randomNonBlankId(): String {
            val length = rng.nextInt(1, 30)
            // Build a string guaranteed to have at least one printable non-whitespace character.
            val chars = CharArray(length) {
                // Use ASCII printable range 33..126 (excludes space and control chars)
                rng.nextInt(33, 127).toChar()
            }
            return String(chars)
        }

        repeat(100) {
            val id = randomNonBlankId()
            val label = analyzingLabel(id)
            assertEquals("ANALYZING $id", label)
            assertFalse("Label must not contain newline for id='$id'", label.contains('\n'))
        }
    }

    /**
     * A null model-id must produce exactly "ANALYZING" (no trailing space or newline).
     */
    @Test
    fun `Property 8 - null id yields ANALYZING`() {
        // null is deterministic, but run it through the same loop count for consistency.
        repeat(100) {
            val label = analyzingLabel(null)
            assertEquals("ANALYZING", label)
        }
    }

    /**
     * A blank (empty or whitespace-only) model-id must produce exactly "ANALYZING".
     */
    @Test
    fun `Property 8 - blank or whitespace-only id yields ANALYZING`() {
        val rng = Random(seed = 99L)
        // Produce empty string, single space, tab, newline, and random multi-whitespace strings.
        val fixedBlanks = listOf("", " ", "\t", "\n", "   ", "\t \n")
        repeat(100) { i ->
            val id: String = when {
                i < fixedBlanks.size -> fixedBlanks[i]
                else -> {
                    // Random string composed purely of whitespace characters.
                    val wsChars = charArrayOf(' ', '\t', '\n', '\r')
                    val len = rng.nextInt(1, 20)
                    String(CharArray(len) { wsChars[rng.nextInt(wsChars.size)] })
                }
            }
            val label = analyzingLabel(id)
            assertEquals("Expected 'ANALYZING' for blank id='${id.replace("\n", "\\n")}'", "ANALYZING", label)
        }
    }

    // -----------------------------------------------------------------------
    // Property 9: Micro-caption membership
    // Feature: analyzing-solar-system-animation, Property 9: Micro-caption membership
    // Validates: Requirements 6.3
    // -----------------------------------------------------------------------

    private val expectedCaptions: Set<String> = setOf(
        "reading the plate\u2026",
        "counting macros\u2026",
        "weighing portions\u2026",
        "estimating calories\u2026",
        "crunching the numbers\u2026"
    )

    /**
     * Every random pick from [analyzingCaptions] must be a member of the fixed five-caption set.
     */
    @Test
    fun `Property 9 - random caption selection always returns a member of the fixed five-caption set`() {
        val rng = Random(seed = 7L)
        repeat(100) {
            val picked = analyzingCaptions.random(rng)
            assertTrue(
                "Picked caption '$picked' is not in the expected set",
                expectedCaptions.contains(picked)
            )
        }
    }

    /**
     * The [analyzingCaptions] list itself must contain exactly the five expected captions
     * (no more, no less) — guards against accidental additions/removals.
     */
    @Test
    fun `Property 9 - analyzingCaptions contains exactly the five fixed captions`() {
        assertEquals(5, analyzingCaptions.size)
        expectedCaptions.forEach { caption ->
            assertTrue("Missing caption: '$caption'", analyzingCaptions.contains(caption))
        }
    }
}
