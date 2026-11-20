package com.anant.fitbuddy.data.backup.mongo

import com.anant.fitbuddy.BuildConfig

/** Public cloud-backup proxy URL (no secrets). */
object MongoUriVault {

    fun isAvailable(): Boolean = baseUrl().isNotBlank()

    fun databaseName(): String =
        BuildConfig.MONGO_DB_NAME.trim().ifBlank { "fitbuddy" }

    fun baseUrl(): String =
        BuildConfig.CLOUD_BACKUP_BASE_URL.trim().trimEnd('/')
}
