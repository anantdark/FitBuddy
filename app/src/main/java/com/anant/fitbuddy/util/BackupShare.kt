package com.anant.fitbuddy.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import java.io.File

object BackupShare {
    /**
     * Share a backup JSON file. Uses [ShareCompat] so ClipData and URI read grants are set on
     * the chooser (required for the system share sheet and file managers to receive the stream).
     *
     * MIME is the catch-all type rather than application/json: many file managers only match
     * broad types, so a JSON-specific type hides them from the share list.
     */
    fun shareJsonFile(context: Context, file: File, chooserTitle: String = "Share backup") {
        shareFile(context, file, mimeType = "*/*", chooserTitle = chooserTitle)
    }

    fun shareTextFile(context: Context, file: File, chooserTitle: String = "Share log") {
        shareFile(context, file, mimeType = "text/plain", chooserTitle = chooserTitle)
    }

    private fun shareFile(
        context: Context,
        file: File,
        mimeType: String,
        chooserTitle: String
    ) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        // ShareCompat sets ClipData + GRANT_READ on the chooser. Plain ACTION_SEND with only
        // EXTRA_STREAM does not — the share sheet then can't grant the URI to file managers.
        val chooser = ShareCompat.IntentBuilder(context)
            .setType(mimeType)
            .setStream(uri)
            .setSubject(file.name)
            .setChooserTitle(chooserTitle)
            .createChooserIntent()
            .apply {
                // Ensure ClipData is present even on older core versions.
                if (clipData == null) {
                    clipData = ClipData.newUri(context.contentResolver, file.name, uri)
                }
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
        context.startActivity(chooser)
    }
}
