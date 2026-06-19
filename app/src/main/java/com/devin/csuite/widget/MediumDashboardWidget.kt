package com.devin.csuite.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.devin.csuite.R

class MediumDashboardWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = context.getWidgetData()
        provideContent {
            GlanceTheme {
                MediumWidgetContent(data)
            }
        }
    }
}

@Composable
private fun MediumWidgetContent(data: WidgetData) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(R.color.widget_background)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Devin Dashboard",
                style = TextStyle(
                    fontSize = 12.sp
                )
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KpiColumn(
                    value = "${String.format("%.0f", data.acuPercent)}%",
                    label = "ACU Usage",
                    modifier = GlanceModifier.defaultWeight()
                )
                KpiColumn(
                    value = "${data.activeSessions}",
                    label = "Sessions",
                    modifier = GlanceModifier.defaultWeight()
                )
                KpiColumn(
                    value = "${data.mau}",
                    label = "MAU",
                    modifier = GlanceModifier.defaultWeight()
                )
            }
        }
    }
}

@Composable
private fun KpiColumn(
    value: String,
    label: String,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = label,
            style = TextStyle(
                fontSize = 11.sp
            )
        )
    }
}

class MediumDashboardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MediumDashboardWidget()
}
