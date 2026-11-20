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

    /** File is neither an envelope nor a legacy payload (Requirements 2.7, 9.6). */
    const val NOT_VALID_BACKUP = "Not a valid backup"

    // --- Local import (ViewModel-level messages) ---

    const val IMPORT_TOO_MANY_ATTEMPTS = "Import failed: too many incorrect password attempts"
    const val IMPORT_ABORTED = "Import aborted"

    // --- Cloud-specific wrappers ---

    const val CLOUD_OFFLINE = "No internet connection. Connect and try again."

    const val CLOUD_UNREACHABLE = "Couldn't reach cloud backup. Try again."

    const val CLOUD_VERIFY_FAILED = "Unable to verify this app. Cloud backup stays off."

    fun cloudRestoreFailed(reason: String?, hasInternet: Boolean? = null): String =
        when {
            reason.isNullOrBlank() || looksLikeTransportFailure(reason) ->
                "Cloud restore failed. ${transportHint(hasInternet, reason)}"
            else -> "Cloud restore failed: $reason"
        }

    /**
     * Drop DNS/socket jargon from toasts. Prefer [hasInternet] when known so offline vs
     * unreachable stay distinct.
     */
    fun userFacingCloudReason(reason: String?, hasInternet: Boolean? = null): String =
        when {
            reason == CLOUD_OFFLINE || reason == CLOUD_VERIFY_FAILED || reason == CLOUD_UNREACHABLE ->
                reason
            reason.isNullOrBlank() || looksLikeTransportFailure(reason) ->
                transportHint(hasInternet, reason)
            else -> reason
        }

    private fun transportHint(hasInternet: Boolean?, reason: String?): String =
        when {
            hasInternet == false || reason == CLOUD_OFFLINE -> CLOUD_OFFLINE
            else -> CLOUD_UNREACHABLE
        }

    private fun looksLikeTransportFailure(message: String): Boolean {
        val m = message.lowercase()
        return "unable to resolve host" in m ||
            "failed to connect" in m ||
            "network is unreachable" in m ||
            "connection refused" in m ||
            "connection reset" in m ||
            "software caused connection abort" in m ||
            "cleartext not permitted" in m ||
            "sslhandshake" in m ||
            "unknownhost" in m ||
            ("timeout" in m && ("connect" in m || "socket" in m || "failed" in m))
    }
}
