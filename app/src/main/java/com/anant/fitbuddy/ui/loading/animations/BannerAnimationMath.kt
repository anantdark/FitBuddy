package com.anant.fitbuddy.ui.loading.animations

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

// ---------------------------------------------------------------------------
// Data model
// ---------------------------------------------------------------------------

/**
 * Elliptical Keplerian orbit around the sun (perspective-flattened).
 *
 * - [orbitA]: semi-major axis as a fraction of canvas width
 * - [orbitB]: semi-minor axis as a fraction of canvas half-height
 * - [angularVel]: rad/ms — strictly decreasing inner → outer (Kepler)
 * - [bodyScale]: relative planet disk size
 * - [hasRing]: faint Saturn-style ring
 */
internal data class PlanetSpec(
    val orbitA: Float,
    val orbitB: Float,
    val angularVel: Double,
    val phase: Double,
    val color: Color,
    val bodyScale: Float = 1f,
    val hasRing: Boolean = false
)

// ---------------------------------------------------------------------------
// Scaled-time accumulator
// ---------------------------------------------------------------------------

internal fun accumulateScaledTime(prev: Double, rawDelta: Long, speed: Float): Double =
    prev + rawDelta.coerceAtLeast(0L) * speed.toDouble()

// ---------------------------------------------------------------------------
// Sun — true left→right end-to-end (planets may clip off-screen)
// ---------------------------------------------------------------------------

/** Sun end-to-end crossing duration in scaled ms (shorter = faster travel). */
internal const val SUN_PERIOD_MS: Double = 4_400.0

/** Calm default playback — sun + planets advance together. */
internal const val BANNER_SPEED_NATURAL: Float = 0.14f

/** Press-and-hold playback — sped-up but still readable. */
internal const val BANNER_SPEED_BOOST: Float = 0.50f

/** Normalized cycle progress in [0, 1]. */
internal fun sunProgress(tMs: Double, periodMs: Double = SUN_PERIOD_MS): Float =
    ((tMs % periodMs) / periodMs).toFloat().coerceIn(0f, 1f)

/**
 * Only softens the sun's wrap teleport. Planets intentionally stay visible while
 * the sun sits on an edge (opposite-side bodies hang into the frame).
 */
internal fun edgeFade(progress: Float, fade: Float = 0.035f): Float {
    val p = progress.coerceIn(0f, 1f)
    val f = fade.coerceIn(0.001f, 0.5f)
    return when {
        p < f -> p / f
        p > 1f - f -> (1f - p) / f
        else -> 1f
    }.coerceIn(0f, 1f)
}

/**
 * Sun X travels nearly edge→edge. Margin is only for the solar disk — outer
 * planets are allowed to leave the canvas.
 */
internal fun sunX(tMs: Double, canvasW: Float): Float {
    val t = sunProgress(tMs)
    val margin = canvasW * 0.045f
    return margin + t * (canvasW - 2f * margin)
}

/** Slight ecliptic bob. */
internal fun sunY(tMs: Double, canvasH: Float): Float {
    val cy = canvasH * 0.44f
    val bob = canvasH * 0.028f
    return cy + bob * sin(2.0 * PI * tMs / (SUN_PERIOD_MS * 0.9) + 0.4).toFloat()
}

internal fun theta(spec: PlanetSpec, tMs: Double): Double =
    spec.phase + spec.angularVel * tMs

/**
 * Planet position on a perspective ellipse around the sun.
 *
 * Returns Triple(x, y, z):
 *   x/y — canvas coords on the ellipse (may be off-screen)
 *   z   — depth cue ∈ [-1, 1]; z ≥ 0 is nearer the camera
 */
internal fun orbitalPoint(
    spec: PlanetSpec,
    tMs: Double,
    sx: Float,
    sy: Float,
    halfH: Float,
    canvasW: Float
): Triple<Float, Float, Float> {
    val th = theta(spec, tMs)
    val cosT = cos(th).toFloat()
    val sinT = sin(th).toFloat()
    val a = spec.orbitA * canvasW
    val b = spec.orbitB * halfH
    val z = sinT
    val foreshorten = 0.86f + 0.14f * ((z + 1f) / 2f)
    val x = sx + a * cosT * foreshorten
    val y = sy + b * sinT
    return Triple(x, y, z)
}

/** Legacy alias — same as [orbitalPoint] with sun derived from time. */
internal fun helicalPoint(
    spec: PlanetSpec,
    tMs: Double,
    cy: Float,
    halfW: Float,
    halfH: Float,
    canvasW: Float
): Triple<Float, Float, Float> {
    val sx = sunX(tMs, canvasW)
    val sy = sunY(tMs, halfH * 2f)
    return orbitalPoint(spec, tMs, sx, sy, halfH, canvasW)
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
    val back = states.filter { it.second.third < 0f }.sortedBy { it.second.third }
    val front = states.filter { it.second.third >= 0f }.sortedBy { it.second.third }
    return Pair(back, front)
}

