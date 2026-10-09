package com.anant.fitbuddy.data.donors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportIdHasherTest {

    @Test
    fun canonicalize_stripsDashesAndLowercases() {
        assertEquals(
            "550e8400e29b41d4a716446655440000",
            SupportIdHasher.canonicalize("550E8400-E29B-41D4-A716-446655440000"),
        )
        assertEquals(
            "550e8400e29b41d4a716446655440000",
            SupportIdHasher.canonicalize("550e8400e29b41d4a716446655440000"),
        )
        assertEquals(
            "550e8400e29b41d4a716446655440000",
            SupportIdHasher.canonicalize("  550e8400-e29b-41d4-a716-446655440000  "),
        )
    }

    @Test
    fun hash_isStableAcrossFormatting() {
        val dashed = "550e8400-e29b-41d4-a716-446655440000"
        val upper = "550E8400-E29B-41D4-A716-446655440000"
        val compact = "550e8400e29b41d4a716446655440000"
        val h1 = SupportIdHasher.hash(dashed)
        val h2 = SupportIdHasher.hash(upper)
        val h3 = SupportIdHasher.hash(compact)
        assertEquals(64, h1.length)
        assertTrue(h1.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals(h1, h2)
        assertEquals(h1, h3)
        // Golden vector shared with config/README.md
        assertEquals(
            "140f39b05a2d9de451b9b7ad2d1f4a26b16fb5e5c8b7cbde6154679102614882",
            h1,
        )
    }

    @Test
    fun hash_philautianSupportId_golden() {
        // Keep in sync with config/donors.json entry for Philautian.
        val id = "2478bc7f-5892-4c83-8a5d-d43aaf39f3f0"
        assertEquals(
            "146a345312644c125e4ae4aae2ceec64fe10e2da99f451bf8d12e656d4ca460b",
            SupportIdHasher.hash(id),
        )
        assertEquals(
            SupportIdHasher.hash(id),
            SupportIdHasher.hash(id.uppercase()),
        )
        assertEquals(
            SupportIdHasher.hash(id),
            SupportIdHasher.hash(id.replace("-", "")),
        )
    }

    @Test
    fun hash_emptyAfterCanonicalize_isEmpty() {
        assertEquals("", SupportIdHasher.hash("   "))
        assertEquals("", SupportIdHasher.hash("---"))
        assertEquals("", SupportIdHasher.hash(""))
    }

    @Test
    fun hash_differsForDifferentIds() {
        assertNotEquals(
            SupportIdHasher.hash("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
            SupportIdHasher.hash("aaaaaaaa-bbbb-cccc-dddd-ffffffffffff"),
        )
    }
}
