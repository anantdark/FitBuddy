package com.anant.fitbuddy.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.SigningInfo
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Thrown when a downloaded update APK is signed with a different certificate than the
 * installed app (Android will refuse the update). Common when a local debug/`*-dev` install
 * tries to update to a differently-signed GitHub release APK.
 */
class ApkSignatureMismatchException(
    message: String = DEFAULT_MESSAGE
) : IllegalStateException(message) {
    companion object {
        const val DEFAULT_MESSAGE =
            "Can't update: this APK is signed differently from your installed FitBuddy " +
                "(common after a local/debug install). Uninstall FitBuddy, then install " +
                "the latest release from GitHub. Future in-app updates will work."
    }
}

/** Downloads a release APK and hands it to the system installer (sideload update flow). */
object ApkInstaller {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    /**
     * Downloads [url] to cache, verifies it is signed with the same cert as the running app,
     * then launches the system package installer.
     * [onProgress] reports fraction complete when Content-Length is known, otherwise -1f
     * (indeterminate) while bytes are flowing.
     */
    suspend fun downloadAndInstall(
        context: Context,
        url: String,
        onProgress: (Float) -> Unit = {}
    ) {
        val appContext = context.applicationContext
        val apkFile = withContext(Dispatchers.IO) {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            response.use { resp ->
                if (!resp.isSuccessful) {
                    error("Download failed: HTTP ${resp.code}")
                }
                val body = resp.body
                val contentLength = body.contentLength().takeIf { it > 0L }
                val file = File(appContext.cacheDir, "fitbuddy-update.apk")
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var totalRead = 0L
                        var lastReported = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            totalRead += read
                            if (contentLength != null) {
                                val pct = ((totalRead * 100) / contentLength).toInt().coerceIn(0, 100)
                                if (pct != lastReported) {
                                    lastReported = pct
                                    onProgress(pct / 100f)
                                }
                            } else if (lastReported < 0) {
                                lastReported = 0
                                onProgress(-1f)
                            }
                        }
                    }
                }
                onProgress(1f)
                file
            }
        }

        if (!signaturesMatchInstalled(appContext, apkFile)) {
            apkFile.delete()
            throw ApkSignatureMismatchException()
        }

        val apkUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        appContext.startActivity(installIntent)
    }

    /** True when [apkFile] and the installed app share at least one signing certificate. */
    private fun signaturesMatchInstalled(context: Context, apkFile: File): Boolean {
        val pm = context.packageManager
        val installed = installedCertDigests(pm, context.packageName) ?: return true
        val incoming = archiveCertDigests(pm, apkFile.absolutePath) ?: return true
        if (installed.isEmpty() || incoming.isEmpty()) return true
        return installed.any { it in incoming }
    }

    private fun installedCertDigests(pm: PackageManager, packageName: String): Set<String>? {
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                val info = pm.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(
                        PackageManager.GET_SIGNING_CERTIFICATES.toLong()
                    )
                )
                digestsFromSigningInfo(info.signingInfo)
            } else {
                @Suppress("DEPRECATION")
                val info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                digestsFromSigningInfo(info.signingInfo)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun archiveCertDigests(pm: PackageManager, apkPath: String): Set<String>? {
        return try {
            val flags = PackageManager.GET_SIGNING_CERTIFICATES
            val info = if (Build.VERSION.SDK_INT >= 33) {
                pm.getPackageArchiveInfo(
                    apkPath,
                    PackageManager.PackageInfoFlags.of(flags.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(apkPath, flags)
            } ?: return null
            // Archive paths need sourceDir set for some OEM PackageManagers.
            info.applicationInfo?.sourceDir = apkPath
            info.applicationInfo?.publicSourceDir = apkPath
            digestsFromSigningInfo(info.signingInfo)
        } catch (_: Exception) {
            null
        }
    }

    private fun digestsFromSigningInfo(signingInfo: SigningInfo?): Set<String> {
        if (signingInfo == null) return emptySet()
        val signatures = if (signingInfo.hasMultipleSigners()) {
            signingInfo.apkContentsSigners
        } else {
            signingInfo.signingCertificateHistory
        }
        return signatures.map { sha256(it.toByteArray()) }.toSet()
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
