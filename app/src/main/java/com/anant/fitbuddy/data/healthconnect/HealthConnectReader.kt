package com.anant.fitbuddy.data.healthconnect

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

data class ExternalWeight(
    val timestampMs: Long,
    val weightKg: Double,
    val clientRecordId: String?
)

data class ExternalBodyFat(
    val timestampMs: Long,
    val bodyFatPct: Double,
    val clientRecordId: String?
)

data class ExternalMuscleMass(
    val timestampMs: Long,
    val muscleMassKg: Double,
    val clientRecordId: String?
)

data class ExternalHeight(
    val timestampMs: Long,
    val heightCm: Double,
    val clientRecordId: String?
)

data class ExternalExercise(
    val recordId: String,
    val title: String,
    val startMs: Long,
    val endMs: Long,
    val caloriesBurned: Int,
    val clientRecordId: String?
)

data class ExternalDailySteps(
    val dateString: String,
    val steps: Long,
    val timestampMs: Long
)

data class ExternalHydrationDay(
    val dateString: String,
    val milliliters: Double
)

data class ExternalSleepNight(
    val endDateString: String,
    val durationMinutes: Int,
    val endMs: Long
)

internal class HealthConnectReader(
    private val manager: HealthConnectManager
) {
    suspend fun readWeights(from: Instant, to: Instant): List<ExternalWeight> {
        val client = manager.client ?: return emptyList()
        if (!manager.hasWeightRead()) return emptyList()
        return runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from, to)
                )
            ).records.mapNotNull { record ->
                val clientId = record.metadata.clientRecordId
                if (HealthConnectManager.isFitBuddyRecord(clientId)) return@mapNotNull null
                ExternalWeight(
                    timestampMs = record.time.toEpochMilli(),
                    weightKg = record.weight.inKilograms,
                    clientRecordId = clientId
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun readBodyFats(from: Instant, to: Instant): List<ExternalBodyFat> {
        val client = manager.client ?: return emptyList()
        if (!manager.hasBodyFatRead()) return emptyList()
        return runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = BodyFatRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from, to)
                )
            ).records.mapNotNull { record ->
                val clientId = record.metadata.clientRecordId
                if (HealthConnectManager.isFitBuddyRecord(clientId)) return@mapNotNull null
                ExternalBodyFat(
                    timestampMs = record.time.toEpochMilli(),
                    bodyFatPct = record.percentage.value,
                    clientRecordId = clientId
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun readMuscleMasses(from: Instant, to: Instant): List<ExternalMuscleMass> {
        val client = manager.client ?: return emptyList()
        if (!manager.hasLeanBodyMassRead()) return emptyList()
        return runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = LeanBodyMassRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from, to)
                )
            ).records.mapNotNull { record ->
                val clientId = record.metadata.clientRecordId
                if (HealthConnectManager.isFitBuddyRecord(clientId)) return@mapNotNull null
                ExternalMuscleMass(
                    timestampMs = record.time.toEpochMilli(),
                    muscleMassKg = record.mass.inKilograms,
                    clientRecordId = clientId
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun readLatestHeight(from: Instant, to: Instant): ExternalHeight? {
        val client = manager.client ?: return null
        if (!manager.hasHeightRead()) return null
        return runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = HeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from, to)
                )
            ).records
                .filterNot { HealthConnectManager.isFitBuddyRecord(it.metadata.clientRecordId) }
                .maxByOrNull { it.time }
                ?.let { record ->
                    ExternalHeight(
                        timestampMs = record.time.toEpochMilli(),
                        heightCm = record.height.inMeters * 100.0,
                        clientRecordId = record.metadata.clientRecordId
                    )
                }
        }.getOrNull()
    }

    suspend fun readExercises(from: Instant, to: Instant): List<ExternalExercise> {
        val client = manager.client ?: return emptyList()
        if (!manager.hasExerciseRead()) return emptyList()
        return runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from, to)
                )
            ).records.mapNotNull { record ->
                val clientId = record.metadata.clientRecordId
                if (HealthConnectManager.isFitBuddyRecord(clientId)) return@mapNotNull null
                val start = record.startTime
                val end = record.endTime
                val kcal = activeCaloriesBetween(start, end)
                val title = record.title?.takeIf { it.isNotBlank() }
                    ?: exerciseTypeLabel(record.exerciseType)
                ExternalExercise(
                    recordId = record.metadata.id,
                    title = title,
                    startMs = start.toEpochMilli(),
                    endMs = end.toEpochMilli(),
                    caloriesBurned = kcal,
                    clientRecordId = clientId
                )
            }
        }.getOrDefault(emptyList())
    }

    suspend fun readDailySteps(from: Instant, to: Instant): List<ExternalDailySteps> {
        val client = manager.client ?: return emptyList()
        if (!manager.hasStepsRead()) return emptyList()
        if (!to.isAfter(from)) return emptyList()
        val zone = ZoneId.systemDefault()
        val startDay = LocalDate.ofInstant(from, zone)
        val endDay = LocalDate.ofInstant(to, zone)
        val out = mutableListOf<ExternalDailySteps>()
        var day = startDay
        while (!day.isAfter(endDay)) {
            val dayStart = day.atStartOfDay(zone).toInstant()
            val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant()
            val clippedStart = if (dayStart.isBefore(from)) from else dayStart
            val clippedEnd = if (dayEnd.isAfter(to)) to else dayEnd
            if (clippedEnd.isAfter(clippedStart)) {
                val steps = runCatching {
                    val result = client.aggregate(
                        AggregateRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter = TimeRangeFilter.between(clippedStart, clippedEnd)
                        )
                    )
                    result[StepsRecord.COUNT_TOTAL] ?: 0L
                }.getOrDefault(0L)
                if (steps > 0L) {
                    out += ExternalDailySteps(
                        dateString = day.toString(),
                        steps = steps,
                        timestampMs = day.atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
                    )
                }
            }
            day = day.plusDays(1)
        }
        return out
    }

    suspend fun readHydrationTodayMl(now: Instant = Instant.now()): ExternalHydrationDay? {
        val client = manager.client ?: return null
        if (!manager.hasHydrationRead()) return null
        val zone = ZoneId.systemDefault()
        val day = LocalDate.ofInstant(now, zone)
        val start = day.atStartOfDay(zone).toInstant()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant()
        return runCatching {
            val result = client.aggregate(
                AggregateRequest(
                    metrics = setOf(HydrationRecord.VOLUME_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            val liters = result[HydrationRecord.VOLUME_TOTAL]?.inLiters ?: 0.0
            if (liters <= 0) null
            else ExternalHydrationDay(dateString = day.toString(), milliliters = liters * 1000.0)
        }.getOrNull()
    }

    /** Most recent completed sleep session ending within the last ~36 hours. */
    suspend fun readRecentSleep(now: Instant = Instant.now()): ExternalSleepNight? {
        val client = manager.client ?: return null
        if (!manager.hasSleepRead()) return null
        val from = now.minusMillis(TimeUnit.HOURS.toMillis(36))
        return runCatching {
            client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(from, now)
                )
            ).records
                .filterNot { HealthConnectManager.isFitBuddyRecord(it.metadata.clientRecordId) }
                .maxByOrNull { it.endTime }
                ?.let { session ->
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(
                        session.endTime.toEpochMilli() - session.startTime.toEpochMilli()
                    ).toInt().coerceAtLeast(0)
                    if (minutes <= 0) return@let null
                    val zone = ZoneId.systemDefault()
                    ExternalSleepNight(
                        endDateString = LocalDate.ofInstant(session.endTime, zone).toString(),
                        durationMinutes = minutes,
                        endMs = session.endTime.toEpochMilli()
                    )
                }
        }.getOrNull()
    }

    private suspend fun activeCaloriesBetween(from: Instant, to: Instant): Int {
        if (!manager.hasActiveCaloriesRead()) return 0
        val client = manager.client ?: return 0
        if (!to.isAfter(from)) return 0
        return runCatching {
            val result = client.aggregate(
                AggregateRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(from, to)
                )
            )
            val energy: Energy? = result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
            energy?.inKilocalories?.toInt()?.coerceAtLeast(0) ?: 0
        }.getOrDefault(0)
    }

    private fun exerciseTypeLabel(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Running"
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walking"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Cycling"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Swimming"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
        ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Strength training"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hiking"
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "Elliptical"
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "Rowing"
        ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
        else -> "Workout"
    }
}
