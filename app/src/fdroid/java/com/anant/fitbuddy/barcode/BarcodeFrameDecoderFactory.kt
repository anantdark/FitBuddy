package com.anant.fitbuddy.barcode

import android.graphics.Rect
import androidx.camera.core.ImageProxy
import zxingcpp.BarcodeReader
import zxingcpp.BarcodeReader.Format

/** F-Droid / OSS build — ZXing-cpp only (no ML Kit on this classpath). */
fun createBarcodeFrameDecoder(): BarcodeFrameDecoder = ZxingBarcodeFrameDecoder()

private class ZxingBarcodeFrameDecoder : BarcodeFrameDecoder {
    private val fastReader = reader(tryHarder = false)
    private val thoroughReader = reader(tryHarder = true)

    override fun decode(imageProxy: ImageProxy, cropRect: Rect?): String? {
        val reader = if (cropRect == null) thoroughReader else fastReader
        if (cropRect != null) {
            imageProxy.setCropRect(cropRect)
        }
        return reader.read(imageProxy)
            .firstOrNull()
            ?.text
            ?.filter(Char::isDigit)
            ?.takeIf { it.isNotBlank() }
    }

    override fun close() = Unit

    private companion object {
        fun reader(tryHarder: Boolean) = BarcodeReader().apply {
            options.formats = setOf(Format.EAN_13, Format.EAN_8, Format.UPC_A, Format.UPC_E)
            options.tryHarder = tryHarder
            options.tryRotate = true
        }
    }
}
