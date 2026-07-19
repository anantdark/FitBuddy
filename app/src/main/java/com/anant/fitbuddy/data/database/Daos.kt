package com.anant.fitbuddy.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1")
    suspend fun getProfileOnce(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    @Query("DELETE FROM user_profile")
    suspend fun clearAll()
}

@Dao
interface FoodLogDao {
    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC")
    fun getAllFoodLogs(): Flow<List<FoodLog>>

    @Query("SELECT * FROM food_logs ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<FoodLog>

    @Query("SELECT * FROM food_logs WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getFoodLogsByDate(dateString: String): Flow<List<FoodLog>>

    @Query("SELECT SUM(calories) FROM food_logs WHERE dateString = :dateString")
    fun getTotalCaloriesForDate(dateString: String): Flow<Int?>

    @Query("SELECT SUM(proteinG) FROM food_logs WHERE dateString = :dateString")
    fun getTotalProteinForDate(dateString: String): Flow<Int?>

    @Query("SELECT SUM(carbsG) FROM food_logs WHERE dateString = :dateString")
    fun getTotalCarbsForDate(dateString: String): Flow<Int?>

    @Query("SELECT SUM(fatsG) FROM food_logs WHERE dateString = :dateString")
    fun getTotalFatsForDate(dateString: String): Flow<Int?>

    /** Single-query daily totals: avoids observing/combining four separate SUM queries. */
    @Query("""
        SELECT
            COALESCE(SUM(calories), 0) AS totalCalories,
            COALESCE(SUM(proteinG), 0) AS totalProtein,
            COALESCE(SUM(carbsG), 0) AS totalCarbs,
            COALESCE(SUM(fatsG), 0) AS totalFats
        FROM food_logs WHERE dateString = :dateString
    """)
    fun getFoodTotalsForDate(dateString: String): Flow<FoodTotals>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLog(log: FoodLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodLogReturningId(log: FoodLog): Long

    @Query("SELECT * FROM food_logs WHERE id = :id")
    suspend fun getById(id: Int): FoodLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<FoodLog>): List<Long>

    @Delete
    suspend fun deleteFoodLog(log: FoodLog)

    @Query("DELETE FROM food_logs")
    suspend fun clearAll()

    // For graphs: returns daily totals for last N days that have logs
    @Query("""
        SELECT dateString, SUM(calories) as totalCalories, SUM(proteinG) as totalProtein, SUM(carbsG) as totalCarbs, SUM(fatsG) as totalFats 
        FROM food_logs 
        GROUP BY dateString 
        ORDER BY dateString DESC 
        LIMIT :limit
    """)
    fun getHistoricalFoodSummaries(limit: Int): Flow<List<FoodDailySummary>>

    /** Every day that has food logs (newest first) — for AI progress context. */
    @Query("""
        SELECT dateString, SUM(calories) as totalCalories, SUM(proteinG) as totalProtein, SUM(carbsG) as totalCarbs, SUM(fatsG) as totalFats 
        FROM food_logs 
        GROUP BY dateString 
        ORDER BY dateString DESC
    """)
    suspend fun getAllFoodDailySummaries(): List<FoodDailySummary>

    @Query("""
        SELECT dateString, SUM(calories) as totalCalories, SUM(proteinG) as totalProtein, SUM(carbsG) as totalCarbs, SUM(fatsG) as totalFats 
        FROM food_logs 
        WHERE dateString >= :startDate AND dateString <= :endDate
        GROUP BY dateString 
        ORDER BY dateString DESC
    """)
    fun getFoodSummariesBetween(startDate: String, endDate: String): Flow<List<FoodDailySummary>>
}

@Dao
interface MealFoodDao {
    @Query("SELECT * FROM meal_foods WHERE mealLogId = :mealLogId ORDER BY orderIndex ASC")
    suspend fun getForMealOnce(mealLogId: Int): List<MealFood>

    @Query("SELECT * FROM meal_foods ORDER BY mealLogId ASC, orderIndex ASC")
    suspend fun getAllOnce(): List<MealFood>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<MealFood>)

    @Query("DELETE FROM meal_foods WHERE mealLogId = :mealLogId")
    suspend fun deleteForMeal(mealLogId: Int)

    @Query("DELETE FROM meal_foods")
    suspend fun clearAll()
}

data class FoodDailySummary(
    val dateString: String,
    val totalCalories: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFats: Int
)

/** Aggregated food totals for a single day (calories + macros). */
data class FoodTotals(
    val totalCalories: Int = 0,
    val totalProtein: Int = 0,
    val totalCarbs: Int = 0,
    val totalFats: Int = 0
)

@Dao
interface ExerciseLogDao {
    @Query("SELECT * FROM exercise_logs ORDER BY timestamp DESC")
    fun getAllExerciseLogs(): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_logs ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<ExerciseLog>