// ---------------------------------------------------------------------------
// Real solar-system composition (compressed AU spacing, true period ratios)
// ---------------------------------------------------------------------------

/**
 * True semi-major axes in AU. Display radii use a^0.42 compression so all eight
 * fit a banner while preserving inner crowding vs outer sprawl.
 */
private val PLANET_AU = doubleArrayOf(
    0.387,  // Mercury
    0.723,  // Venus
    1.000,  // Earth
    1.524,  // Mars
    5.203,  // Jupiter
    9.537,  // Saturn
    19.191, // Uranus
    30.069  // Neptune
)

/**
 * Sidereal orbital periods in Earth years (IAU / NASA mean values).
 * Angular speeds use ω ∝ 1/P so relative motion matches the real solar system.
 */
internal val PLANET_PERIOD_YEARS = doubleArrayOf(
    0.2408467, // Mercury
    0.6151973, // Venus
    1.0000174, // Earth
    1.8808476, // Mars
    11.862615, // Jupiter
    29.447498, // Saturn
    84.016846, // Uranus
    164.79132  // Neptune (slowest)
)

/**
 * Orbits the slowest planet (Neptune) completes during one sun crossing.
 * Kept low so outer bodies drift calmly while the sun still crosses the banner.
 */
// Kept in step with SUN_PERIOD_MS so planet ω stays calm when the sun is sped up.
internal const val SLOWEST_ORBITS_PER_CROSSING: Double = 0.067

private data class BodyLook(
    val bodyScale: Float,
    val phase: Double,
    val hasRing: Boolean = false
)

/** Orbits completed by [spec] during one full [SUN_PERIOD_MS] crossing. */
internal fun orbitsPerCrossing(spec: PlanetSpec): Double =
    spec.angularVel * SUN_PERIOD_MS / (2.0 * PI)

/**
 * Eight planets with real relative speeds (true sidereal period ratios) and
 * AU-compressed orbits. Tempo is anchored on Neptune at
 * [SLOWEST_ORBITS_PER_CROSSING] per sun pass; inner bodies scale by P_nep / P_i.
 *
 * Indices 0–2 carry injected macro colors.
 */
internal fun solarSystemPlanets(protein: Color, carbs: Color, fats: Color): List<PlanetSpec> {
    val looks = listOf(
        BodyLook(0.48f, 0.40),                 // Mercury — protein
        BodyLook(0.70f, 1.90),                 // Venus  — carbs
        BodyLook(0.76f, 3.20),                 // Earth  — fats
        BodyLook(0.58f, 4.60),                 // Mars
        BodyLook(1.55f, 5.80),                 // Jupiter
        BodyLook(1.35f, 1.10, hasRing = true), // Saturn
        BodyLook(1.05f, 2.70),                 // Uranus
        BodyLook(1.00f, 4.90),                 // Neptune
    )
    val colors = listOf(
        protein,
        carbs,
        fats,
        Color(0xFFE57373),
        Color(0xFFFFCC80),
        Color(0xFFFFE082),
        Color(0xFF80DEEA),
        Color(0xFF64B5F6),
    )

    // Compress AU → banner fraction; Neptune ≈ 0.34 so it hangs off-screen at edges.
    val compress = 0.42
    val raw = PLANET_AU.map { it.pow(compress) }
    val maxRaw = raw.last()
    val orbitScale = 0.34 / maxRaw

    val pSlowest = PLANET_PERIOD_YEARS.last()
    val omegaSlowest = SLOWEST_ORBITS_PER_CROSSING * 2.0 * PI / SUN_PERIOD_MS

    return looks.mapIndexed { i, look ->
        val orbitA = (raw[i] * orbitScale).toFloat()
        // Tip the ellipse: outer orbits flatter (more foreshortened disk view).
        val orbitB = (0.20f + orbitA * 1.85f).coerceAtMost(0.92f)
        // Real relative speeds: ω_i / ω_nep = P_nep / P_i
        val omega = omegaSlowest * (pSlowest / PLANET_PERIOD_YEARS[i])
        PlanetSpec(
            orbitA = orbitA,
            orbitB = orbitB,
            angularVel = omega,
            phase = look.phase,
            color = colors[i],
            bodyScale = look.bodyScale,
            hasRing = look.hasRing
        )
    }
}

internal val analyzingCaptions: List<String> = listOf(
    "reading the plate…",
    "counting macros…",
    "weighing portions…",
    "estimating calories…",
    "crunching the numbers…"
)

internal fun analyzingLabel(modelId: String?): String =
    if (!modelId.isNullOrBlank()) "ANALYZING $modelId" else "ANALYZING"
