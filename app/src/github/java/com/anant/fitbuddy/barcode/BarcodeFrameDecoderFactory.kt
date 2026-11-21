package com.anant.fitbuddy.barcode

import android.graphics.Rect
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.TimeUnit

/** GitHub / sideload build — bundled ML Kit barcode model (not on the fdroid classpath). */
fun createBarcodeFrameDecoder(): BarcodeFrameDecoder = MlKitBarcodeFrameDecoder()

private class MlKitBarcodeFrameDecoder : BarcodeFrameDecoder {
    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E
            )
            .build()
    )

    override fun decode(imageProxy: ImageProxy, cropRect: Rect?): String? {
        val mediaImage = imageProxy.image ?: return null
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val barcodes = try {
            Tasks.await(scanner.process(input), PROCESS_TIMEOUT_SEC, TimeUnit.SECONDS)
        } catch (_: Exception) {
            return null
        }
        val match = barcodes.firstOrNull { barcode ->
            if (cropRect == null) {
                true
            } else {
                val box = barcode.boundingBox ?: return@firstOrNull true
                Rect.intersects(cropRect, box)
            }
        }
        return match?.rawValue
            ?.filter(Char::isDigit)
            ?.takeIf { it.isNotBlank() }
    }

    override fun close() {
        scanner.close()
    }

    private companion object {
        const val PROCESS_TIMEOUT_SEC = 2L
    }
}
