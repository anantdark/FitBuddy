package com.anant.fitbuddy.data.remote.oauth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OpenRouterPkceTest {

    @Test
    fun `verifier is url-safe without padding`() {
        val verifier = OpenRouterPkce.generateVerifier()
        assertTrue(verifier.length >= 43)
        assertFalse(verifier.contains("="))
        assertFalse(verifier.contains("+"))
        assertFalse(verifier.contains("/"))
    }

    @Test
    fun `s256 challenge matches RFC 7636 test vector`() {
        // https://datatracker.ietf.org/doc/html/rfc7636#appendix-B
        val verifier = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            OpenRouterPkce.challengeS256(verifier)
        )
    }
}
