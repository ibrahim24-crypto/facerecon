package com.himo.facerecon

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

private const val TAG = "FaceAlignment"

/**
 * Landmark-based face alignment for the embedding stage: levels the eye line,
 * normalizes face scale, and centers the face in a canonical square crop before
 * the bitmap reaches FaceNet. When the decoded keypoints look implausible the
 * caller falls back to a plain bounding-box crop, so behavior can only improve,
 * never regress, relative to the unaligned pipeline.
 */
object FaceAlignment {

    // Canonical placement inside the output crop, as fractions of outputSize:
    // eye midpoint sits slightly above center with the eye line spanning
    // EYE_DIST_FRACTION of the crop width.
    private const val EYE_MID_Y_FRACTION = 0.40f
    private const val EYE_DIST_FRACTION = 0.42f

    // Plausibility guards; outside these the keypoints are treated as garbage.
    private const val MIN_EYE_DIST_PX = 6f
    private const val MIN_EYE_DIST_TO_BOX = 0.12f
    private const val MAX_EYE_DIST_TO_BOX = 0.95f
    private const val MAX_ROLL_DEG = 60f

    fun alignedCrop(source: Bitmap, det: FaceDet, outputSize: Int = 160): Bitmap? {
        val leftEye = det.keypoints.getOrNull(0) ?: return null
        val rightEye = det.keypoints.getOrNull(1) ?: return null

        var dx = rightEye.x - leftEye.x
        var dy = rightEye.y - leftEye.y
        // Direction-agnostic so a left/right keypoint mixup cannot flip the face.
        if (dx < 0f) {
            dx = -dx
            dy = -dy
        }
        val eyeDist = hypot(dx, dy)
        val boxWidth = det.bounds.width().toFloat()

        val plausible = eyeDist >= MIN_EYE_DIST_PX &&
            eyeDist >= MIN_EYE_DIST_TO_BOX * boxWidth &&
            eyeDist <= MAX_EYE_DIST_TO_BOX * boxWidth &&
            abs(Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()) <= MAX_ROLL_DEG
        if (!plausible) {
            Log.d(
                TAG,
                "Keypoints implausible (eyeDist=%.1f, boxW=%.1f), using plain crop".format(eyeDist, boxWidth)
            )
            return null
        }

        val angleDeg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
        val midX = (leftEye.x + rightEye.x) / 2f
        val midY = (leftEye.y + rightEye.y) / 2f
        val scale = (EYE_DIST_FRACTION * outputSize) / eyeDist

        val matrix = Matrix()
        matrix.postRotate(-angleDeg, midX, midY)
        matrix.postScale(scale, scale, midX, midY)
        matrix.postTranslate(outputSize / 2f - midX, EYE_MID_Y_FRACTION * outputSize - midY)

        return try {
            val out = Bitmap.createBitmap(outputSize, outputSize, Bitmap.Config.ARGB_8888)
            Canvas(out).drawBitmap(
                source,
                matrix,
                Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
            )
            out
        } catch (e: Exception) {
            Log.w(TAG, "Aligned crop failed, using plain crop", e)
            null
        }
    }
}

/**
 * The one crop path every embedding must go through — enrollment and
 * recognition alike — so stored and live embeddings stay comparable.
 */
fun FaceDet.cropForEmbedding(source: Bitmap, outputSize: Int = 160): Bitmap? {
    FaceAlignment.alignedCrop(source, this, outputSize)?.let { return it }
    return try {
        val left = bounds.left.coerceIn(0, source.width - 1)
        val top = bounds.top.coerceIn(0, source.height - 1)
        val width = (bounds.right - bounds.left).coerceIn(1, source.width - left)
        val height = (bounds.bottom - bounds.top).coerceIn(1, source.height - top)
        Bitmap.createBitmap(source, left, top, width, height)
    } catch (e: Exception) {
        Log.e(TAG, "Fallback crop failed", e)
        null
    }
}
