package com.himo.facerecon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

data class FaceDet(val bounds: Rect, val score: Float, val keypoints: List<PointF> = emptyList())

class BlazeFaceDetector(
    context: Context,
    private val modelFileName: String = "blaze_face_full_range.tflite"
) : AutoCloseable {

    private val interpreter: Interpreter = Interpreter(
        loadModelFile(context, modelFileName),
        Interpreter.Options().apply { numThreads = 4 }
    )

    fun detect(bitmap: Bitmap): List<FaceDet> {
        synchronized(interpreter) {
            val input = preprocess(bitmap)
            // Output index 0 = reshaped_regressor_face_4 [1,2304,16]; index 1 = reshaped_classifier_face_4 [1,2304,1]
            val regressor = Array(1) { Array(CELLS) { FloatArray(BOX_CHANNELS) } }
            val classifier = Array(1) { Array(CELLS) { FloatArray(1) } }
            interpreter.runForMultipleInputsOutputs(
                arrayOf(input),
                mapOf(0 to regressor, 1 to classifier)
            )

            val scores = FloatArray(CELLS) { i -> sigmoid(classifier[0][i][0]) }
            val boxes = regressor[0]

            var maxScore = 0f
            for (s in scores) if (s > maxScore) maxScore = s

            val candidates = mutableListOf<FaceDet>()
            for (cell in 0 until CELLS) {
                val score = scores[cell]
                if (score < SCORE_THRESHOLD) continue
                if (!isLocalMax(scores, cell)) continue

                val gx = cell % GRID
                val gy = cell / GRID
                // Full-range BlazeFace regression outputs are in 192-input-pixel
                // units relative to the anchor center (MediaPipe decodes as
                // raw/192 * anchorSize(=1 normalized) + anchorCenter(normalized)).
                val anchorX = (gx + 0.5f) * STRIDE
                val anchorY = (gy + 0.5f) * STRIDE
                val cx = anchorX + boxes[cell][0]
                val cy = anchorY + boxes[cell][1]
                val w = boxes[cell][2]
                val h = boxes[cell][3]

                // Keypoints are (x, y) pairs in the same anchor-relative pixel
                // units: right eye, left eye, nose tip, mouth center, tragions.
                val keypoints = List(KEYPOINT_COUNT) { k ->
                    PointF(
                        (anchorX + boxes[cell][4 + k * 2]) * bitmap.width / INPUT_SIZE,
                        (anchorY + boxes[cell][5 + k * 2]) * bitmap.height / INPUT_SIZE
                    )
                }

                val left = ((cx - w / 2f) * bitmap.width / INPUT_SIZE).toInt().coerceIn(0, bitmap.width - 1)
                val top = ((cy - h / 2f) * bitmap.height / INPUT_SIZE).toInt().coerceIn(0, bitmap.height - 1)
                val right = ((cx + w / 2f) * bitmap.width / INPUT_SIZE).toInt().coerceIn(left + 1, bitmap.width)
                val bottom = ((cy + h / 2f) * bitmap.height / INPUT_SIZE).toInt().coerceIn(top + 1, bitmap.height)

                if (right - left < MIN_FACE_PX || bottom - top < MIN_FACE_PX) continue
                candidates.add(FaceDet(Rect(left, top, right, bottom), score, keypoints))
            }

            val result = nonMaxSuppression(candidates, IOU_THRESHOLD).sortedByDescending { it.score }
            Log.d(TAG, "detect ${bitmap.width}x${bitmap.height}: maxScore=%.3f faces=%d".format(maxScore, result.size))
            return result
        }
    }

    /**
     * Whole image plus overlapping half-size tiles, merged with NMS. Tiles give
     * small faces (group photos) more of the 192px detector input. Still images
     * only — too many inference passes for the live camera path.
     */
    fun detectMultiScale(bitmap: Bitmap): List<FaceDet> {
        val detections = mutableListOf<FaceDet>()
        detections += detect(bitmap)

        val tileW = bitmap.width / 2
        val tileH = bitmap.height / 2
        if (tileW < MIN_FACE_PX * 2 || tileH < MIN_FACE_PX * 2) {
            return detections.sortedByDescending { it.score }
        }
        for (ox in intArrayOf(0, bitmap.width - tileW)) {
            for (oy in intArrayOf(0, bitmap.height - tileH)) {
                val tile = Bitmap.createBitmap(bitmap, ox, oy, tileW, tileH)
                for (det in detect(tile)) {
                    detections.add(
                        FaceDet(
                            Rect(
                                det.bounds.left + ox,
                                det.bounds.top + oy,
                                det.bounds.right + ox,
                                det.bounds.bottom + oy
                            ),
                            det.score,
                            det.keypoints.map { PointF(it.x + ox, it.y + oy) }
                        )
                    )
                }
            }
        }
        return nonMaxSuppression(detections, IOU_THRESHOLD).sortedByDescending { it.score }
    }

    private fun preprocess(bitmap: Bitmap): ByteBuffer {
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        if (scaled !== bitmap) scaled.recycle()

        val buffer = ByteBuffer
            .allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        for (px in pixels) {
            buffer.putFloat(2f * ((px shr 16 and 0xFF) / 255f) - 1f) // R
            buffer.putFloat(2f * ((px shr 8 and 0xFF) / 255f) - 1f)  // G
            buffer.putFloat(2f * ((px and 0xFF) / 255f) - 1f)        // B
        }
        buffer.rewind()
        return buffer
    }

    private fun isLocalMax(scores: FloatArray, cell: Int): Boolean {
        val gx = cell % GRID
        val gy = cell / GRID
        for (dy in -1..1) {
            for (dx in -1..1) {
                val nx = gx + dx
                val ny = gy + dy
                if (nx < 0 || ny < 0 || nx >= GRID || ny >= GRID) continue
                if (scores[ny * GRID + nx] > scores[cell]) return false
            }
        }
        return true
    }

    private fun sigmoid(v: Float): Float = 1f / (1f + exp(-v.coerceIn(-100f, 100f)))

    private fun nonMaxSuppression(faces: List<FaceDet>, iouThreshold: Float): List<FaceDet> {
        val kept = mutableListOf<FaceDet>()
        for (f in faces.sortedByDescending { it.score }) {
            if (kept.none { iou(f.bounds, it.bounds) > iouThreshold }) kept.add(f)
        }
        return kept
    }

    private fun iou(a: Rect, b: Rect): Float {
        val interW = max(0, min(a.right, b.right) - max(a.left, b.left))
        val interH = max(0, min(a.bottom, b.bottom) - max(a.top, b.top))
        val inter = interW * interH
        val union = a.width() * a.height() + b.width() * b.height() - inter
        return if (union <= 0) 0f else inter.toFloat() / union
    }

    override fun close() {
        try {
            interpreter.close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close interpreter", e)
        }
    }

    private fun loadModelFile(context: Context, fileName: String): MappedByteBuffer {
        context.assets.openFd(fileName).use { afd ->
            FileInputStream(afd.fileDescriptor).use { stream ->
                return stream.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
                    as MappedByteBuffer
            }
        }
    }

    companion object {
        private const val TAG = "BlazeFaceDetector"
        private const val INPUT_SIZE = 192
        private const val GRID = 48
        private const val CELLS = GRID * GRID
        private const val STRIDE = 4
        private const val BOX_CHANNELS = 16
        private const val KEYPOINT_COUNT = 6
        private const val SCORE_THRESHOLD = 0.5f
        private const val IOU_THRESHOLD = 0.3f
        private const val MIN_FACE_PX = 24
    }
}
