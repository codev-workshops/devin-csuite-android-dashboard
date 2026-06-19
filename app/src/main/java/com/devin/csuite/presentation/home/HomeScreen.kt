package com.devin.csuite.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devin.csuite.core.UiState
import com.devin.csuite.domain.model.ActiveUser
import com.devin.csuite.domain.model.Session
import com.devin.csuite.presentation.components.KpiCard
import com.devin.csuite.presentation.components.ShimmerChartCard
import com.devin.csuite.presentation.components.ShimmerKpiCard
import com.devin.csuite.presentation.components.StaleDataBanner
import com.devin.csuite.presentation.components.animations.AnimatedChartEntry
import com.devin.csuite.presentation.components.formatLastUpdated
import com.devin.csuite.presentation.theme.AccentPrimary
import com.devin.csuite.presentation.theme.AccentSecondary
import com.devin.csuite.presentation.theme.ErrorRed
import com.devin.csuite.presentation.theme.SuccessGreen
import com.devin.csuite.presentation.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTeam: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterExpanded by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = viewModel::refresh
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stale Data Banner
            item {
                StaleDataBanner(
                    visible = uiState.isStale || uiState.isOffline,
                    lastUpdatedText = formatLastUpdated(uiState.lastUpdatedMs),
                    isOffline = uiState.isOffline,
                    onRefresh = viewModel::refresh
                )
            }

            // Critical Alert Banner
            if (uiState.showCriticalAlert) {
                item {
                    CriticalAlertBanner(
                        message = uiState.criticalAlertMessage,
                        onDismiss = { /* allow dismiss */ }
                    )
                }
            }

            // Time Range Filter
            item {
                TimeRangeFilter(
                    selectedRange = uiState.selectedTimeRange,
                    expanded = showFilterExpanded,
                    onToggleExpanded = { showFilterExpanded = !showFilterExpanded },
                    onRangeSelected = { range ->
                        viewModel.setTimeRange(range)
                        showFilterExpanded = false
                    }
                )
            }

            // Hero KPIs
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (val state = uiState.acuState) {
                        is UiState.Loading -> ShimmerKpiCard(modifier = Modifier.weight(1f))
                        is UiState.Success -> KpiCard(
                            title = "ACU Used",
                            value = state.data.acusUsed.toInt(),
                            icon = Icons.Default.Memory,
                            modifier = Modifier.weight(1f),
                            suffix = "",
                            iconTint = AccentPrimary
                        )
                        is UiState.Error -> ErrorKpiCard(
                            title = "ACU",
                            message = state.message,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    when (val state = uiState.activeSessionsState) {
                        is UiState.Loading -> ShimmerKpiCard(modifier = Modifier.weight(1f))
                        is UiState.Success -> KpiCard(
                            title = "Active",
                            value = state.data,
                            icon = Icons.Default.PlayArrow,
                            modifier = Modifier.weight(1f),
                            iconTint = SuccessGreen
                        )
                        is UiState.Error -> ErrorKpiCard(
                            title = "Active",
                            message = state.message,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    when (val state = uiState.mauState) {
                        is UiState.Loading -> ShimmerKpiCard(modifier = Modifier.weight(1f))
                        is UiState.Success -> KpiCard(
                            title = "MAU",
                            value = state.data,
                            icon = Icons.Default.Group,
                            modifier = Modifier.weight(1f),
                            iconTint = AccentSecondary
                        )
                        is UiState.Error -> ErrorKpiCard(
                            title = "MAU",
                            message = state.message,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // PRs KPI
            item {
                when (val state = uiState.prCountState) {
                    is UiState.Loading -> ShimmerKpiCard(modifier = Modifier.fillMaxWidth())
                    is UiState.Success -> KpiCard(
                        title = "Pull Requests",
                        value = state.data,
                        icon = Icons.Default.Code,
                        modifier = Modifier.fillMaxWidth(),
                        iconTint = AccentPrimary
                    )
                    is UiState.Error -> ErrorKpiCard(
                        title = "PRs",
                        message = state.message,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Session Volume Chart
            item {
                AnimatedChartEntry {
                    ChartSection(
                        title = "Session Volume (30d)",
                        state = uiState.sessionChartData
                    )
                }
            }

            // DAU Chart
            item {
                AnimatedChartEntry(delayMillis = 150) {
                    ChartSection(
                        title = "Daily Active Users (30d)",
                        state = uiState.dauChartData
                    )
                }
            }

            // Top Users
            item {
                TopUsersSection(
                    state = uiState.topUsers,
                    onSeeAll = onNavigateToTeam
                )
            }

            // Recent Sessions
            item {
                RecentSessionsSection(state = uiState.recentSessions)
            }
        }
    }
}

@Composable
private fun CriticalAlertBanner(message: String, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = WarningAmber.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = WarningAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = WarningAmber,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = WarningAmber,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun TimeRangeFilter(
    selectedRange: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onRangeSelected: (String) -> Unit
) {
    val ranges = listOf("Today", "7d", "30d", "90d")

    Column {
        if (!expanded) {
            FilterChip(
                selected = true,
                onClick = onToggleExpanded,
                label = { Text(selectedRange) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentPrimary.copy(alpha = 0.15f),
                    selectedLabelColor = AccentPrimary
                )
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ranges) { range ->
                    FilterChip(
                        selected = range == selectedRange,
                        onClick = { onRangeSelected(range) },
                        label = { Text(range) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = AccentPrimary
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ChartSection(
    title: String,
    state: UiState<List<com.devin.csuite.domain.model.MetricDataPoint>>
) {
    when (state) {
        is UiState.Loading -> ShimmerChartCard()
        is UiState.Success -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    if (state.data.isEmpty()) {
                        EmptyChartPlaceholder()
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${state.data.size} data points",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        is UiState.Error -> {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChartPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No data available yet.\nMetrics will appear once your team starts using Devin.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun TopUsersSection(
    state: UiState<List<ActiveUser>>,
    onSeeAll: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Top Users",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "See all",
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentPrimary,
                    modifier = Modifier.clickable(onClick = onSeeAll)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            when (state) {
                is UiState.Loading -> {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        )
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Text(
                            text = "No active users yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.data.forEach { user ->
                            UserRow(user)
                        }
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun UserRow(user: ActiveUser) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = AccentPrimary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.displayName ?: user.userEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${user.sessionCount} sessions",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${String.format("%.1f", user.acusConsumed)} ACU",
            style = MaterialTheme.typography.labelMedium,
            color = AccentSecondary
        )
    }
}

@Composable
private fun RecentSessionsSection(state: UiState<List<Session>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Recent Sessions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (state) {
                is UiState.Loading -> {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        )
                    }
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Text(
                            text = "No recent sessions.\nSessions will appear once your team starts using Devin.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.data.forEach { session ->
                            SessionRow(session)
                        }
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionRow(session: Session) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val statusColor = when (session.status) {
            "running" -> SuccessGreen
            "finished" -> AccentPrimary
            "error" -> ErrorRed
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(statusColor)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title ?: session.sessionId,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = session.status,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${String.format("%.1f", session.acusConsumed)} ACU",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorKpiCard(title: String, message: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Error",
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed
            )
        }
    }
}
