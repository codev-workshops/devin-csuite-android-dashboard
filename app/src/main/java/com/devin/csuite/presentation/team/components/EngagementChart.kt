package com.devin.csuite.presentation.team.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.devin.csuite.domain.model.MetricDataPoint
import com.devin.csuite.presentation.team.EngagementData
import com.devin.csuite.presentation.theme.AccentPrimary
import com.devin.csuite.presentation.theme.AccentSecondary
import com.devin.csuite.presentation.theme.SuccessGreen

data class LineConfig(
    val key: String,
    val label: String,
    val color: Color,
    val data: List<MetricDataPoint>
)

@Composable
fun EngagementChart(
    data: EngagementData,
    visibleLines: Set<String>,
    onToggleLine: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lines = listOf(
        LineConfig("DAU", "DAU (${data.dauTotal})", AccentPrimary, data.dauData),
        LineConfig("WAU", "WAU (${data.wauTotal})", AccentSecondary, data.wauData),
        LineConfig("MAU", "MAU (${data.mauTotal})", SuccessGreen, data.mauData)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Engagement Trends",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                lines.forEach { line ->
                    val isVisible = visibleLines.contains(line.key)
                    LegendItem(
                        label = line.label,
                        color = line.color,
                        isActive = isVisible,
                        onClick = { onToggleLine(line.key) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart
            val visibleLineData = lines.filter { visibleLines.contains(it.key) }
            if (visibleLineData.all { it.data.isEmpty() }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No engagement data available",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                MultiLineChart(
                    lines = visibleLineData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) color else color.copy(alpha = 0.3f))
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isActive) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun MultiLineChart(
    lines: List<LineConfig>,
    modifier: Modifier = Modifier
) {
    val allValues = lines.flatMap { it.data.map { dp -> dp.value } }
    val maxValue = allValues.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val gridColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val padding = 4f

        // Grid lines
        for (i in 0..4) {
            val y = padding + (canvasHeight - 2 * padding) * i / 4
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(canvasWidth - padding, y),
                strokeWidth = 1f
            )
        }

        lines.forEach { line ->
            if (line.data.size < 2) return@forEach
            val points = line.data.mapIndexed { index, dp ->
                val x = padding + (canvasWidth - 2 * padding) * index / (line.data.size - 1).coerceAtLeast(1)
                val y = canvasHeight - padding - ((dp.value / maxValue) * (canvasHeight - 2 * padding)).toFloat()
                Offset(x, y)
            }

            val path = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) moveTo(point.x, point.y)
                    else lineTo(point.x, point.y)
                }
            }

            drawPath(
                path = path,
                color = line.color,
                style = Stroke(
                    width = 2.5f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Data points
            points.forEach { point ->
                drawCircle(
                    color = line.color,
                    radius = 3f,
                    center = point
                )
            }
        }
    }
}
