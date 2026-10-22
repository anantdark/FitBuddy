package com.anant.fitbuddy.data.model

/**
 * Fuzzy name matching for attaching catalog media (ExerciseDB / GymGifs) to custom exercises.
 * Prefers exact normalized matches; falls back to high token-overlap / containment scores.
 */
object ExerciseNameMatcher {

    private val ALIASES = mapOf(
        "db" to "dumbbell",
        "bb" to "barbell",
        "kb" to "kettlebell",
        "bw" to "bodyweight",
        "pu" to "push up",
        "pushup" to "push up",
        "pullup" to "pull up",
        "chinup" to "chin up",
        "situp" to "sit up",
        "lsit" to "l sit",
        "vsit" to "v sit"
    )

    private val STOP = setOf("the", "a", "an", "and", "with", "on", "of", "to", "for", "male", "female")

    fun normalize(raw: String): String {
        var s = raw.trim().lowercase()
            .replace(Regex("[^a-z0-9\\s+]"), " ")
            .replace('+', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
        val tokens = s.split(' ').map { token ->
            ALIASES[token] ?: token
        }
        return tokens.joinToString(" ").replace(Regex("\\s+"), " ").trim()
    }

    fun tokens(raw: String): Set<String> =
        normalize(raw).split(' ').filter { it.isNotBlank() && it !in STOP }.toSet()

    /**
     * Best catalog hit for [query], or null if nothing is confidently similar.
     * [minScore] default 0.72 avoids attaching a wrong GIF to loosely related names.
     */
    fun <T> findBest(
        query: String,
        candidates: Collection<T>,
        nameOf: (T) -> String,
        minScore: Double = 0.72
    ): T? {
        val qNorm = normalize(query)
        if (qNorm.isEmpty() || candidates.isEmpty()) return null

        var exact: T? = null
        var best: T? = null
        var bestScore = 0.0
        val qTokens = tokens(query)

        for (candidate in candidates) {
            val name = nameOf(candidate)
            val nNorm = normalize(name)
            if (nNorm == qNorm) {
                exact = candidate
                break
            }
            val score = similarity(qNorm, qTokens, nNorm, tokens(name))
            if (score > bestScore) {
                bestScore = score
                best = candidate
            }
        }
        if (exact != null) return exact
        return best?.takeIf { bestScore >= minScore }
    }

    private fun similarity(
        qNorm: String,
        qTokens: Set<String>,
        nNorm: String,
        nTokens: Set<String>
    ): Double {
        if (qNorm.isEmpty() || nNorm.isEmpty()) return 0.0
        if (qNorm == nNorm) return 1.0
        // One contains the other (e.g. "plank" vs "weighted front plank")
        if (qNorm in nNorm || nNorm in qNorm) {
            val shorter = minOf(qNorm.length, nNorm.length).toDouble()
            val longer = maxOf(qNorm.length, nNorm.length).toDouble()
            return (0.85 * (shorter / longer)).coerceAtMost(0.95)
        }
        if (qTokens.isEmpty() || nTokens.isEmpty()) return 0.0
        val inter = qTokens.intersect(nTokens).size.toDouble()
        val union = qTokens.union(nTokens).size.toDouble()
        val jaccard = inter / union
        // Require at least half the query tokens to appear in the candidate.
        val coverage = inter / qTokens.size
        return (0.55 * jaccard + 0.45 * coverage)
    }
}
