package com.anant.fitbuddy.data.prompts

import com.anant.fitbuddy.data.region.AppRegion
import com.anant.fitbuddy.data.region.RegionPack
import java.util.concurrent.ConcurrentHashMap

/**
 * Single entry point for LLM prompt text. Templates live under
 * `resources/prompts/` (shared shells + per-region overlays). Edit those files to change
 * prompt wording — not [com.anant.fitbuddy.data.remote.RemoteAiDataSource].
 */
object PromptCatalog {

    private val cache = ConcurrentHashMap<String, String>()

    private val PLACEHOLDER = Regex("""\{\{([A-Z0-9_]+)\}\}""")

    fun load(path: String): String =
        cache.getOrPut(path) {
            val stream = PromptCatalog::class.java.classLoader
                ?.getResourceAsStream(path)
                ?: error("Missing prompt resource: $path")
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }.trim()
        }

    fun render(template: String, vars: Map<String, String>): String =
        PLACEHOLDER.replace(template) { m ->
            vars[m.groupValues[1]].orEmpty()
        }.replace(Regex("\n{3,}"), "\n\n").trim()

    fun regionText(region: AppRegion, fileName: String): String {
        val id = region.name.lowercase()
        return load("prompts/region/$id/$fileName.txt")
    }

    fun analyzePrompt(
        userStateContextJson: String,
        userText: String,
        hasImage: Boolean,
        forceEstimate: Boolean,
        strictClarification: Boolean,
        pack: RegionPack
    ): String {
        val strictBlock = if (strictClarification) {
            load("prompts/shared/fragments/strict_clarification.txt")
        } else {
            ""
        }
        val imageNote = if (hasImage) {
            load("prompts/shared/fragments/image_attached_note.txt")
        } else {
            ""
        }
        val forceNote = if (forceEstimate) {
            load("prompts/shared/fragments/force_estimate_note.txt")
        } else {
            ""
        }
        val portionClause = if (hasImage) {
            "the attached photo makes portion size obvious"
        } else {
            "a count is stated"
        }
        return render(
            load("prompts/shared/analyze.txt"),
            mapOf(
                "ANALYZE_SYSTEM_INTRO" to pack.analyzeSystemIntro,
                "ANALYZE_PROMPT_PRIORS" to pack.analyzePromptPriors,
                "PROMPT_REFERENCE_TABLE" to pack.promptReferenceTable(),
                "PORTION_OBVIOUSNESS_CLAUSE" to portionClause,
                "STRICT_CLARIFICATION_BLOCK" to strictBlock,
                "MEASUREMENT_PROMPT_NOTES" to pack.measurementPromptNotes,
                "USER_STATE_CONTEXT_JSON" to userStateContextJson,
                "IMAGE_NOTE" to imageNote,
                "FORCE_ESTIMATE_NOTE" to forceNote,
                "USER_TEXT" to userText
            )
        )
    }

    fun targetPrompt(contextJson: String, pack: RegionPack): String =
        render(
            load("prompts/shared/target.txt"),
            mapOf(
                "TARGET_SYSTEM_INTRO" to pack.targetSystemIntro,
                "TARGET_COACH_NOTES" to pack.targetCoachNotes,
                "CONTEXT_JSON" to contextJson
            )
        )

    fun progressPrompt(compressedMetrics: String, pack: RegionPack): String =
        render(
            load("prompts/shared/progress.txt"),
            mapOf(
                "PROGRESS_SYSTEM_INTRO" to pack.progressSystemIntro,
                "PROGRESS_FOOD_GUIDANCE" to pack.progressFoodGuidance,
                "COMPRESSED_METRICS" to compressedMetrics
            )
        )

    fun progressChatSystemPrompt(contextJson: String, pack: RegionPack): String =
        render(
            load("prompts/shared/progress_chat.txt"),
            mapOf(
                "PROGRESS_SYSTEM_INTRO" to pack.progressSystemIntro,
                "PROGRESS_FOOD_GUIDANCE" to pack.progressFoodGuidance,
                "CONTEXT_JSON" to contextJson
            )
        )

    fun workoutCaloriesPrompt(contextJson: String): String =
        render(
            load("prompts/shared/workout_calories.txt"),
            mapOf("CONTEXT_JSON" to contextJson)
        )

    fun classifyExercisePrompt(rawName: String, knownExerciseNames: List<String>): String {
        val knownList = knownExerciseNames.joinToString("\n") { "- $it" }
        return render(
            load("prompts/shared/classify_exercise.txt"),
            mapOf(
                "KNOWN_LIST" to knownList,
                "RAW_NAME" to rawName
            )
        )
    }

    fun parseWorkoutPrompt(description: String, knownExerciseNames: List<String>): String {
        val knownList = knownExerciseNames.joinToString("\n") { "- $it" }
        return render(
            load("prompts/shared/parse_workout.txt"),
            mapOf(
                "KNOWN_LIST" to knownList,
                "DESCRIPTION" to description
            )
        )
    }

    fun workoutNamePrompt(exerciseNames: List<String>): String {
        val list = exerciseNames.joinToString("\n") { "- $it" }
        return render(
            load("prompts/shared/workout_name.txt"),
            mapOf("EXERCISE_LIST" to list)
        )
    }

    fun askPortionUserPrompt(dishName: String, askPortionPromptNotes: String): String =
        render(
            load("prompts/shared/ask_portion.txt"),
            mapOf(
                "DISH_NAME" to dishName,
                "ASK_PORTION_PROMPT_NOTES" to askPortionPromptNotes
            )
        )
}
