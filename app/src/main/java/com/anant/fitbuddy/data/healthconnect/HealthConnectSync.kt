package com.anant.fitbuddy.data.healthconnect

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.BodyMeasurementDao
import com.anant.fitbuddy.data.database.ExerciseLog
import com.anant.fitbuddy.data.database.ExerciseLogDao
import com.anant.fitbuddy.data.database.FoodLog
import com.anant.fitbuddy.data.database.FoodLogDao
import com.anant.fitbuddy.data.database.UserProfileDao
import com.anant.fitbuddy.data.database.WaterLogDao
import com.anant.fitbuddy.data.settings.SettingsRepository
import com.anant.fitbuddy.util.DateUtils
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

private val Context.healthConnectSyncStore: DataStore<Preferences> by preferencesDataStore(
    name = "health_connect_sync"
)

data class HealthConnectSyncResult(
    val mealsPushed: Int = 0,
    val weightsImported: Int = 0,
    val bodyFatImported: Int = 0,
    val heightUpdated: Boolean = false,
    val exercisesImported: Int = 0,
    val stepsDaysImported: Int = 0,
    val exercisesExported: Int = 0,
    val error: String? = null
)

/** Latest hydration/sleep pulled from Health Connect for AI user-state context. */
data class HealthConnectWellnessContext(
    val hydrationMlToday: Double? = null,
    val hydrationDate: String? = null,
    val sleepMinutesLastNight: Int? = null,
    val sleepEndedDate: String? = null
)

/**
 * Mirrors FitBuddy data out to Health Connect and pulls companion data in,
 * gated by [AppSettings.healthConnectEnabled] + [HealthConnectMetricPrefs].
 */
