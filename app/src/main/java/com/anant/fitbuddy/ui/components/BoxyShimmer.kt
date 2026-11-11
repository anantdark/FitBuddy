package com.anant.fitbuddy.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/** Warm Glyph white / dark off / Nothing recording-light red. */
val GlyphLedOn = Color(0xFFFFF6E8)
val GlyphLedOff = Color(0xFF2C2C2C)
val GlyphLedRed = Color(0xFFFF2D2D)

/** Ring shimmer effects; cycles on each Activity ON_START (same as donate-heart color). */
enum class GlyphShimmerStyle { SOFT, COMET, WAVE, TWINKLE, BREATHE }

fun initialGlyphShimmerStyleIndex(): Int =
    (System.currentTimeMillis() % GlyphShimmerStyle.entries.size).toInt()

fun nextGlyphShimmerStyleIndex(current: Int): Int =
    (current + 1) % GlyphShimmerStyle.entries.size

fun glyphShimmerStyle(index: Int): GlyphShimmerStyle =
    GlyphShimmerStyle.entries[index.mod(GlyphShimmerStyle.entries.size)]

/** Process-wide shimmer index; advanced from MainScreen with the donate-heart color. */
object GlyphShimmerCycle {
    var index by mutableIntStateOf(initialGlyphShimmerStyleIndex())
        private set

    val style: GlyphShimmerStyle
        get() = glyphShimmerStyle(index)

    fun advance() {
        index = nextGlyphShimmerStyleIndex(index)
    }
}

/** Continuous shimmer phase, 0..2π. */
@Composable
fun rememberLedShimmerPhase(durationMillis: Int = 3200, enabled: Boolean = true): Float {
    if (!enabled) return 0f
    val transition = rememberInfiniteTransition(label = "glyphShimmer")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "glyphShimmerPhase",
    )
    return phase
}

/** One sharp square Glyph cell. */
data class GlyphBoxCell(
    val center: Offset,
    val size: Float,
    /** Rotation in degrees (path tangent); 0 = axis-aligned. */
    val degrees: Float,
)

/**
 * Square ring: equal square boxes per side, corner-to-corner.
 * Box size is resized so [nPerSide] tiles fit each side exactly; stroke
 * thickness matches the box so edges are flush.
 *
 * Returns (boxSize, cells in clockwise order starting at top-middle).
 */
fun buildSquareGlyphCells(
    center: Offset,
    available: Float,
    targetStroke: Float,
): Pair<Float, List<GlyphBoxCell>> {
    // avail = (n+1)*box  →  box edges reach the outer bound of [available].
    // Even n puts a box exactly on the top-middle line, where the ring starts.
    val n = ((available / targetStroke - 1f).roundToInt() / 2 * 2).coerceIn(2, 24)
    val box = available / (n + 1f)
    val halfStroke = box * 0.5f
    // Centerline square inset so box outer edges hit available/2 from center.
    val half = available * 0.5f - halfStroke
    val left = center.x - half
    val top = center.y - half
    val right = center.x + half
    val bottom = center.y + half
    // Centerline side = n * box, so n + 1 boxes sit on each side with corner boxes centered on
    // the corners. Each side emits n boxes (start corner included, end corner left to the next side).
    val cells = ArrayList<GlyphBoxCell>(4 * n)
    for (i in n / 2 until n) {
        cells += GlyphBoxCell(Offset(left + box * i, top), box, 0f)
    }
    for (i in 0 until n) {
        cells += GlyphBoxCell(Offset(right, top + box * i), box, 0f)
    }
    for (i in 0 until n) {
        cells += GlyphBoxCell(Offset(right - box * i, bottom), box, 0f)
    }
    for (i in 0 until n) {
        cells += GlyphBoxCell(Offset(left, bottom - box * i), box, 0f)
    }
    for (i in 0 until n / 2) {
        cells += GlyphBoxCell(Offset(left + box * i, top), box, 0f)
    }
    return box to cells
}

/**
 * Circular ring: equal square boxes around the centerline.
 * Box size resized so [count] tiles span the full circumference.
 */
