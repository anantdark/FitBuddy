package com.anant.fitbuddy.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Requests runtime permissions needed for core FitBuddy features (camera + notifications)
 * once when the UI first becomes ready after cold start.
 *
 * [onDenied] receives permission names that the user rejected (or previously denied).
 */
@Composable
fun RequestStartupPermissions(
    onDenied: (deniedPermissions: List<String>) -> Unit = {}
) {
    val context = LocalContext.current
    var launched by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val denied = result.filterValues { granted -> !granted }.keys.toList()
        if (denied.isNotEmpty()) onDenied(denied)
    }

    LaunchedEffect(Unit) {
        if (launched) return@LaunchedEffect
        val needed = buildList {
            add(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) !=
                PackageManager.PERMISSION_GRANTED
        }
        launched = true
        if (needed.isNotEmpty()) {
            launcher.launch(needed.toTypedArray())
        }
    }
}
