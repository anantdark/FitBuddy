package com.anant.fitbuddy.data.backup

/**
 * Append-only cloud chunk addressing. Head `_id` is the Support ID; later nodes use
 * `{supportId}::c::{n}` for n ≥ 1.
 */
object BackupChunkIds {
    const val SEPARATOR = "::c::"

    fun isValidSupportId(supportId: String): Boolean {
        val id = supportId.trim()
        return id.isNotEmpty() && !id.contains(SEPARATOR)
    }

    fun headId(supportId: String): String = supportId.trim()

    fun chunkId(supportId: String, chunkIndex: Int): String {
        require(chunkIndex >= 0) { "chunkIndex must be >= 0" }
        val id = supportId.trim()
        require(isValidSupportId(id)) { "Support ID must not contain $SEPARATOR" }
        return if (chunkIndex == 0) id else "$id$SEPARATOR$chunkIndex"
    }

    fun parseChunkIndex(chunkId: String, supportId: String): Int? {
        val id = supportId.trim()
        val cid = chunkId.trim()
        if (cid == id) return 0
        val prefix = "$id$SEPARATOR"
        if (!cid.startsWith(prefix)) return null
        return cid.removePrefix(prefix).toIntOrNull()?.takeIf { it >= 1 }
    }
}
