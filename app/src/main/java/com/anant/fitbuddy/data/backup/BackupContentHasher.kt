package com.anant.fitbuddy.data.backup

import java.security.MessageDigest

/**
 * Stable content hash for cloud tip skip. Hashes Moshi JSON with [BackupData.exportedAt]
 * zeroed so an unchanged Room snapshot does not look "new" every upload.
 */
object BackupContentHasher {

    fun hash(data: BackupData, encode: (BackupData) -> String): String =
        sha256Hex(encode(data.copy(exportedAt = 0L)))

    fun sha256Hex(utf8: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(utf8.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}

/** Outcome of [com.anant.fitbuddy.data.repository.FitnessRepository.uploadMongoBackup]. */
data class CloudUploadResult(
    val recordCount: Int,
    /** True when the tip plaintext matched the last uploaded hash and no PUT was sent. */
    val skipped: Boolean = false
)
