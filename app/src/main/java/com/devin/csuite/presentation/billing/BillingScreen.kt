package com.devin.csuite.presentation.billing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.devin.csuite.core.UiState
import com.devin.csuite.domain.model.AcuLimitsResponse
import com.devin.csuite.domain.model.BillingCycle
import com.devin.csuite.domain.model.BillingCyclesResponse
import com.devin.csuite.domain.model.DailyConsumption
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
fun BillingScreen(
    viewModel: BillingViewModel = hiltViewModel()
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
                Text(
                    text = "Billing & Cost",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // Cycle selector
            item {
                CycleSelector(
                    cyclesState = uiState.cyclesState,
                    selectedIndex = uiState.selectedCycleIndex,
                    onSelectCycle = viewModel::selectCycle
                )
            }

            // Current Cycle Card
            item {
                CurrentCycleCard(
                    cyclesState = uiState.cyclesState,
                    acuLimitsState = uiState.acuLimitsState,
                    selectedCycleIndex = uiState.selectedCycleIndex
                )
            }

            // Projection Widget
            item {
                ProjectionWidget(projection = uiState.projection)
            }

            // ACU Limits Display
            item {
                AcuLimitsSection(
                    acuLimitsState = uiState.acuLimitsState,
                    onEditClick = viewModel::showEditLimitSheet
                )
            }

            // Daily Cost Chart
            item {
                DailyCostSection(dailyConsumptionState = uiState.dailyConsumptionState)
            }

            // Historical Cycles
            item {
                HistoricalCyclesSection(cyclesState = uiState.cyclesState)
            }
        }
    }

    // Edit Limit Bottom Sheet
    if (uiState.showEditLimitSheet) {
        EditLimitBottomSheet(
            editValue = uiState.editLimitValue,
            onValueChange = viewModel::updateEditLimitValue,
            onSave = viewModel::requestConfirmation,
            onDismiss = viewModel::dismissEditLimitSheet,
            isLoading = uiState.updateLimitState is UiState.Loading,
            error = uiState.permissionError
        )
    }

    // Confirmation Dialog (Step 2)
    if (uiState.showConfirmDialog) {
        ConfirmLimitDialog(
            newLimit = uiState.editLimitValue,
            onConfirm = viewModel::confirmUpdateLimit,
            onDismiss = viewModel::dismissConfirmation
        )
    }
}

@Composable
private fun CycleSelector(
    cyclesState: UiState<BillingCyclesResponse>,
    selectedIndex: Int,
    onSelectCycle: (Int) -> Unit
) {
    when (cyclesState) {
        is UiState.Loading -> {
            ShimmerKpiCard(modifier = Modifier.fillMaxWidth().height(40.dp))
        }
        is UiState.Success -> {
            val allCycles = buildList {
                cyclesState.data.currentCycle?.let { add(it) }
                addAll(cyclesState.data.cycles)
            }.distinctBy { "${it.cycleStart}-${it.cycleEnd}" }

            if (allCycles.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(allCycles) { index, cycle ->
                        val label = if (index == 0 && cyclesState.data.currentCycle != null) {
                            "Current"
                        } else {
                            cycle.cycleStart.take(7)
                        }
                        FilterChip(
                            selected = index == selectedIndex,
                            onClick = { onSelectCycle(index) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPrimary.copy(alpha = 0.15f),
                                selectedLabelColor = AccentPrimary
                            )
                        )
                    }
                }
            }
        }
        is UiState.Error -> {
            Text(
                text = cyclesState.message,
                style = MaterialTheme.typography.bodySmall,
                color = ErrorRed
            )
        }
    }
}

