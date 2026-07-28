package com.anant.fitbuddy.ui.screens

import android.Manifest
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.anant.fitbuddy.ui.components.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import com.anant.fitbuddy.ui.components.IconButton
import androidx.compose.material3.MaterialTheme
import com.anant.fitbuddy.ui.components.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.anant.fitbuddy.ui.components.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import zxingcpp.BarcodeReader
import zxingcpp.BarcodeReader.Format
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BarcodeScan"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScanDialog(
    onBarcode: (String) -> Unit,
    onDismiss: () -> Unit,
    isLookingUp: Boolean = false,
    onCameraPermissionDenied: () -> Unit = {}
) {
    var requestedCameraOnce by remember { mutableStateOf(false) }
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA) { granted ->
        if (!granted && requestedCameraOnce) {
            onCameraPermissionDenied()
        }
    }
    var manualEntry by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }

    fun submitManual() {
        val code = manualCode.filter { it.isDigit() }
        if (code.isNotBlank()) onBarcode(code)
    }

    Dialog(
        onDismissRequest = { if (!isLookingUp) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when {
                                isLookingUp -> "Looking up product…"
                                manualEntry -> "Enter barcode"
                                else -> "Scan product barcode"
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !isLookingUp) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (manualEntry) {
                        ManualBarcodeEntry(
                            value = manualCode,
                            onValueChange = { manualCode = it.filter { c -> c.isDigit() }.take(14) },
                            onSubmit = ::submitManual,
                            onUseCamera = { manualEntry = false },
                            enabled = !isLookingUp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    } else {
                        when {
                            cameraPermission.status.isGranted -> {
                                Text(
                                    text = "Point at the barcode on the packet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                BarcodeCameraPreview(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    onBarcode = onBarcode,
                                    enabled = !isLookingUp
                                )
                                TextButton(
                                    onClick = { manualEntry = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) { Text("Enter barcode instead") }
                            }
                            else -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Camera access is needed to scan barcodes.")
                                    Button(
                                        onClick = {
                                            requestedCameraOnce = true
                                            cameraPermission.launchPermissionRequest()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Allow camera") }
                                    OutlinedButton(
                                        onClick = { manualEntry = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Enter barcode instead") }
                                }
                            }
                        }
                    }
                }

                // Loading overlay while the Open Food Facts lookup is in flight
                if (isLookingUp) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Looking up product…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            requestedCameraOnce = true
            cameraPermission.launchPermissionRequest()
        }
        onDispose { }
    }
}

@Composable
private fun ManualBarcodeEntry(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onUseCamera: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Type the EAN or UPC digits printed under the barcode.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text("Barcode") },
            placeholder = { Text("e.g. 8901030865422") },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = onSubmit,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Look up") }
        TextButton(
            onClick = onUseCamera,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Use camera instead") }
    }
}

@Composable
private fun BarcodeCameraPreview(
    modifier: Modifier = Modifier,
    onBarcode: (String) -> Unit,
    enabled: Boolean = true
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onBarcodeState by rememberUpdatedState(onBarcode)
    val enabledState by rememberUpdatedState(enabled)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val delivered = AtomicBoolean(false)
        val reader = BarcodeReader().apply {
            options.formats = setOf(
                Format.EAN_13,
                Format.EAN_8,
                Format.UPC_A,
                Format.UPC_E
            )
        }
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        val bindListener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    if (delivered.get() || !enabledState) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val results = imageProxy.use { reader.read(it) }
                    val code = results.firstOrNull()?.text
                    if (code != null && delivered.compareAndSet(false, true)) {
                        mainExecutor.execute { onBarcodeState(code) }
                    }
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                cameraError = e.message ?: "Couldn't open camera"
            }
        }
        cameraProviderFuture.addListener(bindListener, mainExecutor)

        onDispose {
            runCatching {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            }
            executor.shutdown()
        }
    }

    if (cameraError != null) {
        Text(
            text = cameraError ?: "",
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(16.dp)
        )
    } else {
        AndroidView(
            factory = { previewView },
            modifier = modifier
        )
    }
}
