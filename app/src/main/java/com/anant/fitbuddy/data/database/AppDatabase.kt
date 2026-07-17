package com.anant.fitbuddy.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        UserProfile::class,
        FoodLog::class,
        MealFood::class,
        SavedFood::class,
        MealPreset::class,
        ExerciseLog::class,
        ExercisePreset::class,
        BodyMeasurement::class,
        WorkoutSession::class,
        WorkoutExercise::class
    ],
    version = 11,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun foodLogDao(): FoodLogDao
    abstract fun mealFoodDao(): MealFoodDao
    abstract fun savedFoodDao(): SavedFoodDao
    abstract fun mealPresetDao(): MealPresetDao
    abstract fun exerciseLogDao(): ExerciseLogDao
    abstract fun exercisePresetDao(): ExercisePresetDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fitness_tracker_db"
                )
                    // Dev convenience: schema changes during early iteration won't crash the app.
                    // Replace with real Migrations before shipping to production.
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
