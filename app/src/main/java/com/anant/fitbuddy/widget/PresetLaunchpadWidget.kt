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
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.anant.fitbuddy.MainActivity
import com.anant.fitbuddy.R

class PresetLaunchpadWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetRepository.load(context)
        provideContent {
            FitBuddyGlanceTheme {
                PresetLaunchpadContent(context, data)
            }
        }
    }
}

@Composable
private fun PresetLaunchpadContent(context: Context, data: WidgetData) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(20.dp)
            .padding(8.dp),
    ) {
        Text(
            text = context.getString(R.string.widget_preset_launchpad_title),
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(6.dp))
        if (data.presets.isEmpty()) {
            val openPresets = WidgetIntents.openLogAction(context, MainActivity.LOG_ACTION_PRESETS)
            Column(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .defaultWeight()
                    .background(GlanceTheme.colors.surfaceVariant)
                    .cornerRadius(12.dp)
                    .clickable(openPresets)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = context.getString(R.string.widget_preset_launchpad_empty),
                    style = TextStyle(
                        color = GlanceTheme.colors.onSurfaceVariant,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    ),
                    maxLines = 2,
                )
            }
        } else {
            val chips = data.presets.take(4)
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                chips.getOrNull(0)?.let { chip ->
                    PresetChip(context, chip, GlanceModifier.defaultWeight().fillMaxSize(), tonal = 0)
                }
                if (chips.size > 1) {
                    Spacer(GlanceModifier.width(6.dp))
                    chips.getOrNull(1)?.let { chip ->
                        PresetChip(context, chip, GlanceModifier.defaultWeight().fillMaxSize(), tonal = 1)
                    }
                }
            }
            if (chips.size > 2) {
                Spacer(GlanceModifier.height(6.dp))
                Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                    chips.getOrNull(2)?.let { chip ->
                        PresetChip(context, chip, GlanceModifier.defaultWeight().fillMaxSize(), tonal = 2)
                    }
                    if (chips.size > 3) {
                        Spacer(GlanceModifier.width(6.dp))
                        chips.getOrNull(3)?.let { chip ->
                            PresetChip(context, chip, GlanceModifier.defaultWeight().fillMaxSize(), tonal = 0)
                        }
                    } else {
                        Spacer(GlanceModifier.defaultWeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    context: Context,
    chip: WidgetPresetChip,
    modifier: GlanceModifier,
    tonal: Int,
) {
    val action = WidgetIntents.openLogAction(
        context = context,
        action = MainActivity.LOG_ACTION_PRESETS,
        presetId = chip.id,
        presetKind = chip.kind,
    )
    val bg = when (tonal % 3) {
        1 -> GlanceTheme.colors.secondaryContainer
        2 -> GlanceTheme.colors.tertiaryContainer
        else -> GlanceTheme.colors.surfaceVariant
    }
    val fg = when (tonal % 3) {
        1 -> GlanceTheme.colors.onSecondaryContainer
        2 -> GlanceTheme.colors.onTertiaryContainer
        else -> GlanceTheme.colors.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .background(bg)
            .cornerRadius(12.dp)
            .clickable(action)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = chip.name,
            style = TextStyle(
                color = fg,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
            maxLines = 2,
        )
        Text(
            text = "${chip.calories} kcal",
            style = TextStyle(
                color = fg,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
            ),
            maxLines = 1,
        )
    }
}

class PresetLaunchpadWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PresetLaunchpadWidget()
}
