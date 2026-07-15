package com.himo.facerecon

/**
 * Reduces label flicker by requiring a majority of recent frames to agree
 * before changing the displayed name. High-confidence matches are accepted immediately.
 */
class FaceRecognitionStabilizer(
    private val historySize: Int = 5,
    private val majorityRequired: Int = 3,
    private val confidentMargin: Float = 0.2f
) {
    private val histories = mutableMapOf<String, ArrayDeque<String>>()
    private val stableNames = mutableMapOf<String, String>()

    fun stabilize(
        faceId: String,
        rawName: String,
        distance: Float,
        threshold: Float
    ): String {
        if (rawName != "Unknown" && distance < threshold - confidentMargin) {
            stableNames[faceId] = rawName
            histories.getOrPut(faceId) { ArrayDeque() }.apply {
                clear()
                repeat(majorityRequired) { addLast(rawName) }
            }
            return rawName
        }

        val history = histories.getOrPut(faceId) { ArrayDeque() }
        history.addLast(rawName)
        while (history.size > historySize) {
            history.removeFirst()
        }

        val counts = history.groupingBy { it }.eachCount()
        val winner = counts.maxByOrNull { it.value }
        if (winner != null && winner.value >= majorityRequired) {
            stableNames[faceId] = winner.key
            return winner.key
        }

        return stableNames[faceId] ?: "Unknown"
    }

    fun pruneActiveIds(activeIds: Set<String>) {
        val stale = histories.keys.filter { it !in activeIds }
        stale.forEach {
            histories.remove(it)
            stableNames.remove(it)
        }
    }

    fun clear() {
        histories.clear()
        stableNames.clear()
    }
}
