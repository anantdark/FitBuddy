package com.anant.fitbuddy.ui.loading

import androidx.compose.ui.Modifier

/** Runtime inputs passed to a [LoadingAnimation] when it is displayed. */
data class LoadingAnimationScope(
    val modifier: Modifier = Modifier,
    /** Optional status line (e.g. model id for analyzing). */
    val label: String? = null,
    /** Cycling caption lines; falls back to the animation's defaults when empty. */
    val captions: List<String> = emptyList(),
)
