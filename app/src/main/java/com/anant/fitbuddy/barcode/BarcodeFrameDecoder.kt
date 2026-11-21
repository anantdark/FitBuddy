package com.anant.fitbuddy.barcode

import android.graphics.Rect
import androidx.camera.core.ImageProxy

/**
 * On-device barcode engine for a single camera frame.
 *
 * Implementations are **flavor-private**:
 * - `src/github` → Google ML Kit (bundled model)
 * - `src/fdroid` → ZXing-cpp (OSS)
 *
 * Shared UI must only call [createBarcodeFrameDecoder] and this interface — never import
 * `com.google.mlkit` or `zxingcpp` from `main`.
 */
interface BarcodeFrameDecoder : AutoCloseable {
    /**
     * @param cropRect buffer-coordinate ROI for live viewfinder scan; `null` = full frame
     *   (manual "Capture now").
     * @return digit-only product code, or null if none found.
     */
    fun decode(imageProxy: ImageProxy, cropRect: Rect?): String?
}
