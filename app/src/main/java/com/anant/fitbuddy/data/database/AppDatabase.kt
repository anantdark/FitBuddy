package com.anant.fitbuddy.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
                    // Versions 1–10 were pre-production dev iterations with no real migration
                    // path — allow destructive fallback only for those ancient databases.
                    // Version 11 is the first production-shipped schema; any upgrade from v11+
                    // must provide an explicit Migration object so user data is never silently
                    // wiped on an app update.
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Template for the next schema migration. Copy, rename, increment version numbers,
         * add the required ALTER TABLE / CREATE TABLE statements, add the new version to
         * [getDatabase], and bump [AppDatabase] version in the @Database annotation.
         *
         * Example — adding a nullable column to food_logs:
         *
         *   val MIGRATION_11_12 = migration(11, 12) {
         *       it.execSQL("ALTER TABLE food_logs ADD COLUMN notes TEXT")
         *   }
         *
         * Then in getDatabase: .addMigrations(MIGRATION_11_12)
         */
        fun migration(from: Int, to: Int, block: (SupportSQLiteDatabase) -> Unit): Migration =
            object : Migration(from, to) {
                override fun migrate(db: SupportSQLiteDatabase) = block(db)
            }
    }
}
