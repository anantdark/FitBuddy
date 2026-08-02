package com.anant.fitbuddy.ui.screens

import androidx.compose.ui.graphics.Color
import com.anant.fitbuddy.ui.components.MacroCarbsColor
import com.anant.fitbuddy.ui.components.MacroFatsColor
import com.anant.fitbuddy.ui.components.MacroProteinColor
import com.anant.fitbuddy.ui.loading.animations.PLANET_PERIOD_YEARS
import com.anant.fitbuddy.ui.loading.animations.SLOWEST_ORBITS_PER_CROSSING
import com.anant.fitbuddy.ui.loading.animations.orbitsPerCrossing
import com.anant.fitbuddy.ui.loading.animations.solarSystemPlanets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

/**
 * Property and unit tests for the planet configuration helpers in BannerAnimationMath.kt.
 *
 * Feature: analyzing-solar-system-animation
 */
class ConfigPropertiesTest {

    // -----------------------------------------------------------------------
    // Property 5: Kepler-style speed ordering
    // Feature: analyzing-solar-system-animation, Property 5: Kepler-style speed ordering
    // Validates: Requirements 2.6
    // -----------------------------------------------------------------------

    /**
     * For any color inputs, `solarSystemPlanets(...)` must return planets with strictly
     * decreasing `angularVel` from innermost (index 0) to outermost (index 2), matching
     * Kepler's law where inner planets orbit faster than outer ones.
     *
     * **Validates: Requirements 2.6**
     */
    @Test
    fun `Property 5 - angularVel is strictly decreasing from innermost to outermost planet`() {
        val rng = Random(seed = 42L)

        repeat(100) {
            // Randomize the three injected colors (ARGB components as random floats in [0,1]).
            val protein = Color(
                red = rng.nextFloat(),
                green = rng.nextFloat(),
                blue = rng.nextFloat(),
                alpha = rng.nextFloat()
            )
            val carbs = Color(
                red = rng.nextFloat(),
                green = rng.nextFloat(),
                blue = rng.nextFloat(),
                alpha = rng.nextFloat()
            )
            val fats = Color(
                red = rng.nextFloat(),
                green = rng.nextFloat(),
                blue = rng.nextFloat(),
                alpha = rng.nextFloat()
            )

            val planets = solarSystemPlanets(protein, carbs, fats)

            for (i in 0 until planets.size - 1) {
                assertTrue(
                    "Expected angularVel[${i}]=${planets[i].angularVel} > angularVel[${i + 1}]=${planets[i + 1].angularVel} " +
                        "(inner must orbit faster than outer)",
                    planets[i].angularVel > planets[i + 1].angularVel
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Unit tests for planet configuration
    // Validates: Requirements 2.1, 2.7
    // -----------------------------------------------------------------------

    /**
     * `solarSystemPlanets(...)` must return eight planets (Mercury→Neptune). (Requirement 2.1)
     */
    @Test
    fun `solarSystemPlanets returns eight planets`() {
        val planets = solarSystemPlanets(MacroProteinColor, MacroCarbsColor, MacroFatsColor)
        assertEquals("Expected exactly 8 planets", 8, planets.size)
    }

    /**
     * The innermost three planet colors must equal the injected protein/carbs/fats colors:
     * index 0 = protein, index 1 = carbs, index 2 = fats. (Requirement 2.7)
     */
    @Test
    fun `solarSystemPlanets assigns protein carbs fats colors in order`() {
        val planets = solarSystemPlanets(MacroProteinColor, MacroCarbsColor, MacroFatsColor)
        assertEquals("Planet 0 (inner) must use MacroProteinColor", MacroProteinColor, planets[0].color)
        assertEquals("Planet 1 must use MacroCarbsColor", MacroCarbsColor, planets[1].color)
        assertEquals("Planet 2 must use MacroFatsColor", MacroFatsColor, planets[2].color)
    }

    /**
     * Color injection is respected — arbitrary distinct colors are mapped to the innermost
     * three positions regardless of which specific colors are passed.
     */
    @Test
    fun `solarSystemPlanets maps injected colors to planet positions correctly`() {
        val red = Color(0xFFFF0000)
        val green = Color(0xFF00FF00)
        val blue = Color(0xFF0000FF)

        val planets = solarSystemPlanets(red, green, blue)

        assertEquals("Planet 0 must carry the injected protein color", red, planets[0].color)
        assertEquals("Planet 1 must carry the injected carbs color", green, planets[1].color)
        assertEquals("Planet 2 must carry the injected fats color", blue, planets[2].color)
    }

    /**
     * Slowest body (Neptune) must complete the configured orbits-per-crossing so
     * outer planets keep drifting (relative speeds stay real).
     */
    @Test
    fun `slowest planet meets configured orbits per sun crossing`() {
        val planets = solarSystemPlanets(MacroProteinColor, MacroCarbsColor, MacroFatsColor)
        val slowest = planets.last()
        val orbits = orbitsPerCrossing(slowest)
        assertTrue(
            "Neptune orbits/crossing=$orbits, expected >= $SLOWEST_ORBITS_PER_CROSSING",
            orbits >= SLOWEST_ORBITS_PER_CROSSING - 1e-9
        )
        // Every planet must move (positive angular velocity).
        planets.forEachIndexed { i, p ->
            assertTrue("Planet $i angularVel must be > 0", p.angularVel > 0.0)
            assertTrue("Planet $i must complete some orbit fraction", orbitsPerCrossing(p) > 0.0)
        }
    }

    /**
     * Angular-speed ratios must match real sidereal period ratios: ω_i / ω_j = P_j / P_i.
     */
    @Test
    fun `angular speeds match real solar-system period ratios`() {
        val planets = solarSystemPlanets(MacroProteinColor, MacroCarbsColor, MacroFatsColor)
        assertEquals(PLANET_PERIOD_YEARS.size, planets.size)
        val ref = planets.last().angularVel
        val pRef = PLANET_PERIOD_YEARS.last()
        for (i in planets.indices) {
            val expected = ref * (pRef / PLANET_PERIOD_YEARS[i])
            val actual = planets[i].angularVel
            val relErr = abs(actual - expected) / expected
            assertTrue(
                "Planet $i ω=$actual expected=$expected (rel err $relErr)",
                relErr < 1e-9
            )
        }
    }
}
