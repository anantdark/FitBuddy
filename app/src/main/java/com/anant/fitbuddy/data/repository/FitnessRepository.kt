package com.anant.fitbuddy.data.repository

import android.net.Uri
import android.util.Base64
import com.anant.fitbuddy.data.backup.BackupManager
import com.anant.fitbuddy.data.backup.mongo.MongoBackupRepository
import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.BodyMeasurementDao
import com.anant.fitbuddy.data.database.ExerciseDailySummary
import com.anant.fitbuddy.data.database.ExerciseLog
import com.anant.fitbuddy.data.database.ExerciseLogDao
import com.anant.fitbuddy.data.database.FoodDailySummary
import com.anant.fitbuddy.data.database.FoodLog
import com.anant.fitbuddy.data.database.FoodLogDao
import com.anant.fitbuddy.data.database.MealFood
import com.anant.fitbuddy.data.database.MealFoodDao
import com.anant.fitbuddy.data.database.MealPreset
import com.anant.fitbuddy.data.database.MealPresetDao
import com.anant.fitbuddy.data.database.SavedFood
import com.anant.fitbuddy.data.database.SavedFoodDao
import com.anant.fitbuddy.data.database.FoodTotals
import com.anant.fitbuddy.data.database.ExercisePreset
import com.anant.fitbuddy.data.database.ExercisePresetDao
import com.anant.fitbuddy.data.database.UserProfile
import com.anant.fitbuddy.data.database.UserProfileDao
import com.anant.fitbuddy.data.database.WorkoutExercise
import com.anant.fitbuddy.data.database.WorkoutExerciseDao
import com.anant.fitbuddy.data.database.WorkoutSession
import com.anant.fitbuddy.data.database.WorkoutSessionDao
import com.anant.fitbuddy.data.model.COMMON_EXERCISES
import com.anant.fitbuddy.data.model.CommonExercise
import com.anant.fitbuddy.data.model.CustomExerciseResponse
import com.anant.fitbuddy.data.model.Equipment
import com.anant.fitbuddy.data.model.buildExercisePickerList
import com.anant.fitbuddy.data.model.ExerciseDraft
import com.anant.fitbuddy.data.model.FitnessTrackerResponse
import com.anant.fitbuddy.data.model.ExerciseAnalysis
import com.anant.fitbuddy.data.model.FoodAnalysis
import com.anant.fitbuddy.data.model.FoodEntryDraft
import com.anant.fitbuddy.data.model.FoodDraft
import com.anant.fitbuddy.data.model.MealDraft
import com.anant.fitbuddy.data.model.NorthIndianStaples
import com.anant.fitbuddy.data.model.toFoodDraft
import com.anant.fitbuddy.data.model.toFoodEntry
import com.anant.fitbuddy.data.model.toMealDraft
import com.anant.fitbuddy.data.model.toPresetMealFood
import com.anant.fitbuddy.data.model.toSingleFoodMeal
import com.anant.fitbuddy.data.model.Ingredient
import com.anant.fitbuddy.data.model.IngredientDraft
import com.anant.fitbuddy.data.model.LoggedIngredient
import com.anant.fitbuddy.data.model.Macros
import com.anant.fitbuddy.data.model.ModelOption
import com.anant.fitbuddy.data.model.ProgressChatTurn
import com.anant.fitbuddy.data.model.ProgressInsightResponse
import com.anant.fitbuddy.data.model.TargetPlanResponse
import com.anant.fitbuddy.data.model.WorkoutCaloriesResponse
import com.anant.fitbuddy.data.model.WorkoutDraft
import com.anant.fitbuddy.data.model.ScannedProduct
import com.anant.fitbuddy.data.remote.OpenFoodFactsDataSource
import com.anant.fitbuddy.data.remote.RemoteAiDataSource
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import com.anant.fitbuddy.data.settings.ModelCooldown
import com.anant.fitbuddy.data.settings.ModelCooldownPolicy
import com.anant.fitbuddy.data.settings.SettingsRepository
import com.anant.fitbuddy.data.settings.isPlausibleModelIdFor
import com.anant.fitbuddy.util.DateUtils
import com.anant.fitbuddy.util.FoodQuantityParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

