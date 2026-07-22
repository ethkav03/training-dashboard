package com.momentum.android.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumButtonSize
import com.momentum.android.ui.components.MomentumButtonVariant
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.components.MomentumCardTitle
import com.momentum.android.ui.theme.MomentumTheme

data class ChartLegendItem(val color: Color, val label: String)

/**
 * Mirrors frontend/src/components/charts/ChartCard.tsx -- title/subtitle,
 * a legend (only shown for 2+ series, per the dataviz rule that a single
 * series needs no legend box), a chart/table toggle, and an empty state.
 * Unlike the web version's generic column-definition table, `tableContent`
 * is just a Composable slot -- each call site builds its own table rows,
 * which is more idiomatic in Compose than porting a declarative column API.
 */
@Composable
fun MomentumChartCard(
    title: String,
    subtitle: String? = null,
    legend: List<ChartLegendItem> = emptyList(),
    isEmpty: Boolean,
    emptyMessage: String,
    chart: @Composable () -> Unit,
    tableContent: @Composable () -> Unit,
) {
    var showTable by remember { mutableStateOf(false) }

    MomentumCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                MomentumCardTitle(title)
                subtitle?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textMuted) }
            }
            if (!isEmpty) {
                MomentumButton(
                    text = if (showTable) "Chart view" else "Table view",
                    size = MomentumButtonSize.Small,
                    variant = MomentumButtonVariant.Ghost,
                    onClick = { showTable = !showTable },
                )
            }
        }

        if (isEmpty) {
            Text(emptyMessage, color = MomentumTheme.colors.textMuted, style = MaterialTheme.typography.bodySmall)
        } else {
            if (legend.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    legend.forEach { item ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).background(item.color, CircleShape))
                            Text(item.label, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                        }
                    }
                }
            }

            if (showTable) tableContent() else chart()
        }
    }
}
