package com.anant.fitbuddy.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KebabDining
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Pool
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.Rowing
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.ui.graphics.vector.ImageVector
import com.anant.fitbuddy.data.model.LogIconTopic

/** Maps AI / estimator [LogIconTopic] IDs to Material icons for log / library rows. */
object LogItemIcons {

    fun forFood(topic: String?): ImageVector = when (LogIconTopic.normalizeFood(topic)) {
        LogIconTopic.DRINK -> Icons.Filled.LocalCafe
        LogIconTopic.DESSERT -> Icons.Filled.Cake
        LogIconTopic.BAKERY -> Icons.Filled.BakeryDining
        LogIconTopic.BOWL -> Icons.Filled.RiceBowl
        LogIconTopic.PROTEIN -> Icons.Filled.KebabDining
        LogIconTopic.PRODUCE -> Icons.Filled.SetMeal
        else -> Icons.Filled.Restaurant
    }

    fun forExercise(topic: String?): ImageVector = when (LogIconTopic.normalizeExercise(topic)) {
        LogIconTopic.RUN -> Icons.AutoMirrored.Filled.DirectionsRun
        LogIconTopic.WALK -> Icons.AutoMirrored.Filled.DirectionsWalk
        LogIconTopic.CYCLE -> Icons.AutoMirrored.Filled.DirectionsBike
        LogIconTopic.SWIM -> Icons.Filled.Pool
        LogIconTopic.MIND_BODY -> Icons.Filled.SelfImprovement
        LogIconTopic.SPORT -> Icons.Filled.SportsSoccer
        LogIconTopic.CARDIO -> Icons.Filled.LocalFireDepartment
        LogIconTopic.CHEST -> Icons.Filled.FitnessCenter
        LogIconTopic.BACK -> Icons.Filled.Rowing
        LogIconTopic.LEGS -> Icons.AutoMirrored.Filled.DirectionsWalk
        LogIconTopic.ARMS -> Icons.Filled.SportsMma
        LogIconTopic.SHOULDERS -> Icons.Filled.AccessibilityNew
        LogIconTopic.CORE -> Icons.Filled.SportsGymnastics
        else -> Icons.Filled.FitnessCenter
    }
}
