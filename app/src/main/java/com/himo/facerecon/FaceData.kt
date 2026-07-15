package com.himo.facerecon

import android.graphics.Bitmap
import android.graphics.Rect

data class FaceData(
    val bounds: Rect,
    val name: String,
    val id: String = stableId(bounds),
    val confidence: Float = 0f,
    // ^ Recognition distance (euclidean). Lower = more confident. 0 = not computed.
    val enrollmentBitmap: Bitmap? = null,
    val enrollmentEmbedding: FloatArray? = null
) {
    companion object {
        fun stableId(bounds: Rect): String {
            val bucket = 48
            return "${bounds.centerX() / bucket}_${bounds.centerY() / bucket}"
        }
    }
}
