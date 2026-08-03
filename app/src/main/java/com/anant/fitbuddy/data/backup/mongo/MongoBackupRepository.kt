package com.anant.fitbuddy.data.backup.mongo

import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.backup.BackupData
import com.anant.fitbuddy.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** One cloud backup chunk document returned by the Vercel proxy. */
data class CloudBackupDoc(
    val supportId: String,
    val chunkId: String,
    val chunkIndex: Int,
    val nextChunkId: String?,
    val tipChunkId: String?,
    val storageVersion: Int,
    val schemaVersion: Int,
    val exportedAt: Long,
    val appPackage: String,
    val deviceName: String,
    val macId: String,
    val payloadJson: String
)

/**
 * Personal Atlas backup via the fitbuddy-cloud-backup HTTPS proxy. Supports a single legacy
 * document per Support ID and append-only chunk chains (`nextChunkId` / `tipChunkId`).
 */
open class MongoBackupRepository(
    private val http: OkHttpClient = defaultClient()
) {

    /**
     * Legacy tip upload (head = tip). Prefer [uploadChunk] for chain-aware clients.
     */
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
    ) = uploadChunk(
        baseUrl = baseUrl,
        apiKey = apiKey,
        databaseName = databaseName,
        collectionName = collectionName,
        supportId = supportId,
        chunkId = supportId.trim(),
        chunkIndex = 0,
        nextChunkId = null,
        tipChunkId = supportId.trim(),
        payloadJson = payloadJson,
        exportedAt = exportedAt,
        deviceName = deviceName,
        macId = macId
    )

    open suspend fun uploadChunk(
        baseUrl: String,
        apiKey: String,
        databaseName: String,
        collectionName: String,
        supportId: String,
        chunkId: String,
        chunkIndex: Int,
        nextChunkId: String?,
        tipChunkId: String?,
        payloadJson: String,
        exportedAt: Long,
        deviceName: String,
        macId: String,
        headTipChunkId: String? = null
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
            .put("chunkId", chunkId.trim())
            .put("chunkIndex", chunkIndex)
            .put("storageVersion", 2)
            .put("nextChunkId", nextChunkId)
            .apply {
                if (tipChunkId != null) put("tipChunkId", tipChunkId)
                if (headTipChunkId != null) put("headTipChunkId", headTipChunkId)
            }
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)

        val url = backupUrl(baseUrl, id, dbName, collName)
            .newBuilder()
            .addQueryParameter("chainSupport", "1")
            .addQueryParameter("chunkId", chunkId.trim())
            .build()

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .put(body)
            .build()

        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error(errorMessage(response.body.string(), response.code))
            }
        }
    }

    open suspend fun downloadDoc(
        baseUrl: String,
        apiKey: String,
        databaseName: String,
        collectionName: String,
        supportId: String,
        chunkId: String? = null
    ): CloudBackupDoc = withContext(Dispatchers.IO) {
        val id = supportId.trim()
        require(id.isNotBlank()) { "Support ID is required to restore" }
        val dbName = databaseName.trim().ifBlank { AppSettings.DEFAULT_MONGO_DB_NAME }
        val collName = collectionName.trim().ifBlank { AppSettings.DEFAULT_MONGO_COLLECTION }
        val resolvedChunk = chunkId?.trim().orEmpty().ifBlank { id }

        val url = backupUrl(baseUrl, id, dbName, collName)
            .newBuilder()
            .addQueryParameter("maxSchemaVersion", BackupData.CURRENT_VERSION.toString())
            .addQueryParameter("chainSupport", "1")
            .addQueryParameter("chunkId", resolvedChunk)
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
            parseDoc(bodyString, id, resolvedChunk)
        }
    }

    /**
     * Returns the BackupData envelope JSON for [supportId] head (legacy single-doc path).
     */
    suspend fun downloadPayloadJson(
        baseUrl: String,
        apiKey: String,
        databaseName: String,
        collectionName: String,
        supportId: String
    ): String = downloadDoc(
        baseUrl, apiKey, databaseName, collectionName, supportId, chunkId = null
    ).payloadJson

    open suspend fun downloadChain(
        baseUrl: String,
        apiKey: String,
        databaseName: String,
        collectionName: String,
        supportId: String
    ): List<CloudBackupDoc> = withContext(Dispatchers.IO) {
        val docs = ArrayList<CloudBackupDoc>()
        var chunkId: String? = supportId.trim()
        val seen = HashSet<String>()
        while (chunkId != null) {
            if (!seen.add(chunkId)) error("Cloud backup chunk chain loop detected")
            val doc = downloadDoc(
                baseUrl, apiKey, databaseName, collectionName, supportId, chunkId
            )
            docs += doc
            chunkId = doc.nextChunkId?.trim()?.takeIf { it.isNotEmpty() }
        }
        docs
    }

    private fun parseDoc(bodyString: String, supportId: String, fallbackChunkId: String): CloudBackupDoc {
        val json = JSONObject(bodyString)
        val payloadJson = json.optString("payloadJson").takeIf { it.isNotBlank() }
            ?: error("Cloud backup is missing payloadJson")
        return CloudBackupDoc(
            supportId = json.optString("supportId").ifBlank { supportId },
            chunkId = json.optString("chunkId").ifBlank { fallbackChunkId },
            chunkIndex = json.optInt("chunkIndex", 0),
            nextChunkId = json.optString("nextChunkId").takeIf { it.isNotBlank() },
            tipChunkId = json.optString("tipChunkId").takeIf { it.isNotBlank() },
            storageVersion = json.optInt("storageVersion", 1),
            schemaVersion = json.optInt("schemaVersion", 0),
            exportedAt = json.optLong("exportedAt", 0L),
            appPackage = json.optString("appPackage"),
            deviceName = json.optString("deviceName"),
            macId = json.optString("macId"),
            payloadJson = payloadJson
        )
    }

    private fun backupUrl(
        baseUrl: String,
        supportId: String,
        databaseName: String,
        collectionName: String
    ): HttpUrl = "${baseUrl.trimEnd('/')}/api/backup/$supportId".toHttpUrlOrNull()
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
