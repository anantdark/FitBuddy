package com.anant.fitbuddy.data.settings

import android.content.Context
import com.anant.fitbuddy.data.model.ProgressChatSession
import com.anant.fitbuddy.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Persists a kept progress-coach chat across process death.
 * Sessions expire once the UTC date is after the UTC date of [ProgressChatSession.lastMessageAtEpochMs].
 */
class ProgressChatSessionStore(context: Context) {

    private val file = File(context.applicationContext.filesDir, FILE_NAME)
    private val adapter = NetworkModule.moshi.adapter(ProgressChatSession::class.java)
    private val mutex = Mutex()

    /** Returns the session if present and not expired; clears disk when expired/corrupt. */
    suspend fun loadActive(nowEpochMs: Long = System.currentTimeMillis()): ProgressChatSession? =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                val session = readUnlocked() ?: return@withContext null
                if (isExpired(session.lastMessageAtEpochMs, nowEpochMs)) {
                    deleteUnlocked()
                    return@withContext null
                }
                session
            }
        }

    suspend fun save(session: ProgressChatSession) = mutex.withLock {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            file.writeText(adapter.toJson(session))
        }
    }

    suspend fun clear() = mutex.withLock {
        withContext(Dispatchers.IO) { deleteUnlocked() }
    }

    private fun readUnlocked(): ProgressChatSession? {
        if (!file.exists()) return null
        val raw = runCatching { file.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: return null
        return runCatching { adapter.fromJson(raw) }.getOrNull()?.takeIf {
            it.messages.isNotEmpty() && it.metricsContextFull.isNotBlank()
        } ?: run {
            deleteUnlocked()
            null
        }
    }

    private fun deleteUnlocked() {
        runCatching { if (file.exists()) file.delete() }
    }

    companion object {
        const val FILE_NAME = "progress_chat_session.json"

        /** True when [now] is on a later UTC calendar day than [lastMessageAtEpochMs]. */
        fun isExpired(lastMessageAtEpochMs: Long, nowEpochMs: Long): Boolean {
            if (lastMessageAtEpochMs <= 0L) return true
            val lastDay = utcDate(lastMessageAtEpochMs)
            val nowDay = utcDate(nowEpochMs)
            return nowDay.isAfter(lastDay)
        }

        private fun utcDate(epochMs: Long): LocalDate =
            Instant.ofEpochMilli(epochMs).atZone(ZoneOffset.UTC).toLocalDate()
    }
}
