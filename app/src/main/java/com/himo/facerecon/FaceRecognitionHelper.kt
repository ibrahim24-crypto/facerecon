package com.himo.facerecon

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.TensorOperator
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import org.tensorflow.lite.support.tensorbuffer.TensorBufferFloat
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

data class RecognitionResult(
    val name: String,
    val bestDistance: Float,
    val bestCandidateName: String
)

class FaceRecognitionHelper(
    context: Context,
    private val modelFileName: String = "facenet.tflite",
    private val embeddingDim: Int = 128,
    useGpu: Boolean = true,
    useXnnPack: Boolean = true
) {
    private lateinit var interpreter: Interpreter
    private var gpuDelegate: GpuDelegate? = null
    private val inferenceExecutor = Executors.newSingleThreadExecutor()
    private val storage = FaceStorage(context)
    private val knownFaces = mutableMapOf<String, MutableList<StoredFaceSample>>()

    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        .add(StandardizeOp())
        .build()

    init {
        inferenceExecutor.submit {
            // Try GPU delegate first (if requested). If interpreter creation fails with delegate,
            // fall back to CPU-only to avoid crashing the app.
            var options = Interpreter.Options()
            try {
                options = Interpreter.Options().apply {
                    if (useGpu) {
                        try {
                            val delegate = GpuDelegate()
                            gpuDelegate = delegate
                            addDelegate(delegate)
                        } catch (e: Exception) {
                            Log.w(TAG, "GPU delegate not available (create), falling back to CPU", e)
                            numThreads = 4
                        }
                    } else {
                        numThreads = 4
                    }
                    setUseXNNPACK(useXnnPack)
                }

                interpreter = Interpreter(loadModelFile(context, modelFileName), options)
            } catch (e: Exception) {
                Log.w(TAG, "Interpreter init with GPU failed, retrying CPU-only", e)
                try {
                    gpuDelegate?.close()
                } catch (_: Exception) {
                }
                gpuDelegate = null
                val cpuOptions = Interpreter.Options().apply {
                    numThreads = 4
                    setUseXNNPACK(useXnnPack)
                }
                interpreter = Interpreter(loadModelFile(context, modelFileName), cpuOptions)
            }

            loadFromDisk()
            Log.d(TAG, "Loaded model=$modelFileName dim=$embeddingDim")
        }.get()
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val afd = context.assets.openFd(modelName)
        val fis = FileInputStream(afd.fileDescriptor)
        return fis.channel.map(FileChannel.MapMode.READ_ONLY, afd.startOffset, afd.declaredLength)
    }

    private fun loadFromDisk() {
        knownFaces.clear()
        storage.loadAll().forEach { sample ->
            knownFaces.getOrPut(sample.name) { mutableListOf() }.add(sample)
        }
        Log.d(TAG, "Loaded ${knownFaces.values.sumOf { it.size }} samples for ${knownFaces.size} people")
    }

    fun getEmbedding(faceBitmap: Bitmap): FloatArray {
        return inferenceExecutor.submit<FloatArray> {
            val input = imageProcessor.process(TensorImage.fromBitmap(faceBitmap)).buffer
            val output = Array(1) { FloatArray(embeddingDim) }
            interpreter.run(input, output)
            output[0]
        }.get()
    }

    fun recognize(embedding: FloatArray, threshold: Float = DEFAULT_THRESHOLD): RecognitionResult {
        if (knownFaces.isEmpty()) {
            return RecognitionResult("Unknown", Float.MAX_VALUE, "Unknown")
        }

        var bestName = "Unknown"
        var bestAvgDist = Float.MAX_VALUE

        for ((name, samples) in knownFaces) {
            if (samples.isEmpty()) continue
            var sum = 0f
            for (sample in samples) {
                sum += euclideanDistance(embedding, sample.embedding)
            }
            val avg = sum / samples.size
            if (avg < bestAvgDist) {
                bestAvgDist = avg
                bestName = name
            }
        }

        val recognized = if (bestAvgDist < threshold) bestName else "Unknown"
        return RecognitionResult(recognized, bestAvgDist, bestName)
    }

    fun addFaceWithBitmap(name: String, bitmap: Bitmap, embedding: FloatArray) {
        val sample = storage.saveSample(name, bitmap, embedding.copyOf())
        knownFaces.getOrPut(name) { mutableListOf() }.add(sample)
    }

    fun removeSample(sampleId: String) {
        val entry = knownFaces.entries.firstOrNull { (_, samples) ->
            samples.any { it.id == sampleId }
        } ?: return
        entry.value.removeAll { it.id == sampleId }
        if (entry.value.isEmpty()) {
            knownFaces.remove(entry.key)
        }
        storage.deleteSample(sampleId)
    }

    fun removePerson(name: String) {
        knownFaces.remove(name)
        storage.deletePerson(name)
    }

    fun renamePerson(oldName: String, newName: String) {
        val trimmedNew = newName.trim()
        if (oldName == trimmedNew || trimmedNew.isBlank()) return
        if (!knownFaces.containsKey(oldName)) return
        if (knownFaces.containsKey(trimmedNew)) {
            Log.w(TAG, "Cannot rename: '$trimmedNew' already exists")
            return
        }
        storage.renamePerson(oldName, trimmedNew)
        loadFromDisk()
        Log.d(TAG, "Renamed person '$oldName' -> '$trimmedNew'")
    }

    fun getKnownNames(): List<String> = knownFaces.keys.sorted()
    fun getSamplesForName(name: String): List<StoredFaceSample> = knownFaces[name]?.toList() ?: emptyList()
    fun getKnownFaceCount(name: String): Int = knownFaces[name]?.size ?: 0
    fun getAllKnownNames(): Set<String> = knownFaces.keys.toSet()
    fun loadThumbnail(path: String): Bitmap? = storage.loadThumbnail(path)

    private fun euclideanDistance(a: FloatArray, b: FloatArray): Float {
        var sum = 0.0
        for (i in a.indices) {
            val d = a[i] - b[i]
            sum += d * d
        }
        return sqrt(sum).toFloat()
    }

    private class StandardizeOp : TensorOperator {
        override fun apply(input: TensorBuffer?): TensorBuffer {
            val pixels = input!!.floatArray
            val mean = pixels.average().toFloat()
            var std = sqrt(pixels.map { (it - mean).pow(2) }.sum() / pixels.size.toFloat())
            std = max(std, 1f / sqrt(pixels.size.toFloat()))
            for (i in pixels.indices) {
                pixels[i] = (pixels[i] - mean) / std
            }
            val output = TensorBufferFloat.createFixedSize(input.shape, DataType.FLOAT32)
            output.loadArray(pixels)
            return output
        }
    }

    fun close() {
        inferenceExecutor.submit {
            if (::interpreter.isInitialized) {
                try {
                    interpreter.close()
                } catch (_: Exception) {
                }
            }
            try {
                gpuDelegate?.close()
            } catch (_: Exception) {
            }
        }
    }

    companion object {
        private const val TAG = "FaceRecognitionHelper"
        private const val INPUT_SIZE = 160
        const val DEFAULT_THRESHOLD = 10.0f
    }
}
