package com.anant.fitbuddy.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressInsightNormalizeTest {

    @Test
    fun leavesCleanResponseAlone() {
        val input = ProgressInsightResponse(
            summary = "You're on track.",
            recommendations = listOf("Keep logging meals.", "Train 3x/week."),
            bodyScore = 72
        )
        assertEquals(input, input.normalized())
    }

    @Test
    fun recoversPythonStyleRecommendationsEmbeddedInSummary() {
        val mangled = ProgressInsightResponse(
            summary = "Consistent daily logging is the immediate priority.','recommendations':[" +
                "'Weigh yourself each morning.','Track every meal for 14 days.'," +
                "'Start resistance training 3-4x/week.']",
            recommendations = emptyList(),
            bodyScore = null
        )
        val out = mangled.normalized()
        assertEquals("Consistent daily logging is the immediate priority.", out.summary)
        assertEquals(
            listOf(
                "Weigh yourself each morning.",
                "Track every meal for 14 days.",
                "Start resistance training 3-4x/week."
            ),
            out.recommendations
        )
        assertNull(out.bodyScore)
    }

    @Test
    fun recoversSpacedPythonStyleEmbedFromNemotronSmash() {
        val mangled = ProgressInsightResponse(
            summary = "Exercise is inconsistent, with only 1–2 high-burn days per week and " +
                "minimal activity otherwise.', 'recommendations': [" +
                "'Raise protein to ~115 g/day.'," +
                "'Eat closer to 2,000 kcal on training days.']",
            recommendations = emptyList(),
            bodyScore = null
        )
        val out = mangled.normalized()
        assertEquals(
            "Exercise is inconsistent, with only 1–2 high-burn days per week and " +
                "minimal activity otherwise.",
            out.summary
        )
        assertEquals(
            listOf(
                "Raise protein to ~115 g/day.",
                "Eat closer to 2,000 kcal on training days."
            ),
            out.recommendations
        )
    }

    @Test
    fun recoversDoubleQuotedEmbedAndBodyScore() {
        val mangled = ProgressInsightResponse(
            summary = "Sparse data so far.\",\"recommendations\":[" +
                "\"Log weight daily.\",\"Hit protein targets.\"],\"body_score\":null}",
            recommendations = emptyList()
        )
        val out = mangled.normalized()
        assertEquals("Sparse data so far.", out.summary)
        assertEquals(listOf("Log weight daily.", "Hit protein targets."), out.recommendations)
        assertNull(out.bodyScore)
    }

    @Test
    fun recoversEmbeddedBodyScoreInt() {
        val mangled = ProgressInsightResponse(
            summary = "Good trend.','recommendations':['Add dal at lunch.'],'body_score':72}",
            recommendations = emptyList()
        )
        val out = mangled.normalized()
        assertEquals("Good trend.", out.summary)
        assertEquals(listOf("Add dal at lunch."), out.recommendations)
        assertEquals(72, out.bodyScore)
    }

    @Test
    fun cleansTruncatedEmbedEvenWhenRecommendationsAlreadyPresent() {
        val mangled = ProgressInsightResponse(
            summary = "Solid recomposition trend.', 'recommendations': [",
            recommendations = listOf("Raise protein to 115 g/day."),
            bodyScore = 72
        )
        val out = mangled.normalized()
        assertEquals("Solid recomposition trend.", out.summary)
        assertEquals(listOf("Raise protein to 115 g/day."), out.recommendations)
        assertEquals(72, out.bodyScore)
    }
}
