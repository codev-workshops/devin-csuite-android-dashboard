package com.devin.csuite.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.devin.csuite.presentation.theme.WarningAmber

@Composable
fun StaleDataBanner(
    visible: Boolean,
    lastUpdatedText: String,
    isOffline: Boolean = false,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        val bannerDescription = if (isOffline) {
            "Offline. $lastUpdatedText. Tap refresh to retry."
        } else {
            "$lastUpdatedText. Tap refresh to update."
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(WarningAmber.copy(alpha = 0.15f))
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics { contentDescription = bannerDescription },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (isOffline) Icons.Default.CloudOff else Icons.Default.Refresh,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isOffline) "Offline - $lastUpdatedText" else lastUpdatedText,
                    style = MaterialTheme.typography.labelSmall,
                    color = WarningAmber
                )
            }
            TextButton(onClick = onRefresh) {
                Text("Refresh", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
            }
        }
    }
}

fun formatLastUpdated(timestampMs: Long?): String {
    if (timestampMs == null) return "Never updated"
    val elapsed = System.currentTimeMillis() - timestampMs
    return when {
        elapsed < 60_000 -> "Updated just now"
        elapsed < 3_600_000 -> "Last updated ${elapsed / 60_000} min ago"
        elapsed < 86_400_000 -> "Last updated ${elapsed / 3_600_000} hours ago"
        else -> "Last updated ${elapsed / 86_400_000} days ago"
    }
}
