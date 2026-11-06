package com.anant.fitbuddy.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * Opens the public FitBuddy Telegram group in a Telegram client when one is installed
 * (official app, Nekogram, Forkgram, Telegram X, etc.), otherwise in the browser.
 *
 * Requires a `<queries>` intent for the `tg` scheme so Android 11+ package visibility can
 * see clients that handle `tg://` deep links.
 */
object TelegramGroupLauncher {
    const val GROUP_USERNAME = "fitbuddyapp"
    const val WEB_URL = "https://t.me/$GROUP_USERNAME"
    private val TG_URI = Uri.parse("tg://resolve?domain=$GROUP_USERNAME")
    private val WEB_URI = Uri.parse(WEB_URL)

    fun hasTelegramClient(context: Context): Boolean =
        resolveTelegramIntent(context.packageManager) != null

    /**
     * @return `"telegram"` when a client handled the deep link, otherwise `"browser"`.
     */
    fun open(context: Context): String {
        val pm = context.packageManager
        val telegramIntent = resolveTelegramIntent(pm)
        val intent = telegramIntent ?: viewIntent(WEB_URI)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return if (telegramIntent != null) "telegram" else "browser"
    }

    private fun resolveTelegramIntent(pm: PackageManager): Intent? {
        val intent = viewIntent(TG_URI)
        return intent.takeIf { it.resolveActivity(pm) != null }
    }

    private fun viewIntent(uri: Uri): Intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
}
