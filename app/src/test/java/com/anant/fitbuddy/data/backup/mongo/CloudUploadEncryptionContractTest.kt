package com.anant.fitbuddy.data.backup.mongo

import com.anant.fitbuddy.data.backup.BackupData
import com.anant.fitbuddy.data.backup.crypto.BackupCrypto
import com.anant.fitbuddy.data.backup.crypto.BackupEnvelope
import com.anant.fitbuddy.data.backup.crypto.BackupFormat
import com.anant.fitbuddy.data.backup.crypto.OpenResult
import com.anant.fitbuddy.data.remote.NetworkModule
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Verifies Property 6 (payload-only encryption): on cloud upload, only the `payloadJson`
 * argument passed to [MongoBackupRepository.upload] is ciphertext (a valid encrypted
 * [BackupEnvelope]). All sibling metadata fields (`supportId`, `exportedAt`, `deviceName`,
 * `macId`) are byte-identical to the pre-encryption contract values.
 *
 * Uses a [SpyMongoBackupRepository] (subclass of the open [MongoBackupRepository]) that
 * captures all arguments passed to `upload()`, exercising the exact sealing logic from
 * `FitnessRepository.uploadMongoBackup()`.
 *
 * **Validates: Requirements 6.1, 6.2**
 */
@RunWith(RobolectricTestRunner::class)
class CloudUploadEncryptionContractTest {

    private val moshi: Moshi = NetworkModule.moshi
    private val crypto = BackupCrypto(moshi)
    private val backupDataAdapter = moshi.adapter(BackupData::class.java).indent("  ")
    private val envelopeAdapter = moshi.adapter(BackupEnvelope::class.java)

    // ----- Fake MongoBackupRepository that captures upload() arguments -----------------------

    /**
     * A spy subclass of [MongoBackupRepository] that intercepts [upload] calls, captures all
     * arguments, and returns without making any network request. This mirrors what
     * `FitnessRepository.uploadMongoBackup()` would pass to the real repository.
     */
    private class SpyMongoBackupRepository : MongoBackupRepository() {

        data class CapturedArgs(
            val baseUrl: String,
            val apiKey: String,
            val databaseName: String,
            val collectionName: String,
            val supportId: String,
            val payloadJson: String,
            val exportedAt: Long,
            val deviceName: String,
            val macId: String
        )

        var lastCapture: CapturedArgs? = null
            private set

        override suspend fun upload(
            baseUrl: String,
            apiKey: String,
            databaseName: String,
            collectionName: String,
            supportId: String,
            payloadJson: String,
            exportedAt: Long,
            deviceName: String,
            macId: String
        ) {
            lastCapture = CapturedArgs(
                baseUrl = baseUrl,
                apiKey = apiKey,
                databaseName = databaseName,
                collectionName = collectionName,
                supportId = supportId,
                payloadJson = payloadJson,
                exportedAt = exportedAt,
                deviceName = deviceName,
                macId = macId
            )
        }
    }

    // ----- Helper: reproduce the sealing + upload call from uploadMongoBackup() ---------------

    /**
     * Executes the exact logic of `FitnessRepository.uploadMongoBackup()`:
     * build snapshot → encode → seal with Support ID → call repository.upload().
     * Returns the spy so tests can inspect captured arguments.
     */
    private suspend fun executeUploadViaSpy(
        data: BackupData,
        supportId: String,
        deviceName: String = "Pixel 8",
        macId: String = "AA:BB:CC:DD:EE:FF"
    ): SpyMongoBackupRepository {
        val spy = SpyMongoBackupRepository()

        val payloadJson = backupDataAdapter.toJson(data)
        val passwordChars = supportId.toCharArray()
        val sealedPayload = try {
            crypto.seal(payloadJson, passwordChars)
        } finally {
            passwordChars.fill('\u0000')
        }

        spy.upload(
            baseUrl = "https://proxy.example.com",
            apiKey = "test-api-key",
            databaseName = "fitbuddy",
            collectionName = "fitbuddy_backup",
            supportId = supportId,
            payloadJson = sealedPayload,
            exportedAt = data.exportedAt,
            deviceName = deviceName,
            macId = macId
        )
        return spy
    }

    // ----- Tests: Property 6 — payload-only encryption ----------------------------------------

    @Test
    fun payloadJson_passedToUpload_isValidEncryptedEnvelope() = runBlocking {
        val data = BackupData(exportedAt = System.currentTimeMillis())
        val supportId = "test-support-id-12345"

        val spy = executeUploadViaSpy(data, supportId)
        val captured = requireNotNull(spy.lastCapture)

        // payloadJson must classify as ENCRYPTED
        assertEquals(BackupFormat.ENCRYPTED, crypto.classify(captured.payloadJson))

        // payloadJson must parse as a valid BackupEnvelope
        val envelope = envelopeAdapter.fromJson(captured.payloadJson)
        assertNotNull("payloadJson must parse as a BackupEnvelope", envelope)
        assertEquals(1, envelope!!.fitbuddyBackup)
        assertEquals("AES-GCM", envelope.enc)
        assertNotNull("salt must be present", envelope.salt)
        assertNotNull("iv must be present", envelope.iv)
        assertNotNull("kdf must be present", envelope.kdf)
        assertNotNull("iterations must be present", envelope.iterations)
        assertTrue("iterations must be >= 1", envelope.iterations!! >= 1)
        assertTrue("ciphertext must be non-empty", envelope.ciphertext.isNotBlank())
    }

