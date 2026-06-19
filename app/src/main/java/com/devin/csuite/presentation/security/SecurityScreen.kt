package com.devin.csuite.presentation.security

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devin.csuite.core.UiState
import com.devin.csuite.presentation.components.ShimmerChartCard
import com.devin.csuite.presentation.components.ShimmerKpiCard
import com.devin.csuite.presentation.security.components.AuditLogItem
import com.devin.csuite.presentation.security.components.GuardrailViolationCard
import com.devin.csuite.presentation.security.components.IpAccessItem
import com.devin.csuite.presentation.security.components.ViolationSparkline
import com.devin.csuite.presentation.theme.AccentPrimary
import com.devin.csuite.presentation.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    onNavigateBack: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.showConfirmAddIp) {
        ConfirmAddIpDialog(
            ip = uiState.pendingAddIp,
            onConfirm = viewModel::confirmAddIp,
            onDismiss = viewModel::cancelAddIp
        )
    }

    if (uiState.showConfirmRemoveIp != null) {
        ConfirmRemoveIpDialog(
            ip = uiState.showConfirmRemoveIp!!,
            onConfirm = viewModel::confirmRemoveIp,
            onDismiss = viewModel::cancelRemoveIp
        )
    }

    if (uiState.ipMutationError != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissMutationError,
            title = { Text("Error") },
            text = { Text(uiState.ipMutationError!!) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissMutationError) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Security & Compliance",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::showFilterSheet) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filters"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            if (uiState.selectedTab == SecurityTab.IP_ACCESS) {
                FloatingActionButton(
                    onClick = viewModel::showAddIpSheet,
                    containerColor = AccentPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add IP",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SecurityTabRow(
                selectedTab = uiState.selectedTab,
                onTabSelected = viewModel::selectTab
            )

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when (uiState.selectedTab) {
                    SecurityTab.AUDIT_LOGS -> AuditLogsTab(
                        uiState = uiState,
                        onToggleExpand = viewModel::toggleAuditLogExpanded,
                        onLoadMore = viewModel::loadMoreAuditLogs
                    )
                    SecurityTab.GUARDRAILS -> GuardrailsTab(
                        uiState = uiState,
                        onSessionClick = null
                    )
                    SecurityTab.IP_ACCESS -> IpAccessTab(
                        uiState = uiState,
                        onRemoveClick = viewModel::requestRemoveIp
                    )
                }
            }
        }
    }

    if (uiState.showAddIpSheet) {
        AddIpBottomSheet(
            error = uiState.addIpError,
            isLoading = uiState.addIpLoading,
            onDismiss = viewModel::dismissAddIpSheet,
            onSubmit = viewModel::requestAddIp
        )
    }

    if (uiState.showFilterSheet) {
        FilterBottomSheet(
            selectedTab = uiState.selectedTab,
            selectedActionType = uiState.selectedActionTypeFilter,
            selectedSeverity = uiState.selectedSeverityFilter,
            onActionTypeSelected = viewModel::setActionTypeFilter,
            onSeveritySelected = viewModel::setSeverityFilter,
            onDismiss = viewModel::dismissFilterSheet
        )
    }
}

