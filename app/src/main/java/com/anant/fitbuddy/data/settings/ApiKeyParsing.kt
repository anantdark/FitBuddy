package com.anant.fitbuddy.data.settings

/**
 * Parses pasted/stored API keys. Accepts newlines and commas; trims; drops blanks; de-dupes
 * while preserving first-seen order.
 */
fun parseApiKeys(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    val seen = LinkedHashSet<String>()
    raw.split('\n', ',', ';')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .forEach { seen.add(it) }
    return seen.toList()
}

fun joinApiKeys(keys: List<String>): String =
    keys.map { it.trim() }.filter { it.isNotEmpty() }.distinct().joinToString("\n")
