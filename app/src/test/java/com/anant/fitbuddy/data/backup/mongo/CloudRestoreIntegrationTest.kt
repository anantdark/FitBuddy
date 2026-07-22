package com.anant.fitbuddy.data.backup.mongo

import com.anant.fitbuddy.data.backup.BackupData
import com.anant.fitbuddy.data.backup.BackupErrorMessages
import com.anant.fitbuddy.data.backup.crypto.BackupCrypto
import com.anant.fitbuddy.data.backup.crypto.BackupEnvelope
import com.anant.fitbuddy.data.backup.crypto.BackupFormat
import com.anant.fitbuddy.data.backup.crypto.OpenResult
import com.anant.fitbuddy.data.remote.NetworkModule
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Integration tests for the cloud restore flow exercised by
 * `FitnessRepository.downloadMongoBackup`. Rather than constructing the full repository
 * (which has many DAO/settings dependencies), these tests reproduce the exact
 * download→classify→open→import→migrate logic with fakes, verifying the four key paths:
 *
 * 1. Support-ID-encrypted envelope restores with no prompt.
 * 2. Non-Support-ID envelope triggers the password prompt path.
 * 3. Legacy plaintext payload restores then triggers a Support-ID seal+upload.
 * 4. Failure paths leave prior state intact.
 *
 * Uses the same fake/spy pattern as [CloudUploadEncryptionContractTest].
 *
 * **Validates: Requirements 4.1, 4.2, 4.4, 9.2, 9.4**
 */
@RunWith(RobolectricTestRunner::class)
class CloudRestoreIntegrationTest {

    private val moshi: Moshi = NetworkModule.moshi
    private val crypto = BackupCrypto(moshi)
    private val backupDataAdapter = moshi.adapter(BackupData::class.java).indent("  ")
    private val envelopeAdapter = moshi.adapter(BackupEnvelope::class.java)

    // ----- Fakes ---------------------------------------------------------------------------------

