package com.devin.csuite.presentation.sessions.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.devin.csuite.presentation.sessions.StatusDistribution
import com.devin.csuite.presentation.theme.ErrorRed
import com.devin.csuite.presentation.theme.InfoBlue
import com.devin.csuite.presentation.theme.SuccessGreen
import com.devin.csuite.presentation.theme.WarningAmber

@Composable
fun StatusDistributionDonut(
    distribution: StatusDistribution,
    onStatusClick: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (distribution.total == 0) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(distribution = distribution)
                Text(
                    text = "${distribution.total}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (distribution.running > 0) {
                    StatusLegendItem(
                        color = SuccessGreen,
                        label = "Running",
                        count = distribution.running,
                        onClick = { onStatusClick("running") }
                    )
                }
                if (distribution.completed > 0) {
                    StatusLegendItem(
                        color = InfoBlue,
                        label = "Completed",
                        count = distribution.completed,
                        onClick = { onStatusClick("completed") }
                    )
                }
                if (distribution.error > 0) {
                    StatusLegendItem(
                        color = ErrorRed,
                        label = "Error",
                        count = distribution.error,
                        onClick = { onStatusClick("error") }
                    )
                }
                if (distribution.suspended > 0) {
                    StatusLegendItem(
                        color = WarningAmber,
                        label = "Suspended",
                        count = distribution.suspended,
                        onClick = { onStatusClick("suspended") }
                    )
                }
                if (distribution.other > 0) {
                    StatusLegendItem(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "Other",
                        count = distribution.other,
                        onClick = { onStatusClick(null) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    distribution: StatusDistribution,
    modifier: Modifier = Modifier
) {
    val segments = buildList {
        if (distribution.running > 0) add(SuccessGreen to distribution.running)
        if (distribution.completed > 0) add(InfoBlue to distribution.completed)
        if (distribution.error > 0) add(ErrorRed to distribution.error)
        if (distribution.suspended > 0) add(WarningAmber to distribution.suspended)
        if (distribution.other > 0) add(Color.Gray to distribution.other)
    }

    val total = distribution.total.toFloat()

    Canvas(modifier = modifier.size(80.dp)) {
        val strokeWidth = 12f
        val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

        var startAngle = -90f

        segments.forEach { (color, count) ->
            val sweepAngle = (count / total) * 360f
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
private fun StatusLegendItem(
    color: Color,
    label: String,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
