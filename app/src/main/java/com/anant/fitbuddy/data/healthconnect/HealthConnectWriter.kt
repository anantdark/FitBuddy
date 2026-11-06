package com.anant.fitbuddy.data.healthconnect

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MealType
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Volume
import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.ExerciseLog
import com.anant.fitbuddy.data.database.FoodLog
import java.time.Instant

internal class HealthConnectWriter(
    private val manager: HealthConnectManager
) {
    suspend fun writeNutrition(log: FoodLog): Boolean {
        if (log.id <= 0) return false
        val client = manager.client ?: return false
        if (!manager.hasNutritionWrite()) return false
        val start = Instant.ofEpochMilli(log.timestamp)
        if (start.isAfter(Instant.now())) return false
        val end = start.plusSeconds(60)
        val record = NutritionRecord(
            startTime = start,
            startZoneOffset = null,
            endTime = end,
            endZoneOffset = null,
            name = log.dishName,
            mealType = MealType.MEAL_TYPE_UNKNOWN,
            energy = Energy.kilocalories(log.calories.toDouble()),
            protein = Mass.grams(log.proteinG),
            totalCarbohydrate = Mass.grams(log.carbsG),
            totalFat = Mass.grams(log.fatsG),
            metadata = Metadata.manualEntry(
                clientRecordId = HealthConnectManager.foodClientRecordId(log.id)
            )
        )
        return runCatching {
            deleteNutrition(log.id)
            client.insertRecords(listOf(record))
        }.isSuccess
    }

    suspend fun deleteNutrition(foodLogId: Int): Boolean {
        if (foodLogId <= 0) return false
        val client = manager.client ?: return false
        if (!manager.hasNutritionWrite()) return false
        return runCatching {
            client.deleteRecords(
                recordType = NutritionRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf(HealthConnectManager.foodClientRecordId(foodLogId))
            )
        }.isSuccess
    }

    suspend fun writeWeight(measurement: BodyMeasurement): Boolean {
        if (measurement.id <= 0 || measurement.weightKg <= 0) return false
        val client = manager.client ?: return false
        if (!manager.hasWeightWrite()) return false
        val time = Instant.ofEpochMilli(measurement.timestamp)
        if (time.isAfter(Instant.now())) return false
        val record = WeightRecord(
            time = time,
            zoneOffset = null,
            weight = Mass.kilograms(measurement.weightKg),
            metadata = Metadata.manualEntry(
                clientRecordId = HealthConnectManager.weightClientRecordId(measurement.id)
            )
        )
        return runCatching {
            deleteWeight(measurement.id)
            client.insertRecords(listOf(record))
        }.isSuccess
    }

    suspend fun deleteWeight(measurementId: Int): Boolean {
        if (measurementId <= 0) return false
        val client = manager.client ?: return false
        if (!manager.hasWeightWrite()) return false
        return runCatching {
            client.deleteRecords(
                recordType = WeightRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf(HealthConnectManager.weightClientRecordId(measurementId))
            )
        }.isSuccess
    }

    suspend fun writeBodyFat(measurement: BodyMeasurement): Boolean {
        val pct = measurement.bodyFatPct ?: return false
        if (measurement.id <= 0 || pct <= 0) return false
        val client = manager.client ?: return false
        if (!manager.hasBodyFatWrite()) return false
        val time = Instant.ofEpochMilli(measurement.timestamp)
        if (time.isAfter(Instant.now())) return false
        val record = BodyFatRecord(
            time = time,
            zoneOffset = null,
            percentage = Percentage(pct),
            metadata = Metadata.manualEntry(
                clientRecordId = HealthConnectManager.bodyFatClientRecordId(measurement.id)
            )
        )
        return runCatching {
            deleteBodyFat(measurement.id)
            client.insertRecords(listOf(record))
        }.isSuccess
    }

    suspend fun deleteBodyFat(measurementId: Int): Boolean {
        if (measurementId <= 0) return false
        val client = manager.client ?: return false
        if (!manager.hasBodyFatWrite()) return false
        return runCatching {
            client.deleteRecords(
                recordType = BodyFatRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf(HealthConnectManager.bodyFatClientRecordId(measurementId))
            )
        }.isSuccess
    }

    /** Maps FitBuddy muscle mass (kg) to Health Connect lean body mass. */
    suspend fun writeMuscleMass(measurement: BodyMeasurement): Boolean {
        val kg = measurement.muscleMassKg ?: return false
        if (measurement.id <= 0 || kg <= 0) return false
        val client = manager.client ?: return false
        if (!manager.hasLeanBodyMassWrite()) return false
        val time = Instant.ofEpochMilli(measurement.timestamp)
        if (time.isAfter(Instant.now())) return false
        val record = LeanBodyMassRecord(
            time = time,
            zoneOffset = null,
            mass = Mass.kilograms(kg),
            metadata = Metadata.manualEntry(
                clientRecordId = HealthConnectManager.muscleMassClientRecordId(measurement.id)
            )
        )
        return runCatching {
            deleteMuscleMass(measurement.id)
            client.insertRecords(listOf(record))
        }.isSuccess
    }

    suspend fun deleteMuscleMass(measurementId: Int): Boolean {
        if (measurementId <= 0) return false
        val client = manager.client ?: return false
        if (!manager.hasLeanBodyMassWrite()) return false
        return runCatching {
            client.deleteRecords(
                recordType = LeanBodyMassRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf(
                    HealthConnectManager.muscleMassClientRecordId(measurementId)
                )
            )
        }.isSuccess
    }

    /** Single height record under a fixed client id (delete-then-write). */
    suspend fun writeHeight(heightCm: Double): Boolean {
        if (heightCm <= 0) return false
        val client = manager.client ?: return false
        if (!manager.hasHeightWrite()) return false
        runCatching {
            client.deleteRecords(
                recordType = HeightRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf(HealthConnectManager.HEIGHT_CLIENT_ID)
            )
        }
        val record = HeightRecord(
            time = Instant.now(),
            zoneOffset = null,
            height = Length.meters(heightCm / 100.0),
            metadata = Metadata.manualEntry(
                clientRecordId = HealthConnectManager.HEIGHT_CLIENT_ID
            )
        )
        return runCatching { client.insertRecords(listOf(record)) }.isSuccess
    }

    /**
     * Export a water log. Ready for the upcoming water-logging feature —
     * [entryId] should be stable so edits/deletes stay linked.
     */
    suspend fun writeHydration(entryId: Long, milliliters: Double, timestampMs: Long): Boolean {
        if (entryId <= 0L || milliliters <= 0) return false
        val client = manager.client ?: return false
        if (!manager.hasHydrationWrite()) return false
        val start = Instant.ofEpochMilli(timestampMs)
        if (start.isAfter(Instant.now())) return false
        val end = start.plusSeconds(60)
        val record = HydrationRecord(
            startTime = start,
            startZoneOffset = null,
            endTime = end,
            endZoneOffset = null,
            volume = Volume.liters(milliliters / 1000.0),
            metadata = Metadata.manualEntry(
                clientRecordId = HealthConnectManager.hydrationClientRecordId(entryId)
            )
        )
        return runCatching {
            deleteHydration(entryId)
            client.insertRecords(listOf(record))
        }.isSuccess
    }

    suspend fun deleteHydration(entryId: Long): Boolean {
        if (entryId <= 0L) return false
        val client = manager.client ?: return false
        if (!manager.hasHydrationWrite()) return false
        return runCatching {
            client.deleteRecords(
                recordType = HydrationRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf(HealthConnectManager.hydrationClientRecordId(entryId))
            )
        }.isSuccess
    }

    suspend fun writeExercise(log: ExerciseLog): Boolean {
        if (log.id <= 0) return false
        val client = manager.client ?: return false
        if (!manager.hasExerciseWrite()) return false
        val start = Instant.ofEpochMilli(log.timestamp)
        if (start.isAfter(Instant.now())) return false
        val durationSec = (log.durationMinutes.coerceAtLeast(1) * 60L)
        val end = start.plusSeconds(durationSec)
        val session = ExerciseSessionRecord(
            startTime = start,
            startZoneOffset = null,
            endTime = end,
            endZoneOffset = null,
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_OTHER_WORKOUT,
            title = log.activityName,
            metadata = Metadata.manualEntry(
                clientRecordId = HealthConnectManager.exerciseClientRecordId(log.id)
            )
        )
        val records = buildList {
            add(session)
            if (log.caloriesBurned > 0 && manager.hasActiveCaloriesWrite()) {
                add(
                    ActiveCaloriesBurnedRecord(
                        startTime = start,
                        startZoneOffset = null,
                        endTime = end,
                        endZoneOffset = null,
                        energy = Energy.kilocalories(log.caloriesBurned.toDouble()),
                        metadata = Metadata.manualEntry(
                            clientRecordId = "${HealthConnectManager.exerciseClientRecordId(log.id)}-kcal"
                        )
                    )
                )
            }
        }
        return runCatching {
            deleteExercise(log.id)
            client.insertRecords(records)
        }.isSuccess
    }

    suspend fun deleteExercise(exerciseLogId: Int): Boolean {
        if (exerciseLogId <= 0) return false
        val client = manager.client ?: return false
        if (!manager.hasExerciseWrite()) return false
        val ids = listOf(
            HealthConnectManager.exerciseClientRecordId(exerciseLogId),
            "${HealthConnectManager.exerciseClientRecordId(exerciseLogId)}-kcal"
        )
        return runCatching {
            client.deleteRecords(
                recordType = ExerciseSessionRecord::class,
                recordIdsList = emptyList(),
                clientRecordIdsList = listOf(ids[0])
            )
            if (manager.hasActiveCaloriesWrite()) {
                client.deleteRecords(
                    recordType = ActiveCaloriesBurnedRecord::class,
                    recordIdsList = emptyList(),
                    clientRecordIdsList = listOf(ids[1])
                )
            }
        }.isSuccess
    }
}
