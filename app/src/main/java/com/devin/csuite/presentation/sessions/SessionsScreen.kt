package com.devin.csuite.presentation.sessions

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devin.csuite.core.UiState
import com.devin.csuite.presentation.components.ShimmerChartCard
import com.devin.csuite.presentation.components.ShimmerKpiCard
import com.devin.csuite.presentation.sessions.components.ErrorRateIndicator
import com.devin.csuite.presentation.sessions.components.SessionCard
import com.devin.csuite.presentation.sessions.components.SessionFilterSheet
import com.devin.csuite.presentation.sessions.components.StatusDistributionDonut
import com.devin.csuite.presentation.theme.AccentPrimary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    onSessionClick: (String) -> Unit,
    viewModel: SessionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showFilterSheet by remember { mutableStateOf(false) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            lastVisibleItem >= totalItems - 3 && !uiState.isLoadingMore && uiState.hasMore
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            viewModel.loadMore()
        }
    }

    if (showFilterSheet) {
        SessionFilterSheet(
            sheetState = sheetState,
            currentFilters = uiState.filters,
            onApply = { filters ->
                viewModel.applyFilters(filters)
                scope.launch {
                    sheetState.hide()
                    showFilterSheet = false
                }
            },
            onDismiss = {
                scope.launch {
                    sheetState.hide()
                    showFilterSheet = false
                }
            }
        )
    }

    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SessionsHeader(
                activeFilterCount = uiState.activeFilterCount,
                lastUpdated = uiState.lastUpdated,
                onFilterClick = { showFilterSheet = true },
                onRefreshClick = { viewModel.refresh() }
            )

            when (val state = uiState.sessionsState) {
                is UiState.Loading -> {
                    SessionsLoadingContent()
                }
                is UiState.Error -> {
                    SessionsErrorContent(
                        message = state.message,
                        onRetry = { viewModel.refresh() }
                    )
                }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        SessionsEmptyContent(hasFilters = uiState.activeFilterCount > 0)
                    } else {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item(key = "metrics_row") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    StatusDistributionDonut(
                                        distribution = uiState.statusDistribution,
                                        onStatusClick = { status ->
                                            viewModel.filterByStatus(status)
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            item(key = "error_rate") {
                                ErrorRateIndicator(
                                    errorRate = uiState.errorRate,
                                    totalSessions = uiState.totalSessionCount
                                )
                            }

                            items(
                                items = state.data,
                                key = { it.sessionId }
                            ) { session ->
                                SessionCard(
                                    session = session,
                                    onClick = { onSessionClick(session.sessionId) }
                                )
                            }

                            if (uiState.isLoadingMore) {
                                item(key = "loading_more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp,
                                            color = AccentPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionsHeader(
    activeFilterCount: Int,
    lastUpdated: Long,
    onFilterClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sessions",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )

            Row {
                IconButton(onClick = onRefreshClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box {
                    IconButton(onClick = onFilterClick) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (activeFilterCount > 0) AccentPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (activeFilterCount > 0) {
                        Badge(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp),
                            containerColor = AccentPrimary
                        ) {
                            Text(
                                text = "$activeFilterCount",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        if (lastUpdated > 0) {
            val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
            Text(
                text = "Updated ${dateFormat.format(Date(lastUpdated))}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SessionsLoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShimmerKpiCard(modifier = Modifier.weight(1f))
            ShimmerKpiCard(modifier = Modifier.weight(1f))
        }
        ShimmerChartCard()
        ShimmerChartCard()
        ShimmerChartCard()
    }
}

@Composable
private fun SessionsErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Failed to load sessions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.TextButton(onClick = onRetry) {
            Text("Retry", color = AccentPrimary)
        }
    }
}

@Composable
private fun SessionsEmptyContent(
    hasFilters: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (hasFilters) "No sessions match your filters"
            else "No sessions yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasFilters) "Try adjusting your filter criteria"
            else "Sessions will appear once your team starts using Devin",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
