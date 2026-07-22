package com.anant.fitbuddy.data.backup.crypto

import android.util.Base64
import com.squareup.moshi.Moshi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Random

/**
 * Correctness-property tests for [BackupCrypto], the on-device backup encryption core.
 *
 * These exercise the invariants defined in the design document's "Correctness Properties"
 * section against the real platform crypto/Base64 stack (via Robolectric):
 *
 * - Property 1 — round-trip fidelity: `open(seal(x, p), p) == x` byte-for-byte.
 * - Property 2 — password isolation: the serialized envelope never contains the password.
 * - Property 3 — tamper evidence: any mutation of salt/iv/ciphertext yields Corrupt/WrongPassword.
 * - Property 4 — deterministic classification: encrypted / legacy / garbage separate cleanly.
 * - Property 8-adjacent — a blank password leaves the payload as LEGACY_PLAIN and opens with no
 *   password.
 *
 * PBKDF2 at the production 120k iteration count is deliberately expensive per call, so every
 * property is checked over a small, representative set of examples (a handful of payloads and the
 * password lengths 1, 8, 64, 128, 256) plus a short seeded fuzz, keeping the whole class fast.
 */
@RunWith(RobolectricTestRunner::class)
class BackupCryptoTest {

    private val moshi: Moshi = Moshi.Builder().build()
    private val crypto = BackupCrypto(moshi)
    private val envelopeAdapter = moshi.adapter(BackupEnvelope::class.java)

    // A legacy (pre-feature) BackupData document: a top-level JSON object carrying a "version" field.
    private val legacyPayload = """{"version":6,"exportedAt":123,"foodLogs":[],"mealFoods":[]}"""

    // Representative payload JSON strings covering ascii, unicode, quotes/escapes, empty and a
    // moderately large blob (kept to ~2 KB so AES stays negligible and the class runs fast).
    private val representativePayloads = listOf(
        legacyPayload,
        "{}",
        """{"note":"quotes \" and \\ backslashes and \n newlines"}""",
        """{"emoji":"🏋️‍♀️🍚","hindi":"नमस्ते","name":"Ünïcödé"}""",
        """{"blob":"${"x".repeat(2_000)}"}"""
    )

    // The representative password lengths called for by the task (spanning the 1–256 range).
    private val passwordLengths = listOf(1, 8, 64, 128, 256)

    // ---------------------------------------------------------------------------------------------
    // Property 1: round-trip fidelity
    // Validates: Requirements 8.1
    // ---------------------------------------------------------------------------------------------

    @Test
    fun property1_roundTrip_representativePayloadsAndPasswordLengths() = runBlocking {
        // Pair each password length with a representative payload so both dimensions are covered
        // with only a handful of (expensive) PBKDF2 derivations.
        passwordLengths.forEachIndexed { index, len ->
            val payload = representativePayloads[index % representativePayloads.size]
            val password = randomPassword(len, Random(len.toLong() * 31 + payload.length))
            val sealed = crypto.seal(payload, password.copyOf())
            assertEquals(
                "sealed form should classify as ENCRYPTED (len=$len)",
                BackupFormat.ENCRYPTED,
                crypto.classify(sealed)
            )
            val opened = crypto.open(sealed, password.copyOf())
            assertTrue(
                "open should succeed for len=$len, payloadLen=${payload.length} but was $opened",
                opened is OpenResult.Success
            )
            assertEquals(
                "round-trip must be byte-for-byte identical (len=$len, payloadLen=${payload.length})",
                payload,
                (opened as OpenResult.Success).payloadJson
            )
        }
    }

    @Test
    fun property1_roundTrip_seededFuzz() = runBlocking {
        val rng = Random(0xF17B0DDL)
        repeat(5) { i ->
            val payload = randomPayloadJson(rng)
            val len = 1 + rng.nextInt(256) // 1..256
            val password = randomPassword(len, rng)
            val sealed = crypto.seal(payload, password.copyOf())
            val opened = crypto.open(sealed, password.copyOf())
            assertTrue("iteration $i: expected Success but was $opened", opened is OpenResult.Success)
            assertEquals("iteration $i: round-trip mismatch", payload, (opened as OpenResult.Success).payloadJson)
        }
    }

    @Test
    fun property1_reEncryptSamePayload_producesDistinctCiphertextButSamePlaintext() = runBlocking {
        val password = "correct horse battery".toCharArray()
        val a = crypto.seal(legacyPayload, password.copyOf())
        val b = crypto.seal(legacyPayload, password.copyOf())
        // Fresh salt+iv per backup => different envelopes for identical input.
        assertNotEquals("re-sealing the same payload must not be deterministic", a, b)
        assertEquals(legacyPayload, (crypto.open(a, password.copyOf()) as OpenResult.Success).payloadJson)
        assertEquals(legacyPayload, (crypto.open(b, password.copyOf()) as OpenResult.Success).payloadJson)
    }

