package com.anant.fitbuddy.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
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
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = WidgetRepository.load(context)
        provideContent {
            FitBuddyGlanceTheme {
                WaterIntakeContent(context, data)
            }
        }
    }
}

@Composable
private fun WaterIntakeContent(context: Context, data: WidgetData) {
    val density = context.resources.displayMetrics.density
    val layers = remember(data.waterOnlyMl, data.coffeeMl, data.teaMl, data.waterGoalMl) {
        WaterFillLayers(
            waterMl = data.waterOnlyMl,
            coffeeMl = data.coffeeMl,
            teaMl = data.teaMl,
            goalMl = data.waterGoalMl,
        )
    }
    val waveBitmap = remember(layers) {
        WaterWaveBitmap.render(
            widthPx = (180f * density).toInt().coerceAtLeast(160),
            heightPx = (180f * density).toInt().coerceAtLeast(160),
            layers = layers,
        )
    }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(18.dp),
    ) {
        Image(
            provider = ImageProvider(waveBitmap),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize(),
        )
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(GlanceModifier.defaultWeight())
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                TransparentAddButton(
                    label = "+${data.waterQuickAddMl}",
                    kind = WaterDrinkKind.WATER,
                    amountMl = data.waterQuickAddMl,
                    modifier = GlanceModifier.defaultWeight(),
                )
                Spacer(GlanceModifier.width(8.dp))
                TransparentAddButton(
                    label = "+${data.bottleQuickAddMl}",
                    kind = WaterDrinkKind.BOTTLE,
                    amountMl = data.bottleQuickAddMl,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }
    }
}

@Composable
private fun TransparentAddButton(
    label: String,
    kind: WaterDrinkKind,
    amountMl: Int,
    modifier: GlanceModifier = GlanceModifier,
) {
    Text(
        text = label,
        style = TextStyle(
            color = ColorProvider(day = TitleDay, night = TitleNight),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier
            .cornerRadius(14.dp)
            .background(ImageProvider(R.drawable.widget_transparent_chip))
            .padding(vertical = 10.dp, horizontal = 4.dp)
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

private val TitleDay = Color(0xFF0D47A1)
private val TitleNight = Color(0xFFE3F2FD)
