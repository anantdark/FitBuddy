package com.anant.fitbuddy.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

/** Best-effort device labels for cloud backup metadata (non-payload Mongo fields). */
object DeviceIdentity {

    /** User-visible device name, falling back to [Build.MODEL]. */
    fun deviceName(context: Context): String {
        val fromSettings = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        } else {
            null
        }
        return fromSettings?.trim()?.takeIf { it.isNotBlank() }
            ?: Build.MODEL.orEmpty().trim().ifBlank { "Android" }
    }

    /**
     * Hardware MAC when the OS still exposes it; otherwise [Settings.Secure.ANDROID_ID]
     * so cloud docs always carry a stable device-scoped id.
     */
    @SuppressLint("HardwareIds")
    fun macId(context: Context): String {
        readWifiMac()?.let { return it }
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            .orEmpty()
    }

    private fun readWifiMac(): String? {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (iface in interfaces) {
                val name = iface.name?.lowercase(Locale.US).orEmpty()
                if (!name.startsWith("wlan") && !name.startsWith("eth")) continue
                val mac = iface.hardwareAddress ?: continue
                if (mac.isEmpty() || mac.all { it == 0.toByte() }) continue
                val formatted = mac.joinToString(":") { b ->
                    String.format(Locale.US, "%02x", b)
                }
                // Android often returns the placeholder randomized MAC.
                if (formatted == "02:00:00:00:00:00") continue
                return formatted
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
