package com.anant.fitbuddy.data.model

/** A curated exercise suggestion for the workout logger's picker (name + equipment tag). */
data class CommonExercise(val name: String, val equipment: String)

/**
 * Curated list of common gym exercises for the "Add exercise" picker. Includes the dumbbell P/P/L
 * program from Updated_Dumbbell_PPL_Workout_Guide.pdf plus general gym staples.
 */
val COMMON_EXERCISES: List<CommonExercise> = listOf(
    // Dumbbell — push/pull/legs guide
    CommonExercise("Dumbbell Bench Press", Equipment.DUMBBELL),
    CommonExercise("Incline Dumbbell Press", Equipment.DUMBBELL),
    CommonExercise("Seated Shoulder Press", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Shoulder Press", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Lateral Raise", Equipment.DUMBBELL),
    CommonExercise("Overhead Tricep Extension", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Tricep Extension", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Chest Fly", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Pullover", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Incline Row", Equipment.DUMBBELL),
    CommonExercise("One-Arm Dumbbell Row", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Row", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Rear Delt Fly", Equipment.DUMBBELL),
    CommonExercise("Seated Dumbbell Bicep Curl", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Bicep Curl", Equipment.DUMBBELL),
    CommonExercise("Zottman Curl", Equipment.DUMBBELL),
    CommonExercise("Hammer Curl", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Front Raise", Equipment.DUMBBELL),
    CommonExercise("Goblet Squat", Equipment.DUMBBELL),
    CommonExercise("1.5-Rep Goblet Squat", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Deficit Reverse Lunge", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Lunge", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Bulgarian Split Squat", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Deadlift", Equipment.DUMBBELL),
    CommonExercise("Dumbbell Shrug", Equipment.DUMBBELL),
    // Bench (barbell/dumbbell movements done on a bench)
    CommonExercise("Bench Press", Equipment.BENCH),
    CommonExercise("Incline Press", Equipment.BENCH),
    CommonExercise("Decline Press", Equipment.BENCH),
    CommonExercise("Close-Grip Bench Press", Equipment.BENCH),
    CommonExercise("Bench Tricep Dip", Equipment.BENCH),
    CommonExercise("Bulgarian Split Squat", Equipment.BENCH),
    CommonExercise("Calf Raise on Bench Step", Equipment.BENCH),
    CommonExercise("Step-Up", Equipment.BENCH),
    // Barbell
    CommonExercise("Barbell Squat", Equipment.BARBELL),
    CommonExercise("Barbell Deadlift", Equipment.BARBELL),
    CommonExercise("Barbell Row", Equipment.BARBELL),
    CommonExercise("Overhead Press", Equipment.BARBELL),
    CommonExercise("Barbell Curl", Equipment.BARBELL),
    // Bodyweight
    CommonExercise("Push-Up", Equipment.BODYWEIGHT),
    CommonExercise("Pull-Up", Equipment.BODYWEIGHT),
    CommonExercise("Hanging Leg Raise", Equipment.BODYWEIGHT),
    CommonExercise("Hanging Knee Raise", Equipment.BODYWEIGHT),
    CommonExercise("Seated Knee Tucks (Ins and Outs)", Equipment.BODYWEIGHT),
    CommonExercise("Bodyweight Squat", Equipment.BODYWEIGHT),
    CommonExercise("Plank", Equipment.BODYWEIGHT),
    CommonExercise("Lunge", Equipment.BODYWEIGHT),
    CommonExercise("Crunch", Equipment.BODYWEIGHT),
    // Machine
    CommonExercise("Lat Pulldown", Equipment.MACHINE),
    CommonExercise("Leg Press", Equipment.MACHINE),
    CommonExercise("Leg Extension", Equipment.MACHINE),
    CommonExercise("Leg Curl", Equipment.MACHINE),
    CommonExercise("Cable Row", Equipment.MACHINE),
    CommonExercise("Chest Press Machine", Equipment.MACHINE),
    // Cardio
    CommonExercise("Running", Equipment.CARDIO),
    CommonExercise("Jogging", Equipment.CARDIO),
    CommonExercise("Treadmill Run", Equipment.CARDIO),
    CommonExercise("Stationary Bike", Equipment.CARDIO),
    CommonExercise("Rowing Machine", Equipment.CARDIO),
    CommonExercise("Jump Rope", Equipment.CARDIO)
)

/** Equipment groups in display order for the picker's filter chips. */
val EXERCISE_EQUIPMENT_GROUPS: List<String> = listOf(
    Equipment.DUMBBELL,
    Equipment.BENCH,
    Equipment.BARBELL,
    Equipment.BODYWEIGHT,
    Equipment.MACHINE,
    Equipment.CARDIO
)

/** Merges the built-in library with user-saved custom exercises, deduped by name (case-insensitive). */
fun buildExercisePickerList(customNames: List<Pair<String, String>>): List<CommonExercise> {
    val seen = mutableSetOf<String>()
    val result = mutableListOf<CommonExercise>()
    for (exercise in COMMON_EXERCISES) {
        if (seen.add(exercise.name.lowercase())) result.add(exercise)
    }
    for ((name, equipment) in customNames.sortedBy { it.first.lowercase() }) {
        if (seen.add(name.lowercase())) result.add(CommonExercise(name, equipment))
    }
    return result
}
