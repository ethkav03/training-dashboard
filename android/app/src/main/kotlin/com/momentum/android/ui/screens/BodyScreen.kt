package com.momentum.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.momentum.android.network.dto.WeightEntryDto
import com.momentum.android.ui.charts.ChartLegendItem
import com.momentum.android.ui.charts.MomentumChartCard
import com.momentum.android.ui.charts.WeightTrendChart
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumButtonSize
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.components.MomentumCardTitle
import com.momentum.android.ui.forms.WeightEntryForm
import com.momentum.android.ui.theme.MomentumTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** Mirrors frontend/src/pages/progress/BodyTab.tsx. */
@Composable
fun BodyScreen() {
    val context = LocalContext.current
    val viewModel: BodyViewModel = viewModel(factory = remember { BodyViewModel.Factory(context) })
    val uiState by viewModel.state.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<WeightEntryDto?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            MomentumButton(text = "Log weight", size = MomentumButtonSize.Small, onClick = { showForm = true })
        }

        uiState.errorMessage?.let { Text(it, color = MomentumTheme.colors.statusCritical) }

        val trend = uiState.trend
        if (trend != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("Latest")
                    Text(trend.latestWeightKg?.let { "$it kg" } ?: "—", style = MaterialTheme.typography.titleMedium)
                }
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("7-day average")
                    Text(trend.movingAverage.lastOrNull()?.value?.let { "$it kg" } ?: "—", style = MaterialTheme.typography.titleMedium)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("Rate of change")
                    Text(trend.rateOfChangeKgPerWeek?.let { "$it kg/wk" } ?: "—", style = MaterialTheme.typography.titleMedium)
                }
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("Goal")
                    Text(trend.goalWeightKg?.let { "$it kg" } ?: "Not set", style = MaterialTheme.typography.titleMedium)
                }
            }

            MomentumChartCard(
                title = "Weight trend",
                subtitle = "Smoothed 7-day average",
                legend = listOf(ChartLegendItem(MomentumTheme.colors.series4, "7-day average")),
                isEmpty = trend.movingAverage.isEmpty(),
                emptyMessage = "No weigh-ins logged yet — log your first weight to start the trend.",
                chart = { WeightTrendChart(trend) },
                tableContent = {
                    Column {
                        trend.raw.sortedByDescending { it.date }.forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatDate(row.date), style = MaterialTheme.typography.labelSmall)
                                Text("${row.weightKg} kg", style = MaterialTheme.typography.labelSmall)
                                Text(row.source.name, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textMuted)
                            }
                        }
                    }
                },
            )

            MomentumCard {
                MomentumCardTitle("Recent weigh-ins")
                val recent = trend.raw.sortedByDescending { it.date }.take(10)
                if (recent.isEmpty()) {
                    Text("No weigh-ins yet.", color = MomentumTheme.colors.textMuted)
                } else {
                    recent.forEach { entry ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("${entry.weightKg} kg", style = MaterialTheme.typography.bodyMedium)
                                val subtitle = buildString {
                                    append(formatDate(entry.date))
                                    entry.note?.let { append(" · $it") }
                                    if (entry.source.name != "MANUAL") append(" · ${entry.source.name}")
                                }
                                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                            }
                            Row {
                                TextButton(onClick = { editingEntry = entry }) { Text("Edit") }
                                TextButton(onClick = { viewModel.delete(entry.id) }) {
                                    Text("Remove", color = MomentumTheme.colors.statusCritical)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        WeightEntryForm(
            entry = null,
            onDismiss = { showForm = false },
            onCreate = { body -> viewModel.create(body) { showForm = false } },
            onUpdate = { _, _ -> },
        )
    }
    editingEntry?.let { entry ->
        WeightEntryForm(
            entry = entry,
            onDismiss = { editingEntry = null },
            onCreate = {},
            onUpdate = { id, body -> viewModel.update(id, body) { editingEntry = null } },
        )
    }
}

private fun formatDate(iso: String): String =
    runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER) }.getOrDefault(iso)
