package com.anant.fitbuddy.ui.screens

import com.anant.fitbuddy.R

import androidx.compose.ui.res.stringResource

import android.Manifest
import android.graphics.Rect
import android.view.MotionEvent
import android.util.Log
import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageProxy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Rect as ComposeRect
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

private const val TAG = "BarcodeScan"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun BarcodeScanDialog(
    onBarcode: (String) -> Unit,
    onDismiss: () -> Unit,
    isLookingUp: Boolean = false,
    onCameraPermissionDenied: () -> Unit = {},
    barcodeExample: String = "8901030865422"
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
                                isLookingUp -> stringResource(R.string.barcode_looking_up_product)
                                manualEntry -> stringResource(R.string.barcode_enter_barcode)
                                else -> stringResource(R.string.barcode_scan_product_barcode)
                            }
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !isLookingUp) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.donate_close))
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
                            barcodeExample = barcodeExample,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                    } else {
                        when {
                            cameraPermission.status.isGranted -> {
                                Text(
                                    text = stringResource(R.string.barcode_point_at_the_barcode_on_the),
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
                                ) { Text(stringResource(R.string.barcode_enter_barcode_instead)) }
                            }
                            else -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(stringResource(R.string.barcode_camera_access_is_needed_to_scan))
                                    Button(
                                        onClick = {
                                            requestedCameraOnce = true
                                            cameraPermission.launchPermissionRequest()
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(stringResource(R.string.barcode_allow_camera)) }
                                    OutlinedButton(
                                        onClick = { manualEntry = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text(stringResource(R.string.barcode_enter_barcode_instead)) }
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
                                text = stringResource(R.string.barcode_looking_up_product),
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
    barcodeExample: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.barcode_type_the_ean_or_upc_digits),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(stringResource(R.string.barcode_barcode)) },
            placeholder = {
                Text(stringResource(R.string.barcode_e_g_s, barcodeExample))
            },
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
        ) { Text(stringResource(R.string.barcode_look_up)) }
        TextButton(
            onClick = onUseCamera,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.barcode_use_camera_instead)) }
    }
}


private const val CROP_WIDTH_FRACTION = 0.9f
private const val CROP_HEIGHT_FRACTION = 0.5f
private const val BOX_WIDTH_FRACTION = 0.8f
private const val BOX_HEIGHT_FRACTION = 0.3f

private fun scanReader(tryHarder: Boolean) = BarcodeReader().apply {
    options.formats = setOf(Format.EAN_13, Format.EAN_8, Format.UPC_A, Format.UPC_E)
    options.tryHarder = tryHarder
    options.tryRotate = true
}

private fun ImageProxy.centerCrop(widthFraction: Float, heightFraction: Float): Rect {
    val upright = imageInfo.rotationDegrees % 180 == 0
    val uprightW = if (upright) width else height
    val uprightH = if (upright) height else width
    val cropW = (uprightW * widthFraction).toInt()
    val cropH = (uprightH * heightFraction).toInt()
    val bufW = if (upright) cropW else cropH
    val bufH = if (upright) cropH else cropW
    val left = (width - bufW) / 2
    val top = (height - bufH) / 2
    return Rect(left, top, left + bufW, top + bufH)
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
    val delivered = remember { AtomicBoolean(false) }
    val manualRequested = remember { AtomicBoolean(false) }
    LaunchedEffect(enabled) {
        if (enabled) delivered.set(false)
    }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var manualMiss by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torchOn by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val fastReader = scanReader(tryHarder = false)
        val thoroughReader = scanReader(tryHarder = true)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        val bindListener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val resolution = ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1920, 1080),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                        )
                    )
                    .build()
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(resolution)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    if (delivered.get() || !enabledState) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val manual = manualRequested.getAndSet(false)
                    val results = imageProxy.use {
                        if (manual) {
                            thoroughReader.read(it)
                        } else {
                            it.setCropRect(it.centerCrop(CROP_WIDTH_FRACTION, CROP_HEIGHT_FRACTION))
                            fastReader.read(it)
                        }
                    }
                    val code = results.firstOrNull()?.text
                        ?.filter(Char::isDigit)
                        ?.takeIf { it.isNotBlank() }
                    if (code != null && delivered.compareAndSet(false, true)) {
                        mainExecutor.execute { onBarcodeState(code) }
                    } else if (manual) {
                        mainExecutor.execute { manualMiss = true }
                    }
                }
                cameraProvider.unbindAll()
                val bound = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
                camera = bound.takeIf { it.cameraInfo.hasFlashUnit() }
                torchOn = false
                previewView.setOnTouchListener { view, event ->
                    if (event.action == MotionEvent.ACTION_UP) {
                        val point = previewView.meteringPointFactory.createPoint(event.x, event.y)
                        bound.cameraControl.startFocusAndMetering(
                            FocusMeteringAction.Builder(point).build()
                        )
                        view.performClick()
                    }
                    true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Camera bind failed", e)
                cameraError = e.message
                    ?: context.getString(R.string.barcode_couldn_t_open_camera)
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
        Box(modifier = modifier) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
            ViewfinderOverlay(modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (manualMiss) {
                    Text(
                        text = stringResource(R.string.barcode_manual_capture_miss),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            manualMiss = false
                            manualRequested.set(true)
                        },
                        enabled = enabled,
                        modifier = Modifier.align(Alignment.Center)
                    ) { Text(stringResource(R.string.barcode_capture_now)) }
                    if (camera != null) {
                        IconButton(
                            onClick = {
                                torchOn = !torchOn
                                camera?.cameraControl?.enableTorch(torchOn)
                            },
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        ) {
                            Icon(
                                imageVector = if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                contentDescription = stringResource(
                                    if (torchOn) R.string.barcode_flashlight_off else R.string.barcode_flashlight_on
                                ),
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    val scrim = Color.Black.copy(alpha = 0.5f)
    Canvas(modifier = modifier) {
        val boxW = size.width * BOX_WIDTH_FRACTION
        val boxH = size.height * BOX_HEIGHT_FRACTION
        val topLeft = Offset((size.width - boxW) / 2f, (size.height - boxH) / 2f)
        val boxSize = ComposeSize(boxW, boxH)
        val radius = CornerRadius(16.dp.toPx())
        val hole = Path().apply {
            addRoundRect(RoundRect(ComposeRect(topLeft, boxSize), radius))
        }
        clipPath(hole, clipOp = ClipOp.Difference) { drawRect(scrim) }
        drawRoundRect(
            color = Color.White,
            topLeft = topLeft,
            size = boxSize,
            cornerRadius = radius,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
