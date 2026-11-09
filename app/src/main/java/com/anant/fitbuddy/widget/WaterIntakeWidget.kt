package com.anant.fitbuddy.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.anant.fitbuddy.FitBuddyApp
import com.anant.fitbuddy.R
import com.anant.fitbuddy.data.database.WaterDrinkKind
import com.anant.fitbuddy.util.WaterFillLayers
import kotlinx.coroutines.flow.first

class WaterIntakeWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetRepository.load(context)
        provideContent {
            FitBuddyGlanceTheme {
                WaterIntakeContent(context, rememberWidgetData(context, data))
            }
        }
    }
}

@Composable
private fun WaterIntakeContent(context: Context, data: WidgetData) {
    val density = context.resources.displayMetrics.density
    val size = LocalSize.current
    val layers = remember(data.waterOnlyMl, data.coffeeMl, data.teaMl, data.waterGoalMl) {
        WaterFillLayers(
            waterMl = data.waterOnlyMl,
            coffeeMl = data.coffeeMl,
            teaMl = data.teaMl,
            goalMl = data.waterGoalMl,
        )
    }
    val tubeWidth = 44.dp
    val tubeHeight = (size.height.value - 24f).coerceAtLeast(40f)
    val tube = remember(layers, tubeHeight) {
        val wPx = (tubeWidth.value * density).toInt()
        WaterWaveBitmap.render(
            widthPx = wPx,
            heightPx = (tubeHeight * density).toInt(),
            layers = layers,
            cornerRadiusPx = wPx / 2f,
        )
    }
    val percent = (data.waterProgress * 100).toInt()

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(28.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            provider = ImageProvider(tube),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.width(tubeWidth).fillMaxHeight(),
        )
        Spacer(GlanceModifier.width(12.dp))
        Column(
            modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatVolume(data.waterConsumedMl),
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
            )
            Text(
                text = "$percent% of ${formatVolume(data.waterGoalMl)}",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurfaceVariant,
                    fontSize = 11.sp,
                ),
                maxLines = 1,
            )
            Spacer(GlanceModifier.height(8.dp))
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                AddChip(
                    label = "+${data.waterQuickAddMl}",
                    kind = WaterDrinkKind.WATER,
                    amountMl = data.waterQuickAddMl,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(GlanceModifier.width(6.dp))
                AddChip(
                    label = "+${data.bottleQuickAddMl}",
                    kind = WaterDrinkKind.BOTTLE,
                    amountMl = data.bottleQuickAddMl,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }
}

private fun formatVolume(ml: Int): String =
    if (ml >= 1000) String.format(java.util.Locale.US, "%.1f L", ml / 1000f).replace(".0 L", " L")
    else "$ml ml"

@Composable
private fun AddChip(
    label: String,
    kind: WaterDrinkKind,
    amountMl: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = label,
        style = TextStyle(
            color = GlanceTheme.colors.onPrimaryContainer,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
        maxLines = 1,
        modifier = modifier
            .background(GlanceTheme.colors.primaryContainer)
            .cornerRadius(16.dp)
            .padding(vertical = 8.dp, horizontal = 2.dp)
            .clickable(
                actionRunCallback<AddWaterActionCallback>(
                    actionParametersOf(
                        AddWaterActionCallback.amountKey to amountMl,
                        AddWaterActionCallback.kindKey to kind.name,
                    )
                )
            ),
    )
}

class AddWaterActionCallback : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        val kindName = parameters[kindKey] ?: WaterDrinkKind.WATER.name
        val kind = WaterDrinkKind.fromStored(kindName)
        val app = context.applicationContext as? FitBuddyApp ?: return
        val settings = app.settingsRepository.settings.first()
        val amount = parameters[amountKey] ?: settings.quickAddMl(kind)
        app.repository.addWaterLog(amountMl = amount, drinkKind = kind)
        WidgetRefresh.bump()
        WaterIntakeWidget().update(context, glanceId)
    }

    companion object {
        val amountKey = ActionParameters.Key<Int>("water_amount_ml")
        val kindKey = ActionParameters.Key<String>("water_drink_kind")
    }
}

class WaterIntakeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WaterIntakeWidget()
}
