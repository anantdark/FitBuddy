package com.anant.fitbuddy.ui.loading.animations

import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

internal data class PlanetSpec(
    val orbitA: Float,       // horizontal lag amplitude behind the sun (fraction of canvas width)
    val orbitB: Float,       // vertical circle radius (fraction of canvas half-height)
    val angularVel: Double,  // radians per accumulated ms — strictly decreasing inner→outer
    val phase: Double,       // initial angle offset (radians)
    val color: Color
)

// ---------------------------------------------------------------------------
// Scaled-time accumulator
// ---------------------------------------------------------------------------

internal fun accumulateScaledTime(prev: Double, rawDelta: Long, speed: Float): Double =
    prev + rawDelta.coerceAtLeast(0L) * speed.toDouble()

// ---------------------------------------------------------------------------
// Helical orbit math — "chasing the sun" model
//
// The Sun moves left→right. Planets always LAG BEHIND the sun:
//
//   planet.x = sunX(t) − orbitA · W · (1 + cos(θ)) / 2
//                                  ↑
//              (1+cos)/2 maps [-1,1] → [0,1], always ≥ 0,
//              so the offset is always ≤ 0 relative to the sun.
//              When cos(θ) = −1 (top/bottom of circle) the planet
//              is at maximum lag; when cos(θ) = +1 it is right at
//              the sun's x — never ahead of it.
//
//   planet.y = cy + orbitB · halfH · sin(θ)   — full vertical circle
//   planet.z = cos(θ)                          — depth cue ∈ [-1, 1]
//
// This makes each planet's path a helix that always trails the sun:
// the horizontal component is a damped lag, the vertical component
// is the full orbital circle — exactly the "chasing gravity" look.
// ---------------------------------------------------------------------------

internal fun sunX(tMs: Double, canvasW: Float): Float {
    val periodMs = 8000.0
    val t = ((tMs % periodMs) / periodMs).toFloat().coerceIn(0f, 1f)
    val margin = canvasW * 0.06f
    return margin + t * (canvasW - 2f * margin)
}

internal fun theta(spec: PlanetSpec, tMs: Double): Double =
    spec.phase + spec.angularVel * tMs

/**
 * Absolute canvas position of a planet.
 *
 * Returns Triple(x, y, z):
 *   x = sunX(t) − orbitA · W · (1 + cos(θ)) / 2   ← always ≤ sunX, never ahead
 *   y = cy + orbitB · halfH · sin(θ)                ← vertical helix coil
 *   z = cos(θ)                                       ← depth cue
 */
internal fun helicalPoint(
    spec: PlanetSpec,
    tMs: Double,
    cy: Float,
    halfW: Float,
    halfH: Float,
    canvasW: Float
): Triple<Float, Float, Float> {
    val th  = theta(spec, tMs)
    val sx  = sunX(tMs, canvasW)
    val lag = spec.orbitA * canvasW * ((1.0 + cos(th)) / 2.0).toFloat()   // always ≥ 0
    val x   = sx - lag                                                       // always ≤ sunX
    val y   = cy + spec.orbitB * halfH * sin(th).toFloat()
    val z   = cos(th).toFloat()
    return Triple(x, y, z)
}

// ---------------------------------------------------------------------------
// Depth mappings
// ---------------------------------------------------------------------------

internal fun depthScale(z: Float, min: Float = 0.6f, max: Float = 1.25f): Float =
    min + (max - min) * ((z + 1f) / 2f)

internal fun depthAlpha(z: Float, min: Float = 0.45f): Float =
    min + (1f - min) * ((z + 1f) / 2f)

// ---------------------------------------------------------------------------
// Intensity ramps
// ---------------------------------------------------------------------------

internal fun streakScale(speed: Float): Float = speed

internal fun trailAlpha(step: Int, tailSteps: Int, depthAlpha: Float): Float =
    (1f - step.toFloat() / tailSteps.toFloat()) * 0.28f * depthAlpha

// ---------------------------------------------------------------------------
// Draw-order partition
// ---------------------------------------------------------------------------

internal fun orderByDepth(
    states: List<Pair<PlanetSpec, Triple<Float, Float, Float>>>
): Pair<List<Pair<PlanetSpec, Triple<Float, Float, Float>>>,
        List<Pair<PlanetSpec, Triple<Float, Float, Float>>>> {
    val back  = states.filter { it.second.third < 0f }.sortedBy { it.second.third }
    val front = states.filter { it.second.third >= 0f }.sortedBy { it.second.third }
    return Pair(back, front)
}

// ---------------------------------------------------------------------------
// Config
// ---------------------------------------------------------------------------

/**
 * Three planets — each has a different lag amplitude (orbitA) and orbit radius (orbitB)
 * so their helical trails are visually distinct and never overlap.
 *
 * orbitA = how far behind the sun the planet lags at maximum.
 * Larger orbitA = planet swings further back before being "pulled" to the sun.
 */
internal fun solarSystemPlanets(protein: Color, carbs: Color, fats: Color): List<PlanetSpec> =
    listOf(
        PlanetSpec(orbitA = 0.08f, orbitB = 0.28f, angularVel = 0.0110, phase = 0.0,   color = protein),
        PlanetSpec(orbitA = 0.15f, orbitB = 0.52f, angularVel = 0.0071, phase = 2.094, color = carbs),
        PlanetSpec(orbitA = 0.24f, orbitB = 0.78f, angularVel = 0.0047, phase = 4.189, color = fats),
    )

internal val analyzingCaptions: List<String> = listOf(
    "reading the plate…",
    "counting macros…",
    "weighing portions…",
    "estimating calories…",
    "crunching the numbers…"
)

internal fun analyzingLabel(modelId: String?): String =
    if (!modelId.isNullOrBlank()) "ANALYZING $modelId" else "ANALYZING"
