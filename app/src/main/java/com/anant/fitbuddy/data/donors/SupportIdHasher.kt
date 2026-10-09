package com.anant.fitbuddy.data.donors

import java.security.MessageDigest

/**
 * **Source of truth** for Support ID → public donors-list `hash`.
 *
 * Deterministic and stable across app versions. Same algorithm as
 * `config/hash_support_id.sh` and the bash snippet in `config/README.md`.
 *
 * Pipeline:
 * 1. trim whitespace
 * 2. lowercase
 * 3. remove all `-`
 * 4. SHA-256 of UTF-8 bytes
 * 5. lowercase hex (64 chars)
 *
 * UUID variants with/without hyphens or mixed case all hash identically.
 * Never commit raw Support IDs — only this hash belongs in `config/donors.json`.
 */
object SupportIdHasher {

    /** Normalize a Support ID before hashing. Empty after normalize → empty hash. */
    fun canonicalize(supportId: String): String =
        supportId.trim().lowercase().replace("-", "")

    /**
     * SHA-256 hex of [canonicalize]. Returns `""` if the canonical form is empty.
     * Output is always 64 lowercase hex chars for any non-empty Support ID.
     */
    fun hash(supportId: String): String {
        val canonical = canonicalize(supportId)
        if (canonical.isEmpty()) return ""
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(canonical.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }
}
