package com.anant.fitbuddy.data.model

/**
 * Some models smash the JSON schema into the summary string, e.g.
 * `…priority.','recommendations':['Do X.','Do Y.']`. Recover a clean summary + list so the UI
 * can show recommendations as separate bullets.
 *
 * Never throws: bad recovery input returns [this] unchanged.
 */
fun ProgressInsightResponse.normalized(): ProgressInsightResponse =
    runCatching {
        val recovered = recoverEmbeddedRecommendations(summary) ?: return this
        copy(
            summary = recovered.first,
            recommendations = recommendations.ifEmpty { recovered.second },
            bodyScore = bodyScore ?: recovered.third
        )
    }.getOrDefault(this)

/** @return Triple(cleanSummary, recommendations, bodyScore?) or null if nothing embedded. */
private fun recoverEmbeddedRecommendations(
    summary: String
): Triple<String, List<String>, Int?>? {
    val marker = findRecommendationsMarker(summary) ?: return null
    val cleanSummary = summary.substring(0, marker).trim()
        .trimEnd(',', ' ', '\'', '"')
        .trim()
    if (cleanSummary.isEmpty()) return null

    var rest = summary.substring(marker)
    val bracket = rest.indexOf('[')
    if (bracket < 0) return null
    rest = rest.substring(bracket + 1)

    var bodyScore: Int? = null
    val scoreIdx = indexOfBodyScoreField(rest)
    if (scoreIdx != null) {
        val afterColon = rest.substring(scoreIdx)
        val value = afterColon.substringBefore(',').substringBefore('}').trim()
            .trim('\'', '"', ' ')
        bodyScore = value.takeIf { it != "null" }?.toIntOrNull()
        rest = rest.substring(0, scoreIdx).trimEnd(',', ' ', '\'', '"')
    }
    val listEnd = rest.lastIndexOf(']')
    val listBody = (if (listEnd >= 0) rest.substring(0, listEnd) else rest).trim()
    val embeddedRecs = splitEmbeddedStringList(listBody)
    // Always return a cleaned summary once the smash marker is found — models often leave a
    // truncated `,'recommendations':[` tail in summary while still filling the real array field.
    if (cleanSummary == summary.trim()) return null
    return Triple(cleanSummary, embeddedRecs, bodyScore)
}

/**
 * Index of the start of an embedded `,'recommendations':[` / `,"recommendations":[` fragment,
 * or null. Plain [String] scans only — Android's [Regex] rejects some patterns that pass on the JVM
 * (notably `}?`), and that used to crash insight generation with a blank error.
 */
private fun findRecommendationsMarker(summary: String): Int? {
    val lower = summary.lowercase()
    val key = "recommendations"
    var from = 0
    while (true) {
        val keyAt = lower.indexOf(key, from)
        if (keyAt < 0) return null
        var i = keyAt - 1
        while (i >= 0 && lower[i].isWhitespace()) i--
        if (i >= 0 && (lower[i] == '\'' || lower[i] == '"')) i--
        while (i >= 0 && lower[i].isWhitespace()) i--
        val commaAt = i
        if (commaAt >= 0 && lower[commaAt] == ',') {
            var j = keyAt + key.length
            if (j < lower.length && (lower[j] == '\'' || lower[j] == '"')) j++
            while (j < lower.length && lower[j].isWhitespace()) j++
            if (j < lower.length && lower[j] == ':') {
                j++
                while (j < lower.length && lower[j].isWhitespace()) j++
                if (j < lower.length && lower[j] == '[') {
                    var start = commaAt
                    if (start > 0 && (summary[start - 1] == '\'' || summary[start - 1] == '"')) {
                        start--
                    }
                    return start
                }
            }
        }
        from = keyAt + key.length
    }
}

/** Index of `,body_score:` / `"body_score":` inside [rest], or null. */
private fun indexOfBodyScoreField(rest: String): Int? {
    val lower = rest.lowercase()
    val key = "body_score"
    var from = 0
    while (true) {
        val keyAt = lower.indexOf(key, from)
        if (keyAt < 0) return null
        var j = keyAt + key.length
        if (j < lower.length && (lower[j] == '\'' || lower[j] == '"')) j++
        while (j < lower.length && lower[j].isWhitespace()) j++
        if (j < lower.length && lower[j] == ':') {
            // Prefer cuts that look like a trailing field (comma or quote before the key).
            var i = keyAt - 1
            while (i >= 0 && lower[i].isWhitespace()) i--
            if (i >= 0 && (lower[i] == '\'' || lower[i] == '"')) i--
            while (i >= 0 && lower[i].isWhitespace()) i--
            if (i < 0 || lower[i] == ',' || lower[i] == '[') {
                return j + 1
            }
        }
        from = keyAt + key.length
    }
}

/** Splits `'a','b'` / `"a","b"` list bodies from mangled model output. */
private fun splitEmbeddedStringList(body: String): List<String> {
    if (body.isBlank()) return emptyList()
    val out = ArrayList<String>()
    var i = 0
    while (i < body.length) {
        while (i < body.length && body[i] in " \t\n\r,[") i++
        if (i >= body.length) break
        val quote = body[i]
        if (quote == '\'' || quote == '"') {
            i++
            val start = i
            while (i < body.length && body[i] != quote) i++
            out.add(body.substring(start, i).trim())
            if (i < body.length) i++
        } else {
            val start = i
            while (i < body.length && body[i] != ',') i++
            val piece = body.substring(start, i).trim().trimEnd(']')
            if (piece.isNotBlank()) out.add(piece)
        }
        while (i < body.length && body[i] != ',') i++
        if (i < body.length && body[i] == ',') i++
    }
    return out.filter { it.isNotBlank() }
}
