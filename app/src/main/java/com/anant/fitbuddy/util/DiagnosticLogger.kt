package com.anant.fitbuddy.util

import android.content.Context
import android.net.Uri
import android.os.Build
import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.settings.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Opt-in support log for AI connection troubleshooting. Captures provider/model/HTTP
 * breadcrumbs only — never meal text, photos, or API keys. Survives process death while
 * enabled so users can reproduce an issue then export from Settings.
 */
object DiagnosticLogger {

    private const val MAX_LINES = 400
    private const val MAX_LINE_CHARS = 500
    private const val DIR = "diagnostics"
    private const val FILE_NAME = "session.log"

    @Volatile
    private var enabled: Boolean = false
    @Volatile
    private var filesDir: File? = null

    private val lock = Any()
    private val lines = ArrayDeque<String>(MAX_LINES + 1)
    private val _entryCount = MutableStateFlow(0)
    val entryCount: StateFlow<Int> = _entryCount.asStateFlow()

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun init(context: Context) {
        filesDir = File(context.applicationContext.filesDir, DIR).also { it.mkdirs() }
        synchronized(lock) {
            loadFromDiskLocked()
        }
    }

    fun isEnabled(): Boolean = enabled

    /**
     * Turn logging on/off. When [restartSession] is true (user toggle on), clears any prior
     * session and writes a metadata header. Disabling keeps the buffer so the user can export.
     * App startup should call with [restartSession]=false to resume an existing session file.
     */
    fun setEnabled(
        value: Boolean,
        settings: AppSettings? = null,
        restartSession: Boolean = true
    ) {
        enabled = value
        if (value && restartSession) {
            clear()
            if (settings != null) startSession(settings)
        }
    }

    fun clear() {
        synchronized(lock) {
            lines.clear()
            _entryCount.value = 0
            logFile()?.delete()
        }
    }

    fun hasContent(): Boolean = synchronized(lock) { lines.isNotEmpty() }

    fun log(category: String, message: String, details: Map<String, String> = emptyMap()) {
        if (!enabled) return
        val ts = isoFormat.format(Date())
        val detailPart = details.entries
            .joinToString(" ") { (k, v) -> "$k=${scrub(v)}" }
            .takeIf { it.isNotBlank() }
        val raw = buildString {
            append(ts)
            append(' ')
            append(scrub(category))
            append(": ")
            append(scrub(message))
            if (detailPart != null) {
                append(" | ")
                append(detailPart)
            }
        }.take(MAX_LINE_CHARS)
        appendLine(raw)
    }

    /**
     * Writes a shareable copy under cache/share/ and returns it for [BackupShare].
     * Returns null when there is nothing to export.
     */
    fun exportToShareFile(context: Context): File? {
        val body = synchronized(lock) {
            if (lines.isEmpty()) return null
            lines.joinToString("\n") + "\n"
        }
        val dir = File(context.cacheDir, "share").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val out = File(dir, "FitBuddy-diagnostics-$stamp.txt")
        out.writeText(body)
        return out
    }

    fun hostOf(url: String): String =
        runCatching { Uri.parse(url).host }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: url.take(80)

    fun scrub(value: String): String {
        var s = value
        s = Regex("(?i)(bearer\\s+)\\S+").replace(s) { "${it.groupValues[1]}[redacted]" }
        s = Regex("(?i)(sk-[A-Za-z0-9_-]{8,})").replace(s, "[redacted-key]")
        s = Regex("(?i)(AIza[A-Za-z0-9_-]{8,})").replace(s, "[redacted-key]")
        s = Regex("(?i)(api[_-]?key[=:]\\s*)\\S+").replace(s) { "${it.groupValues[1]}[redacted]" }
        s = Regex("eyJ[A-Za-z0-9_-]{20,}").replace(s, "[redacted-jwt]")
        s = Regex("data:image/[^;]+;base64,[A-Za-z0-9+/=]+").replace(s, "[image-omitted]")
        return s.take(MAX_LINE_CHARS)
    }

    private fun startSession(settings: AppSettings) {
        appendLine("=== FitBuddy diagnostic log ===")
        appendLine("started_utc=${isoFormat.format(Date())}")
        appendLine("app=${BuildConfig.APPLICATION_ID} ${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}")
        appendLine("android_sdk=${Build.VERSION.SDK_INT} ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("support_id=${settings.supportId.ifBlank { "(none)" }}")
        appendLine("provider=${settings.provider.name}")
        appendLine("auto_failover=${settings.autoFailoverFor(settings.provider)}")
        appendLine("show_paid=${settings.showPaidFor(settings.provider)}")
        appendLine("photo_model=${scrub(settings.model)}")
        appendLine("text_model=${scrub(settings.textModel)}")
        appendLine("chat_host=${hostOf(settings.chatUrl)}")
        appendLine("configured=${settings.isConfigured}")
        appendLine("keys_count=${settings.keysFor(settings.provider).size}")
        appendLine("--- events ---")
    }

    private fun appendLine(line: String) {
        synchronized(lock) {
            while (lines.size >= MAX_LINES) {
                lines.removeFirst()
            }
            lines.addLast(line)
            _entryCount.value = lines.size
            persistLocked()
        }
    }

    private fun logFile(): File? = filesDir?.let { File(it, FILE_NAME) }

    private fun persistLocked() {
        val file = logFile() ?: return
        file.writeText(lines.joinToString("\n") + "\n")
    }

    private fun loadFromDiskLocked() {
        val file = logFile() ?: return
        if (!file.exists()) return
        val loaded = file.readLines().filter { it.isNotBlank() }
        lines.clear()
        loaded.takeLast(MAX_LINES).forEach { lines.addLast(it) }
        _entryCount.value = lines.size
    }
}
