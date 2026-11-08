package com.anant.fitbuddy.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.anant.fitbuddy.MainActivity
import com.anant.fitbuddy.R

class ActionStripWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            FitBuddyGlanceTheme {
                ActionStripContent(context)
            }
        }
    }
}

@Composable
private fun ActionStripContent(context: Context) {
    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ActionCell(
            context = context,
            iconRes = R.drawable.ic_widget_camera,
            label = context.getString(R.string.widget_action_photo),
            action = MainActivity.LOG_ACTION_CAMERA,
            tint = GlanceTheme.colors.primary,
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
        )
        Spacer(GlanceModifier.width(4.dp))
        ActionCell(
            context = context,
            iconRes = R.drawable.ic_widget_keyboard,
            label = context.getString(R.string.widget_action_text),
            action = MainActivity.LOG_ACTION_TEXT,
            tint = GlanceTheme.colors.tertiary,
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
        )
        Spacer(GlanceModifier.width(4.dp))
        ActionCell(
            context = context,
            iconRes = R.drawable.ic_widget_bookmark,
            label = context.getString(R.string.widget_action_saved),
            action = MainActivity.LOG_ACTION_PRESETS,
            tint = GlanceTheme.colors.secondary,
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
        )
        Spacer(GlanceModifier.width(4.dp))
        ActionCell(
            context = context,
            iconRes = R.drawable.ic_widget_barcode,
            label = context.getString(R.string.widget_action_barcode),
            action = MainActivity.LOG_ACTION_BARCODE,
            tint = GlanceTheme.colors.onSurfaceVariant,
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
        )
    }
}

@Composable
private fun ActionCell(
    context: Context,
    iconRes: Int,
    label: String,
    action: String,
    tint: ColorProvider,
    modifier: GlanceModifier,
) {
    Column(
        modifier = modifier
            .background(GlanceTheme.colors.surfaceVariant)
            .cornerRadius(12.dp)
            .clickable(WidgetIntents.openLogAction(context, action))
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(iconRes),
            contentDescription = label,
            colorFilter = ColorFilter.tint(tint),
            modifier = GlanceModifier.size(22.dp),
        )
        Spacer(GlanceModifier.height(2.dp))
        Text(
            text = label,
            style = TextStyle(
                color = GlanceTheme.colors.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}

class ActionStripWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ActionStripWidget()
}
