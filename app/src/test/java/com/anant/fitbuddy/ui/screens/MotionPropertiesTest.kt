package com.anant.fitbuddy.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/**
 * Property tests for the scaled-time accumulator.
 *
 * Uses plain JUnit4 repeat(100) loops with a fixed Random seed for reproducibility.
 * No new dependencies — exercises only the internal helpers in BannerAnimationMath.kt.
 */
class MotionPropertiesTest {

    // -----------------------------------------------------------------------
    // Property 1: Scaled-time accumulator is monotonic
    //
    // Feature: analyzing-solar-system-animation
    // Property 1: Scaled-time accumulator is monotonic
    // Validates: Requirements 4.1, 3.3
    // -----------------------------------------------------------------------

    @Test
    fun `Property 1 - scaled-time accumulator is monotonic`() {
        val rng = Random(seed = 0x50524F50_31L) // fixed seed for reproducibility
        val iterations = 100

        repeat(iterations) {
            // Generate a random sequence of 20–50 frames
            val frameCount = rng.nextInt(20, 51)

            // Non-negative raw deltas: 0..100 ms per frame (Long)
            val deltas: List<Long> = List(frameCount) { rng.nextLong(0L, 101L) }

            // Positive speed multipliers: 0.1f..3.0f
            val speeds: List<Float> = List(frameCount) { rng.nextFloat() * 2.9f + 0.1f }

            var scaledTime = 0.0
            var prevScaledTime = 0.0

            for (i in 0 until frameCount) {
                scaledTime = accumulateScaledTime(scaledTime, deltas[i], speeds[i])
                assertTrue(
                    "iteration=$it frame=$i: scaledTime=$scaledTime < prevScaledTime=$prevScaledTime (not monotonic)",
                    scaledTime >= prevScaledTime
                )
                prevScaledTime = scaledTime
            }
        }
    }

    // -----------------------------------------------------------------------
    // Property 2: Speed change causes no discontinuity (running-sum equality)
    //
    // Feature: analyzing-solar-system-animation
    // Property 2: Speed change causes no discontinuity (running-sum equality)
    // Validates: Requirements 4.2, 5.3
    // -----------------------------------------------------------------------

    @Test
    fun `Property 2 - speed change causes no discontinuity running-sum equality`() {
        val rng = Random(seed = 0x50524F50_32L) // fixed seed for reproducibility
        val iterations = 100

        repeat(iterations) {
            // Generate 10–30 frames that both schedules share (frames 1..k)
            val sharedFrameCount = rng.nextInt(10, 31)

            // Non-negative raw deltas shared by both schedules
            val deltas: List<Long> = List(sharedFrameCount) { rng.nextLong(0L, 101L) }

            // Schedule A: positive multipliers 0.1..3.0
            val multipliersA: List<Float> = List(sharedFrameCount) { rng.nextFloat() * 2.9f + 0.1f }

            // Schedule B: same deltas but independently chosen multipliers — deliberately different
            // We only require that after frame k the *values agree* for the same schedule, and the
            // running-sum identity holds. The "no discontinuity" property asserts that:
            //   scaledTime_k = sum_{i=0..k-1} max(deltas[i], 0) * multipliers[i]
            // which means if we swap the multiplier at frame k+1 the previously accumulated value
            // is untouched. We verify this by:
            //   (a) running the accumulator and comparing to the hand-computed running sum, and
            //   (b) confirming that prefixing with k frames from schedule A then switching to a
            //       single schedule-B multiplier at frame k still gives the same prefix total.

            // (a) Accumulator matches the running sum for schedule A
            var scaledTimeA = 0.0
            var manualSum = 0.0
            for (i in 0 until sharedFrameCount) {
                scaledTimeA = accumulateScaledTime(scaledTimeA, deltas[i], multipliersA[i])
                manualSum += maxOf(deltas[i], 0L) * multipliersA[i].toDouble()
            }
            assertEquals(
                "iteration=$it: accumulator result $scaledTimeA != running sum $manualSum",
                manualSum,
                scaledTimeA,
                1e-9
            )

            // (b) Changing the multiplier at frame k does not alter the value accumulated in frames 0..k-1
            // Take the prefix of k frames (k = sharedFrameCount - 1), accumulate it, then apply
            // one extra frame with a *different* multiplier. The prefix total must be unchanged.
            val k = sharedFrameCount - 1
            var prefixTotal = 0.0
            for (i in 0 until k) {
                prefixTotal = accumulateScaledTime(prefixTotal, deltas[i], multipliersA[i])
            }
            val prefixSnapshot = prefixTotal

            // Apply frame k with schedule A's multiplier
            val totalWithA = accumulateScaledTime(prefixTotal, deltas[k], multipliersA[k])

            // Apply frame k with a completely different multiplier (schedule B)
            val multiplierB = rng.nextFloat() * 2.9f + 0.1f
            val totalWithB = accumulateScaledTime(prefixSnapshot, deltas[k], multiplierB)

            // The prefix (frames 0..k-1) is identical in both — only the last increment differs
            val expectedA = prefixSnapshot + maxOf(deltas[k], 0L) * multipliersA[k].toDouble()
            val expectedB = prefixSnapshot + maxOf(deltas[k], 0L) * multiplierB.toDouble()

            assertEquals(
                "iteration=$it: totalWithA $totalWithA != expectedA $expectedA",
                expectedA, totalWithA, 1e-9
            )
            assertEquals(
                "iteration=$it: totalWithB $totalWithB != expectedB $expectedB",
                expectedB, totalWithB, 1e-9
            )

            // Both paths start from the same prefix — the multiplier switch only changes the increment
            assertEquals(
                "iteration=$it: prefixSnapshot should be shared base, mismatch",
                prefixSnapshot,
                totalWithA - (maxOf(deltas[k], 0L) * multipliersA[k].toDouble()),
                1e-9
            )
            assertEquals(
                "iteration=$it: prefixSnapshot should be shared base, mismatch",
                prefixSnapshot,
                totalWithB - (maxOf(deltas[k], 0L) * multiplierB.toDouble()),
                1e-9
            )
        }
    }
}
