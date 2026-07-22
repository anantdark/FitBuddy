package com.anant.fitbuddy.data.backup

/**
 * User-facing error messages for backup operations. Each constant maps to a distinct failure mode
 * so users always know what went wrong (Requirements 10.1–10.5, 5.4).
 */
object BackupErrorMessages {

    // --- Encryption / seal failures (10.1, 5.4) ---

    /** Wraps a raw seal/encryption exception into a user-facing message (Requirement 10.1). */
    fun encryptionFailed(reason: String?): String =
        "Backup could not be encrypted" + if (!reason.isNullOrBlank()) ": $reason" else ""

    /** Key derivation specifically failed (Requirement 5.4). */
    const val KEY_DERIVATION_FAILED = "Backup could not be encrypted: key derivation failed"

    // --- Decryption / open failures (10.3, 10.4, 10.5) ---

    /** Password was incorrect (Requirement 10.4). */
    const val INCORRECT_PASSWORD = "Incorrect password"

    /** Envelope exists but is corrupted or truncated — distinct from wrong password (Req 10.5). */
    const val BACKUP_CORRUPT = "Backup is corrupt or damaged"

    /** Bytes cannot be parsed as any known backup format (Requirement 10.3). */
    fun backupUnreadable(reason: String?): String =
        "Backup is unreadable" + if (!reason.isNullOrBlank()) ": $reason" else ""

    /** File is neither an envelope nor a legacy payload (Requirements 2.7, 9.6). */
    const val NOT_VALID_BACKUP = "Not a valid backup"

    // --- Local import (ViewModel-level messages) ---

    const val IMPORT_TOO_MANY_ATTEMPTS = "Import failed: too many incorrect password attempts"
    const val IMPORT_ABORTED = "Import aborted"

    // --- Cloud-specific wrappers ---

    fun cloudRestoreFailed(reason: String?): String =
        "Cloud restore failed" + if (!reason.isNullOrBlank()) ": $reason" else ""
}
