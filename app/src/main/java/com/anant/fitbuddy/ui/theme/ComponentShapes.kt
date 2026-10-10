package com.anant.fitbuddy.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.anant.fitbuddy.data.settings.AppComponentStyle

/** Current component silhouette; defaults to Material rounded. */
val LocalComponentStyle = staticCompositionLocalOf { AppComponentStyle.MATERIAL }

private val SharpCorner = RoundedCornerShape(0.dp)

/** Soft Material You–style corners (cards, buttons, sheets, dialogs). */
val MaterialAppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

/** Sharp, flat panels — zero radius at every Material shape token. */
val MakoAppShapes = Shapes(
    extraSmall = SharpCorner,
    small = SharpCorner,
    medium = SharpCorner,
    large = SharpCorner,
    extraLarge = SharpCorner
)

fun shapesFor(style: AppComponentStyle): Shapes = when (style) {
    AppComponentStyle.MATERIAL -> MaterialAppShapes
    AppComponentStyle.MAKO -> MakoAppShapes
}

@Composable
@ReadOnlyComposable
fun isMakoStyle(): Boolean =
    LocalComponentStyle.current == AppComponentStyle.MAKO

/**
 * Control shape: keep the Material default (often a pill) in Material mode;
 * force sharp corners in Mako (M3 ButtonDefaults stay rounded even when theme shapes are sharp).
 */
@Composable
fun appControlShape(materialDefault: Shape): Shape =
    if (isMakoStyle()) SharpCorner else materialDefault

/**
 * Ad-hoc corner shape that respects [LocalComponentStyle].
 * Use instead of hard-coded [RoundedCornerShape] so Mako mode stays sharp.
 */
@Composable
fun appShape(corner: Dp): Shape =
    if (isMakoStyle()) {
        SharpCorner
    } else {
        RoundedCornerShape(corner)
    }

/** Asymmetric corners (e.g. chat bubbles); all zero in Mako mode. */
@Composable
fun appShape(
    topStart: Dp,
    topEnd: Dp,
    bottomEnd: Dp,
    bottomStart: Dp
): Shape =
    if (isMakoStyle()) {
        SharpCorner
    } else {
        RoundedCornerShape(topStart, topEnd, bottomEnd, bottomStart)
    }

/** Canvas corner radius in px — 0 in Mako. */
@Composable
@ReadOnlyComposable
fun appCornerRadiusPx(materialPx: Float): Float =
    if (isMakoStyle()) 0f else materialPx
