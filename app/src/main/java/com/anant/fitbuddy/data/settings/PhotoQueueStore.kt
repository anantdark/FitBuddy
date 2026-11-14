package com.anant.fitbuddy.data.settings

import android.content.Context
import com.anant.fitbuddy.data.model.QueuedPhotoIndex
import com.anant.fitbuddy.data.model.QueuedPhotoLog
import com.anant.fitbuddy.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Photo logs queued to be analysed later; survives process death. Newest first. */
class PhotoQueueStore(context: Context) {

    private val dir = File(context.applicationContext.filesDir, DIR_NAME)
    private val indexFile = File(dir, INDEX_NAME)
    private val adapter = NetworkModule.moshi.adapter(QueuedPhotoIndex::class.java)
    private val mutex = Mutex()
    private val _items = MutableStateFlow<List<QueuedPhotoLog>>(emptyList())
    val items: StateFlow<List<QueuedPhotoLog>> = _items.asStateFlow()

    suspend fun load() = mutex.withLock {
        withContext(Dispatchers.IO) { _items.value = readUnlocked() }
    }

    suspend fun add(photos: List<ByteArray>, note: String, nowEpochMs: Long = System.currentTimeMillis()) =
        mutex.withLock {
            withContext(Dispatchers.IO) {
                dir.mkdirs()
                val id = UUID.randomUUID().toString()
                val files = photos.mapIndexed { i, bytes ->
                    "${id}_$i.jpg".also { File(dir, it).writeBytes(bytes) }
                }
                val entry = QueuedPhotoLog(id, nowEpochMs, note.trim(), files)
                writeUnlocked(listOf(entry) + readUnlocked())
            }
        }

    /** Replaces photos and note of an existing item, keeping its id and queue position. */
    suspend fun update(id: String, photos: List<ByteArray>, note: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val all = readUnlocked()
            val old = all.firstOrNull { it.id == id } ?: return@withContext
            val stamp = System.currentTimeMillis()
            val files = photos.mapIndexed { i, bytes ->
                "${id}_${stamp}_$i.jpg".also { File(dir, it).writeBytes(bytes) }
            }
            old.photoFiles.forEach { File(dir, it).delete() }
            writeUnlocked(all.map { if (it.id == id) it.copy(note = note.trim(), photoFiles = files) else it })
        }
    }

    suspend fun photos(id: String): List<ByteArray> = mutex.withLock {
        withContext(Dispatchers.IO) {
            val entry = readUnlocked().firstOrNull { it.id == id } ?: return@withContext emptyList()
            entry.photoFiles.mapNotNull { name ->
                runCatching { File(dir, name).readBytes() }.getOrNull()
            }
        }
    }

    suspend fun remove(id: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val all = readUnlocked()
            all.firstOrNull { it.id == id }?.photoFiles?.forEach { File(dir, it).delete() }
            writeUnlocked(all.filterNot { it.id == id })
        }
    }

    private fun readUnlocked(): List<QueuedPhotoLog> {
        val raw = runCatching { indexFile.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        return runCatching { adapter.fromJson(raw)?.items }.getOrNull().orEmpty()
    }

    private fun writeUnlocked(items: List<QueuedPhotoLog>) {
        dir.mkdirs()
        indexFile.writeText(adapter.toJson(QueuedPhotoIndex(items)))
        _items.value = items
    }

    private companion object {
        const val DIR_NAME = "photo_queue"
        const val INDEX_NAME = "index.json"
    }
}
