package com.anant.fitbuddy.util

/**
 * Extracts discrete counts from loose-text food logs (e.g. "4 almonds", "2 rotis and dal",
 * "do roti", "teen paratha") so they can be applied when the AI omits a [quantity] or returns 1
 * by default.
 */
object FoodQuantityParser {

    data class ParsedItem(val quantity: Int, val name: String)

    private val NUMBER_PREFIX = Regex("""(\d+)\s+([a-zA-Z][a-zA-Z\s-]*)""")
    private val NUMBER_SUFFIX = Regex("""([a-zA-Z][a-zA-Z\s-]*?)\s*[x×]\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val HINGLISH_PREFIX = Regex(
        """(?i)^(ek|do|teen|char|paanch|panch|chhe|che|saat|aath|nau|das|piece|pc|pcs)\s+([a-zA-Z][a-zA-Z\s-]*)"""
    )
    private val KATORI_BOWL = Regex(
        """(?i)^(?:(\d+|ek|do|teen|char)\s+)?(katori|bowl)s?\s+(?:of\s+)?([a-zA-Z][a-zA-Z\s-]*)"""
    )
    private val SEGMENT_SPLIT = Regex("""\s+(?:and|with|,|\+|aur)\s+""", RegexOption.IGNORE_CASE)

    private val WORD_COUNTS = mapOf(
        "ek" to 1,
        "do" to 2,
        "teen" to 3,
        "char" to 4,
        "paanch" to 5,
        "panch" to 5,
        "chhe" to 6,
        "che" to 6,
        "saat" to 7,
        "aath" to 8,
        "nau" to 9,
        "das" to 10,
        "piece" to 1,
        "pc" to 1,
        "pcs" to 1
    )

    /** Parses countable segments from free-form food text. */
    fun parseSegments(text: String): List<ParsedItem> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()

        return trimmed.split(SEGMENT_SPLIT)
            .mapNotNull { segment -> parseSegment(segment.trim()) }
    }

    /**
     * Resolves the quantity for an ingredient: prefer an explicit AI [aiQuantity] when > 1,
     * otherwise match the ingredient [name] against counts found in [userText].
     */
    fun quantityForIngredient(userText: String, ingredientName: String, aiQuantity: Int = 1): Int {
        if (aiQuantity > 1) return aiQuantity.coerceIn(1, 999)

        val segments = parseSegments(userText)
        if (segments.isEmpty()) return 1

        val ingNorm = normalize(ingredientName)
        return segments.firstOrNull { namesMatch(ingNorm, normalize(it.name)) }
            ?.quantity
            ?.coerceIn(1, 999)
            ?: 1
    }

    private fun parseSegment(segment: String): ParsedItem? {
        if (segment.isBlank()) return null

        KATORI_BOWL.find(segment)?.let { match ->
            val qtyToken = match.groupValues[1]
            val qty = when {
                qtyToken.isBlank() -> 1
                qtyToken.toIntOrNull() != null -> qtyToken.toInt()
                else -> WORD_COUNTS[qtyToken.lowercase()] ?: 1
            }
            val name = cleanName(match.groupValues[3])
            if (qty in 1..999 && name.isNotBlank()) return ParsedItem(qty, name)
        }

        HINGLISH_PREFIX.find(segment)?.let { match ->
            val qty = WORD_COUNTS[match.groupValues[1].lowercase()] ?: return@let
            val name = cleanName(match.groupValues[2])
            if (qty in 1..999 && name.isNotBlank()) return ParsedItem(qty, name)
        }

        NUMBER_PREFIX.matchEntire(segment)?.let { match ->
            val qty = match.groupValues[1].toIntOrNull() ?: return null
            val name = cleanName(match.groupValues[2])
            if (qty in 1..999 && name.isNotBlank()) return ParsedItem(qty, name)
        }

        NUMBER_PREFIX.find(segment)?.let { match ->
            val qty = match.groupValues[1].toIntOrNull() ?: return null
            val name = cleanName(match.groupValues[2])
            if (qty in 1..999 && name.isNotBlank()) return ParsedItem(qty, name)
        }

        NUMBER_SUFFIX.find(segment)?.let { match ->
            val name = cleanName(match.groupValues[1])
            val qty = match.groupValues[2].toIntOrNull() ?: return null
            if (qty in 1..999 && name.isNotBlank()) return ParsedItem(qty, name)
        }

        return null
    }

    private fun cleanName(raw: String): String =
        raw.trim().trimEnd('.', ',', ';')

    private fun normalize(name: String): String =
        name.lowercase()
            .trim()
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()

    private fun namesMatch(ingredientNorm: String, segmentNorm: String): Boolean {
        if (ingredientNorm.isBlank() || segmentNorm.isBlank()) return false
        if (ingredientNorm == segmentNorm) return true

        val ingStem = stem(ingredientNorm)
        val segStem = stem(segmentNorm)
        if (ingStem == segStem) return true
        if (ingredientNorm.contains(segStem) || segmentNorm.contains(ingStem)) return true

        val ingWords = ingredientNorm.split(' ')
        val segWords = segmentNorm.split(' ')
        return ingWords.any { iw ->
            val iwStem = stem(iw)
            segWords.any { sw -> iwStem == stem(sw) || iwStem.contains(stem(sw)) || stem(sw).contains(iwStem) }
        }
    }

    private fun stem(word: String): String = when {
        word.endsWith("ies") && word.length > 4 -> word.dropLast(3) + "y"
        word.endsWith("es") && word.length > 3 -> word.dropLast(2)
        word.endsWith("s") && word.length > 2 -> word.dropLast(1)
        else -> word
    }
}