class HealthConnectSync(
    private val context: Context,
    private val manager: HealthConnectManager,
    private val settingsRepository: SettingsRepository,
    private val foodLogDao: FoodLogDao,
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val userProfileDao: UserProfileDao,
    private val waterLogDao: WaterLogDao,
) {
    private val writer = HealthConnectWriter(manager)
    private val reader = HealthConnectReader(manager)
    private val mutex = Mutex()

    suspend fun wellnessContext(): HealthConnectWellnessContext {
        val prefs = context.healthConnectSyncStore.data.first()
        return HealthConnectWellnessContext(
            hydrationMlToday = prefs[KEY_HYDRATION_ML],
            hydrationDate = prefs[KEY_HYDRATION_DATE],
            sleepMinutesLastNight = prefs[KEY_SLEEP_MINUTES],
            sleepEndedDate = prefs[KEY_SLEEP_DATE]
        )
    }

    suspend fun mirrorNutrition(log: FoodLog) {
        if (!exportEnabled { it.exportNutrition }) return
        runCatching { writer.writeNutrition(log) }
            .onFailure { Log.w(TAG, "mirror nutrition failed: ${it.message}") }
    }

    suspend fun deleteNutrition(foodLogId: Int) {
        if (!exportEnabled { it.exportNutrition }) return
        runCatching { writer.deleteNutrition(foodLogId) }
            .onFailure { Log.w(TAG, "delete nutrition failed: ${it.message}") }
    }

    suspend fun mirrorMeasurement(measurement: BodyMeasurement) {
        if (!autoSyncEnabled()) return
        val prefs = metricPrefs() ?: return
        if (prefs.exportWeight) {
            runCatching { writer.writeWeight(measurement) }
                .onFailure { Log.w(TAG, "mirror weight failed: ${it.message}") }
        }
        if (prefs.exportBodyFat && measurement.bodyFatPct != null) {
            runCatching { writer.writeBodyFat(measurement) }
                .onFailure { Log.w(TAG, "mirror body fat failed: ${it.message}") }
        }
        if (prefs.exportMuscleMass && measurement.muscleMassKg != null) {
            runCatching { writer.writeMuscleMass(measurement) }
                .onFailure { Log.w(TAG, "mirror muscle mass failed: ${it.message}") }
        }
    }

    suspend fun deleteMeasurement(measurement: BodyMeasurement) {
        if (!autoSyncEnabled()) return
        val prefs = metricPrefs() ?: return
        if (prefs.exportWeight) {
            runCatching { writer.deleteWeight(measurement.id) }
        }
        if (prefs.exportBodyFat) {
            runCatching { writer.deleteBodyFat(measurement.id) }
        }
        if (prefs.exportMuscleMass) {
            runCatching { writer.deleteMuscleMass(measurement.id) }
        }
    }

    suspend fun mirrorHeight(heightCm: Double) {
        if (!exportEnabled { it.exportHeight }) return
        runCatching { writer.writeHeight(heightCm) }
            .onFailure { Log.w(TAG, "mirror height failed: ${it.message}") }
    }

    suspend fun mirrorExercise(log: ExerciseLog) {
        if (!exportEnabled { it.exportExercise }) return
        if (HealthConnectSync.isStepsLogName(log.activityName)) return
        runCatching { writer.writeExercise(log) }
            .onFailure { Log.w(TAG, "mirror exercise failed: ${it.message}") }
    }

    suspend fun deleteExercise(exerciseLogId: Int) {
        if (!exportEnabled { it.exportExercise }) return
        runCatching { writer.deleteExercise(exerciseLogId) }
            .onFailure { Log.w(TAG, "delete exercise failed: ${it.message}") }
    }

    suspend fun mirrorHydration(entryId: Long, milliliters: Double, timestampMs: Long) {
        if (!exportEnabled { it.exportHydration }) return
        runCatching { writer.writeHydration(entryId, milliliters, timestampMs) }
            .onFailure { Log.w(TAG, "mirror hydration failed: ${it.message}") }
    }

    suspend fun deleteHydration(entryId: Long) {
        if (!exportEnabled { it.exportHydration }) return
        runCatching { writer.deleteHydration(entryId) }
    }

    /**
     * Pulls current Health Connect grants, turns off prefs for revoked types, and disables
     * the master sync switch when nothing usable remains. Runs on every Sync now / auto-sync
     * (including when the data pull is debounced).
     *
     * @return aligned metrics, or `null` when sync must stop (permissions revoked).
     */
    private suspend fun reconcilePermissions(): HealthConnectMetricPrefs? {
        val settings = settingsRepository.settings.first()
        val waterOn = settings.waterTrackingEnabled
        val granted = manager.grantedPermissions()
        val aligned = manager.alignToGranted(
            settings.healthConnectMetrics, granted, waterOn
        )
        val visible = manager.visibilityFromGrants(granted, waterOn)
        val noUsableGrants = !visible.anyImport() && !visible.anyExport()
        if (
            aligned != settings.healthConnectMetrics ||
            (noUsableGrants && settings.healthConnectEnabled)
        ) {
            settingsRepository.update {
                it.copy(
                    healthConnectMetrics = aligned,
                    healthConnectEnabled = it.healthConnectEnabled && !noUsableGrants
                )
            }
        }
        if (noUsableGrants) {
            Log.i(TAG, "HC permissions empty or unusable — sync disabled")
            return null
        }
        // Runtime gate: never push hydration when water tracking is off.
        return aligned.forSync(waterOn)
    }

    suspend fun syncNow(force: Boolean = true): HealthConnectSyncResult = mutex.withLock {
        val settings = settingsRepository.settings.first()
        if (!settings.healthConnectEnabled) {
            return HealthConnectSyncResult(error = "Health Connect is off")
        }
        if (!manager.isAvailable()) {
            return HealthConnectSyncResult(error = "Health Connect is unavailable on this device")
        }

        // Always refresh OS grants first (even when the data window is debounced).
        val metrics = reconcilePermissions()
            ?: return HealthConnectSyncResult(error = ERROR_PERMISSIONS_REVOKED)

        val now = System.currentTimeMillis()
        val syncPrefs = context.healthConnectSyncStore.data.first()
        val lastSyncAt = syncPrefs[KEY_LAST_SYNC_AT] ?: 0L
        if (!force && lastSyncAt > 0L && now - lastSyncAt < MIN_AUTO_SYNC_GAP_MS) {
            return HealthConnectSyncResult()
        }

        // First sync or manual Sync now: full window. Auto sync: since last success
        // (tiny overlap covers clock skew / in-flight writes).
        val bootstrapFrom = now - TimeUnit.DAYS.toMillis(SYNC_WINDOW_DAYS)
        val fromMs = when {
            force || lastSyncAt <= 0L -> bootstrapFrom
            else -> maxOf(0L, lastSyncAt - INCREMENTAL_OVERLAP_MS)
        }
        val from = Instant.ofEpochMilli(fromMs)
        val to = Instant.ofEpochMilli(now)

        var mealsPushed = 0
        var weightsImported = 0
        var bodyFatImported = 0
        var heightUpdated = false
        var exercisesImported = 0
        var stepsDaysImported = 0
        var exercisesExported = 0
        var error: String? = null

        runCatching {
            if (metrics.exportNutrition) mealsPushed = pushRecentMeals(fromMs)
            if (metrics.exportWeight || metrics.exportBodyFat || metrics.exportMuscleMass) {
                pushRecentMeasurements(fromMs, metrics)
            }
            if (metrics.exportHeight) {
                userProfileDao.getProfileOnce()?.heightCm?.takeIf { it > 0 }?.let { heightCm ->
                    val lastHeight = syncPrefs[KEY_LAST_EXPORTED_HEIGHT_CM]
                    if (lastHeight == null || kotlin.math.abs(lastHeight - heightCm) >= 0.1) {
                        if (writer.writeHeight(heightCm)) {
                            context.healthConnectSyncStore.edit { prefs ->
                                prefs[KEY_LAST_EXPORTED_HEIGHT_CM] = heightCm
                            }
                        }
                    }
                }
            }
            if (metrics.exportExercise) {
                exercisesExported = pushRecentExercises(fromMs)
            }
            if (metrics.exportHydration) {
                pushRecentHydration(fromMs)
            }
            if (metrics.importWeight) weightsImported = importWeights(from, to)
            if (metrics.importBodyFat) bodyFatImported = importBodyFats(from, to)
            if (metrics.importMuscleMass) importMuscleMasses(from, to)
            if (metrics.importHeight) heightUpdated = importHeight(from, to)
            if (metrics.importExercise) exercisesImported = importExercises(from, to)
            if (metrics.importSteps) stepsDaysImported = importDailySteps(from, to)
            refreshWellnessContext(metrics)

            context.healthConnectSyncStore.edit { prefs ->
                prefs[KEY_LAST_SYNC_AT] = now
            }
        }.onFailure {
            Log.w(TAG, "sync failed: ${it.message}", it)
            error = when (it) {
                is SecurityException -> {
                    reconcilePermissions()
                    ERROR_PERMISSIONS_REVOKED
                }
                else -> it.message ?: "Sync failed"
            }
        }

        HealthConnectSyncResult(
            mealsPushed = mealsPushed,
            weightsImported = weightsImported,
            bodyFatImported = bodyFatImported,
            heightUpdated = heightUpdated,
            exercisesImported = exercisesImported,
            stepsDaysImported = stepsDaysImported,
            exercisesExported = exercisesExported,
            error = error
        )
    }

    /** Live mirror / foreground pull — requires master + auto-sync. Sync now bypasses this. */
    private suspend fun autoSyncEnabled(): Boolean {
        val s = settingsRepository.settings.first()
        return s.healthConnectEnabled && s.healthConnectAutoSync
    }

    private suspend fun metricPrefs(): HealthConnectMetricPrefs? {
        val s = settingsRepository.settings.first()
        return if (s.healthConnectEnabled) {
            s.healthConnectMetrics.forSync(s.waterTrackingEnabled)
        } else {
            null
        }
    }

    private suspend fun exportEnabled(flag: (HealthConnectMetricPrefs) -> Boolean): Boolean {
        if (!autoSyncEnabled()) return false
        val prefs = metricPrefs() ?: return false
        return flag(prefs)
    }

    private suspend fun pushRecentMeals(fromMs: Long): Int {
        if (!manager.hasNutritionWrite()) return 0
        var count = 0
        for (log in foodLogDao.getAllOnce()) {
            if (log.timestamp < fromMs) continue
            if (writer.writeNutrition(log)) count++
        }
        return count
    }

    private suspend fun pushRecentMeasurements(fromMs: Long, metrics: HealthConnectMetricPrefs) {
        for (m in bodyMeasurementDao.getAllOnce()) {
            if (m.timestamp < fromMs) continue
            if (metrics.exportWeight) writer.writeWeight(m)
            if (metrics.exportBodyFat && m.bodyFatPct != null) writer.writeBodyFat(m)
            if (metrics.exportMuscleMass && m.muscleMassKg != null) writer.writeMuscleMass(m)
        }
    }

    private suspend fun pushRecentExercises(fromMs: Long): Int {
        if (!manager.hasExerciseWrite()) return 0
        var count = 0
        for (log in exerciseLogDao.getAllOnce()) {
            if (log.timestamp < fromMs) continue
            if (isStepsLogName(log.activityName)) continue
            if (writer.writeExercise(log)) count++
        }
        return count
    }

    private suspend fun pushRecentHydration(fromMs: Long): Int {
        if (!manager.hasHydrationWrite()) return 0
        var count = 0
        for (log in waterLogDao.getAllOnce()) {
            if (log.timestamp < fromMs) continue
            if (log.amountMl <= 0) continue
            if (
                writer.writeHydration(
                    entryId = log.id.toLong(),
                    milliliters = log.amountMl.toDouble(),
                    timestampMs = log.timestamp
                )
            ) {
                count++
            }
        }
        return count
    }

    private suspend fun importWeights(from: Instant, to: Instant): Int {
        if (!manager.hasWeightRead()) return 0
        var inserted = 0
        for (sample in reader.readWeights(from, to)) {
            if (sample.weightKg <= 0) continue
            val existing = bodyMeasurementDao.getByTimestamp(sample.timestampMs)
            val measurement = BodyMeasurement(
                id = existing?.id ?: 0,
                timestamp = sample.timestampMs,
                dateString = DateUtils.format(sample.timestampMs),
                weightKg = sample.weightKg,
                bodyFatPct = existing?.bodyFatPct,
                bmr = existing?.bmr,
                muscleMassKg = existing?.muscleMassKg
            ).supportedMetricsOnly()
            if (
                bodyMeasurementDao.upsertByTimestampAndSync(
                    measurement = measurement,
                    lastUpdatedTimestamp = System.currentTimeMillis()
                )
            ) {
                inserted++
            }
        }
        return inserted
    }

    private suspend fun importBodyFats(from: Instant, to: Instant): Int {
        if (!manager.hasBodyFatRead()) return 0
        val profileWeight = userProfileDao.getProfileOnce()?.weightKg ?: 0.0
        val latestWeight = bodyMeasurementDao.getLatest().first()?.weightKg ?: profileWeight
        var inserted = 0
        for (sample in reader.readBodyFats(from, to)) {
            if (sample.bodyFatPct <= 0) continue
            val existing = bodyMeasurementDao.getByTimestamp(sample.timestampMs)
            val weight = existing?.weightKg?.takeIf { it > 0 } ?: latestWeight
            if (weight <= 0) continue
            val measurement = BodyMeasurement(
                id = existing?.id ?: 0,
                timestamp = sample.timestampMs,
                dateString = DateUtils.format(sample.timestampMs),
                weightKg = weight,
                bodyFatPct = sample.bodyFatPct,
                bmr = existing?.bmr,
                muscleMassKg = existing?.muscleMassKg
            ).supportedMetricsOnly()
            val wasNew = bodyMeasurementDao.upsertByTimestampAndSync(
                measurement = measurement,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            if (wasNew || existing?.bodyFatPct != sample.bodyFatPct) inserted++
        }
        return inserted
    }

    private suspend fun importMuscleMasses(from: Instant, to: Instant): Int {
        if (!manager.hasLeanBodyMassRead()) return 0
        val profileWeight = userProfileDao.getProfileOnce()?.weightKg ?: 0.0
        val latestWeight = bodyMeasurementDao.getLatest().first()?.weightKg ?: profileWeight
        var inserted = 0
        for (sample in reader.readMuscleMasses(from, to)) {
            if (sample.muscleMassKg <= 0) continue
            val existing = bodyMeasurementDao.getByTimestamp(sample.timestampMs)
            val weight = existing?.weightKg?.takeIf { it > 0 } ?: latestWeight
            if (weight <= 0) continue
            val measurement = BodyMeasurement(
                id = existing?.id ?: 0,
                timestamp = sample.timestampMs,
                dateString = DateUtils.format(sample.timestampMs),
                weightKg = weight,
                bodyFatPct = existing?.bodyFatPct,
                bmr = existing?.bmr,
                muscleMassKg = sample.muscleMassKg
            ).supportedMetricsOnly()
            val wasNew = bodyMeasurementDao.upsertByTimestampAndSync(
                measurement = measurement,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
            if (wasNew || existing?.muscleMassKg != sample.muscleMassKg) inserted++
        }
        return inserted
    }

    private suspend fun importHeight(from: Instant, to: Instant): Boolean {
        if (!manager.hasHeightRead()) return false
        val height = reader.readLatestHeight(from, to) ?: return false
        if (height.heightCm !in 100.0..250.0) return false
        val profile = userProfileDao.getProfileOnce() ?: return false
        if (kotlin.math.abs(profile.heightCm - height.heightCm) < 0.1) return false
        userProfileDao.insertOrUpdateProfile(
            profile.copy(
                heightCm = height.heightCm,
                lastUpdatedTimestamp = System.currentTimeMillis()
            )
        )
        return true
    }

    private suspend fun importExercises(from: Instant, to: Instant): Int {
        if (!manager.hasExerciseRead()) return 0
        val importedIds = context.healthConnectSyncStore.data.first()[KEY_IMPORTED_EXERCISE_IDS]
            ?.toMutableSet()
            ?: mutableSetOf()
        val existing = exerciseLogDao.getAllOnce()
        var inserted = 0
        val newlyImported = mutableSetOf<String>()

        for (session in reader.readExercises(from, to)) {
            if (session.recordId.isBlank()) continue
            if (session.recordId in importedIds) continue
            val match = existing.firstOrNull { log ->
                kotlin.math.abs(log.timestamp - session.startMs) < DUPLICATE_WINDOW_MS &&
                    log.activityName.equals(session.title, ignoreCase = true)
            }
            if (match != null) {
                if (match.healthConnectRecordId.isNullOrBlank()) {
                    exerciseLogDao.insertExerciseLog(
                        match.copy(healthConnectRecordId = session.recordId)
                    )
                }
                newlyImported += session.recordId
                continue
            }
            val durationMin = ((session.endMs - session.startMs) / 60_000L)
                .toInt()
                .coerceAtLeast(1)
            val calories = if (session.caloriesBurned > 0) {
                session.caloriesBurned
            } else {
                (durationMin * 5).coerceAtLeast(1)
            }
            exerciseLogDao.insertExerciseLog(
                ExerciseLog(
                    activityName = session.title.take(120),
                    timestamp = session.startMs,
                    dateString = DateUtils.format(session.startMs),
                    caloriesBurned = calories,
                    durationMinutes = durationMin,
                    iconTopic = null,
                    healthConnectRecordId = session.recordId
                )
            )
            newlyImported += session.recordId
            inserted++
        }

        if (newlyImported.isNotEmpty()) {
            context.healthConnectSyncStore.edit { prefs ->
                prefs[KEY_IMPORTED_EXERCISE_IDS] = importedIds + newlyImported
            }
        }
        return inserted
    }

    private suspend fun importDailySteps(from: Instant, to: Instant): Int {
        if (!manager.hasStepsRead()) return 0
        val existing = exerciseLogDao.getAllOnce()
        var touched = 0
        for (day in reader.readDailySteps(from, to)) {
            if (day.steps < MIN_STEPS_TO_LOG) continue
            val calories = stepsToCalories(day.steps)
            val durationMin = (day.steps / STEPS_PER_MINUTE).toInt().coerceAtLeast(1)
            val name = "Steps (${day.steps})"
            val stepsRecordId = stepsRecordId(day.dateString)
            val prior = existing.firstOrNull { log ->
                log.dateString == day.dateString && isStepsLogName(log.activityName)
            }
            if (prior != null) {
                if (
                    prior.caloriesBurned == calories &&
                    prior.activityName == name &&
                    prior.durationMinutes == durationMin &&
                    prior.healthConnectRecordId == stepsRecordId
                ) {
                    continue
                }
                exerciseLogDao.insertExerciseLog(
                    prior.copy(
                        activityName = name,
                        caloriesBurned = calories,
                        durationMinutes = durationMin,
                        timestamp = day.timestampMs,
                        healthConnectRecordId = stepsRecordId
                    )
                )
            } else {
                exerciseLogDao.insertExerciseLog(
                    ExerciseLog(
                        activityName = name,
                        timestamp = day.timestampMs,
                        dateString = day.dateString,
                        caloriesBurned = calories,
                        durationMinutes = durationMin,
                        iconTopic = null,
                        healthConnectRecordId = stepsRecordId
                    )
                )
            }
            touched++
        }
        return touched
    }

    private suspend fun refreshWellnessContext(metrics: HealthConnectMetricPrefs) {
        val hydration = if (metrics.importHydration) reader.readHydrationTodayMl() else null
        val sleep = if (metrics.importSleep) reader.readRecentSleep() else null
        context.healthConnectSyncStore.edit { prefs ->
            if (hydration != null) {
                prefs[KEY_HYDRATION_ML] = hydration.milliliters
                prefs[KEY_HYDRATION_DATE] = hydration.dateString
            } else if (!metrics.importHydration) {
                prefs.remove(KEY_HYDRATION_ML)
                prefs.remove(KEY_HYDRATION_DATE)
            }
            if (sleep != null) {
                prefs[KEY_SLEEP_MINUTES] = sleep.durationMinutes
                prefs[KEY_SLEEP_DATE] = sleep.endDateString
            } else if (!metrics.importSleep) {
                prefs.remove(KEY_SLEEP_MINUTES)
                prefs.remove(KEY_SLEEP_DATE)
            }
        }
    }

    companion object {
        private const val TAG = "FitBuddy/HC"
        /** Bootstrap / manual Sync now history window (Health Connect foreground default). */
        private const val SYNC_WINDOW_DAYS = 30L
        private const val MIN_AUTO_SYNC_GAP_MS = 60L * 60L * 1000L
        private const val INCREMENTAL_OVERLAP_MS = 5L * 60L * 1000L
        private const val DUPLICATE_WINDOW_MS = 90_000L
        private const val MIN_STEPS_TO_LOG = 500L
        private const val STEPS_PER_MINUTE = 100L
        private const val KCAL_PER_STEP = 0.04

        /** Stable sync error when OS grants are missing or revoked. */
        const val ERROR_PERMISSIONS_REVOKED =
            "Health Connect permissions were revoked — open Integrations and authorize"

        private val KEY_LAST_SYNC_AT = longPreferencesKey("hc_last_sync_at")
        private val KEY_LAST_EXPORTED_HEIGHT_CM = doublePreferencesKey("hc_last_exported_height_cm")
        private val KEY_IMPORTED_EXERCISE_IDS = stringSetPreferencesKey("hc_imported_exercise_ids")
        private val KEY_HYDRATION_ML = doublePreferencesKey("hc_hydration_ml_today")
        private val KEY_HYDRATION_DATE = stringPreferencesKey("hc_hydration_date")
        private val KEY_SLEEP_MINUTES = intPreferencesKey("hc_sleep_minutes")
        private val KEY_SLEEP_DATE = stringPreferencesKey("hc_sleep_date")

        fun stepsRecordId(dateString: String): String = "steps:$dateString"

        fun stepsToCalories(steps: Long): Int =
            (steps * KCAL_PER_STEP).toInt().coerceAtLeast(1)

        fun isStepsLogName(name: String): Boolean =
            name.equals("Steps", ignoreCase = true) ||
                name.startsWith("Steps (", ignoreCase = true)

        fun wellnessJson(context: HealthConnectWellnessContext): JSONObject =
            JSONObject().apply {
                if (context.hydrationMlToday != null && context.hydrationDate != null) {
                    put("hydration_ml_today", context.hydrationMlToday)
                    put("hydration_date", context.hydrationDate)
                }
                if (context.sleepMinutesLastNight != null) {
                    put("sleep_minutes_last_night", context.sleepMinutesLastNight)
                    put("sleep_ended_date", context.sleepEndedDate ?: JSONObject.NULL)
                }
            }
    }
}
