package com.anant.fitbuddy.data.pcsync

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.database.BodyMeasurement
import com.anant.fitbuddy.data.database.ExerciseLog
import com.anant.fitbuddy.data.database.FoodLog
import com.anant.fitbuddy.data.database.MealFood
import com.anant.fitbuddy.data.repository.FitnessRepository
import com.anant.fitbuddy.util.DateUtils
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class PcSyncStatus(
    val syncing: Boolean = false,
    val lastSuccessAt: Long = 0L,
    val lastError: String? = null,
    /** Entries created on the PC and saved on this phone during the last successful sync. */
    val lastReceived: Int = 0
)

/**
 * Pushes the phone's data to the desktop companion and applies entries queued on the PC.
 * Runs while the app is in the foreground: on resume, after local data changes (debounced),
 * and every [FOREGROUND_INTERVAL_MS]. Never throws; failures surface in [status].
 */
class PcSyncManager(
    app: Application,
    private val repository: FitnessRepository,
    private val dataChanges: Flow<Set<String>>,
    moshi: Moshi,
    private val scope: CoroutineScope
) {
    private val appContext = app.applicationContext
    private val store = PcSyncStore(app)
    private val requestAdapter = moshi.adapter(PcSyncRequest::class.java)
    private val responseAdapter = moshi.adapter(PcSyncResponse::class.java)
    private val snapshotAdapter = moshi.adapter(com.anant.fitbuddy.data.backup.BackupData::class.java)
    // Dedicated client: the shared one may log bodies/headers when verbose HTTP logging is on.
    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val mutex = Mutex()
    private val triggers = Channel<Unit>(Channel.CONFLATED)
    private var lastSentHash: String? = null
    private var resumedActivities = 0
    private var foregroundLoop: Job? = null

    private val _config = MutableStateFlow(store.config())
    val config: StateFlow<PcSyncConfig> = _config.asStateFlow()

    private val _status = MutableStateFlow(PcSyncStatus(lastSuccessAt = store.lastSuccessAt))
    val status: StateFlow<PcSyncStatus> = _status.asStateFlow()

    fun start(app: Application) {
        PcSyncWorker.apply(app, _config.value)
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) {
                resumedActivities++
                if (resumedActivities == 1) onForeground()
            }

            override fun onActivityPaused(activity: Activity) {
                resumedActivities = (resumedActivities - 1).coerceAtLeast(0)
                if (resumedActivities == 0) {
                    foregroundLoop?.cancel()
                    foregroundLoop = null
                    requestSync()
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        scope.launch {
            for (unit in triggers) {
                delay(DEBOUNCE_MS)
                syncNow()
            }
        }
        scope.launch {
            dataChanges.collect { requestSync() }
        }
    }

    fun saveConfig(config: PcSyncConfig) {
        store.saveConfig(config)
        _config.value = store.config()
        PcSyncWorker.apply(appContext, _config.value)
        lastSentHash = null
        _status.update { it.copy(lastError = null) }
        requestSync()
    }

    fun requestSync() {
        if (_config.value.isUsable) triggers.trySend(Unit)
    }

    private fun onForeground() {
        requestSync()
        foregroundLoop?.cancel()
        foregroundLoop = scope.launch {
            while (isActive) {
                delay(FOREGROUND_INTERVAL_MS)
                requestSync()
            }
        }
    }

    /** One full exchange (upload, apply PC entries, ack). Returns true on success. */
    suspend fun syncNow(): Boolean = mutex.withLock {
        val cfg = _config.value
        if (!cfg.isUsable) return@withLock false
        _status.update { it.copy(syncing = true) }
        val outcome = withContext(Dispatchers.IO) {
            withTimeoutOrNull(SYNC_TIMEOUT_MS) { runCatching { exchange(cfg) } }
                ?: Result.failure(IllegalStateException("Timed out"))
        }
        outcome.fold(
            onSuccess = { received ->
                val now = System.currentTimeMillis()
                store.lastSuccessAt = now
                _status.value = PcSyncStatus(lastSuccessAt = now, lastReceived = received)
            },
            onFailure = { error ->
                _status.update { it.copy(syncing = false, lastError = describe(error)) }
            }
        )
        outcome.isSuccess
    }

    private suspend fun exchange(cfg: PcSyncConfig): Int {
        var received = 0
        var forceSnapshot = false
        for (round in 0 until MAX_ROUNDS) {
            val snapshot = repository.buildPcSyncSnapshot()
            val hash = sha256(snapshotAdapter.toJson(snapshot.copy(exportedAt = 0L)))
            val sendSnapshot = forceSnapshot || hash != lastSentHash
            val acks = store.pendingAcks
            val response = post(
                cfg,
                PcSyncRequest(
                    sentAt = System.currentTimeMillis(),
                    appVersion = BuildConfig.VERSION_NAME,
                    ackedOps = acks,
                    snapshot = snapshot.takeIf { sendSnapshot },
                    snapshotHash = hash
                )
            )
            store.pendingAcks = store.pendingAcks - acks.toSet()
            if (sendSnapshot) lastSentHash = hash
            if (response.needSnapshot && !sendSnapshot) {
                forceSnapshot = true
                continue
            }
            forceSnapshot = false
            if (response.ops.isEmpty()) break
            val applied = store.appliedOps.toMutableList()
            for (op in response.ops) {
                if (op.id !in applied) {
                    if (apply(op)) received++
                    applied += op.id
                    store.appliedOps = applied
                }
                store.pendingAcks = (store.pendingAcks + op.id).distinct()
            }
        }
        return received
    }

    private suspend fun apply(op: PcOp): Boolean = when (op.type) {
        PcOpType.ADD_MEAL -> op.meal?.let { meal ->
            val foods = meal.foods.map {
                MealFood(
                    mealLogId = 0,
                    name = it.name,
                    servings = it.servings,
                    calories = it.calories,
                    proteinG = it.proteinG,
                    carbsG = it.carbsG,
                    fatsG = it.fatsG,
                    ingredients = it.ingredients
                )
            }
            repository.insertMealFromPc(
                FoodLog(
                    dishName = meal.dishName,
                    timestamp = meal.timestamp,
                    dateString = DateUtils.format(meal.timestamp),
                    calories = foods.sumOf { it.calories },
                    proteinG = foods.sumOf { it.proteinG },
                    carbsG = foods.sumOf { it.carbsG },
                    fatsG = foods.sumOf { it.fatsG }
                ),
                foods
            )
            true
        } ?: false
        PcOpType.ADD_EXERCISE -> op.exercise?.let {
            repository.insertExerciseFromPc(
                ExerciseLog(
                    activityName = it.activityName,
                    timestamp = it.timestamp,
                    dateString = DateUtils.format(it.timestamp),
                    caloriesBurned = it.caloriesBurned,
                    durationMinutes = it.durationMinutes
                )
            )
            true
        } ?: false
        PcOpType.ADD_MEASUREMENT -> op.measurement?.let {
            repository.addMeasurement(
                BodyMeasurement(
                    timestamp = it.timestamp,
                    dateString = DateUtils.format(it.timestamp),
                    weightKg = it.weightKg,
                    bodyFatPct = it.bodyFatPct,
                    muscleMassKg = it.muscleMassKg,
                    bmr = it.bmr
                )
            )
            true
        } ?: false
        PcOpType.DELETE_MEAL -> op.targetId?.let { repository.deleteFoodById(it, op.targetTimestamp) } ?: false
        PcOpType.DELETE_EXERCISE -> op.targetId?.let { repository.deleteExerciseById(it, op.targetTimestamp) } ?: false
        PcOpType.DELETE_MEASUREMENT ->
            op.targetId?.let { repository.deleteMeasurementById(it, op.targetTimestamp) } ?: false
        else -> false
    }

    private fun post(cfg: PcSyncConfig, body: PcSyncRequest): PcSyncResponse {
        val request = Request.Builder()
            .url("${cfg.baseUrl()}/api/v1/sync")
            .header("Authorization", "Bearer ${cfg.token}")
            .post(requestAdapter.toJson(body).toRequestBody(JSON))
            .build()
        http.newCall(request).execute().use { response ->
            when (response.code) {
                401, 403 -> throw PcSyncException("Pairing code rejected by the PC")
                409 -> throw PcSyncException("Update FitBuddy Desktop: protocol mismatch")
            }
            if (!response.isSuccessful) throw PcSyncException("PC answered HTTP ${response.code}")
            val text = response.body.string()
            return responseAdapter.fromJson(text) ?: throw PcSyncException("Empty reply from the PC")
        }
    }

    private fun describe(error: Throwable): String = when (error) {
        is PcSyncException -> error.message.orEmpty()
        is java.net.UnknownHostException -> "PC not found — check the address and Tailscale"
        is java.net.ConnectException -> "PC unreachable — is FitBuddy Desktop running?"
        is java.net.SocketTimeoutException -> "PC didn't answer in time"
        else -> error.message ?: error.javaClass.simpleName
    }

    private fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    private class PcSyncException(message: String) : Exception(message)

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        const val DEBOUNCE_MS = 2_000L
        const val FOREGROUND_INTERVAL_MS = 60_000L
        const val SYNC_TIMEOUT_MS = 60_000L
        const val MAX_ROUNDS = 4
    }
}
