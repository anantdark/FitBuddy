package com.anant.fitbuddy.data.repository

import com.anant.fitbuddy.data.model.FoodDraft

/**
 * Result of routing an LLM payload. This is the clean contract the UI/ViewModel consume,
 * hiding the raw "status" string and null-handling of the API response behind a sealed type.
 */
sealed interface AnalysisOutcome {

    /**
     * Food was recognised. NOT yet persisted: the UI shows the editable ingredient/macro
     * breakdown for review, and only saves when the user confirms.
     */
    data class FoodReady(
        val draft: FoodDraft
    ) : AnalysisOutcome

    /** Exercise was recognised and persisted to Room. */
    data class ExerciseSaved(
        val activityName: String,
        val caloriesBurned: Int
    ) : AnalysisOutcome

    /** The AI needs more detail before it can log anything. */
    data class NeedsClarification(
        val message: String
    ) : AnalysisOutcome

    /** No food/activity could be identified (e.g. a photo with no food in it). Shown as a dialog. */
    data class NotIdentified(
        val message: String
    ) : AnalysisOutcome

    /** Something went wrong (network, parsing, or an unknown status). Shown as a blocking dialog. */
    data class Error(
        val message: String
    ) : AnalysisOutcome
}
