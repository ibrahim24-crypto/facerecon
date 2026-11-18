package com.himo.facerecon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceRecognitionStabilizerTest {

    @Test
    fun confidentMatchIsAcceptedImmediately() {
        val stabilizer = FaceRecognitionStabilizer()

        val decision = stabilizer.stabilize("face-1", "Alice", distance = 0.2f, threshold = 1.0f)

        assertEquals("Alice", decision.name)
        assertFalse(decision.unsure)
    }

    @Test
    fun firstAmbiguousFrameExposesCandidateAsUnsure() {
        val stabilizer = FaceRecognitionStabilizer()

        val decision = stabilizer.stabilize(
            faceId = "face-1",
            rawName = "Unknown",
            distance = 0.9f,
            threshold = 1.0f,
            ambiguous = true,
            candidateName = "Alice"
        )

        assertEquals("Alice", decision.name)
        assertTrue(decision.unsure)
    }

    @Test
    fun ambiguousFramesRemainUnsureUntilAConfidentFrame() {
        val stabilizer = FaceRecognitionStabilizer()

        repeat(2) {
            val unsure = stabilizer.stabilize(
                faceId = "face-1",
                rawName = "Unknown",
                distance = 0.9f,
                threshold = 1.0f,
                ambiguous = true,
                candidateName = "Alice"
            )
            assertEquals("Alice", unsure.name)
            assertTrue(unsure.unsure)
        }

        val confident = stabilizer.stabilize("face-1", "Alice", distance = 0.1f, threshold = 1.0f)
        assertEquals("Alice", confident.name)
        assertFalse(confident.unsure)
    }

    @Test
    fun majorityOfRecentFramesWins() {
        val stabilizer = FaceRecognitionStabilizer()

        repeat(3) { stabilizer.stabilize("face-1", "Alice", distance = 0.85f, threshold = 1.0f) }
        val decision = stabilizer.stabilize("face-1", "Bob", distance = 0.85f, threshold = 1.0f)

        assertEquals("Alice", decision.name)
        assertFalse(decision.unsure)
    }

    @Test
    fun pruneActiveIdsForgetsStaleFaces() {
        val stabilizer = FaceRecognitionStabilizer()
        stabilizer.stabilize("gone", "Alice", distance = 0.1f, threshold = 1.0f)

        stabilizer.pruneActiveIds(emptySet())
        val decision = stabilizer.stabilize("gone", "Unknown", distance = 0.9f, threshold = 1.0f)

        assertEquals("Unknown", decision.name)
        assertFalse(decision.unsure)
    }

    @Test
    fun clearResetsEveryTrack() {
        val stabilizer = FaceRecognitionStabilizer()
        stabilizer.stabilize("face-1", "Alice", distance = 0.1f, threshold = 1.0f)

        stabilizer.clear()
        val decision = stabilizer.stabilize("face-1", "Unknown", distance = 0.9f, threshold = 1.0f)

        assertEquals("Unknown", decision.name)
    }
}
