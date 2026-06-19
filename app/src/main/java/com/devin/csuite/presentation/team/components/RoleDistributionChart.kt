package com.devin.csuite.presentation.team.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.devin.csuite.domain.model.EnterpriseRole
import com.devin.csuite.presentation.theme.AccentPrimary
import com.devin.csuite.presentation.theme.AccentSecondary
import com.devin.csuite.presentation.theme.AccentTertiary
import com.devin.csuite.presentation.theme.InfoBlue
import com.devin.csuite.presentation.theme.SuccessGreen
import com.devin.csuite.presentation.theme.WarningAmber

@Composable
fun RoleDistributionChart(
    roles: List<EnterpriseRole>,
    modifier: Modifier = Modifier
) {
    val roleColors = listOf(
        AccentPrimary, AccentSecondary, SuccessGreen,
        WarningAmber, InfoBlue, AccentTertiary,
        Color(0xFFFF6B6B), Color(0xFF48DBFB)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Role Distribution",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (roles.isEmpty()) {
                Text(
                    text = "No role data available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DonutChart(
                        roles = roles,
                        colors = roleColors,
                        modifier = Modifier.size(120.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        roles.forEachIndexed { index, role ->
                            val color = roleColors[index % roleColors.size]
                            RoleLegendItem(
                                name = role.displayName ?: role.name,
                                count = role.userCount,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    roles: List<EnterpriseRole>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = roles.sumOf { it.userCount }.coerceAtLeast(1)
    val bgColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val strokeWidth = 24f
        val radius = (size.minDimension - strokeWidth) / 2
        val center = this.center

        drawCircle(
            color = bgColor,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        var startAngle = -90f
        roles.forEachIndexed { index, role ->
            val sweep = (role.userCount.toFloat() / total) * 360f
            val color = colors[index % colors.size]
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                topLeft = center - androidx.compose.ui.geometry.Offset(radius, radius),
                size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun RoleLegendItem(
    name: String,
    count: Int,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
