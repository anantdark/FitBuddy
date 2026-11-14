package com.anant.fitbuddy.data.healthconnect

/**
 * Which Health Connect metric toggles are live in this build.
 *
 * **Import** — FitBuddy must consume the value (Room / profile / AI context).
 * **Export** — FitBuddy must have a local source to write.
 *
 * Unavailable metrics stay greyed in Settings, are forced off in prefs, and are
 * omitted from permission requests.
 */
object HealthConnectMetricAvailability {
    // --- Import (consumed in-app?) ---------------------------------------------
    /** → [com.anant.fitbuddy.data.database.BodyMeasurement] / profile weight. */
    const val importWeight = true
    /** → [com.anant.fitbuddy.data.database.UserProfile.heightCm]. */
    const val importHeight = true
    /** → BodyMeasurement.bodyFatPct (profile + analytics). */
    const val importBodyFat = true
    /** Lean body mass → BodyMeasurement.muscleMassKg (profile + analytics). */
    const val importMuscleMass = true
    /** → ExerciseLog. */
    const val importExercise = true
    /** → ExerciseLog steps burn on dashboard. */
    const val importSteps = true
    /** → AI user-state context (hydration_ml_today). */
    const val importHydration = true
    /** → AI user-state context (sleep_minutes_last_night). */
    const val importSleep = true

    // --- Export (local source exists?) ----------------------------------------
    /** FoodLog meals. */
    const val exportNutrition = true
    /** BodyMeasurement.weightKg. */
    const val exportWeight = true
    /** UserProfile.heightCm. */
    const val exportHeight = true
    /** BodyMeasurement.bodyFatPct. */
    const val exportBodyFat = true
    /** BodyMeasurement.muscleMassKg. */
    const val exportMuscleMass = true
    /** WaterLog → HydrationRecord. */
    const val exportHydration = true
    /** ExerciseLog (non-steps). */
    const val exportExercise = true
}

/** Forces unavailable metric flags off so they never request permissions. */
fun HealthConnectMetricPrefs.sanitized(): HealthConnectMetricPrefs = copy(
    importWeight = importWeight && HealthConnectMetricAvailability.importWeight,
    importHeight = importHeight && HealthConnectMetricAvailability.importHeight,
    importBodyFat = importBodyFat && HealthConnectMetricAvailability.importBodyFat,
    importMuscleMass = importMuscleMass && HealthConnectMetricAvailability.importMuscleMass,
    importExercise = importExercise && HealthConnectMetricAvailability.importExercise,
    importSteps = importSteps && HealthConnectMetricAvailability.importSteps,
    importHydration = importHydration && HealthConnectMetricAvailability.importHydration,
    importSleep = importSleep && HealthConnectMetricAvailability.importSleep,
    exportNutrition = exportNutrition && HealthConnectMetricAvailability.exportNutrition,
    exportWeight = exportWeight && HealthConnectMetricAvailability.exportWeight,
    exportHeight = exportHeight && HealthConnectMetricAvailability.exportHeight,
    exportBodyFat = exportBodyFat && HealthConnectMetricAvailability.exportBodyFat,
    exportMuscleMass = exportMuscleMass && HealthConnectMetricAvailability.exportMuscleMass,
    exportHydration = exportHydration && HealthConnectMetricAvailability.exportHydration,
    exportExercise = exportExercise && HealthConnectMetricAvailability.exportExercise,
)

/**
 * Applies runtime product gates on top of [sanitized].
 * Hydration export requires FitBuddy water intake tracking — otherwise there is nothing to push.
 */
fun HealthConnectMetricPrefs.forSync(waterTrackingEnabled: Boolean): HealthConnectMetricPrefs {
    val base = sanitized()
    return base.copy(
        exportHydration = base.exportHydration && waterTrackingEnabled
    )
}
