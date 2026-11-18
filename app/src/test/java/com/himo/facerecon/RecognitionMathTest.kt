package com.himo.facerecon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure recognition rules run on the JVM: no TFLite model and no Android context.
 */
class RecognitionMathTest {

    @Test
    fun l2NormalizeProducesUnitVector() {
        val normalized = l2Normalize(floatArrayOf(3f, 4f))

        assertEquals(0.6f, normalized[0], 0.0001f)
        assertEquals(0.8f, normalized[1], 0.0001f)
    }

    @Test
    fun l2NormalizeOfZeroVectorStaysZero() {
        val normalized = l2Normalize(floatArrayOf(0f, 0f, 0f))

        assertEquals(0f, normalized.sum(), 0f)
    }

    @Test
    fun l2DistanceBetweenIdenticalVectorsIsZero() {
        val vector = l2Normalize(floatArrayOf(1f, 2f, 3f))

        assertEquals(0f, l2Distance(vector, vector), 0.0001f)
    }

    @Test
    fun confidentMatchIsReturnedWithCandidateName() {
        val alice = floatArrayOf(1f, 0f)
        val bob = floatArrayOf(0f, 1f)

        val result = recognizeEmbedding(
            embedding = alice,
            knownFaces = mapOf("Alice" to listOf(alice), "Bob" to listOf(bob)),
            threshold = 1.0f
        )

        assertEquals("Alice", result.name)
        assertEquals("Alice", result.bestCandidateName)
        assertFalse(result.ambiguous)
        assertTrue(result.bestDistance < 0.0001f)
    }

    @Test
    fun closeSecondPlaceIsMarkedAmbiguous() {
        val probe = floatArrayOf(1f, 0f)
        val alice = floatArrayOf(1f, 0.1f)
        val bob = floatArrayOf(1f, 0.15f)

        val result = recognizeEmbedding(
            embedding = probe,
            knownFaces = mapOf("Alice" to listOf(alice), "Bob" to listOf(bob)),
            threshold = 1.0f
        )

        assertTrue(result.ambiguous)
        assertEquals("Unknown", result.name)
        assertEquals("Alice", result.bestCandidateName)
    }

    @Test
    fun topThreeSamplesAreAveragedInsteadOfAllSamples() {
        val near = floatArrayOf(1f, 0f)
        val far = floatArrayOf(0f, 1f)
        val samples = listOf(near, near, near, far, far)

        val result = recognizeEmbedding(
            embedding = near,
            knownFaces = mapOf("Alice" to samples),
            threshold = 0.3f
        )

        assertEquals("Alice", result.name)
        assertFalse(result.ambiguous)
    }

    @Test
    fun distanceAboveThresholdIsUnknown() {
        val result = recognizeEmbedding(
            embedding = floatArrayOf(1f, 0f),
            knownFaces = mapOf("Alice" to listOf(floatArrayOf(-1f, 0f))),
            threshold = 1.0f
        )

        assertEquals("Unknown", result.name)
        assertEquals("Alice", result.bestCandidateName)
        assertFalse(result.ambiguous)
    }

    @Test
    fun emptyGalleryReturnsUnknown() {
        val result = recognizeEmbedding(floatArrayOf(1f, 0f), emptyMap(), 1.0f)

        assertEquals("Unknown", result.name)
        assertEquals("Unknown", result.bestCandidateName)
        assertEquals(Float.MAX_VALUE, result.bestDistance, 0f)
    }
}
