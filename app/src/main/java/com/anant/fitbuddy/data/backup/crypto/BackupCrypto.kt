package com.anant.fitbuddy.data.backup.crypto

import android.util.Base64
import com.anant.fitbuddy.data.backup.BackupErrorMessages
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * On-device backup encryption core (Requirement 5). Wraps a serialized `BackupData` payload JSON
 * in a versioned [BackupEnvelope] using AES-256-GCM with a PBKDF2-HMAC-SHA256 derived key.
 *
 * All key derivation and cipher work runs off the main thread on [Dispatchers.Default], mirroring
 * the existing image-processing pattern. Passwords are handled as [CharArray] and the local
 * password copy plus the derived key bytes are zeroed in a `finally` block so no recoverable copy
 * survives (Requirements 7.4, 7.5). The password is never written into the envelope (Requirement
 * 7.1) — only the ciphertext and non-secret KDF parameters are serialized.
 */
class BackupCrypto(private val moshi: Moshi) {

    private val envelopeAdapter by lazy { moshi.adapter(BackupEnvelope::class.java) }
    private val secureRandom by lazy { SecureRandom() }

    /**
     * Parses the small header of [raw] and maps it to exactly one [BackupFormat]
     * (Requirements 2.1, 9.3). Cheap: only inspects top-level JSON keys, never decrypts.
     */
    fun classify(raw: String): BackupFormat {
        val obj = parseJsonObject(raw) ?: return BackupFormat.UNKNOWN
        if (obj.has(MARKER_FIELD)) {
            val marker = obj.optInt(MARKER_FIELD, 0)
            if (marker < 1) return BackupFormat.UNKNOWN
            return when (obj.optString(ENC_FIELD)) {
                ENC_AES_GCM -> BackupFormat.ENCRYPTED
                ENC_NONE -> BackupFormat.PLAIN_WRAPPED
                else -> BackupFormat.UNKNOWN
            }
        }
        // Not an envelope: a pre-feature raw BackupData document has a top-level "version" field.
        if (obj.has(LEGACY_VERSION_FIELD)) return BackupFormat.LEGACY_PLAIN
        return BackupFormat.UNKNOWN
    }

