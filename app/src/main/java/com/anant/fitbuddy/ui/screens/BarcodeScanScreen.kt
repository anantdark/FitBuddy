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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.DecodeHintType
import com.google.zxing.FormatException
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BarcodeScan"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScanDialog(
    onBarcode: (String) -> Unit,
    onDismiss: () -> Unit,
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
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(if (manualEntry) "Enter barcode" else "Scan product barcode")
                    },
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
                if (manualEntry) {
                    ManualBarcodeEntry(
                        value = manualCode,
                        onValueChange = { manualCode = it.filter { c -> c.isDigit() }.take(14) },
                        onSubmit = ::submitManual,
                        onUseCamera = { manualEntry = false },
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
                                onBarcode = onBarcode
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
    modifier: Modifier = Modifier
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
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onSubmit() }),
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = onSubmit,
            enabled = value.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Look up") }
        TextButton(
            onClick = onUseCamera,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Use camera instead") }
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
        val reader = MultiFormatReader().apply {
            setHints(
                mapOf(
                    DecodeHintType.POSSIBLE_FORMATS to listOf(
                        BarcodeFormat.EAN_13,
                        BarcodeFormat.EAN_8,
                        BarcodeFormat.UPC_A,
                        BarcodeFormat.UPC_E
                    ),
                    DecodeHintType.TRY_HARDER to true
                )
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
                    try {
                        if (delivered.get()) return@setAnalyzer
                        val code = decodeProductBarcode(reader, imageProxy) ?: return@setAnalyzer
                        if (delivered.compareAndSet(false, true)) {
                            onBarcodeState(code)
                        }
                    } finally {
                        imageProxy.close()
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
            reader.reset()
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

/** Decode EAN/UPC from a CameraX YUV frame via ZXing. Returns null when no code is found. */
@androidx.camera.core.ExperimentalGetImage
private fun decodeProductBarcode(reader: MultiFormatReader, imageProxy: ImageProxy): String? {
    val mediaImage = imageProxy.image ?: return null
    val yBuffer = mediaImage.planes[0].buffer
    val yBytes = ByteArray(yBuffer.remaining())
    yBuffer.get(yBytes)
    val width = imageProxy.width
    val height = imageProxy.height
    val source = PlanarYUVLuminanceSource(
        yBytes,
        width,
        height,
        0,
        0,
        width,
        height,
        false
    )
    val bitmap = BinaryBitmap(HybridBinarizer(source))
    return try {
        reader.decodeWithState(bitmap).text
    } catch (_: NotFoundException) {
        null
    } catch (_: ChecksumException) {
        null
    } catch (_: FormatException) {
        null
    } finally {
        reader.reset()
    }
}
