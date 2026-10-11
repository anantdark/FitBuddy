package com.anant.fitbuddy.data.pcsync

import android.content.Context
import androidx.core.content.edit

/**
 * Device-local PC sync settings. Kept out of [com.anant.fitbuddy.data.settings.AppSettings] on
 * purpose so the pairing token never ends up in backups or cloud uploads.
 */
data class PcSyncConfig(
    val enabled: Boolean = false,
    /** `host:port`, e.g. `my-pc:8765` (Tailscale MagicDNS) or `100.64.0.1:8765`. */
    val address: String = "",
    val token: String = ""
) {
    val isUsable: Boolean get() = enabled && address.isNotBlank() && token.isNotBlank()

    fun baseUrl(): String {
        val trimmed = address.trim().trimEnd('/')
        val withScheme = if ("://" in trimmed) trimmed else "http://$trimmed"
        val hostPart = withScheme.substringAfter("://")
        return if (':' in hostPart) withScheme else "$withScheme:$DEFAULT_PORT"
    }

    companion object {
        const val DEFAULT_PORT = 8765
    }
}

internal class PcSyncStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("pc_sync", Context.MODE_PRIVATE)

    fun config(): PcSyncConfig = PcSyncConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        address = prefs.getString(KEY_ADDRESS, "").orEmpty(),
        token = prefs.getString(KEY_TOKEN, "").orEmpty()
    )

    fun saveConfig(config: PcSyncConfig) = prefs.edit {
        putBoolean(KEY_ENABLED, config.enabled)
        putString(KEY_ADDRESS, config.address.trim())
        putString(KEY_TOKEN, config.token.trim())
    }

    /** Ops applied locally whose ack hasn't reached the PC yet. */
    var pendingAcks: List<String>
        get() = prefs.getString(KEY_PENDING_ACKS, "").orEmpty().split('\n').filter { it.isNotBlank() }
        set(value) = prefs.edit { putString(KEY_PENDING_ACKS, value.joinToString("\n")) }

    /** Recently applied op ids, so a lost ack never re-applies the same op. */
    var appliedOps: List<String>
        get() = prefs.getString(KEY_APPLIED, "").orEmpty().split('\n').filter { it.isNotBlank() }
        set(value) = prefs.edit { putString(KEY_APPLIED, value.takeLast(MAX_APPLIED).joinToString("\n")) }

    var lastSuccessAt: Long
        get() = prefs.getLong(KEY_LAST_SUCCESS, 0L)
        set(value) = prefs.edit { putLong(KEY_LAST_SUCCESS, value) }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_ADDRESS = "address"
        const val KEY_TOKEN = "token"
        const val KEY_PENDING_ACKS = "pending_acks"
        const val KEY_APPLIED = "applied_ops"
        const val KEY_LAST_SUCCESS = "last_success_at"
        const val MAX_APPLIED = 1000
    }
}
