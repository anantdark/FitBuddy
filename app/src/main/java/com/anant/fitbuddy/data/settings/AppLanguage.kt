package com.anant.fitbuddy.data.settings

import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * In-app UI language. Defaults to English; options match shipped `values-*` packs
 * that contain translations (empty Weblate stubs such as `zh-MO` are omitted).
 * Tags are BCP-47 (Android `values-zh-rMO` → `zh-MO`).
 */
enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    SPANISH("es"),
    JAPANESE("ja"),
    HINDI("hi");

    /** Native endonym (Español, …) so the picker stays readable before a switch. */
    fun displayName(): String {
        val locale = Locale.forLanguageTag(tag)
        val name = locale.getDisplayName(locale)
        return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    companion object {
        const val DEFAULT_TAG = "en"

        fun fromStored(raw: String?): AppLanguage {
            val tag = raw?.trim().orEmpty()
            if (tag.isEmpty()) return ENGLISH
            return entries.find { it.tag.equals(tag, ignoreCase = true) } ?: ENGLISH
        }

        /** Apply on the main thread — required by [AppCompatDelegate.setApplicationLocales]. */
        fun apply(tag: String) {
            val language = fromStored(tag)
            val desired = language.tag
            val current = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (current.equals(desired, ignoreCase = true)) return
            val locales = LocaleListCompat.forLanguageTags(desired)
            val applyNow = {
                AppCompatDelegate.setApplicationLocales(locales)
            }
            if (Looper.myLooper() == Looper.getMainLooper()) {
                applyNow()
            } else {
                Handler(Looper.getMainLooper()).post(applyNow)
            }
        }
    }
}