@Composable
private fun SecurityTabRow(
    selectedTab: SecurityTab,
    onTabSelected: (SecurityTab) -> Unit
) {
    ScrollableTabRow(
        selectedTabIndex = selectedTab.ordinal,
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = AccentPrimary,
        edgePadding = 16.dp
    ) {
        SecurityTab.entries.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                text = {
                    Text(
                        text = tab.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }
}

@Composable
private fun AuditLogsTab(
    uiState: SecurityUiState,
    onToggleExpand: (String) -> Unit,
    onLoadMore: () -> Unit
) {
    when (val state = uiState.auditLogsState) {
        is UiState.Loading -> {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(5) { ShimmerChartCard() }
            }
        }
        is UiState.Error -> {
            ErrorSection(message = state.message)
        }
        is UiState.Success -> {
            val logs = state.data
            if (logs.isEmpty()) {
                EmptySection(
                    icon = Icons.Default.Security,
                    message = "No audit log entries found"
                )
            } else {
                val listState = rememberLazyListState()
                val shouldLoadMore by remember {
                    derivedStateOf {
                        val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
                        lastVisibleItem != null && lastVisibleItem.index >= logs.size - 3
                    }
                }

                LaunchedEffect(shouldLoadMore) {
                    if (shouldLoadMore && uiState.auditLogHasMore) {
                        onLoadMore()
                    }
                }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs, key = { it.id }) { entry ->
                        AuditLogItem(
                            entry = entry,
                            isExpanded = uiState.expandedAuditLogId == entry.id,
                            onToggleExpand = { onToggleExpand(entry.id) }
                        )
                    }

                    if (uiState.isLoadingMoreAuditLogs) {
                        item {
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

@Composable
private fun GuardrailsTab(
    uiState: SecurityUiState,
    onSessionClick: ((String) -> Unit)?
) {
    if (!uiState.guardrailAvailable) {
        EmptySection(
            icon = Icons.Default.Security,
            message = "Guardrail monitoring not available for your account"
        )
        return
    }

    when (val state = uiState.guardrailState) {
        is UiState.Loading -> {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShimmerChartCard()
                repeat(3) { ShimmerKpiCard(modifier = Modifier.fillMaxWidth()) }
            }
        }
        is UiState.Error -> {
            ErrorSection(message = state.message)
        }
        is UiState.Success -> {
            val violations = state.data
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (uiState.violationTrendState is UiState.Success) {
                    val trendData = (uiState.violationTrendState as UiState.Success).data
                    if (trendData.isNotEmpty()) {
                        item {
                            ViolationSparkline(trendData = trendData)
                        }
                        item { Spacer(modifier = Modifier.height(8.dp)) }
                    }
                }

                if (violations.isEmpty()) {
                    item {
                        EmptySection(
                            icon = Icons.Default.Security,
                            message = "No guardrail violations found"
                        )
                    }
                } else {
                    items(violations, key = { it.id }) { violation ->
                        GuardrailViolationCard(
                            violation = violation,
                            onSessionClick = onSessionClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IpAccessTab(
    uiState: SecurityUiState,
    onRemoveClick: (String) -> Unit
) {
    when (val state = uiState.ipAccessState) {
        is UiState.Loading -> {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(3) { ShimmerKpiCard(modifier = Modifier.fillMaxWidth()) }
            }
        }
        is UiState.Error -> {
            ErrorSection(message = state.message)
        }
        is UiState.Success -> {
            val ipAddresses = state.data
            if (ipAddresses.isEmpty()) {
                EmptySection(
                    icon = Icons.Default.Security,
                    message = "No IP addresses in access list.\nTap + to add one."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ipAddresses, key = { it.ip }) { entry ->
                        IpAccessItem(
                            entry = entry,
                            isRemoving = uiState.removeIpLoading == entry.ip,
                            onRemoveClick = { onRemoveClick(entry.ip) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorSection(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun EmptySection(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ConfirmAddIpDialog(ip: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add IP Address") },
        text = { Text("Are you sure you want to add $ip to the access list?") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Add", color = AccentPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ConfirmRemoveIpDialog(ip: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove IP Address") },
        text = { Text("Are you sure you want to remove $ip from the access list? This cannot be undone.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove", color = ErrorRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddIpBottomSheet(
    error: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var ipInput by remember { mutableStateOf("") }
    var descriptionInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Add IP Address",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = ipInput,
                onValueChange = { ipInput = it },
                label = { Text("IP Address or CIDR") },
                placeholder = { Text("e.g., 192.168.1.0/24") },
                singleLine = true,
                isError = error != null,
                modifier = Modifier.fillMaxWidth()
            )
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed,
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = descriptionInput,
                onValueChange = { descriptionInput = it },
                label = { Text("Description (optional)") },
                placeholder = { Text("e.g., Office network") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(
                onClick = { onSubmit(ipInput, descriptionInput) },
                enabled = ipInput.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AccentPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Add IP Address", color = AccentPrimary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    selectedTab: SecurityTab,
    selectedActionType: String?,
    selectedSeverity: String?,
    onActionTypeSelected: (String?) -> Unit,
    onSeveritySelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                SecurityTab.AUDIT_LOGS -> {
                    Text(
                        text = "Action Type",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val actionTypes = listOf(
                        null to "All",
                        "create" to "Create",
                        "update" to "Update",
                        "delete" to "Delete",
                        "login" to "Login",
                        "logout" to "Logout"
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        actionTypes.forEach { (value, label) ->
                            FilterChip(
                                selected = selectedActionType == value,
                                onClick = {
                                    onActionTypeSelected(value)
                                    onDismiss()
                                },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = AccentPrimary
                                )
                            )
                        }
                    }
                }
                SecurityTab.GUARDRAILS -> {
                    Text(
                        text = "Severity",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val severities = listOf(
                        null to "All",
                        "critical" to "Critical",
                        "high" to "High",
                        "medium" to "Medium",
                        "low" to "Low"
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        severities.forEach { (value, label) ->
                            FilterChip(
                                selected = selectedSeverity == value,
                                onClick = {
                                    onSeveritySelected(value)
                                    onDismiss()
                                },
                                label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentPrimary.copy(alpha = 0.15f),
                                    selectedLabelColor = AccentPrimary
                                )
                            )
                        }
                    }
                }
                SecurityTab.IP_ACCESS -> {
                    Text(
                        text = "No filters available for IP Access List",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
