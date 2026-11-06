package com.anant.fitbuddy.data.healthconnect

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord

enum class HealthConnectSdkStatus {
    Available,
    UpdateRequired,
    Unavailable
}

/**
 * Thin wrapper around [HealthConnectClient]: availability, permission strings, and
 * granted-permission probes. Opt-in only — callers must check [AppSettings.healthConnectEnabled]
 * and per-metric [HealthConnectMetricPrefs].
 */
class HealthConnectManager(private val context: Context) {

    val client: HealthConnectClient?
        get() = if (sdkStatus() == HealthConnectSdkStatus.Available) {
            runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
        } else {
            null
        }

    val nutritionWrite = HealthPermission.getWritePermission(NutritionRecord::class)
    val weightRead = HealthPermission.getReadPermission(WeightRecord::class)
    val weightWrite = HealthPermission.getWritePermission(WeightRecord::class)
    val heightRead = HealthPermission.getReadPermission(HeightRecord::class)
    val heightWrite = HealthPermission.getWritePermission(HeightRecord::class)
    val bodyFatRead = HealthPermission.getReadPermission(BodyFatRecord::class)
    val bodyFatWrite = HealthPermission.getWritePermission(BodyFatRecord::class)
    val leanBodyMassRead = HealthPermission.getReadPermission(LeanBodyMassRecord::class)
    val leanBodyMassWrite = HealthPermission.getWritePermission(LeanBodyMassRecord::class)
    val exerciseRead = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    val exerciseWrite = HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    val activeCaloriesRead =
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    val activeCaloriesWrite =
        HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class)
    val stepsRead = HealthPermission.getReadPermission(StepsRecord::class)
    val hydrationRead = HealthPermission.getReadPermission(HydrationRecord::class)
    val hydrationWrite = HealthPermission.getWritePermission(HydrationRecord::class)
    val sleepRead = HealthPermission.getReadPermission(SleepSessionRecord::class)

    /** Union of permissions for every metric available in this build. */
    fun allPermissions(): Set<String> = permissionsFor(
        HealthConnectMetricPrefs(
            importWeight = HealthConnectMetricAvailability.importWeight,
            importHeight = HealthConnectMetricAvailability.importHeight,
            importBodyFat = HealthConnectMetricAvailability.importBodyFat,
            importMuscleMass = HealthConnectMetricAvailability.importMuscleMass,
            importExercise = HealthConnectMetricAvailability.importExercise,
            importSteps = HealthConnectMetricAvailability.importSteps,
            importHydration = HealthConnectMetricAvailability.importHydration,
            importSleep = HealthConnectMetricAvailability.importSleep,
            exportNutrition = HealthConnectMetricAvailability.exportNutrition,
            exportWeight = HealthConnectMetricAvailability.exportWeight,
            exportHeight = HealthConnectMetricAvailability.exportHeight,
            exportBodyFat = HealthConnectMetricAvailability.exportBodyFat,
            exportMuscleMass = HealthConnectMetricAvailability.exportMuscleMass,
            exportHydration = false,
            exportExercise = HealthConnectMetricAvailability.exportExercise,
        )
    )

    /** Permissions needed for the currently enabled, available metric toggles. */
    fun permissionsFor(prefs: HealthConnectMetricPrefs): Set<String> {
        val p = prefs.sanitized()
        return buildSet {
            if (p.exportNutrition) add(nutritionWrite)
            if (p.importWeight) add(weightRead)
            if (p.exportWeight) add(weightWrite)
            if (p.importHeight) add(heightRead)
            if (p.exportHeight) add(heightWrite)
            if (p.importBodyFat) add(bodyFatRead)
            if (p.exportBodyFat) add(bodyFatWrite)
            if (p.importMuscleMass) add(leanBodyMassRead)
            if (p.exportMuscleMass) add(leanBodyMassWrite)
            if (p.importExercise) {
                add(exerciseRead)
                add(activeCaloriesRead)
            }
            if (p.exportExercise) {
                add(exerciseWrite)
                add(activeCaloriesWrite)
            }
            if (p.importSteps) add(stepsRead)
            if (p.importHydration) add(hydrationRead)
            // WRITE_HYDRATION omitted until exportHydration is available.
            if (p.importSleep) add(sleepRead)
        }
    }

    fun sdkStatus(): HealthConnectSdkStatus =
        when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectSdkStatus.Available
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                HealthConnectSdkStatus.UpdateRequired
            else -> HealthConnectSdkStatus.Unavailable
        }

    fun isAvailable(): Boolean = sdkStatus() == HealthConnectSdkStatus.Available

    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    suspend fun grantedPermissions(): Set<String> =
        runCatching { client?.permissionController?.getGrantedPermissions() }
            .getOrNull()
            .orEmpty()

    suspend fun hasAnyPermission(prefs: HealthConnectMetricPrefs): Boolean {
        val needed = permissionsFor(prefs)
        if (needed.isEmpty()) return false
        return grantedPermissions().any { it in needed }
    }

    suspend fun missingPermissions(prefs: HealthConnectMetricPrefs): Set<String> {
        val granted = grantedPermissions()
        return permissionsFor(prefs) - granted
    }

    /**
     * Turns off any metric whose required permissions are not fully granted.
     * Export hydration stays forced off until water logging ships.
     */
    fun alignToGranted(
        prefs: HealthConnectMetricPrefs,
        granted: Set<String>
    ): HealthConnectMetricPrefs {
        fun has(vararg perms: String): Boolean = perms.isNotEmpty() && perms.all { it in granted }
        return prefs.sanitized().copy(
            importWeight = prefs.importWeight &&
                HealthConnectMetricAvailability.importWeight && has(weightRead),
            importHeight = prefs.importHeight &&
                HealthConnectMetricAvailability.importHeight && has(heightRead),
            importBodyFat = prefs.importBodyFat &&
                HealthConnectMetricAvailability.importBodyFat && has(bodyFatRead),
            importMuscleMass = prefs.importMuscleMass &&
                HealthConnectMetricAvailability.importMuscleMass && has(leanBodyMassRead),
            importExercise = prefs.importExercise &&
                HealthConnectMetricAvailability.importExercise &&
                has(exerciseRead, activeCaloriesRead),
            importSteps = prefs.importSteps &&
                HealthConnectMetricAvailability.importSteps && has(stepsRead),
            importHydration = prefs.importHydration &&
                HealthConnectMetricAvailability.importHydration && has(hydrationRead),
            importSleep = prefs.importSleep &&
                HealthConnectMetricAvailability.importSleep && has(sleepRead),
            exportNutrition = prefs.exportNutrition &&
                HealthConnectMetricAvailability.exportNutrition && has(nutritionWrite),
            exportWeight = prefs.exportWeight &&
                HealthConnectMetricAvailability.exportWeight && has(weightWrite),
            exportHeight = prefs.exportHeight &&
                HealthConnectMetricAvailability.exportHeight && has(heightWrite),
            exportBodyFat = prefs.exportBodyFat &&
                HealthConnectMetricAvailability.exportBodyFat && has(bodyFatWrite),
            exportMuscleMass = prefs.exportMuscleMass &&
                HealthConnectMetricAvailability.exportMuscleMass && has(leanBodyMassWrite),
            exportHydration = false,
            exportExercise = prefs.exportExercise &&
                HealthConnectMetricAvailability.exportExercise &&
                has(exerciseWrite, activeCaloriesWrite),
        )
    }

    /**
     * Merges a permission-sheet result into metric prefs.
     *
     * When [allGranted] is non-empty (API probe worked), fully aligns [desired] to grants.
     * When the probe is empty (common right after the sheet closes), only flips metrics we
     * can confirm from [resultGranted]; already-on metrics stay on so a partial result
     * cannot wipe the rest.
     */
    fun applyPermissionResult(
        previous: HealthConnectMetricPrefs,
        desired: HealthConnectMetricPrefs,
        resultGranted: Set<String>,
        allGranted: Set<String>
    ): HealthConnectMetricPrefs {
        if (allGranted.isNotEmpty()) {
            return alignToGranted(desired, allGranted + resultGranted)
        }
        fun confirmed(vararg perms: String): Boolean =
            perms.isNotEmpty() && perms.all { it in resultGranted }
        fun resolve(want: Boolean, wasOn: Boolean, vararg perms: String): Boolean {
            if (!want) return false
            if (confirmed(*perms)) return true
            return wasOn
        }
        return desired.copy(
            importWeight = resolve(desired.importWeight, previous.importWeight, weightRead),
            importHeight = resolve(desired.importHeight, previous.importHeight, heightRead),
            importBodyFat = resolve(desired.importBodyFat, previous.importBodyFat, bodyFatRead),
            importMuscleMass = resolve(
                desired.importMuscleMass, previous.importMuscleMass, leanBodyMassRead
            ),
            importExercise = resolve(
                desired.importExercise, previous.importExercise,
                exerciseRead, activeCaloriesRead
            ),
            importSteps = resolve(desired.importSteps, previous.importSteps, stepsRead),
            importHydration = resolve(
                desired.importHydration, previous.importHydration, hydrationRead
            ),
            importSleep = resolve(desired.importSleep, previous.importSleep, sleepRead),
            exportNutrition = resolve(
                desired.exportNutrition, previous.exportNutrition, nutritionWrite
            ),
            exportWeight = resolve(desired.exportWeight, previous.exportWeight, weightWrite),
            exportHeight = resolve(desired.exportHeight, previous.exportHeight, heightWrite),
            exportBodyFat = resolve(desired.exportBodyFat, previous.exportBodyFat, bodyFatWrite),
            exportMuscleMass = resolve(
                desired.exportMuscleMass, previous.exportMuscleMass, leanBodyMassWrite
            ),
            exportHydration = false,
            exportExercise = resolve(
                desired.exportExercise, previous.exportExercise,
                exerciseWrite, activeCaloriesWrite
            ),
        ).sanitized()
    }

    suspend fun hasPermission(permission: String): Boolean = permission in grantedPermissions()

    suspend fun hasNutritionWrite(): Boolean = hasPermission(nutritionWrite)
    suspend fun hasWeightRead(): Boolean = hasPermission(weightRead)
    suspend fun hasWeightWrite(): Boolean = hasPermission(weightWrite)
    suspend fun hasHeightRead(): Boolean = hasPermission(heightRead)
    suspend fun hasHeightWrite(): Boolean = hasPermission(heightWrite)
    suspend fun hasBodyFatRead(): Boolean = hasPermission(bodyFatRead)
    suspend fun hasBodyFatWrite(): Boolean = hasPermission(bodyFatWrite)
    suspend fun hasLeanBodyMassRead(): Boolean = hasPermission(leanBodyMassRead)
    suspend fun hasLeanBodyMassWrite(): Boolean = hasPermission(leanBodyMassWrite)
    suspend fun hasExerciseRead(): Boolean = hasPermission(exerciseRead)
    suspend fun hasExerciseWrite(): Boolean = hasPermission(exerciseWrite)
    suspend fun hasActiveCaloriesRead(): Boolean = hasPermission(activeCaloriesRead)
    suspend fun hasActiveCaloriesWrite(): Boolean = hasPermission(activeCaloriesWrite)
    suspend fun hasStepsRead(): Boolean = hasPermission(stepsRead)
    suspend fun hasHydrationRead(): Boolean = hasPermission(hydrationRead)
    suspend fun hasHydrationWrite(): Boolean = hasPermission(hydrationWrite)
    suspend fun hasSleepRead(): Boolean = hasPermission(sleepRead)

    fun openHealthConnectSettingsIntent(): Intent =
        Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun providerInstallOrUpdateIntent(): Intent? =
        when (sdkStatus()) {
            HealthConnectSdkStatus.Available -> null
            HealthConnectSdkStatus.UpdateRequired,
            HealthConnectSdkStatus.Unavailable ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    openHealthConnectSettingsIntent()
                } else {
                    Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("market://details?id=$HC_PROVIDER_PACKAGE")
                        setPackage("com.android.vending")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
        }

    companion object {
        const val CLIENT_PREFIX = "fitbuddy-"
        const val FOOD_PREFIX = "${CLIENT_PREFIX}food-"
        const val EXERCISE_PREFIX = "${CLIENT_PREFIX}exercise-"
        const val WEIGHT_PREFIX = "${CLIENT_PREFIX}weight-"
        const val BODY_FAT_PREFIX = "${CLIENT_PREFIX}bodyfat-"
        const val MUSCLE_MASS_PREFIX = "${CLIENT_PREFIX}musclemass-"
        const val HYDRATION_PREFIX = "${CLIENT_PREFIX}hydration-"
        const val HEIGHT_CLIENT_ID = "${CLIENT_PREFIX}height"
        const val HC_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"

        fun foodClientRecordId(foodLogId: Int): String = "$FOOD_PREFIX$foodLogId"
        fun exerciseClientRecordId(exerciseLogId: Int): String = "$EXERCISE_PREFIX$exerciseLogId"
        fun weightClientRecordId(measurementId: Int): String = "$WEIGHT_PREFIX$measurementId"
        fun bodyFatClientRecordId(measurementId: Int): String = "$BODY_FAT_PREFIX$measurementId"
        fun muscleMassClientRecordId(measurementId: Int): String = "$MUSCLE_MASS_PREFIX$measurementId"
        fun hydrationClientRecordId(entryId: Long): String = "$HYDRATION_PREFIX$entryId"

        fun isFitBuddyRecord(clientRecordId: String?): Boolean =
            clientRecordId?.startsWith(CLIENT_PREFIX) == true
    }
}