    /**
     * Wraps [payloadJson]. A null/blank [password] returns the raw JSON unchanged (legacy plain
     * form, Requirements 1.3/9.1); a non-blank [password] derives a key and returns a serialized
     * AES-GCM [BackupEnvelope] (Requirements 1.2, 8.1).
     */
    suspend fun seal(payloadJson: String, password: CharArray?): String = withContext(Dispatchers.Default) {
        if (isNullOrBlank(password)) return@withContext payloadJson

        val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
        val iv = ByteArray(IV_BYTES).also { secureRandom.nextBytes(it) }
        val passwordCopy = password!!.copyOf()
        var keyBytes: ByteArray? = null
        try {
            keyBytes = try {
                deriveKey(passwordCopy, salt, DEFAULT_ITERATIONS)
            } catch (e: Exception) {
                throw IllegalStateException(BackupErrorMessages.KEY_DERIVATION_FAILED, e)
            }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.ENCRYPT_MODE,
                SecretKeySpec(keyBytes, KEY_ALGORITHM),
                GCMParameterSpec(GCM_TAG_BITS, iv)
            )
            val ciphertext = cipher.doFinal(payloadJson.toByteArray(Charsets.UTF_8))
            val envelope = BackupEnvelope(
                fitbuddyBackup = ENVELOPE_VERSION,
                enc = ENC_AES_GCM,
                kdf = KDF_ID,
                iterations = DEFAULT_ITERATIONS,
                salt = encode(salt),
                iv = encode(iv),
                ciphertext = encode(ciphertext)
            )
            envelopeAdapter.toJson(envelope)
        } finally {
            passwordCopy.fill('\u0000')
            keyBytes?.fill(0)
        }
    }

    /**
     * Opens any supported form. [BackupFormat.LEGACY_PLAIN] and [BackupFormat.PLAIN_WRAPPED] return
     * [OpenResult.Success] without needing a password; [BackupFormat.ENCRYPTED] uses [password] and
     * returns [OpenResult.WrongPassword] on a GCM auth-tag mismatch, [OpenResult.Corrupt] on a
     * malformed envelope, and [OpenResult.Unreadable] when the input is neither an envelope nor a
     * legacy payload (Requirements 5.2, 8.3, 10.5).
     */
    suspend fun open(raw: String, password: CharArray?): OpenResult = withContext(Dispatchers.Default) {
        when (classify(raw)) {
            BackupFormat.LEGACY_PLAIN -> OpenResult.Success(raw)
            BackupFormat.PLAIN_WRAPPED -> openPlainWrapped(raw)
            BackupFormat.ENCRYPTED -> openEncrypted(raw, password)
            BackupFormat.UNKNOWN -> OpenResult.Unreadable
        }
    }

    private fun openPlainWrapped(raw: String): OpenResult {
        val envelope = parseEnvelope(raw) ?: return OpenResult.Corrupt
        val ciphertext = envelope.ciphertext
        return try {
            OpenResult.Success(String(decode(ciphertext), Charsets.UTF_8))
        } catch (_: IllegalArgumentException) {
            OpenResult.Corrupt
        }
    }

    private fun openEncrypted(raw: String, password: CharArray?): OpenResult {
        if (isNullOrBlank(password)) return OpenResult.WrongPassword
        val envelope = parseEnvelope(raw) ?: return OpenResult.Corrupt
        val salt = envelope.salt
        val iv = envelope.iv
        val iterations = envelope.iterations
        if (salt == null || iv == null || iterations == null || iterations < 1) return OpenResult.Corrupt

        val saltBytes: ByteArray
        val ivBytes: ByteArray
        val cipherBytes: ByteArray
        try {
            saltBytes = decode(salt)
            ivBytes = decode(iv)
            cipherBytes = decode(envelope.ciphertext)
        } catch (_: IllegalArgumentException) {
            return OpenResult.Corrupt
        }
        if (ivBytes.size != IV_BYTES || saltBytes.isEmpty() || cipherBytes.isEmpty()) return OpenResult.Corrupt

        val passwordCopy = password!!.copyOf()
        var keyBytes: ByteArray? = null
        return try {
            keyBytes = deriveKey(passwordCopy, saltBytes, iterations)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(keyBytes, KEY_ALGORITHM),
                GCMParameterSpec(GCM_TAG_BITS, ivBytes)
            )
            val plaintext = cipher.doFinal(cipherBytes)
            OpenResult.Success(String(plaintext, Charsets.UTF_8))
        } catch (_: AEADBadTagException) {
            OpenResult.WrongPassword
        } catch (_: Exception) {
            // Any other cipher/derivation failure on a structurally valid envelope is treated as a
            // wrong-password/integrity failure rather than a crash (Requirement 10.5).
            OpenResult.WrongPassword
        } finally {
            passwordCopy.fill('\u0000')
            keyBytes?.fill(0)
        }
    }

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance(KDF_ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun parseEnvelope(raw: String): BackupEnvelope? = try {
        envelopeAdapter.fromJson(raw)
    } catch (_: Exception) {
        null
    }

    private fun parseJsonObject(raw: String): JSONObject? {
        val trimmed = raw.trimStart()
        if (trimmed.isEmpty() || trimmed[0] != '{') return null
        return try {
            JSONObject(raw)
        } catch (_: Exception) {
            null
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    private fun isNullOrBlank(password: CharArray?): Boolean =
        password == null || password.isEmpty() || password.all { it.isWhitespace() }

    companion object {
        const val DEFAULT_ITERATIONS = 120_000
        private const val ENVELOPE_VERSION = 1
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val KEY_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ALGORITHM = "AES"
        private const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
        private const val KDF_ID = "PBKDF2-HMAC-SHA256"
        private const val ENC_AES_GCM = "AES-GCM"
        private const val ENC_NONE = "none"
        private const val MARKER_FIELD = "fitbuddyBackup"
        private const val ENC_FIELD = "enc"
        private const val LEGACY_VERSION_FIELD = "version"
    }
}
