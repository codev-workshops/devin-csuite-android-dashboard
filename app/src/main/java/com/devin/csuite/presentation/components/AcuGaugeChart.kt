package com.devin.csuite.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devin.csuite.presentation.theme.AccentPrimary
import com.devin.csuite.presentation.theme.AccentSecondary
import com.devin.csuite.presentation.theme.ErrorRed
import com.devin.csuite.presentation.theme.SuccessGreen
import com.devin.csuite.presentation.theme.WarningAmber

@Composable
fun AcuGaugeChart(
    used: Double,
    limit: Double,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp,
    strokeWidth: Dp = 12.dp
) {
    val percentage = if (limit > 0) ((used / limit) * 100).coerceIn(0.0, 100.0) else 0.0
    val sweepTarget = (percentage / 100f * 240f).toFloat()

    val animatedSweep = remember { Animatable(0f) }
    LaunchedEffect(sweepTarget) {
        animatedSweep.animateTo(
            targetValue = sweepTarget,
            animationSpec = tween(durationMillis = 1200)
        )
    }

    val gradientColors = when {
        percentage > 95 -> listOf(ErrorRed, ErrorRed)
        percentage > 80 -> listOf(WarningAmber, ErrorRed)
        else -> listOf(AccentPrimary, AccentSecondary)
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val percentageText = String.format("%.0f%%", percentage)
    val absoluteText = "${formatNumber(used.toLong())} / ${formatNumber(limit.toLong())}"
    val percentageColor = when {
        percentage > 95 -> ErrorRed
        percentage > 80 -> WarningAmber
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val strokePx = strokeWidth.toPx()
            val arcSize = Size(
                this.size.width - strokePx,
                this.size.height - strokePx
            )
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)
            val startAngle = 150f

            drawArc(
                color = trackColor,
                startAngle = startAngle,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            if (animatedSweep.value > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = gradientColors,
                        center = Offset(this.size.width / 2f, this.size.height / 2f)
                    ),
                    startAngle = startAngle,
                    sweepAngle = animatedSweep.value,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = percentageText,
                style = MaterialTheme.typography.headlineMedium,
                color = percentageColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = absoluteText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatNumber(value: Long): String {
    return when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}
