package com.anant.fitbuddy.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

/**
 * Opens the Open Food Facts app when installed (Play Store or F-Droid package), otherwise
 * the mobile-app download page.
 *
 * Requires `<queries>` package entries in the manifest so Android 11+ package visibility can
 * see these apps.
 */
object OpenFoodFactsLauncher {
    const val INSTALL_URL = "https://world.openfoodfacts.org/open-food-facts-mobile-app"

    /** Play Store / Smoothie package. */
    private const val PLAY_PACKAGE = "org.openfoodfacts.scanner"

    /** F-Droid package (legacy applicationId). */
    private const val FDROID_PACKAGE = "openfoodfacts.github.scrachx.openfood"

    private val PACKAGES = listOf(PLAY_PACKAGE, FDROID_PACKAGE)

    fun isInstalled(context: Context): Boolean {
        val pm = context.packageManager
        return PACKAGES.any { isPackageInstalled(pm, it) }
    }

    /**
     * Opens OFF for [barcode] when possible (product deep link or launcher), else the install page.
     *
     * @return `"app"` when an OFF package was launched, otherwise `"install"`.
     */
    fun open(context: Context, barcode: String? = null): String {
        val pm = context.packageManager
        val productUri = barcode
            ?.takeIf { it.isNotBlank() }
            ?.let { Uri.parse("https://world.openfoodfacts.org/product/$it") }

        for (pkg in PACKAGES) {
            if (!isPackageInstalled(pm, pkg)) continue
            val intent = launchIntent(pm, pkg, productUri) ?: continue
            if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return "app"
        }

        val install = Intent(Intent.ACTION_VIEW, Uri.parse(INSTALL_URL)).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(install)
        return "install"
    }

    private fun launchIntent(pm: PackageManager, packageName: String, productUri: Uri?): Intent? {
        if (productUri != null) {
            val product = Intent(Intent.ACTION_VIEW, productUri).apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addCategory(Intent.CATEGORY_BROWSABLE)
                setPackage(packageName)
            }
            if (product.resolveActivity(pm) != null) return product
        }
        @Suppress("DEPRECATION")
        return pm.getLaunchIntentForPackage(packageName)
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
}
