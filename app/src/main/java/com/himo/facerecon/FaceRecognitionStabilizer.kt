package com.himo.facerecon

/**
 * A displayed identity plus whether it still needs human review.
 * `unsure` is used by attendance so a shaky match is never silently counted as Present.
 */
data class StabilizedRecognition(
    val name: String,
    val unsure: Boolean = false
)

private data class FrameVote(val name: String, val ambiguous: Boolean)

/**
 * Reduces label flicker by requiring a majority of recent frames to agree before
 * changing the displayed name. Ambiguous matches are kept visible as UNSURE so
 * attendance can route them to review instead of guessing.
 */
class FaceRecognitionStabilizer(
    private val historySize: Int = 5,
    private val majorityRequired: Int = 3,
    private val confidentMargin: Float = 0.2f
) {
    private val histories = mutableMapOf<String, ArrayDeque<FrameVote>>()
    private val stableNames = mutableMapOf<String, String>()

    fun stabilize(
        faceId: String,
        rawName: String,
        distance: Float,
        threshold: Float,
        ambiguous: Boolean = false,
        candidateName: String = rawName
    ): StabilizedRecognition {
        // An ambiguous match still carries its closest person; show that person
        // as UNSURE instead of collapsing it to a plain Unknown.
        val displayName = if (rawName == "Unknown" && ambiguous && candidateName != "Unknown") {
            candidateName
        } else {
            rawName
        }

        if (displayName != "Unknown" && !ambiguous && distance < threshold - confidentMargin) {
            stableNames[faceId] = displayName
            histories.getOrPut(faceId) { ArrayDeque() }.apply {
                clear()
                repeat(majorityRequired) { addLast(FrameVote(displayName, false)) }
            }
            return StabilizedRecognition(displayName)
        }

        val history = histories.getOrPut(faceId) { ArrayDeque() }
        history.addLast(FrameVote(displayName, ambiguous))
        while (history.size > historySize) {
            history.removeFirst()
        }

        val counts = history
            .filter { !it.ambiguous && it.name != "Unknown" }
            .groupingBy { it.name }
            .eachCount()
        val winner = counts.maxByOrNull { it.value }
        val anyAmbiguous = history.any { it.ambiguous }

        if (winner != null && winner.value >= majorityRequired) {
            stableNames[faceId] = winner.key
            return StabilizedRecognition(winner.key, unsure = anyAmbiguous)
        }
        if (anyAmbiguous && winner != null) {
            return StabilizedRecognition(winner.key, unsure = true)
        }
        if (anyAmbiguous) {
            // No confident majority yet: keep the last stable identity if one exists,
            // otherwise expose the closest candidate so attendance can flag UNSURE.
            val fallback = stableNames[faceId]?.takeIf { it != "Unknown" }
                ?: displayName.takeIf { it != "Unknown" }
                ?: "Unknown"
            return StabilizedRecognition(fallback, unsure = true)
        }

        return StabilizedRecognition(stableNames[faceId] ?: "Unknown")
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
