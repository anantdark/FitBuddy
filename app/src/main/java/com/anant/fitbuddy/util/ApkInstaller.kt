package com.anant.fitbuddy.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

/** Downloads a release APK and hands it to the system installer (sideload update flow). */
object ApkInstaller {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    suspend fun downloadAndInstall(context: Context, url: String) {
        val apkFile = withContext(Dispatchers.IO) {
            val response = client.newCall(Request.Builder().url(url).build()).execute()
            response.use {
                if (!it.isSuccessful) {
                    error("Download failed: HTTP ${it.code}")
                }
                val file = File(context.cacheDir, "fitbuddy-update.apk")
                it.body.byteStream().use { input -> file.outputStream().use { output -> input.copyTo(output) } }
                file
            }
        }

        val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(installIntent)
    }
}
