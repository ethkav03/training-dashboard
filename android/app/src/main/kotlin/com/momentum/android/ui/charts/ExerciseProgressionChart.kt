package com.momentum.android.ui.charts

import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentum.android.network.dto.ExerciseProgressionPointDto
import com.momentum.android.ui.theme.MomentumTheme
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

/**
 * Mirrors frontend/src/components/charts/ExerciseProgressionChart.tsx's
 * estimated-1RM line. Same Vico best-recollection caveat as
 * WeightTrendChart.kt/EnergyBalanceChart.kt. Scope trim: only the 1RM line
 * is rendered -- the web version also overlays a best-set-weight scatter
 * with PR points highlighted green, which needs the same multi-mark-type
 * Vico API this project is least sure of verbatim, so it's deferred rather
 * than guessed at alongside everything else in this chart.
 */
@Composable
fun ExerciseProgressionChart(points: List<ExerciseProgressionPointDto>) {
    val lineColor = MomentumTheme.colors.series1
    val entryProducer = remember(points) {
        ChartEntryModelProducer(points.mapIndexed { index, point -> entryOf(index.toFloat(), point.estimated1RM.toFloat()) })
    }

    Chart(
        chart = lineChart(lines = listOf(lineSpec(lineColor = lineColor))),
        chartModelProducer = entryProducer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(),
        modifier = Modifier.height(220.dp),
    )
}