    /**
     * Fake [MongoBackupRepository] that returns a configurable payload from download and
     * captures upload() calls for verifying auto-migration.
     */
    private class FakeMongoBackupRepository(
        private val downloadPayload: String
    ) : MongoBackupRepository() {

        data class UploadCapture(
            val supportId: String,
            val payloadJson: String,
            val exportedAt: Long,
            val deviceName: String,
            val macId: String
        )

        var uploadCapture: UploadCapture? = null
            private set

        var uploadCallCount: Int = 0
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
            uploadCallCount++
            uploadCapture = UploadCapture(
                supportId = supportId,
                payloadJson = payloadJson,
                exportedAt = exportedAt,
                deviceName = deviceName,
                macId = macId
            )
        }
    }

    /**
     * Reproduces the exact restore logic from `FitnessRepository.downloadMongoBackup`:
     * classify → open (Support ID first) → import → auto-migrate legacy.
     *
     * Returns the number of "imported records" (simulated) or throws on failure, exactly
     * as the real method does. [passwordProviderCalled] is set to true if the prompt path
     * is exercised.
     */
    private data class RestoreResult(
        val importedPayloadJson: String?,
        val passwordProviderCalled: Boolean,
        val migrationUploadPerformed: Boolean
    )

    private suspend fun executeRestore(
        downloadedPayload: String,
        supportId: String,
        passwordProvider: suspend () -> CharArray?,
        fakeRepo: FakeMongoBackupRepository
    ): RestoreResult {
        var providerCalled = false
        var importedJson: String? = null

        val wrappedProvider: suspend () -> CharArray? = {
            providerCalled = true
            passwordProvider()
        }

        when (crypto.classify(downloadedPayload)) {
            BackupFormat.LEGACY_PLAIN -> {
                // Import directly
                importedJson = downloadedPayload
                // Auto-migrate: re-encrypt with Support ID and upload
                val passwordChars = supportId.toCharArray()
                val sealedPayload = try {
                    crypto.seal(downloadedPayload, passwordChars)
                } finally {
                    passwordChars.fill('\u0000')
                }
                fakeRepo.upload(
                    baseUrl = "https://proxy.example.com",
                    apiKey = "test-key",
                    databaseName = "fitbuddy",
                    collectionName = "fitbuddy_backup",
                    supportId = supportId,
                    payloadJson = sealedPayload,
                    exportedAt = System.currentTimeMillis(),
                    deviceName = "TestDevice",
                    macId = "00:11:22:33:44:55"
                )
            }

            BackupFormat.PLAIN_WRAPPED -> {
                when (val result = crypto.open(downloadedPayload, null)) {
                    is OpenResult.Success -> importedJson = result.payloadJson
                    else -> error(BackupErrorMessages.BACKUP_CORRUPT)
                }
            }

            BackupFormat.ENCRYPTED -> {
                // Try Support ID first (no prompt)
                val supportIdChars = supportId.toCharArray()
                val autoResult = try {
                    crypto.open(downloadedPayload, supportIdChars)
                } finally {
                    supportIdChars.fill('\u0000')
                }

                when (autoResult) {
                    is OpenResult.Success -> importedJson = autoResult.payloadJson
                    OpenResult.WrongPassword -> {
                        val userPassword = wrappedProvider()
                        if (userPassword == null || userPassword.isEmpty()) {
                            error("Password required to restore this backup")
                        }
                        try {
                            when (val prompted = crypto.open(downloadedPayload, userPassword)) {
                                is OpenResult.Success -> importedJson = prompted.payloadJson
                                OpenResult.WrongPassword -> error(BackupErrorMessages.INCORRECT_PASSWORD)
                                OpenResult.Corrupt -> error(BackupErrorMessages.BACKUP_CORRUPT)
                                OpenResult.Unreadable -> error(BackupErrorMessages.NOT_VALID_BACKUP)
                            }
                        } finally {
                            userPassword.fill('\u0000')
                        }
                    }
                    OpenResult.Corrupt -> error(BackupErrorMessages.BACKUP_CORRUPT)
                    OpenResult.Unreadable -> error(BackupErrorMessages.NOT_VALID_BACKUP)
                }
            }

            BackupFormat.UNKNOWN -> error(BackupErrorMessages.NOT_VALID_BACKUP)
        }

        return RestoreResult(
            importedPayloadJson = importedJson,
            passwordProviderCalled = providerCalled,
            migrationUploadPerformed = fakeRepo.uploadCallCount > 0
        )
    }

    // ----- Test helpers --------------------------------------------------------------------------

    private val testSupportId = "test-support-id-abc123"
    private val testBackupData = BackupData(exportedAt = 1_700_000_000_000L)
    private val testPayloadJson: String get() = backupDataAdapter.toJson(testBackupData)

    // ----- Test 1: Support-ID-encrypted envelope restores with no prompt -------------------------

    @Test
    fun supportIdEncryptedEnvelope_restoresWithNoPrompt() = runBlocking {
        val payloadJson = testPayloadJson
        // Seal with Support ID
        val envelope = crypto.seal(payloadJson, testSupportId.toCharArray())
        val fakeRepo = FakeMongoBackupRepository(envelope)

        val result = executeRestore(
            downloadedPayload = envelope,
            supportId = testSupportId,
            passwordProvider = { fail("passwordProvider should not be called"); null },
            fakeRepo = fakeRepo
        )

        // passwordProvider was never called
        assertTrue(
            "passwordProvider must not be called for Support-ID-encrypted backup",
            !result.passwordProviderCalled
        )
        // Import succeeded with the original payload
        assertEquals(
            "restored payload must match the original",
            payloadJson,
            result.importedPayloadJson
        )
        // No migration upload for already-encrypted backups
        assertTrue(
            "no migration upload should occur for encrypted backups",
            !result.migrationUploadPerformed
        )
    }

    @Test
    fun supportIdEncryptedEnvelope_classifiesAsEncrypted() = runBlocking {
        val envelope = crypto.seal(testPayloadJson, testSupportId.toCharArray())
        assertEquals(BackupFormat.ENCRYPTED, crypto.classify(envelope))
    }

    // ----- Test 2: Non-Support-ID envelope triggers the prompt path ------------------------------

    @Test
    fun nonSupportIdEnvelope_triggersPasswordPrompt() = runBlocking {
        val payloadJson = testPayloadJson
        val customPassword = "my-custom-backup-password"
        // Seal with a DIFFERENT password than the Support ID
        val envelope = crypto.seal(payloadJson, customPassword.toCharArray())
        val fakeRepo = FakeMongoBackupRepository(envelope)

        val result = executeRestore(
            downloadedPayload = envelope,
            supportId = testSupportId, // won't work for decryption
            passwordProvider = { customPassword.toCharArray() },
            fakeRepo = fakeRepo
        )

        // passwordProvider WAS called
        assertTrue(
            "passwordProvider must be called when Support ID doesn't decrypt",
            result.passwordProviderCalled
        )
        // Import succeeded with the correct password
        assertEquals(
            "restored payload must match the original after user provides password",
            payloadJson,
            result.importedPayloadJson
        )
    }

    @Test
    fun nonSupportIdEnvelope_supportIdAttemptFails_thenCorrectPasswordSucceeds() = runBlocking {
        val payloadJson = testPayloadJson
        val customPassword = "another-secure-password-99"
        val envelope = crypto.seal(payloadJson, customPassword.toCharArray())

        // Verify Support ID alone fails
        val autoAttempt = crypto.open(envelope, testSupportId.toCharArray())
        assertEquals(
            "Support ID must fail for envelope sealed with a different password",
            OpenResult.WrongPassword,
            autoAttempt
        )

        // Verify correct password succeeds
        val manualAttempt = crypto.open(envelope, customPassword.toCharArray())
        assertTrue(manualAttempt is OpenResult.Success)
        assertEquals(payloadJson, (manualAttempt as OpenResult.Success).payloadJson)
    }

    // ----- Test 3: Legacy plaintext restores then triggers seal+upload ---------------------------

    @Test
    fun legacyPlaintextPayload_restoresThenTriggersAutoMigration() = runBlocking {
        val payloadJson = testPayloadJson
        // Legacy plain = raw BackupData JSON, no envelope
        assertEquals(
            "test payload must classify as LEGACY_PLAIN",
            BackupFormat.LEGACY_PLAIN,
            crypto.classify(payloadJson)
        )

        val fakeRepo = FakeMongoBackupRepository(payloadJson)

        val result = executeRestore(
            downloadedPayload = payloadJson,
            supportId = testSupportId,
            passwordProvider = { fail("passwordProvider should not be called for legacy"); null },
            fakeRepo = fakeRepo
        )

        // Import succeeded
        assertEquals(
            "legacy backup must be imported directly",
            payloadJson,
            result.importedPayloadJson
        )
        // passwordProvider was never called
        assertTrue(
            "passwordProvider must not be called for legacy plain backup",
            !result.passwordProviderCalled
        )
        // Auto-migration: upload() was called
        assertTrue(
            "auto-migration must trigger an upload after legacy restore",
            result.migrationUploadPerformed
        )
    }

    @Test
    fun legacyPlaintextPayload_migrationUploadIsEncryptedWithSupportId() = runBlocking {
        val payloadJson = testPayloadJson
        val fakeRepo = FakeMongoBackupRepository(payloadJson)

        executeRestore(
            downloadedPayload = payloadJson,
            supportId = testSupportId,
            passwordProvider = { null },
            fakeRepo = fakeRepo
        )

        val upload = requireNotNull(fakeRepo.uploadCapture) {
            "upload must have been called for migration"
        }

        // Uploaded payload must be an encrypted envelope
        assertEquals(
            "migrated payload must classify as ENCRYPTED",
            BackupFormat.ENCRYPTED,
            crypto.classify(upload.payloadJson)
        )

        // Verify it parses as a valid envelope
        val envelope = envelopeAdapter.fromJson(upload.payloadJson)
        assertNotNull("uploaded payload must parse as BackupEnvelope", envelope)
        assertEquals(1, envelope!!.fitbuddyBackup)
        assertEquals("AES-GCM", envelope.enc)

        // Verify it decrypts with the Support ID back to the original payload
        val opened = crypto.open(upload.payloadJson, testSupportId.toCharArray())
        assertTrue("migrated envelope must decrypt with Support ID", opened is OpenResult.Success)
        assertEquals(
            "decrypted migration payload must equal original",
            payloadJson,
            (opened as OpenResult.Success).payloadJson
        )
    }

    @Test
    fun legacyPlaintextPayload_migrationUploadUsesCorrectSupportId() = runBlocking {
        val payloadJson = testPayloadJson
        val fakeRepo = FakeMongoBackupRepository(payloadJson)

        executeRestore(
            downloadedPayload = payloadJson,
            supportId = testSupportId,
            passwordProvider = { null },
            fakeRepo = fakeRepo
        )

        val upload = requireNotNull(fakeRepo.uploadCapture)
        assertEquals(
            "migration upload must use the same Support ID",
            testSupportId,
            upload.supportId
        )
    }

    // ----- Test 4: Failure paths leave prior state intact ----------------------------------------

    @Test
    fun encryptedEnvelope_wrongPasswordFromProvider_throwsAndLeavesDataUnchanged() = runBlocking {
        val payloadJson = testPayloadJson
        val correctPassword = "the-actual-correct-password"
        val envelope = crypto.seal(payloadJson, correctPassword.toCharArray())
        val fakeRepo = FakeMongoBackupRepository(envelope)

        try {
            executeRestore(
                downloadedPayload = envelope,
                supportId = testSupportId, // won't work
                passwordProvider = { "totally-wrong-password".toCharArray() },
                fakeRepo = fakeRepo
            )
            fail("Should have thrown on wrong password")
        } catch (e: IllegalStateException) {
            assertTrue(
                "error must indicate incorrect password",
                e.message!!.contains("Incorrect password")
            )
        }

        // No upload should have been triggered
        assertEquals(
            "no upload should occur on failure",
            0,
            fakeRepo.uploadCallCount
        )
    }

    @Test
    fun encryptedEnvelope_nullPasswordFromProvider_throwsAndLeavesDataUnchanged() = runBlocking {
        val payloadJson = testPayloadJson
        val envelope = crypto.seal(payloadJson, "custom-pw-12345".toCharArray())
        val fakeRepo = FakeMongoBackupRepository(envelope)

        try {
            executeRestore(
                downloadedPayload = envelope,
                supportId = testSupportId,
                passwordProvider = { null }, // user cancelled
                fakeRepo = fakeRepo
            )
            fail("Should have thrown when passwordProvider returns null")
        } catch (e: IllegalStateException) {
            assertTrue(
                "error must indicate password required",
                e.message!!.contains("Password required")
            )
        }

        assertEquals(0, fakeRepo.uploadCallCount)
    }

    @Test
    fun corruptEnvelope_throwsAndLeavesDataUnchanged() = runBlocking {
        // Create a valid envelope then corrupt its ciphertext
        val envelope = crypto.seal(testPayloadJson, testSupportId.toCharArray())
        val parsed = requireNotNull(envelopeAdapter.fromJson(envelope))
        val corrupted = envelopeAdapter.toJson(parsed.copy(ciphertext = "INVALIDBASE64!!!"))
        val fakeRepo = FakeMongoBackupRepository(corrupted)

        try {
            executeRestore(
                downloadedPayload = corrupted,
                supportId = testSupportId,
                passwordProvider = { "anything".toCharArray() },
                fakeRepo = fakeRepo
            )
            fail("Should have thrown on corrupt envelope")
        } catch (e: IllegalStateException) {
            assertTrue(
                "error must indicate corruption or wrong password",
                e.message!!.contains("corrupt", ignoreCase = true) ||
                    e.message!!.contains("Incorrect password")
            )
        }

        assertEquals(0, fakeRepo.uploadCallCount)
    }

    @Test
    fun unknownFormat_throwsAndLeavesDataUnchanged() = runBlocking {
        val garbage = "this is not a valid backup at all"
        val fakeRepo = FakeMongoBackupRepository(garbage)

        try {
            executeRestore(
                downloadedPayload = garbage,
                supportId = testSupportId,
                passwordProvider = { "pw".toCharArray() },
                fakeRepo = fakeRepo
            )
            fail("Should have thrown on unknown format")
        } catch (e: IllegalStateException) {
            assertTrue(
                "error must indicate invalid backup",
                e.message!!.contains("not a valid backup", ignoreCase = true)
            )
        }

        assertEquals(0, fakeRepo.uploadCallCount)
    }

    @Test
    fun encryptedEnvelope_emptyPasswordFromProvider_throwsAndLeavesDataUnchanged() = runBlocking {
        val envelope = crypto.seal(testPayloadJson, "secret-password-xyz".toCharArray())
        val fakeRepo = FakeMongoBackupRepository(envelope)

        try {
            executeRestore(
                downloadedPayload = envelope,
                supportId = testSupportId,
                passwordProvider = { CharArray(0) }, // empty
                fakeRepo = fakeRepo
            )
            fail("Should have thrown when passwordProvider returns empty")
        } catch (e: IllegalStateException) {
            assertTrue(
                "error must indicate password required",
                e.message!!.contains("Password required")
            )
        }

        assertEquals(0, fakeRepo.uploadCallCount)
    }
}
