package com.anant.fitbuddy.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.anant.fitbuddy.MainActivity
import com.anant.fitbuddy.R
import kotlin.math.abs

class CalorieOrbWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetRepository.load(context)
        provideContent {
            FitBuddyGlanceTheme {
                CalorieOrbContent(context, data)
            }
        }
    }
}

@Composable
private fun CalorieOrbContent(context: Context, data: WidgetData) {
    val action = WidgetIntents.openLogAction(context, MainActivity.LOG_ACTION_HUB)
    val remainingLabel = if (data.isOverTarget) "over" else "left"
    val remainingValue = abs(data.remainingCalories)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .clickable(action)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = context.getString(R.string.widget_calorie_orb_today),
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            ),
        )
        Text(
            text = remainingValue.toString(),
            style = TextStyle(
                color = if (data.isOverTarget) {
                    GlanceTheme.colors.error
                } else {
                    GlanceTheme.colors.primary
                },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
        Text(
            text = "kcal $remainingLabel",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(GlanceModifier.height(6.dp))
        LinearProgressIndicator(
            progress = data.calorieProgress,
            modifier = GlanceModifier.fillMaxWidth().height(4.dp),
            color = if (data.isOverTarget) GlanceTheme.colors.error else GlanceTheme.colors.primary,
            backgroundColor = GlanceTheme.colors.surfaceVariant,
        )
        Spacer(GlanceModifier.height(4.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = "Ate ${data.consumedCalories}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = "Burn ${data.burnedCalories}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                    textAlign = TextAlign.End,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
        }
    }
}

class CalorieOrbWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalorieOrbWidget()
}
