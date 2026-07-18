package com.anant.fitbuddy.ui.screens

import android.Manifest
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BarcodeScan"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScanDialog(
    onBarcode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Scan product barcode") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when {
                    cameraPermission.status.isGranted -> {
                        Text(
                            text = "Point at the barcode on the packet",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        BarcodeCameraPreview(
                            modifier = Modifier.fillMaxSize(),
                            onBarcode = onBarcode
                        )
                    }
                    else -> {
                        Text(
                            text = "Camera access is needed to scan barcodes.",
                            modifier = Modifier.padding(16.dp)
                        )
                        androidx.compose.material3.Button(
                            onClick = { cameraPermission.launchPermissionRequest() },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) { Text("Allow camera") }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
        onDispose { }
    }
}

@androidx.camera.core.ExperimentalGetImage
@Composable
private fun BarcodeCameraPreview(
    modifier: Modifier = Modifier,
    onBarcode: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val onBarcodeState by rememberUpdatedState(onBarcode)
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val delivered = AtomicBoolean(false)
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
        val scanner = BarcodeScanning.getClient(options)
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
                    val mediaImage = imageProxy.image
                    if (mediaImage == null || delivered.get()) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )
                    scanner.process(image)
                        .addOnSuccessListener(mainExecutor) { barcodes ->
                            val code = barcodes.firstOrNull()?.rawValue ?: return@addOnSuccessListener
                            if (delivered.compareAndSet(false, true)) {
                                onBarcodeState(code)
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
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
            scanner.close()
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
