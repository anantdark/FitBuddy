package com.anant.fitbuddy.data.backup.mongo

import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.backup.BackupData
import com.anant.fitbuddy.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Personal Atlas backup, uploaded/downloaded via the fitbuddy-cloud-backup HTTPS proxy
 * (Vercel) rather than a direct MongoDB connection — the app never holds Atlas
 * credentials, only a shared API key ([MongoUriVault]). One document per install,
 * keyed by [supportId] as `_id` server-side.
 */
open class MongoBackupRepository(
    private val http: OkHttpClient = defaultClient()
) {

    open suspend fun upload(
        baseUrl: String,
        apiKey: String,
        databaseName: String,
        collectionName: String,
        supportId: String,
        payloadJson: String,
        exportedAt: Long,
        deviceName: String,
        macId: String
    ) = withContext(Dispatchers.IO) {
        val id = supportId.trim()
        require(id.isNotBlank()) { "Support ID is blank — cannot upload backup" }
        val dbName = databaseName.trim().ifBlank { AppSettings.DEFAULT_MONGO_DB_NAME }
        val collName = collectionName.trim().ifBlank { AppSettings.DEFAULT_MONGO_COLLECTION }

        val body = JSONObject()
            .put("payloadJson", payloadJson)
            .put("schemaVersion", BackupData.CURRENT_VERSION)
            .put("exportedAt", exportedAt)
            .put("appPackage", BuildConfig.APPLICATION_ID)
            .put("deviceName", deviceName.trim().take(128))
            .put("macId", macId.trim().take(64))
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(backupUrl(baseUrl, id, dbName, collName))
            .header("Authorization", "Bearer $apiKey")
            .put(body)
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error(errorMessage(response.body.string(), response.code))
            }
        }
    }

    /**
     * Returns the BackupData JSON for [supportId].
     * @throws IllegalStateException when missing or schema too new for this app.
     */
    suspend fun downloadPayloadJson(
        baseUrl: String,
        apiKey: String,
        databaseName: String,
        collectionName: String,
        supportId: String
    ): String = withContext(Dispatchers.IO) {
        val id = supportId.trim()
        require(id.isNotBlank()) { "Support ID is required to restore" }
        val dbName = databaseName.trim().ifBlank { AppSettings.DEFAULT_MONGO_DB_NAME }
        val collName = collectionName.trim().ifBlank { AppSettings.DEFAULT_MONGO_COLLECTION }

        val url = backupUrl(baseUrl, id, dbName, collName)
            .newBuilder()
            .addQueryParameter("maxSchemaVersion", BackupData.CURRENT_VERSION.toString())
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .get()
            .build()

        http.newCall(request).execute().use { response ->
            val bodyString = response.body.string()
            if (!response.isSuccessful) {
                error(errorMessage(bodyString, response.code))
            }
            val json = JSONObject(bodyString)
            json.optString("payloadJson").takeIf { it.isNotBlank() }
                ?: error("Cloud backup is missing payloadJson")
        }
    }

    private fun backupUrl(
        baseUrl: String,
        supportId: String,
        databaseName: String,
        collectionName: String
    ) = "${baseUrl.trimEnd('/')}/api/backup/$supportId".toHttpUrlOrNull()
        ?.newBuilder()
        ?.addQueryParameter("db", databaseName)
        ?.addQueryParameter("collection", collectionName)
        ?.build()
        ?: error("Invalid cloud backup URL: $baseUrl")

    private fun errorMessage(rawBody: String?, code: Int): String {
        val fallback = "Cloud backup request failed (HTTP $code)"
        val body = rawBody?.takeIf { it.isNotBlank() } ?: return fallback
        return runCatching { JSONObject(body).optString("error") }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?: fallback
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        private fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}
