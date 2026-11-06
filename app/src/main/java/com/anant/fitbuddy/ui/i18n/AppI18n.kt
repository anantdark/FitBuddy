package com.anant.fitbuddy.ui.i18n

import android.content.Context
import android.os.LocaleList
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import com.anant.fitbuddy.data.settings.AppLanguage
import java.util.Locale

/** CompositionLocal carrying the user's selected [AppLanguage] across the Compose hierarchy. */
val LocalAppLanguage = compositionLocalOf { AppLanguage.SYSTEM }

/**
 * Internationalization (i18n) plugin for FitBuddy.
 *
 * Supports:
 * - [AppLanguage.SYSTEM]: Follows the device's system locale (Simplified Chinese when `zh*`, English otherwise)
 * - [AppLanguage.ZH_CN]: Simplified Chinese (`简体中文`)
 * - [AppLanguage.EN]: English (`English`)
 */
object AppI18n {

    private val startupSystemLocale: Locale = Locale.getDefault()

    @Volatile
    var currentLanguage: AppLanguage = AppLanguage.SYSTEM
        private set

    /** Returns the original device system locale captured before any in-app override. */
    fun systemLocale(): Locale = startupSystemLocale

    /** Resolves [language] into a concrete UI language ([AppLanguage.ZH_CN] or [AppLanguage.EN]). */
    fun resolveLanguage(
        language: AppLanguage = currentLanguage,
        deviceLocale: Locale = systemLocale()
    ): AppLanguage = when (language) {
        AppLanguage.ZH_CN -> AppLanguage.ZH_CN
        AppLanguage.EN -> AppLanguage.EN
        AppLanguage.SYSTEM -> {
            val lang = deviceLocale.language.lowercase(Locale.ROOT)
            if (lang.startsWith("zh")) AppLanguage.ZH_CN else AppLanguage.EN
        }
    }

    /** True when the effective UI language is Simplified Chinese. */
    fun isChinese(
        language: AppLanguage = currentLanguage,
        deviceLocale: Locale = systemLocale()
    ): Boolean = resolveLanguage(language, deviceLocale) == AppLanguage.ZH_CN

    /** Concrete [Locale] for formatting dates/numbers under the active language. */
    fun resolveLocale(
        language: AppLanguage = currentLanguage,
        deviceLocale: Locale = systemLocale()
    ): Locale = if (isChinese(language, deviceLocale)) {
        Locale.SIMPLIFIED_CHINESE
    } else {
        Locale.US
    }

    /**
     * Applies [language] to [currentLanguage], JVM default [Locale], and Android [Context]
     * resource configuration so both Compose and resource lookups reflect the selection live.
     */
    @Suppress("DEPRECATION")
    fun applyLanguage(context: Context?, language: AppLanguage) {
        currentLanguage = language
        val targetLocale = when (language) {
            AppLanguage.SYSTEM -> startupSystemLocale
            AppLanguage.ZH_CN -> Locale.SIMPLIFIED_CHINESE
            AppLanguage.EN -> Locale.ENGLISH
        }
        Locale.setDefault(targetLocale)
        if (context != null) {
            runCatching {
                val res = context.resources
                val config = res.configuration
                config.setLocale(targetLocale)
                config.setLocales(LocaleList(targetLocale))
                res.updateConfiguration(config, res.displayMetrics)
            }
        }
    }

    /** Translates [text] according to the active language (no-op when English is active). */
    fun translate(
        text: String,
        language: AppLanguage = currentLanguage,
        deviceLocale: Locale = systemLocale()
    ): String {
        if (text.isEmpty() || !isChinese(language, deviceLocale)) return text
        return ZhCnTranslations.translate(text)
    }

    /** Translates an [AnnotatedString] while preserving spans when possible. */
    fun translateAnnotated(
        text: AnnotatedString,
        language: AppLanguage = currentLanguage,
        deviceLocale: Locale = systemLocale()
    ): AnnotatedString {
        if (text.text.isEmpty() || !isChinese(language, deviceLocale)) return text
        if (text.spanStyles.isEmpty() && text.paragraphStyles.isEmpty()) {
            val translated = ZhCnTranslations.translate(text.text)
            return if (translated == text.text) text else AnnotatedString(translated)
        }
        return ZhCnTranslations.translateAnnotated(text)
    }
}
