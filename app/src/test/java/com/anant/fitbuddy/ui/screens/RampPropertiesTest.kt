package com.anant.fitbuddy.ui.screens

import org.junit.Test
import kotlin.random.Random

/**
 * Property tests for the dust-streak and planet-trail intensity ramp helpers.
 *
 * Feature: analyzing-solar-system-animation
 */
class RampPropertiesTest {

    /**
     * Property 6: Dust-streak intensity increases with speed
     *
     * For any two positive speeds s1 < s2, `streakScale(s2) >= streakScale(s1)`.
     * Both streak length and alpha gain are proportional to speed, so faster motion
     * always produces longer, brighter streaks (Requirement 1.3).
     *
     * Tag: Feature: analyzing-solar-system-animation, Property 6: Dust-streak intensity increases with speed
     * Validates: Requirements 1.3
     */
    @Test
    fun `Property 6 - dust-streak intensity increases with speed`() {
        val rng = Random(0x636F6D6574)
        repeat(100) {
            // Generate two distinct positive speeds and order them s1 < s2
            val raw1 = rng.nextFloat() * 5f + 0.001f   // (0.001, 5.001]
            val raw2 = rng.nextFloat() * 5f + 0.001f
            val s1 = minOf(raw1, raw2)
            val s2 = maxOf(raw1, raw2)
            // If they happen to be equal, skip – the property is about strict ordering
            if (s1 == s2) return@repeat

            assert(streakScale(s2) >= streakScale(s1)) {
                "Expected streakScale($s2)=${streakScale(s2)} >= streakScale($s1)=${streakScale(s1)}"
            }
        }
    }

    /**
     * Property 7: Planet trail fades monotonically with distance
     *
     * For any step_near < step_far (with the same tailSteps and depthAlpha), the trail
     * alpha at the nearer step is >= the alpha at the farther step, so the comet tail
     * fades continuously behind the planet (Requirement 3.1).
     *
     * Tag: Feature: analyzing-solar-system-animation, Property 7: Planet trail fades monotonically with distance
     * Validates: Requirements 3.1
     */
    @Test
    fun `Property 7 - planet trail fades monotonically with distance`() {
        val rng = Random(0x636F6D6574)
        repeat(100) {
            // tailSteps in [2, 20]
            val tailSteps = rng.nextInt(2, 21)
            // depthAlpha in [0.45, 1.0] (the range produced by depthAlpha())
            val dA = 0.45f + rng.nextFloat() * 0.55f
            // Two distinct step indices in [1, tailSteps]
            val a = rng.nextInt(1, tailSteps + 1)
            val b = rng.nextInt(1, tailSteps + 1)
            val stepNear = minOf(a, b)
            val stepFar  = maxOf(a, b)
            if (stepNear == stepFar) return@repeat

            val alphaNear = trailAlpha(stepNear, tailSteps, dA)
            val alphaFar  = trailAlpha(stepFar,  tailSteps, dA)
            assert(alphaNear >= alphaFar) {
                "Expected trailAlpha(step=$stepNear, tailSteps=$tailSteps, dA=$dA)=$alphaNear " +
                ">= trailAlpha(step=$stepFar, tailSteps=$tailSteps, dA=$dA)=$alphaFar"
            }
        }
    }
}
