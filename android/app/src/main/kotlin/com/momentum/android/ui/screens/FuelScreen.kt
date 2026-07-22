package com.momentum.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.momentum.android.network.dto.EnergyBalanceGranularity
import com.momentum.android.network.dto.MealType
import com.momentum.android.network.dto.NutritionEntryDto
import com.momentum.android.ui.charts.ChartLegendItem
import com.momentum.android.ui.charts.EnergyBalanceChart
import com.momentum.android.ui.charts.MomentumChartCard
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumButtonSize
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.components.MomentumCardTitle
import com.momentum.android.ui.components.MomentumEditDeleteActions
import com.momentum.android.ui.forms.NutritionEntryForm
import com.momentum.android.ui.theme.MomentumTheme
import kotlin.math.max

private val GRANULARITY_TABS = listOf(
    EnergyBalanceGranularity.DAY to "Day",
    EnergyBalanceGranularity.WEEK to "Week",
    EnergyBalanceGranularity.MONTH to "Month",
    EnergyBalanceGranularity.YEAR to "Year",
)

private val MEAL_SECTIONS = listOf(
    MealType.BREAKFAST to "Breakfast",
    MealType.LUNCH to "Lunch",
    MealType.DINNER to "Dinner",
    MealType.SNACKS to "Snacks",
)

/** Mirrors frontend/src/pages/progress/FuelTab.tsx. */
@Composable
fun FuelScreen() {
    val context = LocalContext.current
    val viewModel: FuelViewModel = viewModel(factory = remember { FuelViewModel.Factory(context) })
    val uiState by viewModel.state.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var formDefaultMealType by remember { mutableStateOf<MealType?>(null) }
    var editingEntry by remember { mutableStateOf<NutritionEntryDto?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    fun openLogForm(mealType: MealType?) {
        formDefaultMealType = mealType
        showForm = true
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            MomentumButton(text = "Log meal", size = MomentumButtonSize.Small, onClick = { openLogForm(null) })
        }

        uiState.errorMessage?.let { Text(it, color = MomentumTheme.colors.statusCritical) }

        val summary = uiState.summary
        if (summary != null) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("Calories consumed")
                    Text("${summary.totalCalories}", style = MaterialTheme.typography.titleMedium)
                    summary.targetCalories?.let {
                        Text("of $it target", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textMuted)
                    }
                }
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("Estimated burn")
                    Text("${summary.estimatedBurn.totalKcal}", style = MaterialTheme.typography.titleMedium)
                    Text("estimate — baseline + training", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textMuted)
                }
            }
            MomentumCard {
                MomentumCardTitle("Energy balance")
                Text(
                    summary.balanceKcal?.let { "${if (it > 0) "+" else ""}$it" } ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (summary.balanceKcal == null) "Set a daily burn baseline in Settings" else "estimate — consumed minus burned",
                    style = MaterialTheme.typography.labelSmall,
                    color = MomentumTheme.colors.textMuted,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Consumed vs. burned", style = MaterialTheme.typography.titleSmall)
                Row {
                    GRANULARITY_TABS.forEach { (value, label) ->
                        TextButton(onClick = { viewModel.setGranularity(value) }) {
                            Text(
                                label,
                                color = if (uiState.granularity == value) MaterialTheme.colorScheme.primary else MomentumTheme.colors.textSecondary,
                            )
                        }
                    }
                }
            }

            val latest = uiState.energyBalance.lastOrNull()
            if (latest != null) {
                val maxValue = max(max(latest.totalCalories, latest.totalBurnKcal), 1)
                MomentumCard {
                    MomentumCardTitle("Latest ${uiState.granularity.name.lowercase()}")
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Consumed", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                            Text("${latest.totalCalories} kcal", style = MaterialTheme.typography.labelSmall)
                        }
                        BalanceBar(fraction = latest.totalCalories.toFloat() / maxValue, color = MomentumTheme.colors.series2)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Burned (estimate)", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                            Text("${latest.totalBurnKcal} kcal", style = MaterialTheme.typography.labelSmall)
                        }
                        BalanceBar(fraction = latest.totalBurnKcal.toFloat() / maxValue, color = MomentumTheme.colors.series1)
                    }
                }
            }

            MomentumChartCard(
                title = "Trend",
                legend = listOf(
                    ChartLegendItem(MomentumTheme.colors.series2, "Consumed"),
                    ChartLegendItem(MomentumTheme.colors.series1, "Burned (estimate)"),
                ),
                isEmpty = uiState.energyBalance.isEmpty(),
                emptyMessage = "Not enough data yet for this view.",
                chart = { EnergyBalanceChart(uiState.energyBalance) },
                tableContent = {
                    Column {
                        uiState.energyBalance.forEach { point ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(point.period, style = MaterialTheme.typography.labelSmall)
                                Text("${point.totalCalories}", style = MaterialTheme.typography.labelSmall)
                                Text("${point.totalBurnKcal}", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    point.balanceKcal?.let { "${if (it > 0) "+" else ""}$it" } ?: "—",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                },
            )
        }

        MomentumCard {
            MomentumCardTitle("Today's meals")
            if (uiState.todayEntries.isEmpty()) {
                Text("No meals logged today yet.", color = MomentumTheme.colors.textMuted)
            }
            MEAL_SECTIONS.forEach { (type, label) ->
                val items = uiState.todayEntries.filter { it.mealType == type }
                val subtotal = items.sumOf { it.calories }
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(label, style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (items.isNotEmpty()) {
                                Text("$subtotal kcal", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                            }
                            TextButton(onClick = { openLogForm(type) }) { Text("+ Add") }
                        }
                    }
                    if (items.isEmpty()) {
                        Text("Nothing logged yet.", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textMuted)
                    } else {
                        items.forEach { entry ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(entry.mealName ?: "Item", style = MaterialTheme.typography.bodyMedium)
                                    val macros = buildString {
                                        append("${entry.calories} kcal")
                                        entry.proteinG?.let { append(" · ${it}g protein") }
                                        entry.carbsG?.let { append(" · ${it}g carbs") }
                                        entry.fatG?.let { append(" · ${it}g fat") }
                                    }
                                    Text(macros, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                                }
                                MomentumEditDeleteActions(
                                    onEdit = { editingEntry = entry },
                                    onDelete = { viewModel.delete(entry.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        NutritionEntryForm(
            entry = null,
            defaultMealType = formDefaultMealType,
            onDismiss = { showForm = false },
            onCreate = { body -> viewModel.create(body) { showForm = false } },
            onUpdate = { _, _ -> },
        )
    }
    editingEntry?.let { entry ->
        NutritionEntryForm(
            entry = entry,
            defaultMealType = null,
            onDismiss = { editingEntry = null },
            onCreate = {},
            onUpdate = { id, body -> viewModel.update(id, body) { editingEntry = null } },
        )
    }
}

@Composable
private fun BalanceBar(fraction: Float, color: Color) {
    Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(MomentumTheme.colors.page, RoundedCornerShape(3.dp))) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .background(color, RoundedCornerShape(3.dp)),
        )
    }
}