    @Query("SELECT * FROM exercise_logs WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getExerciseLogsByDate(dateString: String): Flow<List<ExerciseLog>>

    @Query("SELECT * FROM exercise_logs WHERE id = :id")
    suspend fun getById(id: Int): ExerciseLog?

    @Query("SELECT SUM(caloriesBurned) FROM exercise_logs WHERE dateString = :dateString")
    fun getTotalBurnedForDate(dateString: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExerciseLog(log: ExerciseLog)

    /** Returns generated row ids (same order as [logs]) so callers can remap FKs, e.g. on import. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<ExerciseLog>): List<Long>

    @Delete
    suspend fun deleteExerciseLog(log: ExerciseLog)

    @Query("DELETE FROM exercise_logs")
    suspend fun clearAll()

    // For graphs: returns daily total burned for last N days that have logs
    @Query("""
        SELECT dateString, SUM(caloriesBurned) as totalBurned 
        FROM exercise_logs 
        GROUP BY dateString 
        ORDER BY dateString DESC 
        LIMIT :limit
    """)
    fun getHistoricalExerciseSummaries(limit: Int): Flow<List<ExerciseDailySummary>>

    /** Every day that has exercise logs (newest first) — for AI progress context. */
    @Query("""
        SELECT dateString, SUM(caloriesBurned) as totalBurned 
        FROM exercise_logs 
        GROUP BY dateString 
        ORDER BY dateString DESC
    """)
    suspend fun getAllExerciseDailySummaries(): List<ExerciseDailySummary>

    @Query("""
        SELECT dateString, SUM(caloriesBurned) as totalBurned 
        FROM exercise_logs 
        WHERE dateString >= :startDate AND dateString <= :endDate
        GROUP BY dateString 
        ORDER BY dateString DESC
    """)
    fun getExerciseSummariesBetween(startDate: String, endDate: String): Flow<List<ExerciseDailySummary>>
}

data class ExerciseDailySummary(
    val dateString: String,
    val totalBurned: Int
)

@Dao
interface SavedFoodDao {
    @Query("SELECT * FROM saved_foods ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<SavedFood>>

    @Query("SELECT * FROM saved_foods ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<SavedFood>

    @Query("SELECT * FROM saved_foods WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): SavedFood?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(food: SavedFood)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(foods: List<SavedFood>): List<Long>

    @Delete
    suspend fun delete(food: SavedFood)

    @Query("DELETE FROM saved_foods")
    suspend fun clearAll()
}

@Dao
interface MealPresetDao {
    @Query("SELECT * FROM meal_presets ORDER BY name COLLATE NOCASE ASC")
    fun getAll(): Flow<List<MealPreset>>

    @Query("SELECT * FROM meal_presets ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<MealPreset>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: MealPreset)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(presets: List<MealPreset>): List<Long>

    @Delete
    suspend fun delete(preset: MealPreset)

    @Query("DELETE FROM meal_presets")
    suspend fun clearAll()
}

@Dao
interface ExercisePresetDao {
    @Query("SELECT * FROM exercise_presets ORDER BY name COLLATE NOCASE ASC")
    fun getAllPresets(): Flow<List<ExercisePreset>>

    @Query("SELECT * FROM exercise_presets ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllOnce(): List<ExercisePreset>

    @Query("SELECT * FROM exercise_presets WHERE lower(name) = lower(:name) LIMIT 1")
    suspend fun findByName(name: String): ExercisePreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: ExercisePreset)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(presets: List<ExercisePreset>): List<Long>

    @Delete
    suspend fun deletePreset(preset: ExercisePreset)

    @Query("DELETE FROM exercise_presets")
    suspend fun clearAll()
}

@Dao
interface WorkoutSessionDao {
    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions WHERE dateString = :dateString ORDER BY timestamp DESC")
    fun getForDate(dateString: String): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_sessions ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<WorkoutSession>

    /** Finds the session mirrored to a given exercise log row, so deleting one can cascade. */
    @Query("SELECT * FROM workout_sessions WHERE exerciseLogId = :exerciseLogId LIMIT 1")
    suspend fun getByExerciseLogId(exerciseLogId: Int): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Int): WorkoutSession?

    /** Returns the generated row id, used to attach [WorkoutExercise] rows and the AI result. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession): Long

    /** Returns generated row ids (same order as [sessions]) so callers can remap FKs on import. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<WorkoutSession>): List<Long>

    @Delete
    suspend fun delete(session: WorkoutSession)

    @Query("DELETE FROM workout_sessions")
    suspend fun clearAll()
}

@Dao
interface WorkoutExerciseDao {
    @Query("SELECT * FROM workout_exercises WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getForSession(sessionId: Int): Flow<List<WorkoutExercise>>

    @Query("SELECT * FROM workout_exercises ORDER BY sessionId ASC, orderIndex ASC")
    suspend fun getAllOnce(): List<WorkoutExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(exercises: List<WorkoutExercise>)

    @Query("DELETE FROM workout_exercises WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: Int)

    @Query("DELETE FROM workout_exercises")
    suspend fun clearAll()
}

@Dao
interface BodyMeasurementDao {
    @Query("SELECT * FROM body_measurements ORDER BY timestamp DESC")
    fun getAll(): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurements ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<BodyMeasurement>>

    @Query("SELECT * FROM body_measurements ORDER BY timestamp DESC LIMIT 1")
    fun getLatest(): Flow<BodyMeasurement?>

    @Query("SELECT * FROM body_measurements ORDER BY timestamp DESC")
    suspend fun getAllOnce(): List<BodyMeasurement>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(measurement: BodyMeasurement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(measurements: List<BodyMeasurement>)

    @Delete
    suspend fun delete(measurement: BodyMeasurement)

    @Query("DELETE FROM body_measurements")
    suspend fun clearAll()
}
