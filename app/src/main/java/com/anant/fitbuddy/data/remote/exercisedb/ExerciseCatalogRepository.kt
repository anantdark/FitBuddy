package com.anant.fitbuddy.data.remote.exercisedb

import android.content.Context
import com.anant.fitbuddy.data.model.COMMON_EXERCISES_SEED
import com.anant.fitbuddy.data.model.CatalogExercise
import com.anant.fitbuddy.data.model.mapExerciseDbEquipment
import com.anant.fitbuddy.data.model.titleCaseLabel
import com.anant.fitbuddy.data.remote.NetworkModule
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Loads the ExerciseDB catalog from jsDelivr (user fork), caches JSON on disk, and rewrites GIF
 * URLs to the same CDN. Falls back to [COMMON_EXERCISES_SEED] until the first successful fetch.
 */
class ExerciseCatalogRepository(
    context: Context,
    private val client: OkHttpClient = NetworkModule.okHttpClient(),
    private val moshi: Moshi = NetworkModule.moshi,
) {
    private val appContext = context.applicationContext
    private val cacheDir = File(appContext.filesDir, "exercisedb").also { it.mkdirs() }
    private val exercisesFile = File(cacheDir, "exercises.json")
    private val bodyPartsFile = File(cacheDir, "bodyparts.json")
    private val equipmentsFile = File(cacheDir, "equipments.json")
    private val metaFile = File(cacheDir, "meta.txt")

    private val mutex = Mutex()

    private val exerciseListType =
        Types.newParameterizedType(List::class.java, ExerciseDbExerciseDto::class.java)
    private val namedListType =
        Types.newParameterizedType(List::class.java, ExerciseDbNamedDto::class.java)
    private val exerciseAdapter by lazy { moshi.adapter<List<ExerciseDbExerciseDto>>(exerciseListType) }
    private val namedAdapter by lazy { moshi.adapter<List<ExerciseDbNamedDto>>(namedListType) }

    private val _exercises = MutableStateFlow(COMMON_EXERCISES_SEED)
    val exercises: StateFlow<List<CatalogExercise>> = _exercises.asStateFlow()

    private val _bodyParts = MutableStateFlow<List<String>>(emptyList())
    val bodyParts: StateFlow<List<String>> = _bodyParts.asStateFlow()

    private val _equipments = MutableStateFlow<List<String>>(emptyList())
    val equipments: StateFlow<List<String>> = _equipments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** Loads disk cache into memory (if any), then refreshes from network when stale/missing. */
    suspend fun ensureLoaded(forceRefresh: Boolean = false) = mutex.withLock {
        val hadCache = loadFromDisk()
        val stale = isCacheStale()
        if (!hadCache || stale || forceRefresh) {
            _isLoading.value = true
            try {
                fetchAndCache()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun gifUrlFor(exerciseId: String): String =
        "$CDN_BASE/media/$exerciseId.gif"

    private fun loadFromDisk(): Boolean {
        if (!exercisesFile.isFile) return false
        val dtos = runCatching {
            exerciseAdapter.fromJson(exercisesFile.readText())
        }.getOrNull() ?: return false
        if (dtos.isEmpty()) return false
        _exercises.value = dtos.map { it.toCatalog() }
        _bodyParts.value = readNamedList(bodyPartsFile)
        _equipments.value = readNamedList(equipmentsFile)
        return true
    }

    private fun readNamedList(file: File): List<String> {
        if (!file.isFile) return emptyList()
        val list = runCatching { namedAdapter.fromJson(file.readText()) }.getOrNull().orEmpty()
        return list.map { titleCaseLabel(it.name) }.sortedBy { it.lowercase() }
    }

    private fun isCacheStale(): Boolean {
        val writtenAt = metaFile.takeIf { it.isFile }?.readText()?.trim()?.toLongOrNull() ?: return true
        return System.currentTimeMillis() - writtenAt > CACHE_TTL_MS
    }

    private suspend fun fetchAndCache() = withContext(Dispatchers.IO) {
        val exercisesJson = download(EXERCISES_URL) ?: return@withContext
        val bodyPartsJson = download(BODYPARTS_URL)
        val equipmentsJson = download(EQUIPMENTS_URL)

        val dtos = runCatching { exerciseAdapter.fromJson(exercisesJson) }.getOrNull()
            ?: return@withContext
        if (dtos.isEmpty()) return@withContext

        exercisesFile.writeText(exercisesJson)
        if (bodyPartsJson != null) bodyPartsFile.writeText(bodyPartsJson)
        if (equipmentsJson != null) equipmentsFile.writeText(equipmentsJson)
        metaFile.writeText(System.currentTimeMillis().toString())

        _exercises.value = dtos.map { it.toCatalog() }
        if (bodyPartsJson != null) {
            _bodyParts.value = namedAdapter.fromJson(bodyPartsJson)
                ?.map { titleCaseLabel(it.name) }
                ?.sortedBy { it.lowercase() }
                .orEmpty()
        } else {
            _bodyParts.value = dtos.flatMap { it.bodyParts }
                .map { titleCaseLabel(it) }
                .distinct()
                .sortedBy { it.lowercase() }
        }
        if (equipmentsJson != null) {
            _equipments.value = namedAdapter.fromJson(equipmentsJson)
                ?.map { titleCaseLabel(it.name) }
                ?.sortedBy { it.lowercase() }
                .orEmpty()
        } else {
            _equipments.value = dtos.flatMap { it.equipments }
                .map { titleCaseLabel(it) }
                .distinct()
                .sortedBy { it.lowercase() }
        }
    }

    private fun download(url: String): String? = runCatching {
        val request = Request.Builder().url(url).header("Accept", "application/json").get().build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            response.body.string().takeIf { it.isNotBlank() }
        }
    }.getOrNull()

    private fun ExerciseDbExerciseDto.toCatalog(): CatalogExercise = CatalogExercise(
        exerciseId = exerciseId,
        name = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
        equipmentTag = mapExerciseDbEquipment(equipments, bodyParts),
        gifUrl = gifUrlFor(exerciseId),
        bodyParts = bodyParts.map { titleCaseLabel(it) },
        equipments = equipments.map { titleCaseLabel(it) },
        targetMuscles = targetMuscles.map { titleCaseLabel(it) },
        secondaryMuscles = secondaryMuscles.map { titleCaseLabel(it) },
        instructions = instructions
    )

    companion object {
        private const val CDN_BASE =
            "https://cdn.jsdelivr.net/gh/anantdark/exercisedb-api@main"
        const val EXERCISES_URL = "$CDN_BASE/src/data/exercises.json"
        const val BODYPARTS_URL = "$CDN_BASE/src/data/bodyparts.json"
        const val EQUIPMENTS_URL = "$CDN_BASE/src/data/equipments.json"
        private const val CACHE_TTL_MS = 7L * 24 * 60 * 60 * 1000
    }
}
