package com.anant.fitbuddy.data.backup.crypto

import com.squareup.moshi.JsonClass

/**
 * Versioned, self-describing container that wraps a serialized `BackupData` payload JSON.
 *
 * The envelope is itself Moshi-serialized JSON, so it flows through the existing string-based
 * local file I/O and the proxy `payloadJson` field unchanged. When [enc] is `"AES-GCM"` the
 * payload lives only inside [ciphertext] and the KDF parameters ([kdf], [iterations], [salt],
 * [iv]) are present. When [enc] is `"none"` the container is uniform but unencrypted and the
 * KDF parameters are omitted. The password is never stored in the envelope.
 */
@JsonClass(generateAdapter = true)
data class BackupEnvelope(
    /** Envelope format marker + version. Present (>= 1) on every FitBuddy envelope. */
    val fitbuddyBackup: Int,
    /** `"AES-GCM"` for encrypted payloads, `"none"` for unencrypted-but-wrapped payloads. */
    val enc: String,
    /** Key-derivation function id, e.g. `"PBKDF2-HMAC-SHA256"`. Omitted when [enc] == `"none"`. */
    val kdf: String? = null,
    /** KDF cost (iteration count). Omitted when [enc] == `"none"`. */
    val iterations: Int? = null,
    /** Per-backup random salt, base64-encoded. Omitted when [enc] == `"none"`. */
    val salt: String? = null,
    /** Per-backup random 12-byte GCM nonce, base64-encoded. Omitted when [enc] == `"none"`. */
    val iv: String? = null,
    /**
     * Base64-encoded payload. For [enc] == `"AES-GCM"` this is `AES-GCM(payloadJson)` including the
     * auth tag; for [enc] == `"none"` this is `base64(payloadJson)` so the container is uniform.
     */
    val ciphertext: String
)

/**
 * Result of classifying raw backup bytes into exactly one recognized category.
 *
 * - [ENCRYPTED] — parses as an envelope with `fitbuddyBackup >= 1` and `enc == "AES-GCM"`.
 * - [PLAIN_WRAPPED] — parses as an envelope with `enc == "none"`.
 * - [LEGACY_PLAIN] — a pre-feature raw `BackupData` document (not an envelope).
 * - [UNKNOWN] — neither; triggers the unrecognized/corrupt error path.
 */
enum class BackupFormat {
    ENCRYPTED,
    PLAIN_WRAPPED,
    LEGACY_PLAIN,
    UNKNOWN
}

/**
 * Outcome of opening (decrypting/unwrapping) raw backup bytes.
 *
 * [Success] carries the recovered plaintext payload JSON. The failure variants distinguish an
 * incorrect password ([WrongPassword], GCM auth-tag mismatch) from a malformed envelope
 * ([Corrupt], invalid IV/salt/ciphertext) and from bytes that are neither an envelope nor a
 * legacy payload ([Unreadable]).
 */
sealed interface OpenResult {
    data class Success(val payloadJson: String) : OpenResult
    data object WrongPassword : OpenResult
    data object Corrupt : OpenResult
    data object Unreadable : OpenResult
}
