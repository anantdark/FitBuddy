package com.anant.fitbuddy.crash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SentryCloudIngestTest {

    @Test
    fun envelopeRequest_usesAttestationTokenAndOmitsDsn() {
        val request = SentryCloudIngest.envelopeRequest(
            "https://fitbuddy-cloud-backup.vercel.app/",
            "header.payload",
            byteArrayOf(1, 2, 3)
        )
        assertEquals(
            "https://fitbuddy-cloud-backup.vercel.app/api/sentry",
            request.url.toString()
        )
        assertEquals("Bearer header.payload", request.header("Authorization"))
        assertNull(request.header("X-Sentry-Dsn"))
        assertNull(request.header("X-FitBuddy-Key"))
    }
}
