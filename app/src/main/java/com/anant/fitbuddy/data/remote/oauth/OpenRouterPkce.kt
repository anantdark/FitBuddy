package com.anant.fitbuddy.data.remote.oauth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** PKCE verifier + S256 challenge for OpenRouter OAuth. */
object OpenRouterPkce {

    fun generateVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return base64Url(bytes)
    }

    fun challengeS256(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return base64Url(digest)
    }

    private fun base64Url(bytes: ByteArray): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
}
