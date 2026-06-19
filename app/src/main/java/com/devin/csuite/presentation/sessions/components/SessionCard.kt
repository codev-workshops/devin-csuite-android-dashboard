package com.devin.csuite.presentation.sessions.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.devin.csuite.domain.model.Session
import com.devin.csuite.presentation.theme.ErrorRed
import com.devin.csuite.presentation.theme.InfoBlue
import com.devin.csuite.presentation.theme.SuccessGreen
import com.devin.csuite.presentation.theme.WarningAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SessionCard(
    session: Session,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusPill(status = session.status)
                Spacer(modifier = Modifier.width(8.dp))
                OriginBadge(origin = session.origin)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = session.title ?: session.sessionId,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = session.userEmail ?: "Unknown user",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${String.format("%.1f", session.acusConsumed)} ACUs",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatElapsedTime(session.createdAt, session.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (session.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val visibleTags = session.tags.take(3)
                    val remainingCount = session.tags.size - 3

                    visibleTags.forEach { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.height(24.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = null
                        )
                    }

                    if (remainingCount > 0) {
                        SuggestionChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = "+$remainingCount more",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(24.dp),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    status: String,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (status.lowercase()) {
        "running" -> SuccessGreen to "Running"
        "completed", "exit" -> InfoBlue to "Completed"
        "error", "failed" -> ErrorRed to "Error"
        "suspended" -> WarningAmber to "Suspended"
        "new" -> InfoBlue.copy(alpha = 0.7f) to "New"
        else -> MaterialTheme.colorScheme.onSurfaceVariant to status.replaceFirstChar {
            it.uppercase()
        }
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun OriginBadge(
    origin: String?,
    modifier: Modifier = Modifier
) {
    if (origin == null) return

    val (icon, label) = getOriginIconAndLabel(origin)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

fun getOriginIconAndLabel(origin: String): Pair<ImageVector, String> {
    return when (origin.lowercase()) {
        "webapp" -> Icons.Default.Computer to "Web"
        "slack" -> Icons.AutoMirrored.Filled.Chat to "Slack"
        "teams" -> Icons.AutoMirrored.Filled.Chat to "Teams"
        "api" -> Icons.Default.Api to "API"
        "cli" -> Icons.Default.Terminal to "CLI"
        "linear" -> Icons.AutoMirrored.Filled.OpenInNew to "Linear"
        "jira" -> Icons.AutoMirrored.Filled.OpenInNew to "Jira"
        "scheduled" -> Icons.Default.Schedule to "Scheduled"
        else -> Icons.Default.Code to origin.replaceFirstChar { it.uppercase() }
    }
}

fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "running" -> SuccessGreen
        "completed", "exit" -> InfoBlue
        "error", "failed" -> ErrorRed
        "suspended" -> WarningAmber
        else -> InfoBlue.copy(alpha = 0.5f)
    }
}

private fun formatElapsedTime(createdAt: Long, updatedAt: Long): String {
    val now = System.currentTimeMillis() / 1000
    val startTime = if (createdAt > 1_000_000_000_000L) createdAt / 1000 else createdAt
    val endTime = if (updatedAt > 0) {
        if (updatedAt > 1_000_000_000_000L) updatedAt / 1000 else updatedAt
    } else {
        now
    }

    val elapsed = endTime - startTime
    return when {
        elapsed < 60 -> "${elapsed}s"
        elapsed < 3600 -> "${elapsed / 60}m"
        elapsed < 86400 -> "${elapsed / 3600}h ${(elapsed % 3600) / 60}m"
        else -> "${elapsed / 86400}d ${(elapsed % 86400) / 3600}h"
    }
}