    // ---------------------------------------------------------------------------------------------
    // Blank password (Property 8-adjacent): legacy plain form, opens with no password
    // Validates: Requirements 7.1, 9.3
    // ---------------------------------------------------------------------------------------------

    @Test
    fun blankPassword_leavesPayloadUnchangedAndClassifiesLegacyPlain() = runBlocking {
        val blanks = listOf<CharArray?>(null, CharArray(0), "   ".toCharArray(), "\t\n ".toCharArray())
        for (blank in blanks) {
            val sealed = crypto.seal(legacyPayload, blank?.copyOf())
            assertEquals("blank password must return the raw JSON unchanged", legacyPayload, sealed)
            assertEquals(BackupFormat.LEGACY_PLAIN, crypto.classify(sealed))
        }
    }

    @Test
    fun blankPassword_opensWithoutPassword() = runBlocking {
        val sealed = crypto.seal(legacyPayload, null)
        val opened = crypto.open(sealed, null)
        assertTrue(opened is OpenResult.Success)
        assertEquals(legacyPayload, (opened as OpenResult.Success).payloadJson)
    }

    // ---------------------------------------------------------------------------------------------
    // Property 3: tamper evidence
    // Validates: Requirements 8.3, 8.4, 10.5
    // ---------------------------------------------------------------------------------------------

    @Test
    fun property3_wrongPassword_returnsWrongPassword() = runBlocking {
        val sealed = crypto.seal(legacyPayload, "the-right-password".toCharArray())
        val opened = crypto.open(sealed, "the-wrong-password".toCharArray())
        assertEquals(OpenResult.WrongPassword, opened)
    }

    @Test
    fun property3_flippedByte_inSaltIvOrCiphertext_neverSucceeds() = runBlocking {
        val password = "tamper-test-password"
        // Seal once and reuse for all three fields to keep PBKDF2 derivations minimal.
        val sealed = crypto.seal(legacyPayload, password.toCharArray())
        for (field in listOf("salt", "iv", "ciphertext")) {
            val mutated = mutateEnvelopeField(sealed, field) { bytes ->
                val copy = bytes.copyOf()
                copy[0] = (copy[0].toInt() xor 0x01).toByte() // flip the lowest bit of the first byte
                copy
            }
            val opened = crypto.open(mutated, password.toCharArray())
            assertFalse("flipping a byte of $field must never yield Success: $opened", opened is OpenResult.Success)
            assertTrue(
                "flipping a byte of $field must be Corrupt or WrongPassword: $opened",
                opened is OpenResult.Corrupt || opened is OpenResult.WrongPassword
            )
        }
    }

