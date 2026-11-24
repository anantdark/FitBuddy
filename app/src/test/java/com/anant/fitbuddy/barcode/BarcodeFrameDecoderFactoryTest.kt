package com.anant.fitbuddy.barcode

import com.anant.fitbuddy.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Flavor wiring checks that stay JVM-safe (ML Kit's [BarcodeScanning.getClient] needs a real
 * Android runtime, so we do not instantiate the decoder here).
 */
class BarcodeFrameDecoderFactoryTest {

    @Test
    fun `factory function is linked for this flavor`() {
        val method = Class.forName("com.anant.fitbuddy.barcode.BarcodeFrameDecoderFactoryKt")
            .methods
            .firstOrNull { it.name == "createBarcodeFrameDecoder" }
        assertNotNull(method)
        assertEquals(BarcodeFrameDecoder::class.java, method!!.returnType)
    }

    @Test
    fun `classpath matches distribution flavor`() {
        if (BuildConfig.IS_FDROID) {
            Class.forName("zxingcpp.BarcodeReader")
        } else {
            Class.forName("com.google.mlkit.vision.barcode.BarcodeScanning")
        }
    }
}
