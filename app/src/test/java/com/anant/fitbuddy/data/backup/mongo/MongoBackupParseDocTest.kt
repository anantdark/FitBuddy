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
        // Defensive: Android optString historically returns the word "null" for JSON null.
        val json = JSONObject("""{"nextChunkId":"null"}""")
        assertNull(repo.optionalChunkRef(json, "nextChunkId"))
    }

    @Test
    fun optionalChunkRef_realChunkId_preserved() {
        val id = "ef427ee1-3fd4-4954-90eb-fc69e32380ed::c::1"
        val json = JSONObject("""{"nextChunkId":"$id"}""")
        assertEquals(id, repo.optionalChunkRef(json, "nextChunkId"))
    }
}
