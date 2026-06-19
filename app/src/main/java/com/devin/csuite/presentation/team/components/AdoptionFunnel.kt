package com.devin.csuite.presentation.team.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devin.csuite.presentation.team.FunnelData
import com.devin.csuite.presentation.theme.AccentPrimary
import com.devin.csuite.presentation.theme.AccentSecondary
import com.devin.csuite.presentation.theme.AccentTertiary
import com.devin.csuite.presentation.theme.SuccessGreen

@Composable
fun AdoptionFunnel(
    data: FunnelData,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }

    val textColor = MaterialTheme.colorScheme.onSurface
    val subtextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Adoption Funnel",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(16.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(horizontal = 8.dp)
        ) {
            val progress = animationProgress.value
            val canvasWidth = size.width
            val canvasHeight = size.height

            val stages = listOf(
                FunnelStage("Total Users", data.totalUsers, AccentPrimary),
                FunnelStage("MAU", data.mau, AccentSecondary),
                FunnelStage("WAU", data.wau, AccentTertiary),
                FunnelStage("DAU", data.dau, SuccessGreen)
            )

            val maxValue = stages.maxOfOrNull { it.value }?.coerceAtLeast(1) ?: 1
            val stageHeight = canvasHeight / stages.size
            val horizontalPadding = canvasWidth * 0.05f

            stages.forEachIndexed { index, stage ->
                val widthFraction = if (maxValue > 0) {
                    (stage.value.toFloat() / maxValue).coerceIn(0.2f, 1f)
                } else 0.2f

                val animatedWidth = widthFraction * progress
                val nextWidthFraction = if (index < stages.size - 1) {
                    val nextValue = stages[index + 1].value
                    if (maxValue > 0) (nextValue.toFloat() / maxValue).coerceIn(0.2f, 1f)
                    else 0.2f
                } else {
                    widthFraction * 0.8f
                }
                val animatedNextWidth = nextWidthFraction * progress

                val availableWidth = canvasWidth - 2 * horizontalPadding
                val topWidth = animatedWidth * availableWidth
                val bottomWidth = animatedNextWidth * availableWidth
                val yTop = index * stageHeight
                val yBottom = yTop + stageHeight

                val topLeft = (canvasWidth - topWidth) / 2
                val bottomLeft = (canvasWidth - bottomWidth) / 2

                val path = Path().apply {
                    moveTo(topLeft, yTop)
                    lineTo(topLeft + topWidth, yTop)
                    lineTo(bottomLeft + bottomWidth, yBottom)
                    lineTo(bottomLeft, yBottom)
                    close()
                }
                drawPath(path, stage.color.copy(alpha = 0.7f + 0.3f * progress))

                val conversionText = if (index > 0 && stages[index - 1].value > 0) {
                    val rate = (stage.value.toFloat() / stages[index - 1].value * 100)
                    "${String.format("%.0f", rate)}%"
                } else ""

                drawFunnelLabel(
                    textMeasurer = textMeasurer,
                    label = stage.label,
                    value = formatNumber(stage.value),
                    conversion = conversionText,
                    centerY = yTop + stageHeight / 2,
                    canvasWidth = canvasWidth,
                    textColor = textColor,
                    subtextColor = subtextColor,
                    progress = progress
                )
            }
        }
    }
}

private data class FunnelStage(
    val label: String,
    val value: Int,
    val color: Color
)

private fun DrawScope.drawFunnelLabel(
    textMeasurer: TextMeasurer,
    label: String,
    value: String,
    conversion: String,
    centerY: Float,
    canvasWidth: Float,
    textColor: Color,
    subtextColor: Color,
    progress: Float
) {
    val labelStyle = TextStyle(fontSize = 12.sp, color = textColor.copy(alpha = progress))
    val valueStyle = TextStyle(fontSize = 16.sp, color = textColor.copy(alpha = progress))
    val conversionStyle = TextStyle(fontSize = 10.sp, color = subtextColor.copy(alpha = progress))

    val valueLayout = textMeasurer.measure(value, valueStyle)
    val labelLayout = textMeasurer.measure(label, labelStyle)

    val totalHeight = valueLayout.size.height + labelLayout.size.height + 2
    val startY = centerY - totalHeight / 2

    drawText(
        textLayoutResult = valueLayout,
        topLeft = Offset(
            (canvasWidth - valueLayout.size.width) / 2,
            startY
        )
    )
    drawText(
        textLayoutResult = labelLayout,
        topLeft = Offset(
            (canvasWidth - labelLayout.size.width) / 2,
            startY + valueLayout.size.height + 2
        )
    )

    if (conversion.isNotEmpty()) {
        val conversionLayout = textMeasurer.measure(conversion, conversionStyle)
        drawText(
            textLayoutResult = conversionLayout,
            topLeft = Offset(
                canvasWidth - conversionLayout.size.width - 8,
                centerY - conversionLayout.size.height / 2
            )
        )
    }
}

private fun formatNumber(value: Int): String {
    return when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}
