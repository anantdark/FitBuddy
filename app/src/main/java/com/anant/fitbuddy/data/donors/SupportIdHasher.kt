package com.anant.fitbuddy.data.donors

import java.security.MessageDigest

/**
 * Canonical Support ID hashing for the public donors list.
 *
 * Input is normalized as: trim → lowercase → strip `-` → UTF-8 → SHA-256 → lowercase hex.
 * Generated ids are `UUID.randomUUID().toString()` (lowercase + hyphens); pasted variants
 * with/without dashes or mixed case all produce the same hash.
 */
object SupportIdHasher {

    fun canonicalize(supportId: String): String =
        supportId.trim().lowercase().replace("-", "")

    fun hash(supportId: String): String {
        val canonical = canonicalize(supportId)
        if (canonical.isEmpty()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { b -> "%02x".format(b) }
    }
}
