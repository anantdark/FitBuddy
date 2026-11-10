package com.anant.fitbuddy.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.anant.fitbuddy.MainActivity
import com.anant.fitbuddy.R
import kotlin.math.abs

class CalorieOrbWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetRepository.load(context)
        provideContent {
            FitBuddyGlanceTheme {
                CalorieOrbContent(context, rememberWidgetData(context, data))
            }
        }
    }
}

@Composable
private fun CalorieOrbContent(context: Context, data: WidgetData) {
    val action = WidgetIntents.openLogAction(context, MainActivity.LOG_ACTION_HUB)
    val over = data.isOverTarget
    val accent = if (over) GlanceTheme.colors.error else GlanceTheme.colors.primary
    val trackArgb = GlanceTheme.colors.surfaceVariant.getColor(context).toArgb()
    val accentArgb = accent.getColor(context).toArgb()
    val density = context.resources.displayMetrics.density
    val compact = LocalSize.current.height < 130.dp
    val bigSp = if (compact) 18.sp else 24.sp
    val smallSp = if (compact) 9.sp else 11.sp
    val ring = remember(data.calorieProgress, trackArgb, accentArgb) {
        CalorieRingBitmap.render(
            sizePx = (160f * density).toInt(),
            progress = data.calorieProgress,
            trackColor = trackArgb,
            progressColor = accentArgb,
        )
    }

    if (compact) {
        CompactOrb(context, data, ring, accent, over, action)
        return
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 10.dp)
            .clickable(action),
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
        Box(
            modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(ring),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.fillMaxSize(),
            )
            Column(
                modifier = GlanceModifier.padding(top = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = abs(data.remainingCalories).toString(),
                    style = TextStyle(
                        color = accent,
                        fontSize = bigSp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    ),
                )
                Text(
                    text = if (over) {
                        context.getString(R.string.widget_calorie_over)
                    } else {
                        context.getString(R.string.widget_calorie_orb_preview_left)
                    },
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurface,
                        fontSize = smallSp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
        }
        Spacer(GlanceModifier.height(4.dp))
        Row(modifier = GlanceModifier.fillMaxWidth()) {
            Text(
                text = context.getString(R.string.widget_calorie_ate, data.consumedCalories),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 10.sp,
                ),
                modifier = GlanceModifier.defaultWeight(),
            )
            Text(
                text = context.getString(R.string.widget_calorie_burn, data.burnedCalories),
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

@Composable
private fun CompactOrb(
    context: Context,
    data: WidgetData,
    ring: android.graphics.Bitmap,
    accent: androidx.glance.unit.ColorProvider,
    over: Boolean,
    action: androidx.glance.action.Action,
) {
    val ringSize = (LocalSize.current.height - 20.dp).coerceAtLeast(48.dp)
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(10.dp)
            .clickable(action),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = GlanceModifier.size(ringSize), contentAlignment = Alignment.Center) {
            Image(
                provider = ImageProvider(ring),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.fillMaxSize(),
            )
            Text(
                text = abs(data.remainingCalories).toString(),
                style = TextStyle(
                    color = accent,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                ),
                maxLines = 1,
            )
        }
        Spacer(GlanceModifier.width(10.dp))
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                text = context.getString(R.string.widget_calorie_orb_today),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                maxLines = 1,
            )
            Text(
                text = if (over) {
                    context.getString(R.string.widget_calorie_over)
                } else {
                    context.getString(R.string.widget_calorie_orb_preview_left)
                },
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                text = context.getString(R.string.widget_calorie_ate, data.consumedCalories),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                maxLines = 1,
            )
            Text(
                text = context.getString(R.string.widget_calorie_burn, data.burnedCalories),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp),
                maxLines = 1,
            )
        }
    }
}

class CalorieOrbWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = CalorieOrbWidget()
}
