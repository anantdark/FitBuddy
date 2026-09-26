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
        ExerciseUsage::class,
        BodyMeasurement::class,
        WorkoutSession::class,
        WorkoutExercise::class
    ],
    version = 18,
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
    abstract fun exerciseUsageDao(): ExerciseUsageDao
    abstract fun bodyMeasurementDao(): BodyMeasurementDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun workoutExerciseDao(): WorkoutExerciseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** Adds manual [sortOrder] for saved foods and meal presets. */
        val MIGRATION_11_12 = migration(11, 12) { db ->
            db.execSQL(
                "ALTER TABLE saved_foods ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "ALTER TABLE meal_presets ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0"
            )
            // Preserve previous name-ASC order as the initial manual order.
            db.execSQL(
                """
                UPDATE saved_foods SET sortOrder = (
                    SELECT COUNT(*) FROM saved_foods AS s2
                    WHERE s2.name COLLATE NOCASE < saved_foods.name COLLATE NOCASE
                       OR (s2.name COLLATE NOCASE = saved_foods.name COLLATE NOCASE
                           AND s2.id < saved_foods.id)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                UPDATE meal_presets SET sortOrder = (
                    SELECT COUNT(*) FROM meal_presets AS m2
                    WHERE m2.name COLLATE NOCASE < meal_presets.name COLLATE NOCASE
                       OR (m2.name COLLATE NOCASE = meal_presets.name COLLATE NOCASE
                           AND m2.id < meal_presets.id)
                )
                """.trimIndent()
            )
        }

        /**
         * Replaces manual [sortOrder] with [lastUsedAt] (seeded from createdAt).
         * Must rebuild tables: Room rejects leftover columns and ALTER … DEFAULT 0
         * (entity has no ColumnInfo defaultValue).
         */
        val MIGRATION_12_13 = migration(12, 13) { db ->
            db.execSQL(
                """
                CREATE TABLE saved_foods_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    calories INTEGER NOT NULL,
                    proteinG INTEGER NOT NULL,
                    carbsG INTEGER NOT NULL,
                    fatsG INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    barcode TEXT,
                    ingredients TEXT,
                    lastUsedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO saved_foods_new (
                    id, name, calories, proteinG, carbsG, fatsG,
                    createdAt, barcode, ingredients, lastUsedAt
                )
                SELECT
                    id, name, calories, proteinG, carbsG, fatsG,
                    createdAt, barcode, ingredients, createdAt
                FROM saved_foods
                """.trimIndent()
            )
            db.execSQL("DROP TABLE saved_foods")
            db.execSQL("ALTER TABLE saved_foods_new RENAME TO saved_foods")

            db.execSQL(
                """
                CREATE TABLE meal_presets_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    calories INTEGER NOT NULL,
                    proteinG INTEGER NOT NULL,
                    carbsG INTEGER NOT NULL,
                    fatsG INTEGER NOT NULL,
                    createdAt INTEGER NOT NULL,
                    foods TEXT,
                    lastUsedAt INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO meal_presets_new (
                    id, name, calories, proteinG, carbsG, fatsG,
                    createdAt, foods, lastUsedAt
                )
                SELECT
                    id, name, calories, proteinG, carbsG, fatsG,
                    createdAt, foods, createdAt
                FROM meal_presets
                """.trimIndent()
            )
            db.execSQL("DROP TABLE meal_presets")
            db.execSQL("ALTER TABLE meal_presets_new RENAME TO meal_presets")
        }

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
                    .addMigrations(
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18
                    )
                    .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * Additive only: nullable FreeScale restore blob for full reading round-trip.
         * Existing rows stay NULL; no rewrite of display fields. Never shown in FitBuddy UI.
         */
        val MIGRATION_13_14 = migration(13, 14) { db ->
            db.execSQL(
                "ALTER TABLE body_measurements ADD COLUMN freescalePayloadJson TEXT"
            )
        }

        /** Adds the optional AI-recommended or manually entered target body weight. */
        val MIGRATION_14_15 = migration(14, 15) { db ->
            db.execSQL("ALTER TABLE user_profile ADD COLUMN targetWeightKg REAL")
        }

        /**
         * Adds change-point nutrition target history on the profile row, seeded from current
         * targets so all past days resolve to today's values until the next real change.
         * Column must be NOT NULL to match [UserProfile.nutritionTargetHistory].
         */
        val MIGRATION_15_16 = migration(15, 16) { db ->
            db.execSQL(
                "ALTER TABLE user_profile ADD COLUMN nutritionTargetHistory TEXT NOT NULL DEFAULT '[]'"
            )
            db.execSQL(
                """
                UPDATE user_profile SET nutritionTargetHistory = (
                    '[{"from":"1970-01-01","kcal":' || dailyTargetCalories ||
                    ',"proteinG":' || targetProteinG ||
                    ',"carbsG":' || targetCarbsG ||
                    ',"fatsG":' || targetFatsG || '}]'
                )
                WHERE nutritionTargetHistory = '[]'
                """.trimIndent()
            )
        }

        /**
         * v16 shipped with nullable nutritionTargetHistory (ALTER … TEXT without NOT NULL),
         * which fails Room's schema check against the non-null entity field. Rebuild the
         * profile table so the column is NOT NULL; preserve existing seeded JSON.
         */
        val MIGRATION_16_17 = migration(16, 17) { db ->
            db.execSQL(
                """
                CREATE TABLE user_profile_new (
                    id INTEGER NOT NULL PRIMARY KEY,
                    age INTEGER NOT NULL,
                    weightKg REAL NOT NULL,
                    heightCm REAL NOT NULL,
                    dailyTargetCalories INTEGER NOT NULL,
                    targetProteinG INTEGER NOT NULL,
                    targetCarbsG INTEGER NOT NULL,
                    targetFatsG INTEGER NOT NULL,
                    lastUpdatedTimestamp INTEGER NOT NULL,
                    sex TEXT,
                    goal TEXT NOT NULL,
                    activityLevel TEXT NOT NULL,
                    goalRationale TEXT,
                    targetWeightKg REAL,
                    nutritionTargetHistory TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO user_profile_new (
                    id, age, weightKg, heightCm, dailyTargetCalories,
                    targetProteinG, targetCarbsG, targetFatsG, lastUpdatedTimestamp,
                    sex, goal, activityLevel, goalRationale, targetWeightKg,
                    nutritionTargetHistory
                )
                SELECT
                    id, age, weightKg, heightCm, dailyTargetCalories,
                    targetProteinG, targetCarbsG, targetFatsG, lastUpdatedTimestamp,
                    sex, goal, activityLevel, goalRationale, targetWeightKg,
                    COALESCE(
                        NULLIF(nutritionTargetHistory, ''),
                        '[{"from":"1970-01-01","kcal":' || dailyTargetCalories ||
                        ',"proteinG":' || targetProteinG ||
                        ',"carbsG":' || targetCarbsG ||
                        ',"fatsG":' || targetFatsG || '}]'
                    )
                FROM user_profile
                """.trimIndent()
            )
            db.execSQL("DROP TABLE user_profile")
            db.execSQL("ALTER TABLE user_profile_new RENAME TO user_profile")
        }

        /** Adds exercise_usage for recent/frequent workout picker ranking. */
        val MIGRATION_17_18 = migration(17, 18) { db ->
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS exercise_usage (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    exerciseId TEXT,
                    lastUsedAt INTEGER NOT NULL,
                    useCount INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_exercise_usage_name ON exercise_usage (name)"
            )
        }

        /**
         * Template for the next schema migration. Copy, rename, increment version numbers,
         * add the required ALTER TABLE / CREATE TABLE statements, add the new version to
         * [getDatabase], and bump [AppDatabase] version in the @Database annotation.
         */
        fun migration(from: Int, to: Int, block: (SupportSQLiteDatabase) -> Unit): Migration =
            object : Migration(from, to) {
                override fun migrate(db: SupportSQLiteDatabase) = block(db)
            }
    }
}
