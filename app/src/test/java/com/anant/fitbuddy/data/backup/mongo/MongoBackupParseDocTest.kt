package com.anant.fitbuddy.data.backup.mongo

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MongoBackupParseDocTest {

    private val repo = MongoBackupRepository()

    @Test
    fun optionalChunkRef_jsonNull_isNull() {
        val json = JSONObject("""{"nextChunkId":null,"tipChunkId":null}""")
        assertNull(repo.optionalChunkRef(json, "nextChunkId"))
        assertNull(repo.optionalChunkRef(json, "tipChunkId"))
    }

    @Test
    fun optionalChunkRef_missingKey_isNull() {
        val json = JSONObject("{}")
        assertNull(repo.optionalChunkRef(json, "nextChunkId"))
    }

    @Test
    fun optionalChunkRef_literalNullString_isNull() {
        // Defensive: Android JSONObject.optString historically returns the word "null" for JSON null.
        val json = JSONObject("""{"nextChunkId":"null"}""")
        assertNull(repo.optionalChunkRef(json, "nextChunkId"))
    }

    @Test
    fun optionalChunkRef_realChunkId_preserved() {
        val id = "ef427ee1-3fd4-4954-90eb-fc69e32380ed::c::1"
        val json = JSONObject("""{"nextChunkId":"$id"}""")
        assertEquals(id, repo.optionalChunkRef(json, "nextChunkId"))
    }

    @Test
    fun formatExportedAtIso_isReadableUtc() {
        assertEquals(
            "2026-11-20T16:05:40.539Z",
            MongoBackupRepository.formatExportedAtIso(1_795_190_740_539L),
        )
    }

    @Test
    fun parseExportedAt_isoString() {
        val json = JSONObject("""{"exportedAt":"2026-11-20T16:05:40.539Z"}""")
        assertEquals(1_795_190_740_539L, repo.parseExportedAt(json))
    }

    @Test
    fun parseExportedAt_legacyEpochMillisNumber() {
        val json = JSONObject().put("exportedAt", 1_795_190_740_539L)
        assertEquals(1_795_190_740_539L, repo.parseExportedAt(json))
    }

    @Test
    fun parseExportedAt_legacyEpochMillisString() {
        val json = JSONObject("""{"exportedAt":"1795190740539"}""")
        assertEquals(1_795_190_740_539L, repo.parseExportedAt(json))
    }

    @Test
    fun parseExportedAt_missing_isZero() {
        assertEquals(0L, repo.parseExportedAt(JSONObject("{}")))
    }
}
