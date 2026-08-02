package com.anant.fitbuddy.ui.loading

import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.ui.loading.animations.AppleTreeLoadingAnimation
import com.anant.fitbuddy.ui.loading.animations.JapanRowingLoadingAnimation
import com.anant.fitbuddy.ui.loading.animations.SolarSystemLoadingAnimation
import com.anant.fitbuddy.ui.loading.animations.TirangaInsightLoadingAnimation
import com.anant.fitbuddy.ui.loading.animations.TirangaLoadingAnimation
import kotlin.random.Random

/**
 * Static catalogue of loading animations. Add a new animation by implementing
 * [LoadingAnimation] and appending it to [all].
 */
object LoadingAnimationRegistry {
    val all: List<LoadingAnimation> = listOf(
        SolarSystemLoadingAnimation,
        TirangaLoadingAnimation,
        AppleTreeLoadingAnimation,
        JapanRowingLoadingAnimation,
        TirangaInsightLoadingAnimation,
    )

    fun forSlot(slot: LoadingAnimationSlot): List<LoadingAnimation> =
        all.filter { slot in it.slots }

    fun byId(id: String): LoadingAnimation? =
        all.firstOrNull { it.id == id }

    fun random(slot: LoadingAnimationSlot, rng: Random = Random.Default): LoadingAnimation {
        val eligible = forSlot(slot)
        require(eligible.isNotEmpty()) { "No loading animations registered for $slot" }
        return eligible[rng.nextInt(eligible.size)]
    }

    /**
     * Resolves [choice] for [slot].
     * - [AppSettings.LOADING_ANIM_OFF] → null (caller shows spinner)
     * - [AppSettings.LOADING_ANIM_RANDOM] → random eligible animation
     * - animation id → that animation if it supports [slot], else random for the slot
     */
    fun resolve(
        slot: LoadingAnimationSlot,
        choice: String,
        rng: Random = Random.Default
    ): LoadingAnimation? {
        when (choice) {
            AppSettings.LOADING_ANIM_OFF -> return null
            AppSettings.LOADING_ANIM_RANDOM, "" -> return random(slot, rng)
        }
        val fixed = byId(choice)
        return if (fixed != null && slot in fixed.slots) fixed else random(slot, rng)
    }
}
