package com.anant.fitbuddy.bridge

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.anant.fitbuddy.FitBuddyApp
import kotlinx.coroutines.runBlocking

/**
 * Same-device bridge for FreeScale: upsert / export body measurements via [call].
 *
 * Authority: `${applicationId}.bridge.measurements`
 * Allowed callers: `com.anant.freescale` and `com.anant.freescale.debug`.
 */
class MeasurementBridgeProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle {
        return try {
            assertCallerAllowed()
            when (method) {
                METHOD_PING -> Bundle().apply { putBoolean(KEY_OK, true) }
                METHOD_UPSERT -> upsert(extras)
                METHOD_EXPORT_ALL -> exportAll()
                else -> errorBundle("Unknown method: $method")
            }
        } catch (t: Throwable) {
            Log.w(TAG, "call($method) failed: ${t.message}")
            errorBundle(t.message ?: "Bridge call failed")
        }
    }

    private fun upsert(extras: Bundle?): Bundle {
        val json = extras?.getString(KEY_JSON)
            ?: return errorBundle("Missing json")
        return try {
            val incoming = ScaleBridgeJson.decodeMeasurement(json)
            val app = context?.applicationContext as? FitBuddyApp
                ?: return errorBundle("App not ready")
            val inserted = runBlocking {
                app.repository.upsertMeasurementByTimestamp(incoming)
            }
            Bundle().apply {
                putBoolean(KEY_OK, true)
                putBoolean(KEY_INSERTED, inserted)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "upsert failed", t)
            errorBundle(t.message ?: "Upsert failed")
        }
    }

    private fun exportAll(): Bundle {
        return try {
            val app = context?.applicationContext as? FitBuddyApp
                ?: return errorBundle("App not ready")
            val rows = runBlocking { app.repository.getAllMeasurementsOnce() }
            Bundle().apply {
                putBoolean(KEY_OK, true)
                putString(KEY_JSON, ScaleBridgeJson.encodeArray(rows))
            }
        } catch (t: Throwable) {
            Log.e(TAG, "exportAll failed", t)
            errorBundle(t.message ?: "Export failed")
        }
    }

    private fun assertCallerAllowed() {
        val pkg = callingPackage
            ?: error("Missing calling package")
        if (pkg !in ALLOWED_PACKAGES) {
            Log.w(TAG, "reject caller=$pkg allowed=$ALLOWED_PACKAGES")
            error("Caller not allowed: $pkg")
        }
        Log.i(TAG, "allow caller=$pkg")
    }

    private fun errorBundle(message: String): Bundle =
        Bundle().apply {
            putBoolean(KEY_OK, false)
            putString(KEY_ERROR, message)
        }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        private const val TAG = "FitBuddy/Bridge"

        const val METHOD_PING = "ping"
        const val METHOD_UPSERT = "upsert"
        const val METHOD_EXPORT_ALL = "exportAll"

        const val KEY_OK = "ok"
        const val KEY_ERROR = "error"
        const val KEY_JSON = "json"
        const val KEY_INSERTED = "inserted"

        val ALLOWED_PACKAGES = setOf(
            "com.anant.freescale",
            "com.anant.freescale.debug",
        )
    }
}