class FitnessRepository(
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
    private val remoteAiDataSource: RemoteAiDataSource,
    private val openFoodFactsDataSource: OpenFoodFactsDataSource,
    private val settingsRepository: SettingsRepository,
    private val backupManager: BackupManager,
    private val mongoBackupRepository: MongoBackupRepository = MongoBackupRepository()
) {
    val activeProfile: Flow<UserProfile?> = userProfileDao.getProfile()
    val allFoodLogs: Flow<List<FoodLog>> = foodLogDao.getAllFoodLogs()
    val allExerciseLogs: Flow<List<ExerciseLog>> = exerciseLogDao.getAllExerciseLogs()
    val savedFoods: Flow<List<SavedFood>> = savedFoodDao.getAll()
    val mealPresets: Flow<List<MealPreset>> = mealPresetDao.getAll()
    val exercisePresets: Flow<List<ExercisePreset>> = exercisePresetDao.getAllPresets()

    // --- Body measurements ------------------------------------------------------------------

    val bodyMeasurements: Flow<List<BodyMeasurement>> = bodyMeasurementDao.getAll()
    val latestMeasurement: Flow<BodyMeasurement?> = bodyMeasurementDao.getLatest()
    fun getRecentMeasurements(limit: Int): Flow<List<BodyMeasurement>> = bodyMeasurementDao.getRecent(limit)

    /** Saves a reading and mirrors its weight onto the profile so the dashboard/AI use current weight. */
    suspend fun addMeasurement(measurement: BodyMeasurement) {
        bodyMeasurementDao.insert(measurement)
        userProfileDao.getProfileOnce()?.let { profile ->
            userProfileDao.insertOrUpdateProfile(
                profile.copy(
                    weightKg = measurement.weightKg,
                    lastUpdatedTimestamp = measurement.timestamp
                )
            )
        }
    }

    suspend fun deleteMeasurement(measurement: BodyMeasurement) =
        bodyMeasurementDao.delete(measurement)

    // --- AI planning ------------------------------------------------------------------------

    /** Asks the AI to recommend a goal + daily calorie/macro targets from the user's context. */
    suspend fun designTargets(contextJson: String): TargetPlanResponse {
        val settings = settingsRepository.settings.first()
        check(settings.isConfigured) {
            "Connect an AI provider in Settings to get AI target recommendations."
        }
        val (result, _) = withAiFailover(settings) { s ->
            remoteAiDataSource.designTargets(s, contextJson)
        }
        return result
    }

    /** Asks the AI to summarise progress and give recommendations from compressed trend data. */
    suspend fun summarizeProgress(compressedMetrics: String): ProgressInsightResponse {
        val settings = settingsRepository.settings.first()
        check(settings.isConfigured) {
            "Connect an AI provider in Settings to get AI progress insights."
        }
        val (result, _) = withAiFailover(settings) { s ->
            remoteAiDataSource.summarizeProgress(s, compressedMetrics)
        }
        return result
    }

    /** Follow-up progress coach chat using full progress JSON in the system prompt. */
    suspend fun chatProgressInsight(
        contextJson: String,
        history: List<ProgressChatTurn>
    ): String {
        val settings = settingsRepository.settings.first()
        check(settings.isConfigured) {
            "Connect an AI provider in Settings to chat about your progress."
        }
        val (result, _) = withAiFailover(settings) { s ->
            remoteAiDataSource.chatProgressCoach(s, contextJson, history)
        }
        return result
    }

    // --- Backup -----------------------------------------------------------------------------

    suspend fun exportData(uri: Uri): Int = backupManager.exportTo(uri)

    suspend fun importData(uri: Uri): Int = backupManager.importFrom(uri)

    /**
     * Uploads a BackupData v5 JSON snapshot to the configured personal Atlas cluster.
     * Runtime gate is URI non-blank only — not developerModeUnlocked.
     */
    suspend fun uploadMongoBackup(): Int {
        val settings = settingsRepository.settings.first()
        if (settings.mongoDbUri.isBlank()) {
            error("MongoDB URI not configured")
        }
        val supportId = settings.supportId.ifBlank { settingsRepository.ensureSupportId() }
        return try {
            val data = backupManager.buildBackupData()
            val payloadJson = backupManager.encode(data)
            mongoBackupRepository.upload(
                connectionUri = settings.mongoDbUri,
                databaseName = settings.mongoDbName,
                supportId = supportId,
                payloadJson = payloadJson,
                exportedAt = data.exportedAt
            )
            val count = backupManager.countRecords(data)
            settingsRepository.setMongoUploadStatus(ok = true)
            count
        } catch (e: Exception) {
            settingsRepository.setMongoUploadStatus(
                ok = false,
                error = e.message ?: e.javaClass.simpleName
            )
            throw e
        }
    }

    /** Restores the Atlas document whose `_id` is [supportId]. */
    suspend fun downloadMongoBackup(supportId: String): Int {
        val settings = settingsRepository.settings.first()
        if (settings.mongoDbUri.isBlank()) {
            error("MongoDB URI not configured")
        }
        val id = supportId.trim()
        if (id.isBlank()) {
            error("Support ID is required to restore")
        }
        val payloadJson = mongoBackupRepository.downloadPayloadJson(
            connectionUri = settings.mongoDbUri,
            databaseName = settings.mongoDbName,
            supportId = id
        )
        return backupManager.importFromJson(payloadJson)
    }

    fun getFoodLogsForDate(dateString: String): Flow<List<FoodLog>> = foodLogDao.getFoodLogsByDate(dateString)
    fun getExerciseLogsForDate(dateString: String): Flow<List<ExerciseLog>> = exerciseLogDao.getExerciseLogsByDate(dateString)

    fun getExerciseBurnToday(dateString: String): Flow<Int?> = exerciseLogDao.getTotalBurnedForDate(dateString)

    /** One query for all of today's food calories + macros (replaces four SUM observers). */
    fun getFoodTotalsToday(dateString: String): Flow<FoodTotals> = foodLogDao.getFoodTotalsForDate(dateString)

    fun getWeeklyFoodSummaries(): Flow<List<FoodDailySummary>> = foodLogDao.getHistoricalFoodSummaries(7)
    fun getMonthlyFoodSummaries(): Flow<List<FoodDailySummary>> = foodLogDao.getHistoricalFoodSummaries(30)

    fun getFoodSummariesBetween(startDate: String, endDate: String): Flow<List<FoodDailySummary>> =
        foodLogDao.getFoodSummariesBetween(startDate, endDate)

    fun getWeeklyExerciseSummaries(): Flow<List<ExerciseDailySummary>> = exerciseLogDao.getHistoricalExerciseSummaries(7)
    fun getMonthlyExerciseSummaries(): Flow<List<ExerciseDailySummary>> = exerciseLogDao.getHistoricalExerciseSummaries(30)

    fun getExerciseSummariesBetween(startDate: String, endDate: String): Flow<List<ExerciseDailySummary>> =
        exerciseLogDao.getExerciseSummariesBetween(startDate, endDate)

    /** All logged food days (newest first) for AI progress insights. */
    suspend fun getAllFoodDailySummaries(): List<FoodDailySummary> =
        foodLogDao.getAllFoodDailySummaries()

    /** All logged exercise days (newest first) for AI progress insights. */
    suspend fun getAllExerciseDailySummaries(): List<ExerciseDailySummary> =
        exerciseLogDao.getAllExerciseDailySummaries()

    /** All body readings (newest first) for AI progress insights. */
    suspend fun getAllBodyMeasurementsOnce(): List<BodyMeasurement> =
        bodyMeasurementDao.getAllOnce()

    suspend fun saveProfile(profile: UserProfile) {
        userProfileDao.insertOrUpdateProfile(profile)
    }

    suspend fun deleteFood(log: FoodLog) = foodLogDao.deleteFoodLog(log)

    /** Deleting an exercise log also removes its workout session + exercises, if it was mirrored from one. */
    suspend fun deleteExercise(log: ExerciseLog) {
        workoutSessionDao.getByExerciseLogId(log.id)?.let { session ->
            workoutExerciseDao.deleteForSession(session.id)
            workoutSessionDao.delete(session)
        }
        exerciseLogDao.deleteExerciseLog(log)
    }

    // --- Workout sessions -------------------------------------------------------------------

    /** A previously logged workout session plus its exercises, for viewing/editing. */
    data class WorkoutSessionDetails(
        val session: WorkoutSession,
        val exercises: List<WorkoutExercise>
    )

    /**
     * Looks up the workout session mirrored to [exerciseLogId] (if the exercise entry was logged
     * via the structured workout dialog rather than photo/text/preset) along with its exercises,
     * so the UI can show/edit the sets, reps and weight that were actually logged.
     */
    suspend fun getWorkoutDetails(exerciseLogId: Int): WorkoutSessionDetails? {
        val session = workoutSessionDao.getByExerciseLogId(exerciseLogId) ?: return null
        val exercises = workoutExerciseDao.getForSession(session.id).first()
        return WorkoutSessionDetails(session, exercises)
    }

    /**
     * Persists a workout session + its exercises, estimates calories burned (AI when configured,
     * else an offline MET-based estimate) using [contextJson] and [weightKg], then mirrors the
     * result into [ExerciseLog] so it counts toward daily burn totals/dashboard/progress like any
     * other exercise entry. Returns the estimate shown to the user.
     */
    suspend fun logWorkoutSession(
        draft: WorkoutDraft,
        weightKg: Double,
        contextJson: String,
        timestamp: Long = System.currentTimeMillis()
    ): WorkoutCaloriesResponse {
        val result = estimateWorkoutCalories(draft, weightKg, contextJson)

        val dateString = DateUtils.format(timestamp)

        val sessionId = workoutSessionDao.insert(
            WorkoutSession(
                name = draft.name,
                timestamp = timestamp,
                dateString = dateString,
                durationMinutes = draft.durationMinutes
            )
        ).toInt()

        workoutExerciseDao.insertAll(draft.exercises.toWorkoutExercises(sessionId))

        val exerciseLogId = exerciseLogDao.insertAll(
            listOf(
                ExerciseLog(
                    activityName = draft.name,
                    timestamp = timestamp,
                    dateString = dateString,
                    caloriesBurned = result.caloriesBurned,
                    durationMinutes = result.durationMinutes
                )
            )
        ).first().toInt()

        workoutSessionDao.insert(
            WorkoutSession(
                id = sessionId,
                name = draft.name,
                timestamp = timestamp,
                dateString = dateString,
                durationMinutes = result.durationMinutes,
                caloriesBurned = result.caloriesBurned,
                exerciseLogId = exerciseLogId
            )
        )

        return result
    }

    /**
     * Updates a previously logged workout session's name/duration/exercises in place and
     * re-estimates calories burned, mirroring the new total onto the linked [ExerciseLog] row
     * (identified by [exerciseLogId], if any) so dashboard/progress totals stay in sync.
     */
    suspend fun updateWorkoutSession(
        sessionId: Int,
        exerciseLogId: Int?,
        draft: WorkoutDraft,
        weightKg: Double,
        contextJson: String
    ): WorkoutCaloriesResponse {
        val existingSession = workoutSessionDao.getById(sessionId)
            ?: error("This workout no longer exists")

        val result = estimateWorkoutCalories(draft, weightKg, contextJson)

        workoutExerciseDao.deleteForSession(sessionId)
        workoutExerciseDao.insertAll(draft.exercises.toWorkoutExercises(sessionId))

        workoutSessionDao.insert(
            existingSession.copy(
                name = draft.name,
                durationMinutes = result.durationMinutes,
                caloriesBurned = result.caloriesBurned
            )
        )

        if (exerciseLogId != null) {
            exerciseLogDao.getById(exerciseLogId)?.let { existingLog ->
                exerciseLogDao.insertExerciseLog(
                    existingLog.copy(
                        activityName = draft.name,
                        caloriesBurned = result.caloriesBurned,
                        durationMinutes = result.durationMinutes
                    )
                )
            }
        }

        return result
    }

    private fun List<ExerciseDraft>.toWorkoutExercises(sessionId: Int) =
        mapIndexed { index, ex ->
            WorkoutExercise(
                sessionId = sessionId,
                name = ex.name,
                sets = ex.sets,
                reps = ex.reps,
                weightKg = ex.weightKg,
                orderIndex = index,
                equipment = ex.equipment,
                durationMinutes = ex.durationMinutes,
                distanceKm = ex.distanceKm
            )
        }

    /**
     * Estimates calories burned via AI when configured, otherwise uses the offline MET-based
     * estimate. When a provider is configured, network/API failures are surfaced to the caller
     * instead of silently falling back.
     */
    private suspend fun estimateWorkoutCalories(
        draft: WorkoutDraft,
        weightKg: Double,
        contextJson: String
    ): WorkoutCaloriesResponse {
        val settings = settingsRepository.settings.first()
        if (!settings.isConfigured) {
            return estimateWorkoutCaloriesOffline(draft, weightKg)
        }

        val (aiResult, _) = try {
            withAiFailover(settings) { s ->
                remoteAiDataSource.estimateWorkoutCalories(s, contextJson)
            }
        } catch (e: Exception) {
            throw IllegalStateException(formatAiConnectionError(e), e)
        }

        if (aiResult.caloriesBurned <= 0) {
            throw IllegalStateException("The AI returned an invalid calorie estimate. Please try again.")
        }
        return aiResult
    }

    /**
     * MET-based fallback used when no AI provider is configured, the AI call fails, or the AI
     * returns a non-positive estimate: picks a representative MET (metabolic equivalent) for the
     * session's dominant equipment type and scales by body weight and duration.
     * kcal = MET * weight_kg * hours.
     */
    private fun estimateWorkoutCaloriesOffline(draft: WorkoutDraft, weightKg: Double): WorkoutCaloriesResponse {
        val weight = weightKg.takeIf { it > 0 } ?: 70.0
        val equipmentSet = draft.exercises.map { it.equipment }.toSet()
        val met = when {
            Equipment.CARDIO in equipmentSet -> 7.0
            Equipment.BODYWEIGHT in equipmentSet && equipmentSet.size == 1 -> 5.0
            else -> 4.0 // dumbbell/barbell/bench/machine resistance training, averaged incl. rest
        }
        val duration = WorkoutDraft.estimateDurationMinutes(draft.exercises)
            .takeIf { draft.exercises.isNotEmpty() }
            ?: draft.durationMinutes.takeIf { it > 0 }
            ?: WorkoutDraft.DEFAULT_DURATION_MINUTES
        val calories = (met * weight * (duration / 60.0)).roundToInt().coerceAtLeast(1)
        return WorkoutCaloriesResponse(
            caloriesBurned = calories,
            durationMinutes = duration,
            intensityNote = "Estimated offline (no AI provider connected)"
        )
    }

    /**
     * Single entry point used by the ViewModel. Sends the loose text (and optional photo)
     * along with the locally computed user-state context to the AI, then routes the reply.
     *
     * Falls back to the bundled offline simulator when no API key is configured (text only).
     * When the preferred provider is configured: Auto rotates keys then models on that platform
     * only; if all fail, returns [AnalysisOutcome.Error] (user must change platform manually).
     */
    suspend fun analyze(
        userText: String,
        imageBytes: ByteArray?,
        userStateContextJson: String,
        forceEstimate: Boolean = false,
        customTimestamp: Long? = null
    ): AnalysisOutcome {
        val settings = settingsRepository.settings.first()
        val forceOffline = settings.developerModeUnlocked && settings.forceOfflineAiSimulator

        // Preferred provider configured: live AI with same-platform key/model failover.
        // Surface real failures instead of silently faking a result (silent offline fallback
        // made every photo come back as the simulator's "Mixed Plate").
        if (settings.isConfigured && !forceOffline) {
            return try {
                val dataUrl = imageBytes?.let {
                    "data:image/jpeg;base64," + Base64.encodeToString(it, Base64.NO_WRAP)
                }
                val (response, failoverNote) = withAiFailover(
                    settings,
                    preferVisionModels = imageBytes != null
                ) { active ->
                    remoteAiDataSource.analyze(
                        active, userText, userStateContextJson, dataUrl, forceEstimate
                    )
                }
                processResponse(response, customTimestamp = customTimestamp, userText = userText)
                    .withFailoverNote(failoverNote)
            } catch (e: Exception) {
                AnalysisOutcome.Error(formatAiConnectionError(e))
            }
        }

        // Not configured / forced offline: the offline simulator cannot read images.
        if (imageBytes != null) {
            return AnalysisOutcome.Error(
                if (forceOffline) {
                    "Force offline simulator can't analyse photos. Turn it off or use text."
                } else {
                    "Connect an AI provider in Settings to analyse food photos."
                }
            )
        }

        return try {
            processResponse(
                simulateAIService(userText, inferMode(userText, false)),
                customTimestamp = customTimestamp,
                userText = userText
            )
        } catch (e: Exception) {
            AnalysisOutcome.Error(e.message ?: "Analysis failed")
        }
    }

    /** Last raw AI JSON from the remote layer (developer "Show raw AI JSON"). */
    fun lastRawAiJson(): String? = remoteAiDataSource.lastRawJson

    /**
     * Reads the payload `status` and routes the data, returning a clean outcome.
     * - SUCCESS -> build an editable [FoodDraft] for review (NOT persisted yet)
     * - EXERCISE_LOGGED -> insert exercise, return [AnalysisOutcome.ExerciseSaved]
     * - CLARIFICATION_REQUIRED -> surface the question to the UI
     */
    suspend fun processResponse(
        response: FitnessTrackerResponse,
        customTimestamp: Long? = null,
        userText: String = ""
    ): AnalysisOutcome {
        val timestamp = customTimestamp ?: System.currentTimeMillis()
        val dateString = DateUtils.format(timestamp)

        return when (response.status) {
            "SUCCESS" -> {
                val food = response.foodAnalysis
                    ?: return AnalysisOutcome.Error("Missing food analysis in response")
                AnalysisOutcome.FoodReady(buildFoodDraft(food, timestamp, userText))
            }

            "EXERCISE_LOGGED" -> {
                val exercise = response.exerciseAnalysis
                val activityName = exercise?.activityDetected
                val caloriesBurned = exercise?.caloriesBurned
                if (activityName == null || caloriesBurned == null) {
                    return AnalysisOutcome.Error("Missing exercise analysis in response")
                }
                val durationMinutes = exercise.durationMinutes ?: 30
                exerciseLogDao.insertExerciseLog(
                    ExerciseLog(
                        activityName = activityName,
                        timestamp = timestamp,
                        dateString = dateString,
                        caloriesBurned = caloriesBurned,
                        durationMinutes = durationMinutes
                    )
                )
                AnalysisOutcome.ExerciseSaved(activityName, caloriesBurned)
            }

            "CLARIFICATION_REQUIRED" -> AnalysisOutcome.NeedsClarification(
                response.clarificationMessage ?: "Could you add a few more details?"
            )

            "NOT_IDENTIFIED" -> AnalysisOutcome.NotIdentified(
                response.clarificationMessage ?: "Couldn't identify any food in this item."
            )

            else -> AnalysisOutcome.Error("Unexpected status: ${response.status}")
        }
    }

    /**
     * Converts an AI [FoodAnalysis] into an editable [FoodDraft]. Uses the returned ingredient
     * breakdown when present; otherwise falls back to a single "Estimated serving" ingredient
     * (assumed 250 g) carrying the whole-dish macros so the user can still adjust the portion.
     */
    private fun buildFoodDraft(food: FoodAnalysis, timestamp: Long, userText: String = ""): FoodDraft {
        val ingredients = food.ingredients
            ?.filter { it.weightG > 0 || it.calories > 0 }
            ?.takeIf { it.isNotEmpty() }
            ?.map { ing ->
                val quantity = FoodQuantityParser.quantityForIngredient(
                    userText = userText,
                    ingredientName = ing.name,
                    aiQuantity = ing.quantity
                )
                IngredientDraft.fromAbsolute(
                    name = ing.name,
                    weightG = ing.weightG.coerceAtLeast(1),
                    calories = ing.calories,
                    protein = ing.proteinG,
                    carbs = ing.carbsG,
                    fats = ing.fatsG,
                    quantity = quantity
                )
            }
            ?: listOf(
                IngredientDraft.fromAbsolute(
                    name = "Estimated serving",
                    weightG = 250,
                    calories = food.macros.calories,
                    protein = food.macros.proteinG,
                    carbs = food.macros.carbsG,
                    fats = food.macros.fatsG
                )
            )
        // Dish-level macros are ignored; FoodDraft totals are the sum of ingredients.
        return FoodDraft(dishName = food.dishName, timestamp = timestamp, ingredients = ingredients)
    }

    /** OpenRouter vision models for the Settings dropdown (free, or free+paid). */
    suspend fun fetchFreeVisionModels(
        apiKey: String,
        includePaid: Boolean = false
    ): List<ModelOption> =
        remoteAiDataSource.fetchFreeVisionModels(apiKey, includePaid)

    /** OpenRouter chat models for the text-query model dropdown. */
    suspend fun fetchFreeModels(
        apiKey: String,
        includePaid: Boolean = false
    ): List<ModelOption> =
        remoteAiDataSource.fetchFreeModels(apiKey, includePaid)

    /** Vision-capable Gemini models for the Settings dropdown. */
    suspend fun fetchGeminiVisionModels(
        apiKey: String,
        includePaid: Boolean = false
    ): List<ModelOption> =
        remoteAiDataSource.fetchGeminiVisionModels(apiKey, includePaid)

    /** Gemini chat models for the text-query model dropdown. */
    suspend fun fetchGeminiTextModels(
        apiKey: String,
        includePaid: Boolean = false
    ): List<ModelOption> =
        remoteAiDataSource.fetchGeminiTextModels(apiKey, includePaid)

    /** Vision-capable Ollama models (local or Cloud). */
    suspend fun fetchOllamaVisionModels(baseUrl: String, apiKey: String = ""): List<ModelOption> =
        remoteAiDataSource.fetchOllamaVisionModels(baseUrl, apiKey)

    /** All Ollama models on the host for the text-query dropdown. */
    suspend fun fetchOllamaTextModels(baseUrl: String, apiKey: String = ""): List<ModelOption> =
        remoteAiDataSource.fetchOllamaTextModels(baseUrl, apiKey)

    /**
     * Drops models that do not answer chat with HTTP 200 or 429 (Refresh-models reachability).
     */
    suspend fun filterReachableModels(
        models: List<ModelOption>,
        chatUrl: String,
        authHeader: String?
    ): List<ModelOption> =
        remoteAiDataSource.filterReachableModels(models, chatUrl, authHeader)

    /**
     * Persists a user-confirmed (possibly edited) meal to Room using its live totals. Pass a
     * non-zero [id] to update an existing log in place (REPLACE conflict strategy); the default
     * (0) auto-generates a new row.
     */
    /**
     * Persists a user-confirmed meal (one or more foods) to Room. Pass a non-zero [id] to update
     * an existing meal in place; the default (0) inserts a new row.
     */
    suspend fun saveMealDraft(draft: MealDraft, id: Int = 0) {
        val mealLog = FoodLog(
            id = id,
            dishName = draft.name,
            timestamp = draft.timestamp,
            dateString = DateUtils.format(draft.timestamp),
            calories = draft.totalCalories,
            proteinG = draft.totalProtein,
            carbsG = draft.totalCarbs,
            fatsG = draft.totalFats
        )
        val mealId = if (id != 0) {
            foodLogDao.insertFoodLog(mealLog)
            id
        } else {
            foodLogDao.insertFoodLogReturningId(mealLog).toInt()
        }
        mealFoodDao.deleteForMeal(mealId)
        mealFoodDao.insertAll(draft.foods.mapIndexed { index, food -> food.toMealFood(mealId, index) })
    }

    /** Convenience: log a single reviewed food as a one-food meal. */
    suspend fun saveFoodDraft(draft: FoodDraft, id: Int = 0) {
        saveMealDraft(draft.toSingleFoodMeal(), id)
    }

    data class MealDetails(
        val meal: FoodLog,
        val foods: List<MealFood>
    )

    suspend fun getMealDetails(mealLogId: Int): MealDetails? {
        val meal = foodLogDao.getById(mealLogId) ?: return null
        val foods = mealFoodDao.getForMealOnce(mealLogId)
        return MealDetails(meal, foods)
    }

    /** Rebuilds an editable meal draft from a stored log, preserving the food → ingredient tree. */
    suspend fun mealLogToMealDraft(log: FoodLog): MealDraft {
        val foods = mealFoodDao.getForMealOnce(log.id)
        val entries = if (foods.isNotEmpty()) {
            foods.map { it.toFoodEntryDraft() }
        } else {
            listOf(legacyMealFoodFallback(log))
        }
        return MealDraft(
            name = log.dishName,
            timestamp = log.timestamp,
            foods = entries
        )
    }

    /** @deprecated Use [mealLogToMealDraft]; kept for single-food edit paths. */
    suspend fun foodLogToDraft(log: FoodLog): FoodDraft {
        val meal = mealLogToMealDraft(log)
        val first = meal.foods.firstOrNull() ?: legacyMealFoodFallback(log)
        return first.toFoodDraft().copy(dishName = log.dishName, timestamp = log.timestamp)
    }

    private fun legacyMealFoodFallback(log: FoodLog): FoodEntryDraft = FoodEntryDraft(
        name = log.dishName,
        servings = 1.0,
        ingredients = listOf(
            IngredientDraft.fromAbsolute(
                name = "Estimated serving",
                weightG = 250,
                calories = log.calories,
                protein = log.proteinG,
                carbs = log.carbsG,
                fats = log.fatsG
            )
        )
    )

    private fun FoodEntryDraft.toMealFood(mealLogId: Int, orderIndex: Int): MealFood = MealFood(
        mealLogId = mealLogId,
        name = name,
        servings = servings,
        orderIndex = orderIndex,
        calories = totalCalories,
        proteinG = totalProtein,
        carbsG = totalCarbs,
        fatsG = totalFats,
        ingredients = ingredients.map { LoggedIngredient.fromIngredientDraft(it) },
        presetId = presetId,
        barcode = barcode
    )

    private fun MealFood.toFoodEntryDraft(): FoodEntryDraft = FoodEntryDraft(
        name = name,
        servings = servings,
        ingredients = ingredients
            ?.takeIf { it.isNotEmpty() }
            ?.map { it.toIngredientDraft() }
            ?: listOf(
                IngredientDraft.fromAbsolute(
                    name = "Estimated serving",
                    weightG = 250,
                    calories = calories,
                    protein = proteinG,
                    carbs = carbsG,
                    fats = fatsG
                )
            ),
        presetId = presetId,
        barcode = barcode
    )

    // --- Saved foods & meal presets ---------------------------------------------------------

    /** Saves a single food to the library (for meal building, not dashboard quick-log). */
    suspend fun saveDraftAsSavedFood(draft: FoodDraft) {
        savedFoodDao.insert(
            SavedFood(
                name = draft.dishName,
                calories = draft.totalCalories,
                proteinG = draft.totalProtein,
                carbsG = draft.totalCarbs,
                fatsG = draft.totalFats,
                createdAt = System.currentTimeMillis(),
                ingredients = draft.ingredients.map { LoggedIngredient.fromIngredientDraft(it) }
            )
        )
    }

    /** Saves a full meal as a reusable preset for one-tap dashboard logging. */
    suspend fun saveMealDraftAsPreset(draft: MealDraft) {
        mealPresetDao.insert(
            MealPreset(
                name = draft.name,
                calories = draft.totalCalories,
                proteinG = draft.totalProtein,
                carbsG = draft.totalCarbs,
                fatsG = draft.totalFats,
                createdAt = System.currentTimeMillis(),
                foods = draft.foods.map { it.toPresetMealFood() }
            )
        )
    }

    suspend fun deleteSavedFood(food: SavedFood) = savedFoodDao.delete(food)

    suspend fun deleteMealPreset(preset: MealPreset) = mealPresetDao.delete(preset)

    /** Looks up a packaged product by EAN/UPC barcode via Open Food Facts. */
    suspend fun lookupProductByBarcode(barcode: String): ScannedProduct {
        val code = OpenFoodFactsDataSource.normalizeBarcode(barcode)
        require(code.isNotBlank()) { "Invalid barcode" }
        savedFoodDao.findByBarcode(code)?.let { saved ->
            return ScannedProduct(
                barcode = code,
                name = saved.name,
                calories = saved.calories,
                proteinG = saved.proteinG,
                carbsG = saved.carbsG,
                fatsG = saved.fatsG
            )
        }
        return openFoodFactsDataSource.lookupBarcode(code)
    }

    /** Saves a scanned product to the saved-food library. */
    suspend fun saveScannedProductAsSavedFood(product: ScannedProduct) {
        val entry = product.toFoodEntry()
        savedFoodDao.insert(
            SavedFood(
                name = product.name,
                calories = product.calories,
                proteinG = product.proteinG,
                carbsG = product.carbsG,
                fatsG = product.fatsG,
                createdAt = System.currentTimeMillis(),
                barcode = product.barcode,
                ingredients = entry.ingredients.map { LoggedIngredient.fromIngredientDraft(it) }
            )
        )
    }

    /**
     * Normalises [rawName] via AI when configured (else offline heuristics), saves it for the
     * workout picker if new, and returns the canonical exercise entry.
     */
    suspend fun classifyCustomExercise(rawName: String): CommonExercise {
        val trimmed = rawName.trim()
        require(trimmed.isNotBlank()) { "Exercise name is required" }

        findKnownExercise(trimmed)?.let { return it }

        val settings = settingsRepository.settings.first()
        val knownNames = buildExercisePickerList(
            exercisePresetDao.getAllOnce().map { it.name to it.equipment }
        ).map { it.name }

        val classified = if (settings.isConfigured) {
            runCatching {
                val (result, _) = withAiFailover(settings) { s ->
                    remoteAiDataSource.classifyExercise(s, trimmed, knownNames)
                }
                result
            }.getOrElse { classifyExerciseOffline(trimmed) }
        } else {
            classifyExerciseOffline(trimmed)
        }

        val name = classified.canonicalName.trim().ifBlank { trimmed }.let(::titleCaseExerciseName)
        val equipment = normaliseEquipment(classified.equipment)
        val exercise = CommonExercise(name, equipment)

        findKnownExercise(name)?.let { return it }

        ensureExercisePreset(name, equipment)
        return exercise
    }

    /**
     * Parses a natural-language workout description into [ExerciseDraft] rows via AI.
     * Requires a configured AI provider.
     */
    suspend fun parseWorkoutDescription(description: String): List<ExerciseDraft> {
        val trimmed = description.trim()
        require(trimmed.isNotBlank()) { "Describe the exercises you want to add" }

        val settings = settingsRepository.settings.first()
        check(settings.isConfigured) {
            "Connect an AI provider in Settings to infer exercises from text."
        }

        val knownNames = buildExercisePickerList(
            exercisePresetDao.getAllOnce().map { it.name to it.equipment }
        ).map { it.name }

        val (parsed, _) = withAiFailover(settings) { s ->
            remoteAiDataSource.parseWorkoutDescription(s, trimmed, knownNames)
        }
        if (parsed.exercises.isEmpty()) {
            throw IllegalStateException("Couldn't find any exercises in that description.")
        }

        return parsed.exercises.map { row ->
            val name = titleCaseExerciseName(row.name.trim().ifBlank { "Exercise" })
            val equipment = normaliseEquipment(row.equipment)
            ensureExercisePreset(name, equipment)
            val isCardio = equipment == Equipment.CARDIO
            ExerciseDraft(
                name = name,
                sets = if (isCardio) 1 else row.sets.coerceAtLeast(1),
                reps = if (isCardio) 1 else row.reps.coerceAtLeast(1),
                weightKg = row.weightKg?.takeIf { it > 0 },
                equipment = equipment,
                durationMinutes = row.durationMinutes?.takeIf { it > 0 },
                distanceKm = row.distanceKm?.takeIf { it > 0 }
            )
        }
    }

    private suspend fun ensureExercisePreset(name: String, equipment: String) {
        if (exercisePresetDao.findByName(name) == null &&
            COMMON_EXERCISES.none { it.name.equals(name, ignoreCase = true) }
        ) {
            exercisePresetDao.insertPreset(
                ExercisePreset(
                    name = name,
                    equipment = equipment,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    private fun titleCaseExerciseName(raw: String): String =
        raw.split("\\s+".toRegex()).joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

    private suspend fun findKnownExercise(name: String): CommonExercise? {
        COMMON_EXERCISES.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
        exercisePresetDao.findByName(name)?.let { return CommonExercise(it.name, it.equipment) }
        return null
    }

    private fun classifyExerciseOffline(rawName: String): CustomExerciseResponse {
        val name = rawName.trim().split("\\s+".toRegex()).joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        val lower = rawName.lowercase()
        val equipment = when {
            "dumbbell" in lower || lower.startsWith("db ") || " db" in lower -> Equipment.DUMBBELL
            "barbell" in lower || lower.startsWith("bb ") -> Equipment.BARBELL
            "treadmill" in lower || "bike" in lower || "rower" in lower ||
                "cardio" in lower || "run" in lower -> Equipment.CARDIO
            "pull-up" in lower || "push-up" in lower || "pushup" in lower ||
                "pullup" in lower || "bodyweight" in lower -> Equipment.BODYWEIGHT
            "machine" in lower || "cable" in lower || "pulldown" in lower -> Equipment.MACHINE
            "bench" in lower || "press" in lower -> Equipment.BENCH
            else -> Equipment.OTHER
        }
        return CustomExerciseResponse(name, equipment)
    }

    private fun normaliseEquipment(raw: String): String {
        val trimmed = raw.trim()
        val allowed = listOf(
            Equipment.DUMBBELL,
            Equipment.BENCH,
            Equipment.BARBELL,
            Equipment.BODYWEIGHT,
            Equipment.MACHINE,
            Equipment.CARDIO,
            Equipment.OTHER
        )
        return allowed.firstOrNull { it.equals(trimmed, ignoreCase = true) } ?: Equipment.OTHER
    }

    /** Quick-logs a saved meal preset onto [timestamp]'s calendar day (default: now). */
    suspend fun logMealPreset(
        preset: MealPreset,
        timestamp: Long = System.currentTimeMillis()
    ) {
        saveMealDraft(preset.toMealDraft().copy(timestamp = timestamp))
    }

    /** Quick-logs a single saved food as a one-food meal onto [timestamp]'s calendar day. */
    suspend fun logSavedFood(
        food: SavedFood,
        timestamp: Long = System.currentTimeMillis()
    ) {
        saveMealDraft(
            MealDraft(
                name = food.name,
                timestamp = timestamp,
                foods = listOf(food.toFoodEntry())
            )
        )
    }

    /** Heuristic used only by the offline simulator to pick a plausible response type. */
    private fun inferMode(text: String, hasImage: Boolean): String {
        val query = text.lowercase()
        val exerciseKeywords = listOf(
            "walk", "ran", "run", "jog", "gym", "workout", "exercise",
            "yoga", "cricket", "swim", "cycle", "cycling", "lifting", "weight", "play"
        )
        return when {
            exerciseKeywords.any { it in query } -> "EXERCISE_LOGGED"
            text.isBlank() && !hasImage -> "CLARIFICATION_REQUIRED"
            else -> "SUCCESS"
        }
    }

    /**
     * MOCK API Sandbox Engine.
     * Processes input string and produces simulated FitnessTrackerResponses
     * so that the user can immediately play with the AI functionality offline.
     */
    fun simulateAIService(input: String, mode: String): FitnessTrackerResponse {
        val query = input.lowercase().trim()
        return when (mode) {
            "SUCCESS" -> {
                // Align totals with [NorthIndianStaples] mid-range priors where possible.
                val s = NorthIndianStaples
                val (dish, c, p, cr, f) = when {
                    "chole" in query || "chana masala" in query || "chole bhature" in query ||
                        "bhature" in query || "bhatura" in query -> {
                        val chole = s.CHOLE_KATORI
                        val bhatura = s.BHATURA
                        val ghee = s.GHEE_TSP
                        Quintet(
                            "Chole Bhature",
                            chole.calories + bhatura.calories + ghee.calories,
                            chole.proteinG + bhatura.proteinG,
                            chole.carbsG + bhatura.carbsG,
                            chole.fatsG + bhatura.fatsG + ghee.fatsG
                        )
                    }
                    "rajma" in query -> {
                        val dal = s.DAL_KATORI.copy(name = "Rajma")
                        val rice = s.RICE_BOWL
                        Quintet(
                            "Rajma Chawal",
                            dal.calories + rice.calories,
                            dal.proteinG + rice.proteinG,
                            dal.carbsG + rice.carbsG,
                            dal.fatsG + rice.fatsG
                        )
                    }
                    "paratha" in query || "parantha" in query -> {
                        val paratha = s.PARATHA
                        val curd = s.CURD_KATORI
                        Quintet(
                            "Aloo Paratha with Curd",
                            paratha.calories + curd.calories,
                            paratha.proteinG + curd.proteinG,
                            paratha.carbsG + curd.carbsG,
                            paratha.fatsG + curd.fatsG
                        )
                    }
                    "kadhi" in query -> Quintet("Kadhi Pakora with Rice", 450, 14, 58, 16)
                    "dal makhani" in query -> {
                        val dal = s.DAL_KATORI.copy(calories = 220, proteinG = 12, carbsG = 24, fatsG = 10)
                        val roti = s.ROTI
                        val ghee = s.GHEE_TSP
                        Quintet(
                            "Dal Makhani with Roti",
                            dal.calories + roti.calories + ghee.calories,
                            dal.proteinG + roti.proteinG,
                            dal.carbsG + roti.carbsG,
                            dal.fatsG + roti.fatsG + ghee.fatsG
                        )
                    }
                    "butter chicken" in query || "murgh makhani" in query ->
                        Quintet("Butter Chicken with Naan", 680, 36, 52, 32)
                    "palak paneer" in query -> {
                        val roti = s.ROTI
                        Quintet(
                            "Palak Paneer with Roti",
                            375 + roti.calories,
                            19 + roti.proteinG,
                            20 + roti.carbsG,
                            22 + roti.fatsG
                        )
                    }
                    "samosa" in query -> {
                        val samosa = s.SAMOSA
                        Quintet(
                            "Samosa (1 piece)",
                            samosa.calories,
                            samosa.proteinG,
                            samosa.carbsG,
                            samosa.fatsG
                        )
                    }
                    "biryani" in query -> Quintet("Chicken Biryani", 540, 28, 62, 18)
                    "paneer" in query -> {
                        val roti = s.ROTI
                        Quintet(
                            "Paneer Butter Masala & Roti",
                            515 + roti.calories,
                            21 + roti.proteinG,
                            50 + roti.carbsG,
                            24 + roti.fatsG
                        )
                    }
                    "lassi" in query -> Quintet("Sweet Lassi", 220, 8, 32, 6)
                    "chai" in query || "tea" in query -> Quintet("Indian Milk Chai", 110, 3, 16, 4)
                    "sabzi" in query || "sabji" in query -> {
                        val roti = s.ROTI
                        val sabzi = s.SABZI_DRY
                        val ghee = s.GHEE_TSP
                        Quintet(
                            "Roti & Mixed Sabzi",
                            roti.calories * 2 + sabzi.calories + ghee.calories,
                            roti.proteinG * 2 + sabzi.proteinG,
                            roti.carbsG * 2 + sabzi.carbsG,
                            roti.fatsG * 2 + sabzi.fatsG + ghee.fatsG
                        )
                    }
                    "dal" in query && "chawal" in query -> {
                        val dal = s.DAL_KATORI
                        val rice = s.RICE_BOWL
                        Quintet(
                            "Dal Chawal",
                            dal.calories + rice.calories,
                            dal.proteinG + rice.proteinG,
                            dal.carbsG + rice.carbsG,
                            dal.fatsG + rice.fatsG
                        )
                    }
                    "dal" in query -> {
                        val dal = s.DAL_KATORI
                        val roti = s.ROTI
                        val ghee = s.GHEE_TSP
                        Quintet(
                            "Dal Tadka with Roti",
                            dal.calories + roti.calories + ghee.calories,
                            dal.proteinG + roti.proteinG,
                            dal.carbsG + roti.carbsG,
                            dal.fatsG + roti.fatsG + ghee.fatsG
                        )
                    }
                    "roti" in query || "chapati" in query || "phulka" in query -> {
                        val roti = s.ROTI
                        val dal = s.DAL_KATORI
                        Quintet(
                            "Wheat Roti & Dal",
                            roti.calories * 2 + dal.calories,
                            roti.proteinG * 2 + dal.proteinG,
                            roti.carbsG * 2 + dal.carbsG,
                            roti.fatsG * 2 + dal.fatsG
                        )
                    }
                    "dosa" in query -> Quintet("Masala Dosa with Chutney", 390, 8, 54, 14)
                    "idli" in query -> Quintet("Idli with Sambhar (2 pcs)", 210, 6, 40, 2)
                    "almond" in query -> {
                        val qty = FoodQuantityParser.parseSegments(input).firstOrNull()?.quantity ?: 1
                        Quintet(
                            if (qty > 1) "Almonds ($qty)" else "Almond",
                            qty * 7,
                            qty,
                            qty,
                            qty * 6
                        )
                    }
                    else -> Quintet(
                        input.ifBlank { "North Indian Thali" }
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                        350, 12, 45, 10
                    )
                }
                FitnessTrackerResponse(
                    status = "SUCCESS",
                    clarificationMessage = null,
                    foodAnalysis = FoodAnalysis(
                        dishName = dish,
                        macros = Macros(c, p, cr, f),
                        ingredients = buildSimIngredients(input, query, c, p, cr, f)
                    ),
                    exerciseAnalysis = null
                )
            }
            "EXERCISE_LOGGED" -> {
                val (act, burn, dur) = when {
                    "walk" in query || "walked" in query -> Triplet("Brisk Walk", 180, 40)
                    "run" in query || "jog" in query || "jogged" in query -> Triplet("Outdoor Running", 450, 35)
                    "gym" in query || "weight" in query || "lifting" in query -> Triplet("Weight Training", 240, 45)
                    "yoga" in query -> Triplet("Surya Namaskar & Yoga", 150, 30)
                    "cricket" in query || "play" in query -> Triplet("Cricket Match Play", 320, 60)
                    else -> Triplet("Cardio Workout", 280, 30)
                }
                FitnessTrackerResponse(
                    status = "EXERCISE_LOGGED",
                    clarificationMessage = null,
                    foodAnalysis = null,
                    exerciseAnalysis = ExerciseAnalysis(
                        activityDetected = act,
                        caloriesBurned = burn,
                        durationMinutes = dur
                    )
                )
            }
            "CLARIFICATION_REQUIRED" -> {
                val message = when {
                    query.isEmpty() -> "What did you consume or what exercise did you perform?"
                    "samosa" in query -> "Did you have 1 single samosa, or a plate of 2?"
                    "biryani" in query -> "Was it veg biryani, or chicken biryani? Also, specify the portion size."
                    "paratha" in query || "parantha" in query ->
                        "Was it aloo, gobi, or plain paratha? How many, and with curd or butter?"
                    "chole" in query || "bhature" in query ->
                        "How many bhaturas, and was it a full chole plate or a small serving?"
                    "chai" in query -> "Was there sugar added to your tea, and did you use full-cream milk?"
                    else -> "Could you specify the quantity or serving portion of '$input' for a precise analysis?"
                }
                FitnessTrackerResponse(
                    status = "CLARIFICATION_REQUIRED",
                    clarificationMessage = message,
                    foodAnalysis = null,
                    exerciseAnalysis = null
                )
            }
            else -> {
                FitnessTrackerResponse("UNKNOWN", "Invalid simulator selection", null, null)
            }
        }
    }

    /**
     * Offline-only: splits a dish's total macros across a plausible ingredient list (name, weight,
     * fraction of macros). The last ingredient absorbs rounding remainder so per-ingredient sums
     * exactly equal the dish totals.
     */
    private fun buildSimIngredients(
        input: String,
        query: String,
        c: Int,
        p: Int,
        cr: Int,
        f: Int
    ): List<Ingredient> {
        // name to (weightG, fractionOfMacros)
        val parts: List<Pair<String, Pair<Int, Double>>> = when {
            "chole" in query || "bhature" in query || "bhatura" in query -> listOf(
                "Chole" to (NorthIndianStaples.CHOLE_KATORI.weightG to 0.38),
                "Bhatura" to (NorthIndianStaples.BHATURA.weightG to 0.52),
                "Ghee/oil" to (NorthIndianStaples.GHEE_TSP.weightG to 0.10)
            )
            "rajma" in query -> listOf(
                "Rajma" to (NorthIndianStaples.DAL_KATORI.weightG to 0.45),
                "Cooked rice" to (NorthIndianStaples.RICE_BOWL.weightG to 0.55)
            )
            "paratha" in query || "parantha" in query -> listOf(
                "Aloo paratha" to (NorthIndianStaples.PARATHA.weightG to 0.70),
                "Curd" to (NorthIndianStaples.CURD_KATORI.weightG to 0.30)
            )
            "kadhi" in query -> listOf(
                "Kadhi pakora" to (200 to 0.55), "Cooked rice" to (150 to 0.45)
            )
            "dal makhani" in query -> listOf(
                "Dal makhani" to (NorthIndianStaples.DAL_KATORI.weightG to 0.60),
                "Wheat roti" to (NorthIndianStaples.ROTI.weightG to 0.28),
                "Ghee/oil" to (NorthIndianStaples.GHEE_TSP.weightG to 0.12)
            )
            "butter chicken" in query || "murgh makhani" in query -> listOf(
                "Butter chicken" to (200 to 0.55), "Naan" to (100 to 0.45)
            )
            "palak paneer" in query -> listOf(
                "Palak paneer" to (180 to 0.70), "Wheat roti" to (70 to 0.30)
            )
            "samosa" in query -> listOf(
                "Samosa" to (NorthIndianStaples.SAMOSA.weightG to 1.0)
            )
            "biryani" in query -> listOf(
                "Basmati rice" to (180 to 0.45), "Chicken" to (90 to 0.40), "Oil & spices" to (20 to 0.15)
            )
            "paneer" in query -> listOf(
                "Paneer" to (100 to 0.40), "Butter gravy" to (120 to 0.35), "Roti" to (60 to 0.25)
            )
            "lassi" in query -> listOf(
                "Curd" to (200 to 0.55), "Sugar" to (20 to 0.35), "Milk" to (50 to 0.10)
            )
            "chai" in query || "tea" in query -> listOf(
                "Milk" to (100 to 0.60), "Sugar" to (10 to 0.30), "Tea decoction" to (90 to 0.10)
            )
            "sabzi" in query || "sabji" in query -> listOf(
                "Wheat roti" to (NorthIndianStaples.ROTI.weightG * 2 to 0.40),
                "Mixed sabzi" to (NorthIndianStaples.SABZI_DRY.weightG to 0.50),
                "Ghee/oil" to (NorthIndianStaples.GHEE_TSP.weightG to 0.10)
            )
            "dal" in query && "chawal" in query -> listOf(
                "Dal tadka" to (NorthIndianStaples.DAL_KATORI.weightG to 0.40),
                "Cooked rice" to (NorthIndianStaples.RICE_BOWL.weightG to 0.60)
            )
            "dal" in query -> listOf(
                "Dal tadka" to (NorthIndianStaples.DAL_KATORI.weightG to 0.50),
                "Wheat roti" to (NorthIndianStaples.ROTI.weightG to 0.38),
                "Ghee/oil" to (NorthIndianStaples.GHEE_TSP.weightG to 0.12)
            )
            "dosa" in query -> listOf(
                "Dosa crepe" to (90 to 0.45), "Potato masala" to (80 to 0.40), "Chutney" to (40 to 0.15)
            )
            "roti" in query || "chapati" in query || "phulka" in query -> listOf(
                "Wheat roti" to (NorthIndianStaples.ROTI.weightG * 2 to 0.45),
                "Dal" to (NorthIndianStaples.DAL_KATORI.weightG to 0.55)
            )
            "idli" in query -> listOf(
                "Idli" to (120 to 0.50), "Sambhar" to (150 to 0.50)
            )
            "almond" in query -> {
                val qty = FoodQuantityParser.parseSegments(input).firstOrNull()?.quantity ?: 1
                listOf("Almond" to (qty to 1.0))
            }
            else -> {
                val parsed = FoodQuantityParser.parseSegments(input)
                if (parsed.size == 1) {
                    listOf(parsed[0].name.replaceFirstChar { it.titlecase() } to (parsed[0].quantity to 1.0))
                } else {
                    listOf("Estimated serving" to (250 to 1.0))
                }
            }
        }

        var accC = 0; var accP = 0; var accCr = 0; var accF = 0
        return parts.mapIndexed { index, (name, spec) ->
            val (weightOrQty, frac) = spec
            val isLast = index == parts.lastIndex
            val ic = if (isLast) c - accC else Math.round(c * frac).toInt()
            val ip = if (isLast) p - accP else Math.round(p * frac).toInt()
            val icr = if (isLast) cr - accCr else Math.round(cr * frac).toInt()
            val ifa = if (isLast) f - accF else Math.round(f * frac).toInt()
            accC += ic; accP += ip; accCr += icr; accF += ifa

            val isDiscreteSingle = frac >= 1.0 && parts.size == 1 && weightOrQty > 1
            val quantity = if (isDiscreteSingle) weightOrQty else 1
            val weightG = if (isDiscreteSingle) weightOrQty else weightOrQty

            Ingredient(
                name = name,
                quantity = quantity,
                weightG = weightG,
                calories = ic,
                proteinG = ip,
                carbsG = icr,
                fatsG = ifa
            )
        }
    }

    private data class Quintet<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
    private data class Triplet<A, B, C>(val first: A, val second: B, val third: C)

    /**
     * Tries AI calls within the preferred platform when [AppSettings.aiAutoFailover] is on:
     * same model + next API key → other models. Never switches platforms — if every key/model
     * fails (timeout, rate limit, etc.), the last error is thrown so the user can change
     * platform in Settings.
     * Rate-limited models are skipped until the next UTC midnight (persisted across app
     * restarts). After that, newer requests try the preferred selected model first again.
     * No background retry. Success updates the green “active” model only — not the dropdown.
     * When Auto is off: preferred provider + selected model only; rotates API keys on failure,
     * then surfaces the error (no model or platform change).
     */
    private suspend fun <T> withAiFailover(
        settings: AppSettings,
        preferVisionModels: Boolean = false,
        block: suspend (AppSettings) -> T
    ): Pair<T, String?> {
        check(settings.isConfigured) { "No AI provider configured" }

        val platform = settings.provider
        val preferredSelected = settings.modelFor(preferVisionModels)
        val keys = attemptKeys(settings, platform)

        if (!settings.aiAutoFailover) {
            var lastError: Exception? = null
            for (key in keys) {
                try {
                    val attempt = settings.withKey(platform, key)
                    val result = block(attempt)
                    settingsRepository.setActiveAiModel(
                        platform,
                        attempt.modelFor(preferVisionModels),
                        forPhoto = preferVisionModels
                    )
                    return result to null
                } catch (e: Exception) {
                    lastError = e
                }
            }
            throw lastError ?: IllegalStateException("Couldn't connect to AI")
        }

        val now = System.currentTimeMillis()
        val cooldowns = settingsRepository.modelCooldowns()
        var lastError: Exception? = null
        val models = modelLadder(settings, platform, preferVisionModels)
            .filter { modelId ->
                val until = cooldowns[ModelCooldown.keyOf(platform, modelId)] ?: 0L
                until <= now
            }
        for (modelId in models) {
            var rateLimitedOnModel = false
            var lastModelError: Exception? = null
            for (key in keys) {
                try {
                    val attempt = settings
                        .withModel(platform, modelId)
                        .withKey(platform, key)
                    val result = block(attempt)
                    settingsRepository.setActiveAiModel(
                        platform,
                        modelId,
                        forPhoto = preferVisionModels
                    )
                    val note = if (
                        modelId != preferredSelected && preferredSelected.isNotBlank()
                    ) {
                        val modality = if (preferVisionModels) "photo" else "text"
                        "Auto failover · $modality model → $modelId"
                    } else {
                        null
                    }
                    return result to note
                } catch (e: Exception) {
                    lastError = e
                    lastModelError = e
                    if (ModelCooldownPolicy.isRateLimitError(e)) rateLimitedOnModel = true
                }
            }
            if (rateLimitedOnModel && lastModelError != null) {
                settingsRepository.markModelCooldown(platform, modelId, lastModelError, now)
            }
        }
        throw lastError ?: IllegalStateException("Couldn't connect to AI")
    }

    /** Keys to try for [platform]; local Ollama uses a single empty key (no auth). */
    private fun attemptKeys(settings: AppSettings, platform: AiProvider): List<String> {
        if (platform == AiProvider.OLLAMA && !settings.ollamaUseCloud) return listOf("")
        val keys = if (platform == AiProvider.OPENROUTER) {
            settings.openRouterAttemptKeys()
        } else {
            settings.keysFor(platform)
        }
        return keys.ifEmpty { listOf("") }
    }

    /**
     * Catalog ordered by platform ranking (Gemini Flash ladder, OpenRouter/Ollama Gemma-first).
     * Selected model is tried first only when it is a plausible id for [platform] — prevents
     * Gemini Studio ids (e.g. gemini-3-flash-preview) from being sent to OpenRouter.
     */
    private suspend fun modelLadder(
        settings: AppSettings,
        platform: AiProvider,
        preferVisionModels: Boolean
    ): List<String> {
        val selectedRaw = settings.copy(provider = platform).modelFor(preferVisionModels)
        val selected = selectedRaw.takeIf { isPlausibleModelIdFor(platform, it) }.orEmpty()
        val listKey = settings.keysFor(platform).firstOrNull().orEmpty()
        val includePaid = settings.showPaidModels
        val catalog = runCatching {
            when (platform) {
                AiProvider.OPENROUTER -> if (preferVisionModels) {
                    remoteAiDataSource.fetchFreeVisionModels(listKey, includePaid)
                } else {
                    remoteAiDataSource.fetchFreeModels(listKey, includePaid)
                }
                AiProvider.GEMINI -> if (preferVisionModels) {
                    remoteAiDataSource.fetchGeminiVisionModels(listKey, includePaid)
                } else {
                    remoteAiDataSource.fetchGeminiTextModels(listKey, includePaid)
                }
                AiProvider.OLLAMA -> {
                    val base = settings.ollamaEffectiveBaseUrl
                    val key = if (settings.ollamaUseCloud) listKey else ""
                    if (preferVisionModels) {
                        remoteAiDataSource.fetchOllamaVisionModels(base, key)
                    } else {
                        remoteAiDataSource.fetchOllamaTextModels(base, key)
                    }
                }
            }
        }.getOrDefault(emptyList())
            .map { it.id }
            .filter { isPlausibleModelIdFor(platform, it) }

        return buildList {
            if (selected.isNotBlank()) add(selected)
            catalog.filter { it != selected }.forEach { add(it) }
        }.ifEmpty {
            listOfNotNull(selected.takeIf { it.isNotBlank() })
        }
    }

    private fun formatAiConnectionError(e: Throwable): String {
        val detail = e.message?.takeIf { it.isNotBlank() }
        return detail?.let { "Couldn't connect to AI: $it" }
            ?: "Couldn't connect to AI. Check your network and AI provider settings."
    }
}
