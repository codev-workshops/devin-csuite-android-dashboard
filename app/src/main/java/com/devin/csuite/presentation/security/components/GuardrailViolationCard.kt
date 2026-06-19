package com.devin.csuite.presentation.security.components

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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devin.csuite.domain.model.security.GuardrailViolation
import com.devin.csuite.presentation.theme.ErrorRed
import com.devin.csuite.presentation.theme.SuccessGreen
import com.devin.csuite.presentation.theme.WarningAmber

@Composable
fun GuardrailViolationCard(
    violation: GuardrailViolation,
    onSessionClick: ((String) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SeverityBadge(severity = violation.severity)
                Text(
                    text = formatRelativeTime(violation.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = violation.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            if (violation.ruleName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Rule: ${violation.ruleName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (violation.sessionId != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Session: ${violation.sessionId}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (onSessionClick != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textDecoration = if (onSessionClick != null) TextDecoration.Underline else null,
                    modifier = if (onSessionClick != null) {
                        Modifier.clickable { onSessionClick(violation.sessionId) }
                    } else {
                        Modifier
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SeverityBadge(severity: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor) = when (severity.lowercase()) {
        "critical", "high" -> ErrorRed.copy(alpha = 0.15f) to ErrorRed
        "medium", "warning" -> WarningAmber.copy(alpha = 0.15f) to WarningAmber
        "low", "info" -> SuccessGreen.copy(alpha = 0.15f) to SuccessGreen
        else -> Color.Gray.copy(alpha = 0.15f) to Color.Gray
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = severity.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = textColor
        )
    }
}
