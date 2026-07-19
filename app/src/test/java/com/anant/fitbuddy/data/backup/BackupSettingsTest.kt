package com.anant.fitbuddy.data.backup

import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.ExerciseLog
import com.anant.fitbuddy.data.database.ExercisePreset
import com.anant.fitbuddy.data.database.FoodLog
import com.anant.fitbuddy.data.database.MealFood
import com.anant.fitbuddy.data.database.MealPreset
import com.anant.fitbuddy.data.database.SavedFood
import com.anant.fitbuddy.data.database.UserProfile
import com.anant.fitbuddy.data.database.WorkoutExercise
import com.anant.fitbuddy.data.database.WorkoutSession
import com.anant.fitbuddy.data.model.LoggedIngredient
import com.anant.fitbuddy.data.model.PresetMealFood
import com.anant.fitbuddy.data.remote.NetworkModule
import com.anant.fitbuddy.data.settings.AiProvider
import com.anant.fitbuddy.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSettingsTest {

    @Test
    fun fromToAppSettings_preservesEveryBackedUpField() {
        val original = AppSettings.withKeys(
            openRouterKeys = listOf("or-key-1", "or-key-2"),
            geminiKeys = listOf("gem-key"),
            ollamaKeys = listOf("ollama-key"),
            base = AppSettings(
                provider = AiProvider.OLLAMA,
                openRouterOAuthKey = "or-oauth",
                openRouterModel = "google/gemma-test",
                openRouterTextModel = "google/gemma-text",
                geminiModel = "gemini-2.5-flash",
                geminiTextModel = "gemini-2.5-flash-lite",
                ollamaBaseUrl = "http://10.0.0.2:11434",
                ollamaModel = "llava",
                ollamaTextModel = "llama3.2",
                ollamaUseCloud = true,
                aiAutoFailover = false,
                showPaidModels = true,
                activeAiProvider = AiProvider.GEMINI,
                activePhotoModel = "gemini-2.5-flash",
                activeTextModel = "gemini-2.5-flash-lite",
                dynamicColor = false,
                autoCheckUpdates = false,
                supportId = "support-xyz",
                crashReportingEnabled = false,
                easterEggDiscovered = true,
                dailyLogReminderEnabled = false,
                dailyLogReminderHour = 21,
                dailyLogReminderMinute = 30,
                developerModeUnlocked = true,
                forceOfflineAiSimulator = true,
                showRawAiJson = true,
                strictClarification = true,
                verboseHttpLogging = true,
                cloudBackupEnabled = true,
                cloudAutoUploadEnabled = false,
                mongoDbName = "fitbuddy_prod",
                mongoCollectionName = "fitbuddy_backup_dev",
                // Ephemeral — must NOT round-trip through BackupSettings.
                mongoLastUploadAt = 1_700_000_000_000L,
                mongoLastUploadOk = true,
                mongoLastError = "should-not-restore"
            )
        )
        val restored = BackupSettings.from(original).toAppSettings()
        assertEquals(normalizeForBackupParity(original), normalizeForBackupParity(restored))
        assertEquals(0L, restored.mongoLastUploadAt)
        assertEquals(false, restored.mongoLastUploadOk)
        assertEquals("", restored.mongoLastError)
        assertTrue(restored.cloudBackupEnabled)
        assertFalse(restored.cloudAutoUploadEnabled)
    }

    @Test
    fun from_doesNotExportMongoUri() {
        val backup = BackupSettings.from(
            AppSettings(cloudBackupEnabled = true, supportId = "abc")
        )
        assertEquals("", backup.mongoDbUri)
        assertEquals(AppSettings.DEFAULT_MONGO_DB_NAME, backup.mongoDbName)
        assertEquals(AppSettings.DEFAULT_MONGO_COLLECTION, backup.mongoCollectionName)
    }

    @Test
    fun roundTrip_preservesAiKeysAndReminder() {
        val original = AppSettings.withKeys(
            openRouterKeys = listOf("or-key-1", "or-key-2"),
            geminiKeys = listOf("gem-key"),
            base = AppSettings(
                provider = AiProvider.GEMINI,
                openRouterOAuthKey = "or-oauth",
                geminiModel = "gemini-2.5-flash",
                dailyLogReminderEnabled = false,
                dailyLogReminderHour = 21,
                dailyLogReminderMinute = 30,
                supportId = "support-xyz",
                cloudBackupEnabled = true
            )
        )
        val restored = BackupSettings.from(original).toAppSettings()
        assertEquals(AiProvider.GEMINI, restored.provider)
        assertEquals(listOf("or-key-1", "or-key-2"), restored.openRouterApiKeys)
        assertEquals("or-oauth", restored.openRouterOAuthKey)
        assertEquals(listOf("gem-key"), restored.geminiApiKeys)
        assertEquals("gem-key", restored.geminiApiKey)
        assertEquals(false, restored.dailyLogReminderEnabled)
        assertEquals(21, restored.dailyLogReminderHour)
        assertEquals(30, restored.dailyLogReminderMinute)
        assertEquals("support-xyz", restored.supportId)
        assertTrue(restored.cloudBackupEnabled)
    }

    @Test
    fun backupData_moshiRoundTrip_preservesAllTablesAndNestedFields() {
        val moshi = NetworkModule.moshi
        val adapter = moshi.adapter(BackupData::class.java)
        val ingredient = LoggedIngredient(
            name = "Rice",
            quantity = 1,
            unitWeightG = 150,
            kcalPer100 = 130.0,
            proteinPer100 = 2.5,
            carbsPer100 = 28.0,
            fatsPer100 = 0.3
        )
        val data = BackupData(
            exportedAt = 123L,
            profile = UserProfile(
                id = 1,
                age = 30,
                weightKg = 72.5,
                heightCm = 175.0,
                dailyTargetCalories = 2200,
                targetProteinG = 150,
                targetCarbsG = 220,
                targetFatsG = 70,
                lastUpdatedTimestamp = 99L,
                sex = "MALE",
                goal = "GAIN_MUSCLE",
                activityLevel = "ACTIVE",
                goalRationale = "Build lean mass"
            ),
            measurements = listOf(
                BodyMeasurement(
                    id = 1,
                    timestamp = 10L,
                    dateString = "2026-07-19",
                    weightKg = 72.5,
                    bmi = 23.7,
                    bodyFatPct = 18.0,
                    muscleRatePct = 42.0,
                    bodyWaterPct = 55.0,
                    boneMassKg = 3.1,
                    bmr = 1700,
                    metabolicAge = 28,
                    visceralFat = 7.0,
                    subcutaneousFatPct = 15.0,
                    proteinMassKg = 12.0,
                    muscleMassKg = 30.0,
                    fatFreeMassKg = 59.0,
                    skeletalMuscleMassKg = 28.0,
                    waterWeightKg = 40.0,
                    fatMassKg = 13.5
                )
            ),
            foodLogs = listOf(
                FoodLog(
                    id = 2,
                    dishName = "Breakfast",
                    timestamp = 11L,
                    dateString = "2026-07-19",
                    calories = 500,
                    proteinG = 30,
                    carbsG = 50,
                    fatsG = 15
                )
            ),
            mealFoods = listOf(
                MealFood(
                    id = 3,
                    mealLogId = 2,
                    name = "Oats",
                    servings = 1.5,
                    orderIndex = 1,
                    calories = 250,
                    proteinG = 10,
                    carbsG = 40,
                    fatsG = 5,
                    ingredients = listOf(ingredient),
                    presetId = 4,
                    barcode = "890123"
                )
            ),
            exerciseLogs = listOf(
                ExerciseLog(
                    id = 5,
                    activityName = "Run",
                    timestamp = 12L,
                    dateString = "2026-07-19",
                    caloriesBurned = 300,
                    durationMinutes = 30
                )
            ),
            savedFoods = listOf(
                SavedFood(
                    id = 4,
                    name = "Oats",
                    calories = 150,
                    proteinG = 5,
                    carbsG = 27,
                    fatsG = 3,
                    createdAt = 13L,
                    barcode = "890123",
                    ingredients = listOf(ingredient)
                )
            ),
            mealPresets = listOf(
                MealPreset(
                    id = 6,
                    name = "Lunch box",
                    calories = 600,
                    proteinG = 40,
                    carbsG = 60,
                    fatsG = 20,
                    createdAt = 14L,
                    foods = listOf(
                        PresetMealFood(
                            name = "Oats",
                            servings = 1.0,
                            calories = 150,
                            proteinG = 5,
                            carbsG = 27,
                            fatsG = 3,
                            ingredients = listOf(ingredient),
                            savedFoodId = 4,
                            barcode = "890123"
                        )
                    )
                )
            ),
            exercisePresets = listOf(
                ExercisePreset(id = 7, name = "Bench", equipment = "Barbell", createdAt = 15L)
            ),
            workoutSessions = listOf(
                WorkoutSession(
                    id = 8,
                    name = "Push",
                    timestamp = 16L,
                    dateString = "2026-07-19",
                    durationMinutes = 45,
                    caloriesBurned = 250,
                    exerciseLogId = 5
                )
            ),
            workoutExercises = listOf(
                WorkoutExercise(
                    id = 9,
                    sessionId = 8,
                    name = "Bench",
                    sets = 3,
                    reps = 8,
                    weightKg = 60.0,
                    orderIndex = 0,
                    equipment = "Barbell",
                    durationMinutes = 5,
                    distanceKm = null
                ),
                WorkoutExercise(
                    id = 10,
                    sessionId = 8,
                    name = "Treadmill",
                    sets = 1,
                    reps = 1,
                    weightKg = null,
                    orderIndex = 1,
                    equipment = "Cardio",
                    durationMinutes = 20,
                    distanceKm = 3.5
                )
            ),
            settings = BackupSettings.from(
                AppSettings(
                    provider = AiProvider.OPENROUTER,
                    openRouterApiKeys = listOf("k"),
                    cloudBackupEnabled = true
                )
            )
        )
        val parsed = adapter.fromJson(adapter.toJson(data))!!
        assertEquals(BackupData.CURRENT_VERSION, parsed.version)
        assertEquals(data.exportedAt, parsed.exportedAt)
        assertEquals(data.profile, parsed.profile)
        assertEquals(data.measurements, parsed.measurements)
        assertEquals(data.foodLogs, parsed.foodLogs)
        assertEquals(data.mealFoods, parsed.mealFoods)
        assertEquals(data.exerciseLogs, parsed.exerciseLogs)
        assertEquals(data.savedFoods, parsed.savedFoods)
        assertEquals(data.mealPresets, parsed.mealPresets)
        assertEquals(data.exercisePresets, parsed.exercisePresets)
        assertEquals(data.workoutSessions, parsed.workoutSessions)
        assertEquals(data.workoutExercises, parsed.workoutExercises)
        assertEquals(data.settings, parsed.settings)
        assertTrue(parsed.presets.isEmpty())
    }

    @Test
    fun backupData_moshiRoundTrip_includesSettingsAndExercisePresets() {
        val moshi = NetworkModule.moshi
        val adapter = moshi.adapter(BackupData::class.java)
        val data = BackupData(
            exportedAt = 123L,
            exercisePresets = emptyList(),
            settings = BackupSettings(
                provider = "OLLAMA",
                ollamaBaseUrl = "http://10.0.0.2:11434",
                ollamaModel = "llava",
                ollamaApiKeys = listOf("cloud-key")
            )
        )
        val parsed = adapter.fromJson(adapter.toJson(data))!!
        assertEquals(BackupData.CURRENT_VERSION, parsed.version)
        val settings = requireNotNull(parsed.settings)
        assertEquals("OLLAMA", settings.provider)
        assertEquals("http://10.0.0.2:11434", settings.ollamaBaseUrl)
        assertEquals(listOf("cloud-key"), settings.ollamaApiKeys)
        assertTrue(parsed.exercisePresets.isEmpty())
    }

    @Test
    fun legacyBackup_missingSettingsAndExercisePresets_defaultsCleanly() {
        val json = """
            {
              "version": 4,
              "exportedAt": 1,
              "foodLogs": [],
              "mealFoods": [],
              "savedFoods": [],
              "mealPresets": []
            }
        """.trimIndent()
        val parsed = NetworkModule.moshi.adapter(BackupData::class.java).fromJson(json)!!
        assertEquals(4, parsed.version)
        assertNull(parsed.settings)
        assertTrue(parsed.exercisePresets.isEmpty())
    }

    /**
     * Strip ephemeral upload status and normalize active keys the same way restore does,
     * so equality fails if a new [AppSettings] field is forgotten in [BackupSettings].
     */
    private fun normalizeForBackupParity(settings: AppSettings): AppSettings {
        val or = settings.keysFor(AiProvider.OPENROUTER)
        val gem = settings.keysFor(AiProvider.GEMINI)
        val ol = settings.keysFor(AiProvider.OLLAMA)
        return settings.copy(
            openRouterApiKeys = or,
            openRouterApiKey = or.firstOrNull().orEmpty(),
            geminiApiKeys = gem,
            geminiApiKey = gem.firstOrNull().orEmpty(),
            ollamaApiKeys = ol,
            ollamaApiKey = ol.firstOrNull().orEmpty(),
            mongoLastUploadAt = 0L,
            mongoLastUploadOk = false,
            mongoLastError = ""
        )
    }
}
