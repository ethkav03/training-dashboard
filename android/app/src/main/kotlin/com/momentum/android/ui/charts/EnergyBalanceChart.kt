package com.momentum.android.ui.charts

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentum.android.network.dto.EnergyBalancePointDto
import com.momentum.android.ui.theme.MomentumTheme
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.component.lineComponent
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

/**
 * Mirrors frontend/src/components/charts/EnergyBalanceChart.tsx's grouped
 * consumed-vs-burned bars. Same Vico best-recollection caveat as
 * WeightTrendChart.kt -- verify against https://patrykandpatrick.com/vico/guide
 * if this doesn't compile, particularly the two-series grouped-column setup
 * (ChartEntryModelProducer with multiple entry collections + ColumnChart's
 * merge mode), which is the part of this file I'm least certain of verbatim.
 * `lineComponent`'s package was already wrong once (guessed
 * chart.column, actually needed component) -- if it's still unresolved,
 * the fallback is to drop the custom `columns` param entirely and let
 * `columnChart()` use its own default styling (loses brand-color matching
 * for this one chart, but compiles and still shows two visually distinct bars).
 */
@Composable
fun EnergyBalanceChart(points: List<EnergyBalancePointDto>) {
    val consumedColor = MomentumTheme.colors.series2
    val burnedColor = MomentumTheme.colors.series1

    if (points.isEmpty()) {
        Text("Not enough data yet for this view.")
        return
    }

    val entryProducer = remember(points) {
        val consumed = points.mapIndexed { index, point -> entryOf(index.toFloat(), point.totalCalories.toFloat()) }
        val burned = points.mapIndexed { index, point -> entryOf(index.toFloat(), point.totalBurnKcal.toFloat()) }
        ChartEntryModelProducer(consumed, burned)
    }

    Chart(
        chart = columnChart(
            columns = listOf(
                lineComponent(color = consumedColor, thickness = 8.dp),
                lineComponent(color = burnedColor, thickness = 8.dp),
            ),
        ),
        chartModelProducer = entryProducer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(),
        modifier = Modifier.height(220.dp),
    )
}
