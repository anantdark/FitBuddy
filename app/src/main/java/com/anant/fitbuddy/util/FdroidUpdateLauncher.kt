package com.anant.fitbuddy.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/**
 * Opens FitBuddy's F-Droid package page in a client app when possible, otherwise the website.
 *
 * Single client: launch it directly (order: F-Droid → F-Droid Basic → Droid-ify → website).
 * Several clients: show the system app picker among those clients only (no browser in the list).
 *
 * Requires `<queries>` package entries in the manifest so Android 11+ package visibility can see
 * these apps (otherwise we falsely fall back to the site).
 */
object FdroidUpdateLauncher {
    const val PACKAGE_ID = "com.anant.fitbuddy"
    const val WEB_URL = "https://f-droid.org/packages/$PACKAGE_ID/"

    private val WEB_URI = Uri.parse(WEB_URL)
    private val DEEP_LINK = Uri.parse("fdroid.app://details?id=$PACKAGE_ID")

    private val CLIENTS = listOf(
        Client("F-Droid", "org.fdroid.fdroid", listOf(DEEP_LINK, WEB_URI)),
        Client("F-Droid Basic", "org.fdroid.basic", listOf(DEEP_LINK, WEB_URI)),
        Client("Droid-ify", "com.looker.droidify", listOf(WEB_URI, DEEP_LINK)),
    )

    /**
     * @return human-readable destination ("F-Droid", "F-Droid Basic", "Droid-ify",
     * "app picker", or "website")
     */
    fun open(context: Context): String {
        val pm = context.packageManager
        val installed = CLIENTS.mapNotNull { client ->
            if (!isPackageInstalled(pm, client.packageName)) return@mapNotNull null
            val intent = launchIntentFor(pm, client) ?: return@mapNotNull null
            client to intent
        }

        when (installed.size) {
            0 -> {
                context.startActivity(
                    viewIntent(WEB_URI, packageName = null)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return "website"
            }
            1 -> {
                val (client, intent) = installed.first()
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                return client.label
            }
            else -> {
                val intents = installed.map { it.second }
                val chooser = Intent.createChooser(intents.first(), "Update with").apply {
                    putExtra(
                        Intent.EXTRA_INITIAL_INTENTS,
                        intents.drop(1).toTypedArray()
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                return "app picker"
            }
        }
    }

    private fun launchIntentFor(pm: PackageManager, client: Client): Intent? {
        for (uri in client.uris) {
            val intent = viewIntent(uri, client.packageName)
            if (intent.resolveActivity(pm) != null) return intent
        }
        // Package is installed but resolve failed (rare) — still try https targeted launch.
        return viewIntent(WEB_URI, client.packageName)
    }

    private fun viewIntent(uri: Uri, packageName: String?): Intent =
        Intent(Intent.ACTION_VIEW, uri).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
            if (packageName != null) setPackage(packageName)
        }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(packageName, 0)
            }
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }

    private data class Client(
        val label: String,
        val packageName: String,
        val uris: List<Uri>,
    )
}