@Composable
private fun CurrentCycleCard(
    cyclesState: UiState<BillingCyclesResponse>,
    acuLimitsState: UiState<AcuLimitsResponse>,
    selectedCycleIndex: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (cyclesState) {
                is UiState.Loading -> {
                    ShimmerKpiCard(modifier = Modifier.fillMaxWidth())
                }
                is UiState.Success -> {
                    val allCycles = buildList {
                        cyclesState.data.currentCycle?.let { add(it) }
                        addAll(cyclesState.data.cycles)
                    }.distinctBy { "${it.cycleStart}-${it.cycleEnd}" }

                    val selectedCycle = allCycles.getOrNull(selectedCycleIndex)
                    if (selectedCycle != null) {
                        Text(
                            text = "Billing Cycle",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${selectedCycle.cycleStart.take(10)} to ${selectedCycle.cycleEnd.take(10)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // ACU Gauge (reused from components)
                        when (acuLimitsState) {
                            is UiState.Loading -> ShimmerKpiCard(modifier = Modifier.size(160.dp))
                            is UiState.Success -> {
                                AcuGaugeChart(
                                    used = if (selectedCycleIndex == 0) acuLimitsState.data.acusUsed else selectedCycle.acusUsed,
                                    limit = if (selectedCycleIndex == 0) acuLimitsState.data.acuLimit else selectedCycle.acuLimit,
                                    size = 180.dp
                                )
                            }
                            is UiState.Error -> {
                                AcuGaugeChart(
                                    used = selectedCycle.acusUsed,
                                    limit = selectedCycle.acuLimit,
                                    size = 180.dp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Usage stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CycleStat(
                                label = "Used",
                                value = String.format("%.0f", selectedCycle.acusUsed)
                            )
                            CycleStat(
                                label = "Limit",
                                value = String.format("%.0f", selectedCycle.acuLimit)
                            )
                            CycleStat(
                                label = "Remaining",
                                value = String.format("%.0f", (selectedCycle.acuLimit - selectedCycle.acusUsed).coerceAtLeast(0.0))
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Utilization progress bar
                        val utilization = if (selectedCycle.acuLimit > 0) {
                            (selectedCycle.acusUsed / selectedCycle.acuLimit).toFloat().coerceIn(0f, 1f)
                        } else 0f
                        val progressColor = when {
                            utilization > 0.95f -> ErrorRed
                            utilization > 0.80f -> WarningAmber
                            else -> AccentPrimary
                        }

                        LinearProgressIndicator(
                            progress = { utilization },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = progressColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        )
                        Text(
                            text = "${String.format("%.1f", utilization * 100)}% utilized",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        EmptyBillingState("No billing cycle data available.")
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = cyclesState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun CycleStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProjectionWidget(projection: ProjectionData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Projection",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            if (projection == null) {
                Text(
                    text = "Projection will be calculated once consumption data is available.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val projColor = when {
                    projection.projectedPercentage > 95 -> ErrorRed
                    projection.projectedPercentage > 80 -> WarningAmber
                    else -> SuccessGreen
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = String.format("%.0f ACUs", projection.projectedTotal),
                            style = MaterialTheme.typography.headlineMedium,
                            color = projColor
                        )
                        Text(
                            text = "projected by end of cycle",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (projection.isOverBudget) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ErrorRed.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Over budget",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = ErrorRed
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProjectionDetail("Avg daily burn", String.format("%.1f ACUs", projection.dailyAvg))
                    ProjectionDetail("Days remaining", "${projection.remainingDays}")
                    ProjectionDetail("Current used", String.format("%.0f", projection.currentUsed))
                }
            }
        }
    }
}

@Composable
private fun ProjectionDetail(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AcuLimitsSection(
    acuLimitsState: UiState<AcuLimitsResponse>,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ACU Limits",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit ACU limit",
                        tint = AccentPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            when (acuLimitsState) {
                is UiState.Loading -> ShimmerKpiCard(modifier = Modifier.fillMaxWidth())
                is UiState.Success -> {
                    val data = acuLimitsState.data
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        LimitStat("Limit", String.format("%.0f", data.acuLimit), AccentPrimary)
                        LimitStat("Used", String.format("%.0f", data.acusUsed), WarningAmber)
                        LimitStat("Remaining", String.format("%.0f", data.remaining), SuccessGreen)
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = acuLimitsState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun LimitStat(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
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
private fun DailyCostSection(dailyConsumptionState: UiState<List<DailyConsumption>>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Daily Cost Breakdown",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (dailyConsumptionState) {
                is UiState.Loading -> ShimmerChartCard()
                is UiState.Success -> {
                    val data = dailyConsumptionState.data
                    if (data.isEmpty()) {
                        EmptyBillingState("No daily cost data available yet.")
                    } else {
                        DailyCostBars(data = data)
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = dailyConsumptionState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyCostBars(data: List<DailyConsumption>) {
    val maxVal = data.maxOfOrNull { it.acusConsumed } ?: 1.0

    Column {
        data.takeLast(14).forEach { day ->
            val fraction = (day.acusConsumed / maxVal).toFloat().coerceIn(0f, 1f)
            val orgLabel = day.orgName ?: day.orgId ?: ""

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
                Spacer(modifier = Modifier.width(4.dp))
                LinearProgressIndicator(
                    progress = { fraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = AccentSecondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format("%.0f", day.acusConsumed),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun HistoricalCyclesSection(cyclesState: UiState<BillingCyclesResponse>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Historical Cycles",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            when (cyclesState) {
                is UiState.Loading -> ShimmerChartCard()
                is UiState.Success -> {
                    val cycles = cyclesState.data.cycles
                    if (cycles.isEmpty()) {
                        EmptyBillingState("No historical billing cycles available.")
                    } else {
                        cycles.forEachIndexed { index, cycle ->
                            ExpandableCycleItem(
                                cycle = cycle,
                                previousCycle = cycles.getOrNull(index + 1)
                            )
                            if (index < cycles.lastIndex) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = cyclesState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandableCycleItem(cycle: BillingCycle, previousCycle: BillingCycle?) {
    var expanded by remember { mutableStateOf(false) }

    val delta = if (previousCycle != null && previousCycle.acusUsed > 0) {
        ((cycle.acusUsed - previousCycle.acusUsed) / previousCycle.acusUsed * 100)
    } else null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${cycle.cycleStart.take(10)} - ${cycle.cycleEnd.take(10)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${String.format("%.0f", cycle.acusUsed)} ACUs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (delta != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val deltaColor = if (delta > 0) ErrorRed else SuccessGreen
                        Text(
                            text = "${if (delta > 0) "+" else ""}${String.format("%.1f", delta)}% vs prev",
                            style = MaterialTheme.typography.labelSmall,
                            color = deltaColor
                        )
                    }
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                DetailRow("Status", cycle.status.replaceFirstChar { it.uppercase() })
                DetailRow("ACU Limit", String.format("%.0f", cycle.acuLimit))
                DetailRow("ACUs Used", String.format("%.0f", cycle.acusUsed))
                val utilization = if (cycle.acuLimit > 0) {
                    (cycle.acusUsed / cycle.acuLimit * 100)
                } else 0.0
                DetailRow("Utilization", String.format("%.1f%%", utilization))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditLimitBottomSheet(
    editValue: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Edit ACU Limit",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = editValue,
                onValueChange = onValueChange,
                label = { Text("New ACU Limit") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = ErrorRed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = editValue.toDoubleOrNull() != null && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save Changes")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ConfirmLimitDialog(
    newLimit: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Confirm ACU Limit Change")
        },
        text = {
            Text(
                "Are you sure? This changes the ACU limit for the entire enterprise to $newLimit ACUs."
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary)
            ) {
                Text("Confirm")
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
private fun EmptyBillingState(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Payments,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
