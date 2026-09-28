package com.anant.fitbuddy.util

/**
 * Parses GitHub CI release bodies into user-facing "What's new" bullets.
 * Drops headers/metadata and maintenance commit types (chore/ci/build/…).
 */
object ReleaseNotes {

    private val commitHashSuffix = Regex("""\s*\([0-9a-f]{7,40}\)\s*$""")
    private val nonUserFacingPrefix = Regex(
        """^(chore|ci|build|refactor|style|test)(\(|:| )""",
        RegexOption.IGNORE_CASE
    )

    /**
     * @param raw full release markdown body
     * @param limit max bullets to show in the update dialog
     */
    fun highlights(raw: String, limit: Int = 8): List<String> =
        raw.lineSequence()
            .map { it.trim() }
            .mapNotNull { line ->
                when {
                    line.startsWith("- ") -> line.removePrefix("- ")
                    line.startsWith("• ") -> line.removePrefix("• ")
                    else -> null
                }
            }
            .map { line ->
                line.replace(commitHashSuffix, "").trim()
            }
            .filter { it.isNotBlank() }
            .filterNot { nonUserFacingPrefix.containsMatchIn(it) }
            .distinct()
            .take(limit)
            .toList()
}
