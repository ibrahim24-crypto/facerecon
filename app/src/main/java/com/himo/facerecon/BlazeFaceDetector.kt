package com.himo.facerecon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

data class FaceDet(val bounds: Rect, val score: Float)

class BlazeFaceDetector(
    context: Context,
    private val modelFileName: String = "blaze_face_full_range.tflite"
) : AutoCloseable {

    private var interpreter: Interpreter? = null
    private var gpuDelegate: GpuDelegate? = null

    init {
        try {
            val options = Interpreter.Options().apply {
                gpuDelegate = try {
                    GpuDelegate()
                } catch (e: Exception) {
                    Log.w(TAG, "GPU delegate unavailable, using CPU", e)
                    null
                }?.also { addDelegate(it) }
                if (gpuDelegate == null) numThreads = 4
            }
            interpreter = Interpreter(loadModelFile(context, modelFileName), options)
        } catch (e: Exception) {
            Log.w(TAG, "Interpreter init with GPU failed, retrying CPU-only", e)
            try {
                interpreter?.close()
            } catch (_: Exception) {
            }
            gpuDelegate?.close()
            gpuDelegate = null
            interpreter = Interpreter(
                loadModelFile(context, modelFileName),
                Interpreter.Options().apply { numThreads = 4 }
            )
        }
    }

    fun detect(bitmap: Bitmap): List<FaceDet> {
        val interp = interpreter ?: return emptyList()
        synchronized(interp) {
            val input = preprocess(bitmap)
            val classifier = Array(1) { Array(CELLS) { FloatArray(1) } }
            val regressor = Array(1) { Array(CELLS) { FloatArray(BOX_CHANNELS) } }
            interp.run(input, classifier)
            interp.run(input, regressor)

            val scores = FloatArray(CELLS) { i -> classifier[0][i][0] }
            val boxes = regressor[0]

            val candidates = mutableListOf<FaceDet>()
            for (cell in 0 until CELLS) {
                val score = sigmoidNormalize(scores[cell])
                if (score < SCORE_THRESHOLD) continue
                if (!isLocalMax(scores, cell)) continue

                val gx = cell % GRID
                val gy = cell / GRID
                val cx = (gx + boxes[cell][0]) * STRIDE
                val cy = (gy + boxes[cell][1]) * STRIDE
                val w = boxes[cell][2] * INPUT_SIZE
                val h = boxes[cell][3] * INPUT_SIZE

                val left = ((cx - w / 2f) * bitmap.width / INPUT_SIZE).toInt().coerceIn(0, bitmap.width - 1)
                val top = ((cy - h / 2f) * bitmap.height / INPUT_SIZE).toInt().coerceIn(0, bitmap.height - 1)
                val right = ((cx + w / 2f) * bitmap.width / INPUT_SIZE).toInt().coerceIn(left + 1, bitmap.width)
                val bottom = ((cy + h / 2f) * bitmap.height / INPUT_SIZE).toInt().coerceIn(top + 1, bitmap.height)

                if (right - left < MIN_FACE_PX || bottom - top < MIN_FACE_PX) continue
                candidates.add(FaceDet(Rect(left, top, right, bottom), score))
            }

            return nonMaxSuppression(candidates, IOU_THRESHOLD).sortedByDescending { it.score }
        }
    }

    private fun preprocess(bitmap: Bitmap): Array<Array<FloatArray>> {
        val scaled = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        scaled.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        if (scaled !== bitmap) scaled.recycle()

        val input = Array(INPUT_SIZE) { FloatArray(INPUT_SIZE * 3) }
        for (y in 0 until INPUT_SIZE) {
            for (x in 0 until INPUT_SIZE) {
                val px = pixels[y * INPUT_SIZE + x]
                input[y][x * 3] = 2f * ((px shr 16 and 0xFF) / 255f) - 1f
                input[y][x * 3 + 1] = 2f * ((px shr 8 and 0xFF) / 255f) - 1f
                input[y][x * 3 + 2] = 2f * ((px and 0xFF) / 255f) - 1f
            }
        }
        return arrayOf(input)
    }

    private fun isLocalMax(scores: FloatArray, cell: Int): Boolean {
        val gx = cell % GRID
        val gy = cell / GRID
        for (dy in -1..1) {
            for (dx in -1..1) {
                val nx = gx + dx
                val ny = gy + dy
                if (nx < 0 || ny < 0 || nx >= GRID || ny >= GRID) continue
                val n = scores[ny * GRID + nx]
                if (n > scores[cell]) return false
            }
        }
        return true
    }

    private fun sigmoidNormalize(v: Float): Float = when {
        v in 0f..1f -> v
        v < 0f -> 1f / (1f + exp(-v))
        else -> v
    }

    private fun nonMaxSuppression(faces: List<FaceDet>, iouThreshold: Float): List<FaceDet> {
        val sorted = faces.sortedByDescending { it.score }
        val kept = mutableListOf<FaceDet>()
        for (f in sorted) {
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
            interpreter?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to close interpreter", e)
        }
        interpreter = null
        try {
            gpuDelegate?.close()
        } catch (_: Exception) {
        }
        gpuDelegate = null
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
        private const val OUTPUT_CLASSIFIER = "reshaped_classifier_face_4"
        private const val OUTPUT_REGRESSOR = "reshaped_regressor_face_4"
        private const val SCORE_THRESHOLD = 0.5f
        private const val IOU_THRESHOLD = 0.3f
        private const val MIN_FACE_PX = 24
    }
}
