package com.anant.fitbuddy.ui.loading

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable

/** A registered wait animation that can be picked randomly for one or more slots. */
interface LoadingAnimation {
    val id: String
    /** Human-readable label for developer Settings. */
    @get:StringRes
    val displayNameRes: Int
    val slots: Set<LoadingAnimationSlot>
    /** Default rotating captions for this animation; resolved via stringResource in the host. */
    val defaultCaptionRes: List<Int> get() = emptyList()

    @Composable
    fun Content(scope: LoadingAnimationScope)
}
