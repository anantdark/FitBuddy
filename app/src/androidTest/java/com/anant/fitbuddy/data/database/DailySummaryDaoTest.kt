package com.anant.fitbuddy.data.database

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DailySummaryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var foodDao: FoodLogDao
    private lateinit var exerciseDao: ExerciseLogDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        foodDao = db.foodLogDao()
        exerciseDao = db.exerciseLogDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun foodSummariesBetweenAndAllAggregateByDay() = runBlocking {
        foodDao.insertFoodLog(
            FoodLog(
                dateString = "2026-07-10",
                timestamp = 1L,
                dishName = "Idli",
                calories = 200,
                proteinG = 6,
                carbsG = 40,
                fatsG = 2
            )
        )
        foodDao.insertFoodLog(
            FoodLog(
                dateString = "2026-07-10",
                timestamp = 2L,
                dishName = "Sambar",
                calories = 100,
                proteinG = 4,
                carbsG = 15,
                fatsG = 3
            )
        )
        foodDao.insertFoodLog(
            FoodLog(
                dateString = "2026-07-18",
                timestamp = 3L,
                dishName = "Dal",
                calories = 350,
                proteinG = 16,
                carbsG = 48,
                fatsG = 8
            )
        )

        val between = foodDao.getFoodSummariesBetween("2026-07-10", "2026-07-17").first()
        assertEquals(1, between.size)
        assertEquals("2026-07-10", between[0].dateString)
        assertEquals(300, between[0].totalCalories)
        assertEquals(10, between[0].totalProtein)

        val all = foodDao.getAllFoodDailySummaries()
        assertEquals(2, all.size)
        assertEquals("2026-07-18", all[0].dateString)
        assertEquals("2026-07-10", all[1].dateString)
        assertEquals(350, all[0].totalCalories)
    }

    @Test
    fun exerciseSummariesBetweenAndAllAggregateByDay() = runBlocking {
        exerciseDao.insertExerciseLog(
            ExerciseLog(
                dateString = "2026-07-10",
                timestamp = 1L,
                activityName = "Walk",
                caloriesBurned = 180,
                durationMinutes = 40
            )
        )
        exerciseDao.insertExerciseLog(
            ExerciseLog(
                dateString = "2026-07-10",
                timestamp = 2L,
                activityName = "Yoga",
                caloriesBurned = 120,
                durationMinutes = 30
            )
        )
        exerciseDao.insertExerciseLog(
            ExerciseLog(
                dateString = "2026-07-19",
                timestamp = 3L,
                activityName = "Run",
                caloriesBurned = 400,
                durationMinutes = 35
            )
        )

        val between = exerciseDao.getExerciseSummariesBetween("2026-07-01", "2026-07-15").first()
        assertEquals(1, between.size)
        assertEquals(300, between[0].totalBurned)

        val all = exerciseDao.getAllExerciseDailySummaries()
        assertEquals(2, all.size)
        assertEquals("2026-07-19", all[0].dateString)
        assertEquals(400, all[0].totalBurned)
    }
}
