package com.anant.fitbuddy.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object BackupShare {
    fun shareJsonFile(context: Context, file: File, chooserTitle: String = "Share backup") {
        shareFile(context, file, mimeType = "application/json", chooserTitle = chooserTitle)
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
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, file.name)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
