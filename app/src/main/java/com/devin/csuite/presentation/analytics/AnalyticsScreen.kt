package com.devin.csuite.presentation.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devin.csuite.core.UiState
import com.devin.csuite.domain.model.AcuLimitsResponse
import com.devin.csuite.domain.model.DailyConsumption
import com.devin.csuite.domain.model.MetricDataPoint
import com.devin.csuite.domain.model.SessionMetricsResponse
import com.devin.csuite.presentation.components.AcuGaugeChart
import com.devin.csuite.presentation.components.ShimmerChartCard
import com.devin.csuite.presentation.components.ShimmerKpiCard
import com.devin.csuite.presentation.theme.AccentPrimary
import com.devin.csuite.presentation.theme.AccentSecondary
import com.devin.csuite.presentation.theme.ErrorRed
import com.devin.csuite.presentation.theme.SuccessGreen
import com.devin.csuite.presentation.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

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
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Usage Analytics",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = viewModel::toggleOriginFilter) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter by origin",
                            tint = if (uiState.selectedOrigins.isNotEmpty()) AccentPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ACU Consumption Section
            item {
                ConsumptionSection(
                    consumptionState = uiState.consumptionState,
                    acuLimitsState = uiState.acuLimitsState
                )
            }

            // Sessions Section
            item {
                SessionsSection(sessionMetricsState = uiState.sessionMetricsState)
            }

            // Productivity Section
            item {
                ProductivitySection(
                    prChartData = uiState.prChartData,
                    searchChartData = uiState.searchChartData
                )
            }
        }
    }

    if (uiState.showOriginFilter) {
        OriginFilterBottomSheet(
            selectedOrigins = uiState.selectedOrigins,
            onToggleOrigin = viewModel::toggleOrigin,
            onClear = viewModel::clearOrigins,
            onDismiss = viewModel::toggleOriginFilter
        )
    }
}

@Composable
private fun ConsumptionSection(
    consumptionState: UiState<List<DailyConsumption>>,
    acuLimitsState: UiState<AcuLimitsResponse>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "ACU Consumption",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // ACU Gauge
            when (acuLimitsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ShimmerKpiCard(modifier = Modifier.size(160.dp))
                    }
                }
                is UiState.Success -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AcuGaugeChart(
                            used = acuLimitsState.data.acusUsed,
                            limit = acuLimitsState.data.acuLimit
                        )
                    }
                }
                is UiState.Error -> {
                    SectionError(message = acuLimitsState.message)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Daily consumption bar chart
            when (consumptionState) {
                is UiState.Loading -> ShimmerChartCard()
                is UiState.Success -> {
                    if (consumptionState.data.isEmpty()) {
                        SectionEmptyState(
                            icon = Icons.Default.BarChart,
                            message = "No consumption data available yet.\nACU burn data will appear as your team uses Devin."
                        )
                    } else {
                        DailyConsumptionChart(data = consumptionState.data)
                    }
                }
                is UiState.Error -> SectionError(message = consumptionState.message)
            }
        }
    }
}

@Composable
private fun DailyConsumptionChart(data: List<DailyConsumption>) {
    val maxValue = data.maxOfOrNull { it.acusConsumed } ?: 1.0

    Column {
        Text(
            text = "Daily ACU Burn",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))

        data.takeLast(14).forEach { day ->
            val fraction = (day.acusConsumed / maxValue).toFloat().coerceIn(0f, 1f)
            val barColor = when {
                fraction > 0.8f -> ErrorRed
                fraction > 0.5f -> WarningAmber
                else -> AccentPrimary
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.date.takeLast(5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = barColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = String.format("%.0f", day.acusConsumed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )
            }
        }

        // Burn rate projection
        if (data.size >= 3) {
            val recentAvg = data.takeLast(7).map { it.acusConsumed }.average()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Avg daily burn rate",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format("%.1f ACUs/day", recentAvg),
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentPrimary
                )
            }
        }
    }
}

