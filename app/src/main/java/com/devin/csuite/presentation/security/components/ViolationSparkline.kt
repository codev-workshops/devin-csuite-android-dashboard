package com.devin.csuite.presentation.security.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.devin.csuite.domain.model.security.ViolationTrendPoint
import com.devin.csuite.presentation.theme.AccentPrimary
import com.devin.csuite.presentation.theme.AccentSecondary
import com.devin.csuite.presentation.theme.ErrorRed

@Composable
fun ViolationSparkline(
    trendData: List<ViolationTrendPoint>,
    modifier: Modifier = Modifier
) {
    if (trendData.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Violations Trend (30d)",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            val lineColor = ErrorRed
            val fillGradient = Brush.verticalGradient(
                colors = listOf(
                    ErrorRed.copy(alpha = 0.3f),
                    ErrorRed.copy(alpha = 0.0f)
                )
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                val maxCount = trendData.maxOfOrNull { it.count } ?: 1
                val points = trendData.mapIndexed { index, point ->
                    val x = if (trendData.size > 1) {
                        (index.toFloat() / (trendData.size - 1)) * size.width
                    } else {
                        size.width / 2f
                    }
                    val y = size.height - (point.count.toFloat() / maxCount) * (size.height - 8f) - 4f
                    Offset(x, y)
                }

                if (points.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(points.first().x, size.height)
                        lineTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                        lineTo(points.last().x, size.height)
                        close()
                    }
                    drawPath(fillPath, fillGradient)

                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(
                        linePath,
                        color = lineColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    points.forEach { point ->
                        drawCircle(
                            color = lineColor,
                            radius = 3.dp.toPx(),
                            center = point
                        )
                    }
                } else if (points.size == 1) {
                    drawCircle(
                        color = lineColor,
                        radius = 4.dp.toPx(),
                        center = points[0]
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            val total = trendData.sumOf { it.count }
            Text(
                text = "$total violations in last 30 days",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
