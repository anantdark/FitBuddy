package com.anant.fitbuddy.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesTest {

    @Test
    fun `highlights keep feat and fix, drop chore ci and hashes`() {
        val body = """
            ## FitBuddy 3.3.24 (build 124)

            **Version code:** 124

            ### What's new
            - feat: smarter workout filter order (4b9583b)
            - fix: keep AI credentials after reinstall (5f0d0ca)
            - chore(fdroid): bump to 3.2.79 [skip ci] (738a896)
            - ci: install available Android SDK packages (63b4465)
            - refactor: tidy viewmodel (abcdef0)
        """.trimIndent()

        val highlights = ReleaseNotes.highlights(body)
        assertEquals(
            listOf(
                "feat: smarter workout filter order",
                "fix: keep AI credentials after reinstall",
            ),
            highlights
        )
    }

    @Test
    fun `highlights accept bullet character from F-Droid style notes`() {
        val highlights = ReleaseNotes.highlights("• feat: public donor thank-you\n• chore: ignore me")
        assertEquals(listOf("feat: public donor thank-you"), highlights)
        assertFalse(highlights.any { it.contains("chore") })
    }

    @Test
    fun `highlights respect limit`() {
        val body = (1..10).joinToString("\n") { "- feat: change $it" }
        assertEquals(3, ReleaseNotes.highlights(body, limit = 3).size)
        assertTrue(ReleaseNotes.highlights(body, limit = 3).first().contains("change 1"))
    }
}