fun buildCircleGlyphCells(
    center: Offset,
    available: Float,
    targetStroke: Float,
): Pair<Float, List<GlyphBoxCell>> {
    // B = avail / (n/PI + 1) so outer radius = avail/2 and n*B = circumference.
    val approxN = (PI.toFloat() * (available / targetStroke - 1f)).roundToInt().coerceIn(16, 72)
    val box = available / (approxN / PI.toFloat() + 1f)
    val radius = available * 0.5f - box * 0.5f
    val circumference = (2f * PI.toFloat() * radius).coerceAtLeast(1f)
    val count = (circumference / box).roundToInt().coerceIn(16, 96)
    // Re-fit box so count * box == circumference exactly (true equal split).
    val fitted = circumference / count
    // Keep squares: use fitted for along-path pitch; cross size = fitted too,
    // and re-derive radius so outer edge still matches available.
    val outer = available * 0.5f
    val r = outer - fitted * 0.5f
    val cells = ArrayList<GlyphBoxCell>(count)
    for (i in 0 until count) {
        // Start at top (-90°) and go clockwise.
        val deg = -90f + 360f * (i + 0.5f) / count
        val rad = Math.toRadians(deg.toDouble())
        val c = Offset(
            x = center.x + r * cos(rad).toFloat(),
            y = center.y + r * sin(rad).toFloat(),
        )
        // Tangent for a circle clockwise: angle + 90°.
        cells += GlyphBoxCell(
            center = c,
            size = fitted,
            degrees = deg + 90f,
        )
    }
    return fitted to cells
}

/**
 * Draw equal sharp square Glyph boxes with the previous soft shimmer.
 * End lit box is Nothing red.
 */
fun DrawScope.drawGlyphBoxes(
    cells: List<GlyphBoxCell>,
    fillFrac: Float,
    phase: Float,
    onColor: Color = GlyphLedOn,
    offColor: Color = GlyphLedOff,
    endColor: Color = GlyphLedRed,
    shimmerEnabled: Boolean = true,
    style: GlyphShimmerStyle = GlyphShimmerStyle.SOFT,
) {
    if (cells.isEmpty()) return
    val litCount = (cells.size * fillFrac.coerceIn(0f, 1f))
        .let { kotlin.math.ceil(it.toDouble()).toInt() }
        .coerceIn(0, cells.size)

    cells.forEachIndexed { i, cell ->
        val lit = i < litCount
        val isEnd = lit && i == litCount - 1
        val shimmer = if (shimmerEnabled) glyphShimmer(style, i, cells.size, phase) else 1f
        val color = when {
            isEnd -> endColor.copy(alpha = shimmer)
            lit -> onColor.copy(alpha = shimmer)
            else -> offColor.copy(alpha = 0.90f)
        }
        val half = cell.size * 0.5f
        rotate(degrees = cell.degrees, pivot = cell.center) {
            drawRect(
                color = color,
                topLeft = Offset(cell.center.x - half, cell.center.y - half),
                size = Size(cell.size, cell.size),
            )
        }
    }
}

private fun scatter(index: Int): Float {
    val seed = index * 12.9898f + 78.233f
    val v = (sin(seed.toDouble()) * 43758.5453).toFloat()
    return v - floor(v)
}

/** Brightness alpha (0.25..1) for cell [index] of [count] at [phase] (0..2π). */
private fun glyphShimmer(style: GlyphShimmerStyle, index: Int, count: Int, phase: Float): Float {
    val twoPi = (2f * PI).toFloat()
    val pos = index.toFloat() / count
    val turn = phase / twoPi
    return when (style) {
        GlyphShimmerStyle.SOFT -> {
            val frac = scatter(index)
            val pulse = sin((phase * (0.7f + frac) + frac * 6.28f).toDouble()).toFloat()
            (0.88f + pulse * 0.12f).coerceIn(0.80f, 1f)
        }
        GlyphShimmerStyle.COMET -> {
            // Bright head circling the ring with a fading tail behind it.
            val behind = (turn - pos).let { it - floor(it) }
            val tail = (1f - behind / 0.28f).coerceIn(0f, 1f)
            0.55f + 0.45f * tail * tail
        }
        GlyphShimmerStyle.WAVE -> {
            val w = sin((phase - pos * twoPi * 3f).toDouble()).toFloat()
            0.72f + 0.28f * w
        }
        GlyphShimmerStyle.TWINKLE -> {
            val frac = scatter(index)
            val t = (turn * 2f + frac).let { it - floor(it) }
            val spark = (1f - kotlin.math.abs(t - 0.5f) * 8f).coerceIn(0f, 1f)
            0.62f + 0.38f * spark
        }
        GlyphShimmerStyle.BREATHE -> {
            0.65f + 0.35f * (0.5f + 0.5f * sin((phase - PI / 2).toDouble()).toFloat())
        }
    }
}
