package com.anant.fitbuddy.bridge

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.database.BodyMeasurement

/**
 * Client for FreeScale's [ContentProvider] bridge (`*.bridge.measurements`).
 *
 * Debug FitBuddy prefers FreeScale Dev; release prefers release FreeScale.
 */
object FreeScaleBridge {
    private const val TAG = "FitBuddy/FreeScale"

    const val METHOD_PING = "ping"
    const val METHOD_EXPORT_LATEST = "exportLatest"

    private val preferredPackages: List<String> =
        if (BuildConfig.DEBUG) {
            listOf("com.anant.freescale.debug", "com.anant.freescale")
        } else {
            listOf("com.anant.freescale", "com.anant.freescale.debug")
        }

    fun isAvailable(context: Context): Boolean =
        resolveAuthority(context) != null

    fun exportLatest(context: Context): Result<BodyMeasurement> = runCatching {
        val authority = resolveAuthority(context)
            ?: error("FreeScale is not installed")
        Log.i(TAG, "exportLatest via $authority")
        val result = context.contentResolver.call(
            Uri.parse("content://$authority"),
            METHOD_EXPORT_LATEST,
            null,
            null,
        ) ?: error("FreeScale did not respond")
        if (!result.getBoolean("ok", false)) {
            error(result.getString("error") ?: "FreeScale export failed")
        }
        val json = result.getString("json")
            ?: error("FreeScale returned empty reading")
        ScaleBridgeJson.decodeMeasurement(json)
    }

    private fun resolveAuthority(context: Context): String? {
        val pm = context.packageManager
        Log.i(TAG, "resolveAuthority DEBUG=${BuildConfig.DEBUG} prefer=$preferredPackages")
        for (pkg in preferredPackages) {
            if (!isPackageInstalled(pm, pkg)) {
                Log.i(TAG, "skip $pkg (not installed / not visible)")
                continue
            }
            val authority = "$pkg.bridge.measurements"
            val ping = runCatching {
                context.contentResolver.call(
                    Uri.parse("content://$authority"),
                    METHOD_PING,
                    null,
                    null,
                )
            }
            ping.onFailure { t -> Log.w(TAG, "ping $authority threw", t) }
            val bundle = ping.getOrNull()
            if (bundle == null) {
                Log.w(TAG, "ping $authority returned null")
                continue
            }
            if (bundle.getBoolean("ok", false)) {
                Log.i(TAG, "using authority $authority")
                return authority
            }
            Log.w(TAG, "ping $authority ok=false error=${bundle.getString("error")}")
        }
        return null
    }

    private fun isPackageInstalled(pm: PackageManager, packageName: String): Boolean =
        try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
}