    @Test
    fun property3_truncatedByte_inSaltIvOrCiphertext_neverSucceeds() = runBlocking {
        val password = "tamper-test-password"
        val sealed = crypto.seal(legacyPayload, password.toCharArray())
        for (field in listOf("salt", "iv", "ciphertext")) {
            val mutated = mutateEnvelopeField(sealed, field) { bytes ->
                bytes.copyOf(bytes.size - 1) // drop the last byte
            }
            val opened = crypto.open(mutated, password.toCharArray())
            assertFalse("truncating $field must never yield Success: $opened", opened is OpenResult.Success)
            assertTrue(
                "truncating $field must be Corrupt or WrongPassword: $opened",
                opened is OpenResult.Corrupt || opened is OpenResult.WrongPassword
            )
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Property 2: password isolation
    // Validates: Requirements 6.3, 6.4, 7.1
    // ---------------------------------------------------------------------------------------------

    @Test
    fun property2_serializedEnvelope_containsNoPasswordSubstring() = runBlocking {
        val rng = Random(0xBADC0DEL)
        // A short sweep across distinctive password lengths. Lengths >= 16 are used deliberately:
        // the substring check is only a meaningful leak-detector when an accidental contiguous
        // collision with the random base64 envelope is astronomically unlikely. A 1–2 char password
        // would collide with the base64 alphabet by chance, which says nothing about a real leak
        // (round-trip coverage of lengths 1–256 lives in property1_*).
        for (len in listOf(16, 64, 128, 256)) {
            val payload = randomPayloadJson(rng)
            val password = randomPassword(len, rng)
            val pwString = String(password)
            val sealed = crypto.seal(payload, password.copyOf())
            assertFalse(
                "len=$len: envelope must not contain the password substring",
                sealed.contains(pwString)
            )
        }
    }

    @Test
    fun property2_distinctivePassword_absentFromEnvelope() = runBlocking {
        val password = "SUPER-SECRET-MARKER-1234567890"
        // Embed a distinctive token inside the payload too, to prove we only search for the password.
        val payload = """{"version":1,"note":"contains SUPER-SECRET-MARKER but that is data"}"""
        val sealed = crypto.seal(payload, password.toCharArray())
        assertFalse("password must not leak into the serialized envelope", sealed.contains(password))
    }

    // ---------------------------------------------------------------------------------------------
    // Property 4: deterministic classification
    // Validates: Requirements 9.3
    // ---------------------------------------------------------------------------------------------

    @Test
    fun property4_classify_separatesEncryptedLegacyPlainWrappedAndUnknown() = runBlocking {
        // Encrypted envelope.
        val encrypted = crypto.seal(legacyPayload, "a-password".toCharArray())
        assertEquals(BackupFormat.ENCRYPTED, crypto.classify(encrypted))

        // Legacy plain BackupData document.
        assertEquals(BackupFormat.LEGACY_PLAIN, crypto.classify(legacyPayload))

        // Plain-wrapped envelope (enc == "none").
        val plainWrapped = envelopeAdapter.toJson(
            BackupEnvelope(
                fitbuddyBackup = 1,
                enc = "none",
                ciphertext = Base64.encodeToString(legacyPayload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            )
        )
        assertEquals(BackupFormat.PLAIN_WRAPPED, crypto.classify(plainWrapped))

        // Garbage / unrecognized inputs.
        val garbage = listOf(
            "",
            "   ",
            "not json at all",
            "[]",
            "[1,2,3]",
            "{}",
            """{"foo":"bar"}""",              // object without version marker or envelope marker
            """{"fitbuddyBackup":0,"enc":"AES-GCM","ciphertext":"x"}""", // marker < 1
            """{"fitbuddyBackup":1,"enc":"ROT13","ciphertext":"x"}""",   // unknown enc
            "\u0000\u0001\u0002 binary"
        )
        for (g in garbage) {
            assertEquals("expected UNKNOWN for '$g'", BackupFormat.UNKNOWN, crypto.classify(g))
        }
    }

    @Test
    fun property4_differentMarkers_areNeverClassifiedIdentically() = runBlocking {
        val encrypted = crypto.classify(crypto.seal(legacyPayload, "pw-123456".toCharArray()))
        val legacy = crypto.classify(legacyPayload)
        val unknown = crypto.classify("garbage")
        // The three distinct marker categories must resolve to three distinct classifications.
        assertNotEquals(encrypted, legacy)
        assertNotEquals(encrypted, unknown)
        assertNotEquals(legacy, unknown)
    }

    @Test
    fun open_onUnknownInput_returnsUnreadable() = runBlocking {
        assertEquals(OpenResult.Unreadable, crypto.open("not a backup", "pw".toCharArray()))
        assertEquals(OpenResult.Unreadable, crypto.open("{}", null))
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    /**
     * Decodes the base64 [field] of a serialized envelope, applies [transform] to the raw bytes,
     * re-encodes it, and returns the re-serialized envelope JSON.
     */
    private fun mutateEnvelopeField(
        sealed: String,
        field: String,
        transform: (ByteArray) -> ByteArray
    ): String {
        val envelope = requireNotNull(envelopeAdapter.fromJson(sealed))
        val original = when (field) {
            "salt" -> requireNotNull(envelope.salt)
            "iv" -> requireNotNull(envelope.iv)
            "ciphertext" -> envelope.ciphertext
            else -> error("unknown field $field")
        }
        val mutatedB64 = Base64.encodeToString(transform(Base64.decode(original, Base64.NO_WRAP)), Base64.NO_WRAP)
        val mutated = when (field) {
            "salt" -> envelope.copy(salt = mutatedB64)
            "iv" -> envelope.copy(iv = mutatedB64)
            "ciphertext" -> envelope.copy(ciphertext = mutatedB64)
            else -> error("unknown field $field")
        }
        return envelopeAdapter.toJson(mutated)
    }

    /** Random non-blank password of exactly [len] chars drawn from printable ascii + some unicode. */
    private fun randomPassword(len: Int, rng: Random): CharArray {
        require(len >= 1)
        val alphabet = ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" +
            "!@#\$%^&*()_+-=[]{}|;:,.<>?/~ éñ🍚ॐ").toCharArray()
        val out = CharArray(len)
        for (i in 0 until len) out[i] = alphabet[rng.nextInt(alphabet.size)]
        // Guarantee at least one non-whitespace char so seal actually encrypts.
        if (out.all { it.isWhitespace() }) out[0] = 'x'
        return out
    }

    /** Builds a small random but valid JSON object string to use as a payload. */
    private fun randomPayloadJson(rng: Random): String {
        val fields = 1 + rng.nextInt(5)
        val sb = StringBuilder("{\"version\":${rng.nextInt(10)}")
        for (i in 0 until fields) {
            val value = buildString {
                repeat(rng.nextInt(40)) { append(('a' + rng.nextInt(26))) }
            }
            sb.append(",\"k$i\":\"$value\"")
        }
        sb.append("}")
        return sb.toString()
    }
}
