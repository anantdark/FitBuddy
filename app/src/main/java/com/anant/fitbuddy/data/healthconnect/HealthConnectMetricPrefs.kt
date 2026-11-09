package com.anant.fitbuddy.data.healthconnect

/**
 * Per-metric Health Connect import/export switches. Master gate is
 * [com.anant.fitbuddy.data.settings.AppSettings.healthConnectEnabled].
 * Defaults are on so enabling HC syncs the full recommended set; users can turn each off.
 */
data class HealthConnectMetricPrefs(
    // --- Import ---
    val importWeight: Boolean = true,
    val importHeight: Boolean = true,
    val importBodyFat: Boolean = true,
    /** Health Connect LeanBodyMass → FitBuddy [BodyMeasurement.muscleMassKg]. */
    val importMuscleMass: Boolean = true,
    val importExercise: Boolean = true,
    val importSteps: Boolean = true,
    val importHydration: Boolean = true,
    val importSleep: Boolean = true,
    // --- Export ---
    val exportNutrition: Boolean = true,
    val exportWeight: Boolean = true,
    val exportHeight: Boolean = true,
    val exportBodyFat: Boolean = true,
    val exportMuscleMass: Boolean = true,
    val exportHydration: Boolean = true,
    val exportExercise: Boolean = true,
) {
    fun anyImport(): Boolean =
        importWeight || importHeight || importBodyFat || importMuscleMass ||
            importExercise || importSteps || importHydration || importSleep

    fun anyExport(): Boolean =
        exportNutrition || exportWeight || exportHeight || exportBodyFat ||
            exportMuscleMass || exportHydration || exportExercise
}