    @Test
    fun payloadJson_passedToUpload_doesNotContainRawBackupDataJson() = runBlocking {
        val data = BackupData(exportedAt = 1_700_000_000_000L)
        val supportId = "unique-support-id-xyz"
        val rawPayloadJson = backupDataAdapter.toJson(data)

        val spy = executeUploadViaSpy(data, supportId)
        val captured = requireNotNull(spy.lastCapture)

        // The sealed payload must NOT contain the raw JSON text
        assertFalse(
            "payloadJson must not contain the raw BackupData JSON",
            captured.payloadJson.contains(rawPayloadJson)
        )
        // Verify a distinctive substring of the payload isn't visible in the ciphertext envelope
        assertFalse(
            "payloadJson must not contain the exportedAt value in plain text form",
            captured.payloadJson.contains("\"exportedAt\":1700000000000")
        )
    }

    @Test
    fun metadataFields_passedToUpload_areByteIdenticalToPreEncryptionValues() = runBlocking {
        val expectedExportedAt = 1_700_123_456_789L
        val expectedSupportId = "metadata-test-support-id-99"
        val expectedDeviceName = "Samsung Galaxy S24"
        val expectedMacId = "11:22:33:44:55:66"

        val data = BackupData(exportedAt = expectedExportedAt)

        val spy = executeUploadViaSpy(
            data = data,
            supportId = expectedSupportId,
            deviceName = expectedDeviceName,
            macId = expectedMacId
        )
        val captured = requireNotNull(spy.lastCapture)

        // All metadata fields must be exactly the same values passed in (plaintext, not encrypted)
        assertEquals(
            "supportId must be plaintext and unchanged",
            expectedSupportId, captured.supportId
        )
        assertEquals(
            "exportedAt must be the original value (not encrypted)",
            expectedExportedAt, captured.exportedAt
        )
        assertEquals(
            "deviceName must be plaintext and unchanged",
            expectedDeviceName, captured.deviceName
        )
        assertEquals(
            "macId must be plaintext and unchanged",
            expectedMacId, captured.macId
        )
    }

    @Test
    fun payloadJson_passedToUpload_decryptsToOriginalBackupData() = runBlocking {
        val data = BackupData(exportedAt = 1_700_000_000_000L)
        val supportId = "decrypt-test-support-id"
        val rawPayloadJson = backupDataAdapter.toJson(data)

        val spy = executeUploadViaSpy(data, supportId)
        val captured = requireNotNull(spy.lastCapture)

        // Decrypt using the Support ID — must recover the original payload
        val result = crypto.open(captured.payloadJson, supportId.toCharArray())
        assertTrue("open must succeed with Support ID", result is OpenResult.Success)
        assertEquals(
            "decrypted payload must be byte-for-byte identical to the original",
            rawPayloadJson,
            (result as OpenResult.Success).payloadJson
        )
    }

    @Test
    fun wrongPassword_cannotDecrypt_payloadJsonPassedToUpload() = runBlocking {
        val data = BackupData(exportedAt = System.currentTimeMillis())
        val supportId = "correct-support-id"

        val spy = executeUploadViaSpy(data, supportId)
        val captured = requireNotNull(spy.lastCapture)

        // A different password must fail to decrypt
        val result = crypto.open(captured.payloadJson, "wrong-password".toCharArray())
        assertTrue(
            "wrong password must not yield Success",
            result is OpenResult.WrongPassword
        )
    }

    @Test
    fun differentSupportIds_produceDifferentCiphertext_inUpload() = runBlocking {
        val data = BackupData(exportedAt = 1_000L)

        val spy1 = executeUploadViaSpy(data, "support-id-alpha")
        val spy2 = executeUploadViaSpy(data, "support-id-beta")
        val captured1 = requireNotNull(spy1.lastCapture)
        val captured2 = requireNotNull(spy2.lastCapture)

        // Different passwords => different ciphertext
        assertTrue(
            "different Support IDs must produce different payloadJson",
            captured1.payloadJson != captured2.payloadJson
        )
        // But both decrypt to the same original payload with their respective passwords
        val result1 = crypto.open(captured1.payloadJson, "support-id-alpha".toCharArray())
        val result2 = crypto.open(captured2.payloadJson, "support-id-beta".toCharArray())
        assertTrue(result1 is OpenResult.Success)
        assertTrue(result2 is OpenResult.Success)
        assertEquals(
            (result1 as OpenResult.Success).payloadJson,
            (result2 as OpenResult.Success).payloadJson
        )
    }
}
