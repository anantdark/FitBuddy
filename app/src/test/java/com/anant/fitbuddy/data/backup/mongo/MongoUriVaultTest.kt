package com.anant.fitbuddy.data.backup.mongo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MongoUriVaultTest {

    @Test
    fun encodeDecode_roundTripsUri() {
        val plain = "mongodb+srv://user:p%40ss@cluster0.example.mongodb.net/?retryWrites=true"
        val mask = "fitbuddy.mongo.v1"
        val blob = MongoUriVault.encode(plain, mask)
        assertTrue(blob.isNotBlank())
        assertEquals(plain, MongoUriVault.decode(blob, mask))
    }

    @Test
    fun decode_emptyMask_returnsBytesAsUtf8() {
        val plain = "mongodb://localhost"
        val blob = java.util.Base64.getEncoder().encodeToString(plain.toByteArray(Charsets.UTF_8))
        // Empty mask: encode still XORs with empty → would fail; decode with empty skips XOR.
        assertEquals(plain, MongoUriVault.decode(blob, ""))
    }
}
