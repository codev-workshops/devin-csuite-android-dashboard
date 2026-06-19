package com.devin.csuite.presentation.team

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devin.csuite.core.UiState
import com.devin.csuite.domain.model.EnterpriseRole
import com.devin.csuite.presentation.components.ShimmerChartCard
import com.devin.csuite.presentation.team.components.ActiveUsersList
import com.devin.csuite.presentation.team.components.AdoptionFunnel
import com.devin.csuite.presentation.team.components.EngagementChart
import com.devin.csuite.presentation.team.components.OrgBreakdownChart
import com.devin.csuite.presentation.team.components.RoleDistributionChart
import com.devin.csuite.presentation.theme.AccentPrimary
import com.devin.csuite.presentation.theme.ErrorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamScreen(
    viewModel: TeamViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showRoleDropdown by remember { mutableStateOf(false) }

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
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Team & Adoption",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Box {
                        IconButton(onClick = { showRoleDropdown = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter by role",
                                tint = if (uiState.selectedRoleFilter != null) AccentPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        RoleFilterDropdown(
                            expanded = showRoleDropdown,
                            roles = (uiState.roleDistributionState as? UiState.Success)?.data
                                ?: emptyList(),
                            selectedRole = uiState.selectedRoleFilter,
                            onRoleSelected = { role ->
                                viewModel.setRoleFilter(role)
                                showRoleDropdown = false
                            },
                            onDismiss = { showRoleDropdown = false }
                        )
                    }
                }
            }

            // Engagement Chart
            item {
                when (val state = uiState.engagementState) {
                    is UiState.Loading -> ShimmerChartCard()
                    is UiState.Success -> {
                        EngagementChart(
                            data = state.data,
                            visibleLines = uiState.visibleLines,
                            onToggleLine = viewModel::toggleLine
                        )
                    }
                    is UiState.Error -> SectionError(
                        title = "Engagement Trends",
                        icon = Icons.Default.Timeline,
                        message = state.message
                    )
                }
            }

            // Adoption Funnel
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    when (val state = uiState.funnelState) {
                        is UiState.Loading -> ShimmerChartCard()
                        is UiState.Success -> {
                            if (state.data.totalUsers == 0 && state.data.mau == 0) {
                                SectionEmptyState(
                                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                                    message = "Adoption funnel will appear once users start engaging with Devin."
                                )
                            } else {
                                AdoptionFunnel(
                                    data = state.data,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                        is UiState.Error -> SectionError(
                            title = "Adoption Funnel",
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            message = state.message
                        )
                    }
                }
            }

            // Active Users List
            item {
                when (val state = uiState.activeUsersState) {
                    is UiState.Loading -> ShimmerChartCard()
                    is UiState.Success -> {
                        ActiveUsersList(
                            users = state.data,
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = viewModel::setSearchQuery
                        )
                    }
                    is UiState.Error -> SectionError(
                        title = "Active Users",
                        icon = Icons.Default.Group,
                        message = state.message
                    )
                }
            }

            // Org Breakdown
            item {
                when (val state = uiState.orgBreakdownState) {
                    is UiState.Loading -> ShimmerChartCard()
                    is UiState.Success -> {
                        OrgBreakdownChart(data = state.data)
                    }
                    is UiState.Error -> SectionError(
                        title = "Organization Breakdown",
                        icon = Icons.Default.Group,
                        message = state.message
                    )
                }
            }

            // Role Distribution
            item {
                when (val state = uiState.roleDistributionState) {
                    is UiState.Loading -> ShimmerChartCard()
                    is UiState.Success -> {
                        RoleDistributionChart(roles = state.data)
                    }
                    is UiState.Error -> SectionError(
                        title = "Role Distribution",
                        icon = Icons.Default.PieChart,
                        message = state.message
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun RoleFilterDropdown(
    expanded: Boolean,
    roles: List<EnterpriseRole>,
    selectedRole: String?,
    onRoleSelected: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    "All Roles",
                    color = if (selectedRole == null) AccentPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            },
            onClick = { onRoleSelected(null) }
        )
        roles.forEach { role ->
            DropdownMenuItem(
                text = {
                    Text(
                        role.displayName ?: role.name,
                        color = if (selectedRole == role.name) AccentPrimary
                        else MaterialTheme.colorScheme.onSurface
                    )
                },
                onClick = { onRoleSelected(role.name) }
            )
        }
    }
}

@Composable
private fun SectionError(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ErrorRed.copy(alpha = 0.6f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed
                )
            }
        }
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
