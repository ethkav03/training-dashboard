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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.momentum.android.ui.charts.ChartLegendItem
import com.momentum.android.ui.charts.ExerciseProgressionChart
import com.momentum.android.ui.charts.MomentumChartCard
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.components.MomentumCardTitle
import com.momentum.android.ui.theme.MomentumTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** Mirrors frontend/src/pages/training/ExerciseProgressionPage.tsx. */
@Composable
fun ExerciseProgressionScreen(exerciseName: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: ExerciseProgressionViewModel = viewModel(factory = remember { ExerciseProgressionViewModel.Factory(context) })
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(exerciseName) { viewModel.load(exerciseName) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            TextButton(onClick = onBack) { Text("← Training") }
            Text(exerciseName, style = MaterialTheme.typography.headlineSmall)
        }

        uiState.errorMessage?.let { Text(it, color = MomentumTheme.colors.statusCritical) }

        val points = uiState.points
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MomentumCard(modifier = Modifier.fillMaxWidth()) {
                MomentumCardTitle("Best weight")
                Text(points.maxOfOrNull { it.bestWeightKg }?.let { "$it kg" } ?: "—", style = MaterialTheme.typography.titleMedium)
            }
            MomentumCard(modifier = Modifier.fillMaxWidth()) {
                MomentumCardTitle("Best est. 1RM")
                Text(points.maxOfOrNull { it.estimated1RM }?.let { "$it kg" } ?: "—", style = MaterialTheme.typography.titleMedium)
            }
            MomentumCard(modifier = Modifier.fillMaxWidth()) {
                MomentumCardTitle("Sessions logged")
                Text("${points.size}", style = MaterialTheme.typography.titleMedium)
            }
        }

        MomentumChartCard(
            title = "Progression",
            subtitle = "Estimated 1RM per session",
            legend = listOf(ChartLegendItem(MomentumTheme.colors.series1, "Estimated 1RM")),
            isEmpty = points.isEmpty(),
            emptyMessage = "No sessions logged for this exercise yet.",
            chart = { ExerciseProgressionChart(points) },
            tableContent = {
                Column {
                    points.forEach { point ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatDate(point.date), style = MaterialTheme.typography.labelSmall)
                            Text("${point.bestWeightKg} kg", style = MaterialTheme.typography.labelSmall)
                            Text("${point.estimated1RM} kg", style = MaterialTheme.typography.labelSmall)
                            Text(if (point.isPr) "PR" else "", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.statusGood)
                        }
                    }
                }
            },
        )
    }
}

private fun formatDate(iso: String): String =
    runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER) }.getOrDefault(iso)
