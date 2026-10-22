package com.anant.fitbuddy.data.remote.gymgifs

import android.content.Context
import com.anant.fitbuddy.data.model.CatalogExercise
import com.anant.fitbuddy.data.model.Equipment
import com.anant.fitbuddy.data.model.ExerciseNameMatcher
import com.anant.fitbuddy.data.model.titleCaseLabel
import com.anant.fitbuddy.data.remote.NetworkModule
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Fallback exercise media catalog from the user's [ExerciseGymGifsDB] fork on jsDelivr.
 * Used when a custom exercise is not in the primary ExerciseDB catalog — looks up by name and
 * returns a [CatalogExercise] with a rewritten GIF URL pointing at this fork.
 */
class GymGifsCatalogRepository(
    context: Context,
    private val client: OkHttpClient = NetworkModule.okHttpClient(),
    private val moshi: Moshi = NetworkModule.moshi,
) {
    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.filesDir, "gymgifs").also { it.mkdirs() }
    private val exercisesFile = File(cacheDir, "exercises-en.json")
    private val metaFile = File(cacheDir, "meta.txt")

    private val mutex = Mutex()
    private val fileAdapter by lazy { moshi.adapter(GymGifsExercisesFileDto::class.java) }

    @Volatile
    private var cached: List<CatalogExercise> = emptyList()

    /** Ensures the fallback index is in memory (disk cache, then CDN if stale/missing). */
    suspend fun ensureLoaded(forceRefresh: Boolean = false) = mutex.withLock {
        if (cached.isNotEmpty() && !forceRefresh && !isCacheStale()) return@withLock
        if (!forceRefresh && loadFromDisk()) return@withLock
        fetchAndCache()
    }

    /**
     * Exact or fuzzy name match against the fallback catalog. Loads the index on first use.
     * Returns null when nothing is confidently similar.
     */
    suspend fun findByName(query: String): CatalogExercise? {
        ensureLoaded()
        val list = cached
        if (list.isEmpty()) return null
        return ExerciseNameMatcher.findBest(query, list, nameOf = { it.name })
    }

    private fun loadFromDisk(): Boolean {
        if (!exercisesFile.isFile) return false
        val dto = runCatching { fileAdapter.fromJson(exercisesFile.readText()) }.getOrNull()
            ?: return false
        if (dto.exercises.isEmpty()) return false
        cached = dto.exercises.map { it.toCatalog() }
        return true
    }

    private fun isCacheStale(): Boolean {
        val writtenAt = metaFile.takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull()
            ?: return true
        return System.currentTimeMillis() - writtenAt > CACHE_TTL_MS
    }

    private suspend fun fetchAndCache() = withContext(Dispatchers.IO) {
        val json = download(EXERCISES_URL) ?: return@withContext
        val dto = runCatching { fileAdapter.fromJson(json) }.getOrNull() ?: return@withContext
        if (dto.exercises.isEmpty()) return@withContext
        exercisesFile.writeText(json)
        metaFile.writeText(System.currentTimeMillis().toString())
        cached = dto.exercises.map { it.toCatalog() }
    }

    private fun download(url: String): String? = runCatching {
        val request = Request.Builder().url(url).header("Accept", "application/json").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body.string().takeIf { it.isNotBlank() }
        }
    }.getOrNull()

    private fun GymGifsExerciseDto.toCatalog(): CatalogExercise {
        val relative = file.ifBlank {
            gifUrl?.substringAfter("$CDN_UPSTREAM/")?.substringAfter("$CDN_BASE/")
                ?: "$muscle/$slug.gif"
        }.trimStart('/')
        return CatalogExercise(
            exerciseId = id,
            name = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            equipmentTag = mapGymGifsEquipment(equipment, bodyPart),
            gifUrl = gifUrlFor(relative),
            bodyParts = listOfNotNull(bodyPart.takeIf { it.isNotBlank() }?.let(::titleCaseLabel)),
            equipments = listOfNotNull(equipment.takeIf { it.isNotBlank() }?.let(::titleCaseLabel)),
            targetMuscles = listOfNotNull(muscle.takeIf { it.isNotBlank() }?.let(::titleCaseLabel)),
            secondaryMuscles = secondaryMuscles.map { titleCaseLabel(it) },
            instructions = instructions
        )
    }

    companion object {
        /** Prefer the user's fork so GIF URLs stay under anantdark. */
        private const val CDN_BASE =
            "https://cdn.jsdelivr.net/gh/anantdark/ExerciseGymGifsDB@main"
        private const val CDN_UPSTREAM =
            "https://cdn.jsdelivr.net/gh/JahelCuadrado/ExerciseGymGifsDB@main"
        const val EXERCISES_URL = "$CDN_BASE/api/en/exercises.json"
        private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000

        fun gifUrlFor(relativeFile: String): String =
            "$CDN_BASE/${relativeFile.trimStart('/')}"

        fun mapGymGifsEquipment(equipment: String, bodyPart: String = ""): String {
            if (bodyPart.trim().equals("cardio", ignoreCase = true)) return Equipment.CARDIO
            return when (equipment.trim().lowercase()) {
                "dumbbell" -> Equipment.DUMBBELL
                "barbell", "ez-bar" -> Equipment.BARBELL
                "bodyweight" -> Equipment.BODYWEIGHT
                "cable", "machine", "lever", "smith", "sled" -> Equipment.MACHINE
                "band", "kettlebell", "other", "" -> Equipment.OTHER
                else -> Equipment.OTHER
            }
        }
    }
}