@Composable
private fun SessionsSection(sessionMetricsState: UiState<SessionMetricsResponse>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sessions",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (sessionMetricsState) {
                is UiState.Loading -> {
                    ShimmerKpiCard(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(12.dp))
                    ShimmerChartCard()
                }
                is UiState.Success -> {
                    val metrics = sessionMetricsState.data
                    if (metrics.totalSessions == 0 && metrics.data.isEmpty()) {
                        SectionEmptyState(
                            icon = Icons.Default.Terminal,
                            message = "No session data available yet.\nSession analytics will populate as sessions are created."
                        )
                    } else {
                        // Total sessions big-number card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = formatLargeNumber(metrics.totalSessions),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "total sessions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        if (metrics.data.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Session Volume Trend",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            MiniBarChart(
                                dataPoints = metrics.data,
                                barColor = AccentSecondary
                            )
                        }
                    }
                }
                is UiState.Error -> SectionError(message = sessionMetricsState.message)
            }
        }
    }
}

@Composable
private fun ProductivitySection(
    prChartData: UiState<PrChartData>,
    searchChartData: UiState<List<MetricDataPoint>>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Productivity",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // PR metrics
            Text(
                text = "Pull Requests",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (prChartData) {
                is UiState.Loading -> ShimmerChartCard()
                is UiState.Success -> {
                    val data = prChartData.data
                    if (data.totalPrs == 0 && data.data.isEmpty()) {
                        SectionEmptyState(
                            icon = Icons.Default.Code,
                            message = "No PR data available yet.\nPR metrics will appear once Devin starts opening PRs."
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatItem(
                                label = "Opened",
                                value = data.totalPrs.toString(),
                                color = AccentPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            StatItem(
                                label = "Merged",
                                value = data.mergedPrs.toString(),
                                color = SuccessGreen,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (data.data.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            MiniBarChart(
                                dataPoints = data.data,
                                barColor = AccentPrimary
                            )
                        }
                    }
                }
                is UiState.Error -> SectionError(message = prChartData.message)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Search metrics
            Text(
                text = "Searches",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            when (searchChartData) {
                is UiState.Loading -> ShimmerChartCard()
                is UiState.Success -> {
                    if (searchChartData.data.isEmpty()) {
                        SectionEmptyState(
                            icon = Icons.Default.Search,
                            message = "No search data available yet.\nSearch metrics will appear as users search in Devin."
                        )
                    } else {
                        MiniBarChart(
                            dataPoints = searchChartData.data,
                            barColor = AccentSecondary
                        )
                    }
                }
                is UiState.Error -> SectionError(message = searchChartData.message)
            }
        }
    }
}

@Composable
private fun MiniBarChart(
    dataPoints: List<MetricDataPoint>,
    barColor: androidx.compose.ui.graphics.Color
) {
    val maxVal = dataPoints.maxOfOrNull { it.value } ?: 1.0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        dataPoints.takeLast(30).forEach { point ->
            val fraction = (point.value / maxVal).toFloat().coerceIn(0.01f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .height((fraction * 80).dp)
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(barColor.copy(alpha = 0.4f + fraction * 0.6f))
            )
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(12.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SectionEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun SectionError(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = ErrorRed,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun OriginFilterBottomSheet(
    selectedOrigins: Set<String>,
    onToggleOrigin: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter by Origin",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (selectedOrigins.isNotEmpty()) {
                    TextButton(onClick = onClear) {
                        Text("Clear all", color = AccentPrimary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AnalyticsViewModel.ORIGIN_OPTIONS.forEach { origin ->
                    FilterChip(
                        selected = selectedOrigins.contains(origin),
                        onClick = { onToggleOrigin(origin) },
                        label = { Text(origin) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPrimary.copy(alpha = 0.15f),
                            selectedLabelColor = AccentPrimary
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatLargeNumber(value: Int): String {
    return when {
        value >= 1_000_000 -> String.format("%.1fM", value / 1_000_000.0)
        value >= 1_000 -> String.format("%.1fK", value / 1_000.0)
        else -> value.toString()
    }
}
