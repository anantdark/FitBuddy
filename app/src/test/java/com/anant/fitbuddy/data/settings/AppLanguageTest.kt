package com.anant.fitbuddy.data.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun fromStored_defaultsToEnglish() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStored(null))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStored(""))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStored("  "))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStored("nope"))
    }

    @Test
    fun fromStored_recognisesShippedTags() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStored("en"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromStored("ES"))
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromStored("ja"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromStored("HI"))
        // Empty / untranslated packs fall back to English until Weblate fills them.
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromStored("zh-MO"))
    }
}
