package com.anant.fitbuddy.ui.loading

import androidx.compose.runtime.Composable

/** A registered wait animation that can be picked randomly for one or more slots. */
interface LoadingAnimation {
    val id: String
    /** Human-readable label for developer Settings. */
    val displayName: String
    val slots: Set<LoadingAnimationSlot>
    val defaultCaptions: List<String> get() = emptyList()

    @Composable
    fun Content(scope: LoadingAnimationScope)
}
