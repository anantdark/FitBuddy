package com.anant.fitbuddy.data.model

/**
 * Offline seed used until the ExerciseDB catalog is fetched from jsDelivr. Same curated set as
 * the former hardcoded picker.
 */
val COMMON_EXERCISES_SEED: List<CatalogExercise> = listOf(
    CatalogExercise("Dumbbell Bench Press", Equipment.DUMBBELL),
    CatalogExercise("Incline Dumbbell Press", Equipment.DUMBBELL),
    CatalogExercise("Seated Shoulder Press", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Shoulder Press", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Lateral Raise", Equipment.DUMBBELL),
    CatalogExercise("Overhead Tricep Extension", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Tricep Extension", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Chest Fly", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Pullover", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Incline Row", Equipment.DUMBBELL),
    CatalogExercise("One-Arm Dumbbell Row", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Row", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Rear Delt Fly", Equipment.DUMBBELL),
    CatalogExercise("Seated Dumbbell Bicep Curl", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Bicep Curl", Equipment.DUMBBELL),
    CatalogExercise("Zottman Curl", Equipment.DUMBBELL),
    CatalogExercise("Hammer Curl", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Front Raise", Equipment.DUMBBELL),
    CatalogExercise("Goblet Squat", Equipment.DUMBBELL),
    CatalogExercise("1.5-Rep Goblet Squat", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Deficit Reverse Lunge", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Lunge", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Bulgarian Split Squat", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Deadlift", Equipment.DUMBBELL),
    CatalogExercise("Dumbbell Shrug", Equipment.DUMBBELL),
    CatalogExercise("Bench Press", Equipment.BENCH),
    CatalogExercise("Incline Press", Equipment.BENCH),
    CatalogExercise("Decline Press", Equipment.BENCH),
    CatalogExercise("Close-Grip Bench Press", Equipment.BENCH),
    CatalogExercise("Bench Tricep Dip", Equipment.BENCH),
    CatalogExercise("Bulgarian Split Squat", Equipment.BENCH),
    CatalogExercise("Calf Raise on Bench Step", Equipment.BENCH),
    CatalogExercise("Step-Up", Equipment.BENCH),
    CatalogExercise("Barbell Squat", Equipment.BARBELL),
    CatalogExercise("Barbell Deadlift", Equipment.BARBELL),
    CatalogExercise("Barbell Row", Equipment.BARBELL),
    CatalogExercise("Overhead Press", Equipment.BARBELL),
    CatalogExercise("Barbell Curl", Equipment.BARBELL),
    CatalogExercise("Push-Up", Equipment.BODYWEIGHT),
    CatalogExercise("Pull-Up", Equipment.BODYWEIGHT),
    CatalogExercise("Hanging Leg Raise", Equipment.BODYWEIGHT),
    CatalogExercise("Hanging Knee Raise", Equipment.BODYWEIGHT),
    CatalogExercise("Seated Knee Tucks (Ins and Outs)", Equipment.BODYWEIGHT),
    CatalogExercise("Bodyweight Squat", Equipment.BODYWEIGHT),
    CatalogExercise("Plank", Equipment.BODYWEIGHT),
    CatalogExercise("Lunge", Equipment.BODYWEIGHT),
    CatalogExercise("Crunch", Equipment.BODYWEIGHT),
    CatalogExercise("Lat Pulldown", Equipment.MACHINE),
    CatalogExercise("Leg Press", Equipment.MACHINE),
    CatalogExercise("Leg Extension", Equipment.MACHINE),
    CatalogExercise("Leg Curl", Equipment.MACHINE),
    CatalogExercise("Cable Row", Equipment.MACHINE),
    CatalogExercise("Chest Press Machine", Equipment.MACHINE),
    CatalogExercise("Running", Equipment.CARDIO),
    CatalogExercise("Jogging", Equipment.CARDIO),
    CatalogExercise("Treadmill Run", Equipment.CARDIO),
    CatalogExercise("Stationary Bike", Equipment.CARDIO),
    CatalogExercise("Rowing Machine", Equipment.CARDIO),
    CatalogExercise("Jump Rope", Equipment.CARDIO)
)

/**
 * Merges catalog exercises with user-saved custom presets (deduped by name, case-insensitive).
 * Customs without a catalog match are appended.
 */
fun mergeCatalogWithCustoms(
    catalog: List<CatalogExercise>,
    customNames: List<Pair<String, String>>
): List<CatalogExercise> {
    val seen = mutableSetOf<String>()
    val result = mutableListOf<CatalogExercise>()
    for (exercise in catalog) {
        if (seen.add(exercise.name.lowercase())) result.add(exercise)
    }
    for ((name, equipment) in customNames.sortedBy { it.first.lowercase() }) {
        if (seen.add(name.lowercase())) {
            result.add(CatalogExercise(name = name, equipmentTag = equipment))
        }
    }
    return result
}
