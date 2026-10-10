package com.anant.fitbuddy.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import com.anant.fitbuddy.data.model.LogIconTopic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LogItemIconsTest {

    @Test
    fun `food topics map to distinct icons`() {
        assertEquals(Icons.Filled.LocalCafe, LogItemIcons.forFood(LogIconTopic.DRINK))
        assertEquals(Icons.Filled.Restaurant, LogItemIcons.forFood(LogIconTopic.MEAL))
        assertEquals(Icons.Filled.Restaurant, LogItemIcons.forFood(null))
        assertEquals(Icons.Filled.Restaurant, LogItemIcons.forFood("not-a-topic"))
    }

    @Test
    fun `exercise topics map to distinct icons`() {
        assertEquals(
            Icons.AutoMirrored.Filled.DirectionsRun,
            LogItemIcons.forExercise(LogIconTopic.RUN)
        )
        assertEquals(
            Icons.Filled.SelfImprovement,
            LogItemIcons.forExercise(LogIconTopic.MIND_BODY)
        )
        assertEquals(Icons.Filled.FitnessCenter, LogItemIcons.forExercise(LogIconTopic.CHEST))
        assertEquals(Icons.Filled.FitnessCenter, LogItemIcons.forExercise(null))
    }

    @Test
    fun `different topics are not the same glyph`() {
        assertNotEquals(
            LogItemIcons.forFood(LogIconTopic.DRINK),
            LogItemIcons.forFood(LogIconTopic.BOWL)
        )
        assertNotEquals(
            LogItemIcons.forExercise(LogIconTopic.RUN),
            LogItemIcons.forExercise(LogIconTopic.STRENGTH)
        )
        assertNotEquals(
            LogItemIcons.forExercise(LogIconTopic.BACK),
            LogItemIcons.forExercise(LogIconTopic.ARMS)
        )
    }
}
