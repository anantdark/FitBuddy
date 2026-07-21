package com.anant.fitbuddy.data.backup.mongo

import com.anant.fitbuddy.BuildConfig
import com.anant.fitbuddy.data.backup.BackupData
import com.anant.fitbuddy.data.settings.AppSettings
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClients
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import java.util.concurrent.TimeUnit

/**
 * Personal Atlas backup: one document per install, keyed by [supportId] as `_id`.
 * Top-level fields include [appPackage], [deviceName], and [macId] (alongside schema /
 * support metadata). [payloadJson] is the same Moshi BackupData v5 JSON as local
 * file export/import — device fields are not embedded in that JSON.
 *
 * Uses [AndroidDnsClient] so `mongodb+srv://` works on Android (no JNDI).
 */
class MongoBackupRepository(
    private val dnsClient: AndroidDnsClient = AndroidDnsClient()
) {

    suspend fun upload(
        connectionUri: String,
        databaseName: String,
        collectionName: String,
        supportId: String,
        payloadJson: String,
        exportedAt: Long,
        deviceName: String,
        macId: String
    ) = withContext(Dispatchers.IO) {
        val uri = connectionUri.trim()
        require(uri.isNotBlank()) { "MongoDB URI is blank" }
        val id = supportId.trim()
        require(id.isNotBlank()) { "Support ID is blank — cannot upload backup" }
        val dbName = databaseName.trim().ifBlank { AppSettings.DEFAULT_MONGO_DB_NAME }
        val collName = collectionName.trim().ifBlank { AppSettings.DEFAULT_MONGO_COLLECTION }
        createClient(uri).use { client ->
            val collection = client.getDatabase(dbName).getCollection(collName)
            val doc = Document("_id", id)
                .append("schemaVersion", BackupData.CURRENT_VERSION)
                .append("exportedAt", exportedAt)
                .append("supportId", id)
                .append("appPackage", BuildConfig.APPLICATION_ID)
                .append("deviceName", deviceName.trim().take(128))
                .append("macId", macId.trim().take(64))
                .append("payloadJson", payloadJson)
            collection.replaceOne(
                Filters.eq("_id", id),
                doc,
                ReplaceOptions().upsert(true)
            )
        }
    }

    /**
     * Returns the BackupData JSON for [supportId].
     * @throws IllegalStateException when missing or schema too new for this app.
     */
    suspend fun downloadPayloadJson(
        connectionUri: String,
        databaseName: String,
        collectionName: String,
        supportId: String
    ): String = withContext(Dispatchers.IO) {
        val uri = connectionUri.trim()
        require(uri.isNotBlank()) { "MongoDB URI is blank" }
        val id = supportId.trim()
        require(id.isNotBlank()) { "Support ID is required to restore" }
        val dbName = databaseName.trim().ifBlank { AppSettings.DEFAULT_MONGO_DB_NAME }
        val collName = collectionName.trim().ifBlank { AppSettings.DEFAULT_MONGO_COLLECTION }
        createClient(uri).use { client ->
            val collection = client.getDatabase(dbName).getCollection(collName)
            val doc = collection.find(Filters.eq("_id", id)).first()
                ?: collection.find(
                    Filters.and(
                        Filters.eq("_id", LEGACY_DOC_ID),
                        Filters.eq("supportId", id)
                    )
                ).first()
                ?: error("No cloud backup found for Support ID $id")
            val schemaVersion = doc.getInteger("schemaVersion", 0)
            if (schemaVersion > BackupData.CURRENT_VERSION) {
                error(
                    "Cloud backup schema v$schemaVersion is newer than this app " +
                        "(supports up to v${BackupData.CURRENT_VERSION})"
                )
            }
            doc.getString("payloadJson")
                ?.takeIf { it.isNotBlank() }
                ?: error("Cloud backup is missing payloadJson")
        }
    }

    private fun createClient(uri: String) = MongoClients.create(
        MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(uri, dnsClient))
            .dnsClient(dnsClient)
            .applyToSocketSettings { builder ->
                builder.connectTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
            }
            .applyToClusterSettings { builder ->
                builder.serverSelectionTimeout(30, TimeUnit.SECONDS)
            }
            .build()
    )

    companion object {
        /** Pre–support-id-keyed uploads used a fixed `_id`. Still readable if supportId matches. */
        const val LEGACY_DOC_ID = "latest"
    }
}
