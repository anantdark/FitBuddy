package com.anant.fitbuddy.data.donors

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SupportIdHasherTest {

    @Test
    fun canonicalize_stripsDashesAndLowercases() {
        assertEquals(
            "550e8400e29b41d4a716446655440000",
            SupportIdHasher.canonicalize("550E8400-E29B-41D4-A716-446655440000")
        )
        assertEquals(
            "550e8400e29b41d4a716446655440000",
            SupportIdHasher.canonicalize("550e8400e29b41d4a716446655440000")
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
        assertEquals(h1, h2)
        assertEquals(h1, h3)
    }

    @Test
    fun hash_differsForDifferentIds() {
        assertNotEquals(
            SupportIdHasher.hash("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
            SupportIdHasher.hash("aaaaaaaa-bbbb-cccc-dddd-ffffffffffff")
        )
    }
}
