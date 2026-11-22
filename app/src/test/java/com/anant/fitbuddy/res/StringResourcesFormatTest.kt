package com.anant.fitbuddy.res

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.util.regex.Pattern

/**
 * Guards against [java.util.UnknownFormatConversionException] crashes from Android
 * string resources (e.g. literal `Body fat (%)` passed through [String.format] /
 * `getString(id, …)`).
 *
 * Every `%` in a format-eligible string must be either `%%` or a valid
 * [Formatter] conversion. Strings that intentionally keep a bare `%` must set
 * `formatted="false"` and must never be called with format args.
 */
class StringResourcesFormatTest {

    @Test
    fun allStringResources_haveValidFormatSpecifiersOrAreMarkedUnformatted() {
        val roots = listOf(
            File("src/main/res"),
            File("src/debug/res"),
        )
        val files = roots
            .filter { it.isDirectory }
            .flatMap { it.walkTopDown().filter { f -> f.name == "strings.xml" }.toList() }
        assertTrue("Expected at least values/strings.xml", files.isNotEmpty())

        val failures = mutableListOf<String>()
        for (file in files) {
            for (entry in parseStrings(file.readText())) {
                if (entry.formattedFalse) continue
                val issue = validateFormatPattern(entry.value)
                if (issue != null) {
                    failures += "${file.path} :: ${entry.name}: $issue — \"${entry.value}\""
                }
            }
        }

        if (failures.isNotEmpty()) {
            fail(
                "Invalid Android format strings (escape literal %% or set formatted=\"false\"):\n" +
                    failures.joinToString("\n"),
            )
        }
    }

    @Test
    fun formattedFalseStrings_areNeverCalledWithFormatArgs() {
        val kotlinRoots = listOf(File("src/main/java"), File("src/debug/java"))
        val formattedFalseNames = mutableSetOf<String>()
        File("src/main/res").walkTopDown()
            .filter { it.name == "strings.xml" }
            .forEach { file ->
                parseStrings(file.readText())
                    .filter { it.formattedFalse }
                    .forEach { formattedFalseNames += it.name }
            }
        if (formattedFalseNames.isEmpty()) return

        // Trailing comma after the id still matches; Kotlin trailing-comma-only
        // calls are rare for formatted=false strings and are treated as suspicious.
        val callPattern = Pattern.compile(
            """(?:stringResource|getString)\(\s*R\.string\.(\w+)\s*,""",
        )
        val failures = mutableListOf<String>()
        for (root in kotlinRoots.filter { it.isDirectory }) {
            root.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                val matcher = callPattern.matcher(file.readText())
                while (matcher.find()) {
                    val name = matcher.group(1) ?: continue
                    if (name in formattedFalseNames) {
                        failures += "${file.path}: R.string.$name is formatted=\"false\" but called with format args"
                    }
                }
            }
        }
        if (failures.isNotEmpty()) {
            fail(failures.joinToString("\n"))
        }
    }

    private data class StringEntry(
        val name: String,
        val value: String,
        val formattedFalse: Boolean,
    )

    private fun parseStrings(xml: String): List<StringEntry> {
        val out = mutableListOf<StringEntry>()
        val stringTag = Pattern.compile("""<string\s+([^>]+)>(.*?)</string>""", Pattern.DOTALL)
        val itemTag = Pattern.compile("""<item\s+([^>]+)>(.*?)</item>""", Pattern.DOTALL)
        val nameAttr = Pattern.compile("""name="([^"]+)"""")
        val formattedAttr = Pattern.compile("""formatted="([^"]+)"""")
        val quantityAttr = Pattern.compile("""quantity="([^"]+)"""")

        val stringMatcher = stringTag.matcher(xml)
        while (stringMatcher.find()) {
            val attrs = stringMatcher.group(1) ?: continue
            val nameMatcher = nameAttr.matcher(attrs)
            if (!nameMatcher.find()) continue
            val name = nameMatcher.group(1) ?: continue
            val value = stringMatcher.group(2) ?: continue
            val formattedMatcher = formattedAttr.matcher(attrs)
            val formattedFalse = formattedMatcher.find() && formattedMatcher.group(1) == "false"
            out += StringEntry(name, unescapeXml(value), formattedFalse)
        }

        val itemMatcher = itemTag.matcher(xml)
        while (itemMatcher.find()) {
            val attrs = itemMatcher.group(1) ?: continue
            val quantityMatcher = quantityAttr.matcher(attrs)
            if (!quantityMatcher.find()) continue
            val quantity = quantityMatcher.group(1) ?: continue
            val value = itemMatcher.group(2) ?: continue
            out += StringEntry("plurals[$quantity]", unescapeXml(value), formattedFalse = false)
        }
        return out
    }

    private fun unescapeXml(raw: String): String =
        raw
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("\\n", "\n")
            .replace("\\'", "'")
            .replace("\\\"", "\"")

    /**
     * Returns an error message if [pattern] would throw UnknownFormatConversionException
     * when formatted, or null if every `%` is `%%` or a valid conversion.
     */
    private fun validateFormatPattern(pattern: String): String? {
        var i = 0
        while (i < pattern.length) {
            if (pattern[i] != '%') {
                i++
                continue
            }
            if (i + 1 < pattern.length && pattern[i + 1] == '%') {
                i += 2
                continue
            }
            var j = i + 1
            if (j < pattern.length && pattern[j].isDigit()) {
                var k = j
                while (k < pattern.length && pattern[k].isDigit()) k++
                if (k < pattern.length && pattern[k] == '$') {
                    j = k + 1
                }
            }
            while (j < pattern.length && pattern[j] in "-#+ 0,(") j++
            while (j < pattern.length && pattern[j].isDigit()) j++
            if (j < pattern.length && pattern[j] == '.') {
                j++
                if (j < pattern.length && pattern[j] == '*') {
                    j++
                } else {
                    while (j < pattern.length && pattern[j].isDigit()) j++
                }
            }
            if (j >= pattern.length) {
                return "incomplete format specifier at end of string"
            }
            val conv = pattern[j]
            if (conv == 't' || conv == 'T') {
                if (j + 1 >= pattern.length) {
                    return "incomplete date/time conversion %$conv"
                }
                i = j + 2
                continue
            }
            if (conv !in VALID_CONVERSIONS) {
                return "UnknownFormatConversionException Conversion = '$conv'"
            }
            i = j + 1
        }
        return null
    }

    companion object {
        private val VALID_CONVERSIONS = setOf(
            'b', 'B', 'h', 'H', 's', 'S', 'c', 'C',
            'd', 'o', 'x', 'X', 'e', 'E', 'f', 'g', 'G', 'a', 'A', 'n',
        )
    }
}
