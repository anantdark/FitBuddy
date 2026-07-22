package com.anant.fitbuddy.data.backup

import android.content.Context
import android.net.Uri
import com.anant.fitbuddy.data.database.BodyMeasurementDao
import com.anant.fitbuddy.data.database.ExerciseLogDao
import com.anant.fitbuddy.data.database.ExercisePresetDao
import com.anant.fitbuddy.data.database.FoodLogDao
import com.anant.fitbuddy.data.database.MealFoodDao
import com.anant.fitbuddy.data.database.MealPresetDao
import com.anant.fitbuddy.data.database.SavedFoodDao
import com.anant.fitbuddy.data.database.UserProfileDao
import com.anant.fitbuddy.data.database.WorkoutExerciseDao
import com.anant.fitbuddy.data.database.WorkoutSessionDao
import com.anant.fitbuddy.data.backup.crypto.BackupCrypto
import com.anant.fitbuddy.data.backup.crypto.BackupFormat
import com.anant.fitbuddy.data.backup.crypto.BackupPasswordStore
import com.anant.fitbuddy.data.backup.crypto.OpenResult
import com.anant.fitbuddy.data.settings.SettingsRepository
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class BackupManager(
    private val context: Context,
    private val userProfileDao: UserProfileDao,
    private val foodLogDao: FoodLogDao,
    private val mealFoodDao: MealFoodDao,
    private val exerciseLogDao: ExerciseLogDao,
    private val savedFoodDao: SavedFoodDao,
    private val mealPresetDao: MealPresetDao,
    private val exercisePresetDao: ExercisePresetDao,
    private val bodyMeasurementDao: BodyMeasurementDao,
    private val workoutSessionDao: WorkoutSessionDao,
    private val workoutExerciseDao: WorkoutExerciseDao,
    private val settingsRepository: SettingsRepository,
    val crypto: BackupCrypto,
    moshi: Moshi
) {
    private val adapter = moshi.adapter(BackupData::class.java).indent("  ")

    val appContext: Context get() = context.applicationContext

    suspend fun buildBackupData(): BackupData = withContext(Dispatchers.IO) {
        snapshot()
    }

    /** Moshi JSON for [data] (same format as file export). */
    fun encode(data: BackupData): String = adapter.toJson(data)

    suspend fun toJson(): String = withContext(Dispatchers.IO) {
        encode(snapshot())
    }

    fun countRecords(data: BackupData, legacyFoodCount: Int? = null): Int {
        val foods = legacyFoodCount ?: data.savedFoods.size
        return data.measurements.size + data.foodLogs.size + data.mealFoods.size +
            data.exerciseLogs.size + foods + data.mealPresets.size +
            data.exercisePresets.size + data.workoutSessions.size +
            data.workoutExercises.size + if (data.settings != null) 1 else 0
    }

    /**
     * Exports a snapshot to [uri]. A null/blank [password] writes the legacy raw JSON form
     * (Requirements 1.3, 9.1); a non-blank [password] seals the payload into an encrypted
     * [com.anant.fitbuddy.data.backup.crypto.BackupEnvelope] before writing (Requirement 1.2).
     * Sealing happens before any bytes are written, so a seal failure aborts the export with no
     * partial file (Requirement 1.5). Returns the total record count on success.
     */
    suspend fun exportTo(uri: Uri, password: CharArray? = null): Int = withContext(Dispatchers.IO) {
        val data = snapshot()
        val json = encode(data)
        // Seal first: if this fails it throws before we open/write the file, so no bytes are written.
        val output = crypto.seal(json, password)
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(output.toByteArray(Charsets.UTF_8))
        } ?: error("Couldn't open the selected file for writing")
        countRecords(data)
    }

    /**
     * Imports a backup from [uri]. The raw bytes are classified (within the 3s budget,
     * Requirement 2.1) before any data is touched:
     * - [BackupFormat.LEGACY_PLAIN]/[BackupFormat.PLAIN_WRAPPED] import directly with no prompt
     *   (Requirements 2.3, 9.1).
     * - [BackupFormat.ENCRYPTED] obtains the password via the suspend [passwordProvider], opens the
     *   envelope, and imports the decrypted plaintext (Requirement 2.4). A wrong password, corrupt
     *   envelope, or absent/blank password leaves existing data untouched.
     * - [BackupFormat.UNKNOWN] returns [BackupImportResult.Unrecognized] without touching data
     *   (Requirement 2.7).
     *
     * Returns a [BackupImportResult] so callers can surface distinct messages and drive retries
     * rather than catching thrown exceptions.
     */
    suspend fun importFrom(
        uri: Uri,
        passwordProvider: suspend () -> CharArray? = { null }
    ): BackupImportResult = withContext(Dispatchers.IO) {
        val raw = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes().toString(Charsets.UTF_8)
        } ?: error("Couldn't open the selected file for reading")
        importRaw(raw, passwordProvider)
    }

    private suspend fun importRaw(
        raw: String,
        passwordProvider: suspend () -> CharArray?
    ): BackupImportResult = when (crypto.classify(raw)) {
        BackupFormat.LEGACY_PLAIN -> BackupImportResult.Success(importFromJsonInternal(raw))
        BackupFormat.PLAIN_WRAPPED -> openThenImport(raw, null)
        BackupFormat.ENCRYPTED -> {
            val password = passwordProvider()
            if (password == null || password.isEmpty()) {
                BackupImportResult.PasswordRequired
            } else {
                try {
                    val result = openThenImport(raw, password)
                    // On a successful decrypt, remember the password on-device so later cloud
                    // uploads keep this custom encryption instead of downgrading to the Support ID.
                    if (result is BackupImportResult.Success) {
                        rememberCustomPasswordIfNeeded(password)
                    }
                    result
                } finally {
                    password.fill('\u0000')
                }
            }
        }
        BackupFormat.UNKNOWN -> BackupImportResult.Unrecognized
    }

    /**
     * After a successful encrypted local import, persists [password] (encrypted, device-local via
     * [BackupPasswordStore]) and flags a custom cloud password so subsequent manual/auto cloud
     * uploads seal with it rather than the Support ID. Skipped when [password] equals the current
     * Support ID (that's the default, not a custom password). Best-effort: any failure is swallowed
     * so it never blocks a successful import.
     */
    private suspend fun rememberCustomPasswordIfNeeded(password: CharArray) {
        runCatching {
            val supportId = settingsRepository.settings.first().supportId
            val isSupportId = supportId.isNotBlank() && supportId.toCharArray().contentEquals(password)
            if (!isSupportId) {
                settingsRepository.setCloudBackupPasswordBlob(BackupPasswordStore.encrypt(password))
                settingsRepository.setCloudBackupPasswordSet(true)
            }
        }
    }

    private suspend fun openThenImport(raw: String, password: CharArray?): BackupImportResult =
        when (val opened = crypto.open(raw, password)) {
            is OpenResult.Success -> BackupImportResult.Success(importFromJsonInternal(opened.payloadJson))
            OpenResult.WrongPassword -> BackupImportResult.WrongPassword
            OpenResult.Corrupt -> BackupImportResult.Corrupt
            OpenResult.Unreadable -> BackupImportResult.Unrecognized
        }

    suspend fun importFromJson(json: String): Int = withContext(Dispatchers.IO) {
        importFromJsonInternal(json)
    }

    private suspend fun snapshot(): BackupData {
        val settings = settingsRepository.settings.first()
        return BackupData(
            exportedAt = System.currentTimeMillis(),
            profile = userProfileDao.getProfileOnce(),
            measurements = bodyMeasurementDao.getAllOnce(),
            foodLogs = foodLogDao.getAllOnce(),
            mealFoods = mealFoodDao.getAllOnce(),
            exerciseLogs = exerciseLogDao.getAllOnce(),
            savedFoods = savedFoodDao.getAllOnce(),
            mealPresets = mealPresetDao.getAllOnce(),
            exercisePresets = exercisePresetDao.getAllOnce(),
            workoutSessions = workoutSessionDao.getAllOnce(),
            workoutExercises = workoutExerciseDao.getAllOnce(),
            settings = BackupSettings.from(settings)
        )
    }

    private suspend fun importFromJsonInternal(json: String): Int {
        val data = adapter.fromJson(json)
            ?: error("The selected file isn't a valid FitBuddy backup")

        userProfileDao.clearAll()
        bodyMeasurementDao.clearAll()
        foodLogDao.clearAll()
        mealFoodDao.clearAll()
        exerciseLogDao.clearAll()
        savedFoodDao.clearAll()
        mealPresetDao.clearAll()
        exercisePresetDao.clearAll()
        workoutExerciseDao.clearAll()
        workoutSessionDao.clearAll()

        data.profile?.let { userProfileDao.insertOrUpdateProfile(it.copy(id = 1)) }
        bodyMeasurementDao.insertAll(data.measurements.map { it.copy(id = 0) })

        val legacyFoods = if (data.savedFoods.isNotEmpty()) data.savedFoods else data.presets
        val savedFoodIdMap = BackupIdRemapper.idMap(
            oldIds = legacyFoods.map { it.id },
            newIds = savedFoodDao.insertAll(legacyFoods.map { it.copy(id = 0) })
        )

        mealPresetDao.insertAll(
            data.mealPresets.map { BackupIdRemapper.remapMealPreset(it, savedFoodIdMap) }
        )
        exercisePresetDao.insertAll(data.exercisePresets.map { it.copy(id = 0) })

        val mealLogIdMap = BackupIdRemapper.idMap(
            oldIds = data.foodLogs.map { it.id },
            newIds = foodLogDao.insertAll(data.foodLogs.map { it.copy(id = 0) })
        )

        val remappedMealFoods = data.mealFoods.mapNotNull { food ->
            BackupIdRemapper.remapMealFood(food, mealLogIdMap, savedFoodIdMap)
        }
        mealFoodDao.insertAll(remappedMealFoods)

        val exerciseLogIdMap = BackupIdRemapper.idMap(
            oldIds = data.exerciseLogs.map { it.id },
            newIds = exerciseLogDao.insertAll(data.exerciseLogs.map { it.copy(id = 0) })
        )

        val remappedSessions = data.workoutSessions.map { session ->
            session.copy(
                id = 0,
                exerciseLogId = session.exerciseLogId?.let { exerciseLogIdMap[it] }
            )
        }
        val sessionIdMap = BackupIdRemapper.idMap(
            oldIds = data.workoutSessions.map { it.id },
            newIds = workoutSessionDao.insertAll(remappedSessions)
        )

        val remappedExercises = data.workoutExercises.mapNotNull { exercise ->
            sessionIdMap[exercise.sessionId]?.let { newSessionId ->
                exercise.copy(id = 0, sessionId = newSessionId)
            }
        }
        workoutExerciseDao.insertAll(remappedExercises)

        data.settings?.let { backupSettings ->
            // firstName/lastName/cloudBackupPasswordSet are device-local (not in BackupData v5) — keep current values.
            val current = settingsRepository.settings.first()
            settingsRepository.save(
                backupSettings.toAppSettings().copy(
                    firstName = current.firstName,
                    lastName = current.lastName,
                    cloudBackupPasswordSet = current.cloudBackupPasswordSet
                )
            )
        }

        return countRecords(data, legacyFoodCount = legacyFoods.size)
    }
}

/**
 * Outcome of a local backup import. Distinguishes success (with the restored record count) from the
 * recoverable failure modes so the caller (ViewModel, task 2.3) can surface distinct messages and
 * drive up-to-5-attempt retries without relying on thrown exceptions. Every non-success variant
 * leaves existing on-device data unchanged.
 */
sealed interface BackupImportResult {
    /** Import completed; [recordCount] records were written (Requirement 1.6 reporting). */
    data class Success(val recordCount: Int) : BackupImportResult

    /** Encrypted backup opened with an incorrect password (Requirements 2.5, 10.4). */
    data object WrongPassword : BackupImportResult

    /** Envelope parsed but its fields/ciphertext are malformed or truncated (Requirement 10.5). */
    data object Corrupt : BackupImportResult

    /** Bytes are neither a valid envelope nor a legacy payload (Requirements 2.7, 9.6). */
    data object Unrecognized : BackupImportResult

    /** Backup is encrypted but no password was supplied (provider returned null/empty or the user
     *  cancelled the prompt); the import is aborted with data unchanged (Requirement 2.6). */
    data object PasswordRequired : BackupImportResult
}
