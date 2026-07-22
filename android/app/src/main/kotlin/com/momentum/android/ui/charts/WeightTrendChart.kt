package com.momentum.android.ui.charts

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentum.android.network.dto.WeightTrendDto
import com.momentum.android.ui.theme.MomentumTheme
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf

/**
 * Mirrors frontend/src/components/charts/WeightTrendChart.tsx's moving-
 * average line. NOTE: the exact Vico Compose API (package paths, composable
 * names) is written from best recollection, not verified against live docs
 * -- same caveat as every other unverified Android dependency in this
 * project. If this doesn't compile, check
 * https://patrykandpatrick.com/vico/guide for the current API shape.
 *
 * Scope trim for this pass: only the smoothed 7-day average line is
 * rendered. The web version also overlays raw weigh-ins as a scatter and a
 * dashed goal-weight reference line -- Vico supports both, but combining
 * multiple mark types on one Chart is the part of its API I'm least sure of
 * verbatim, so it's deferred rather than guessed at three ways at once.
 */
@Composable
fun WeightTrendChart(trend: WeightTrendDto) {
    val averageColor = MomentumTheme.colors.series4
    val entryProducer = remember(trend.movingAverage) {
        ChartEntryModelProducer(
            trend.movingAverage.mapIndexed { index, point -> entryOf(index.toFloat(), point.value.toFloat()) },
        )
    }

    if (trend.movingAverage.isEmpty()) {
        Text("Not enough data yet for this view.")
        return
    }

    Chart(
        chart = lineChart(lines = listOf(lineSpec(lineColor = averageColor))),
        chartModelProducer = entryProducer,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(),
        modifier = Modifier.height(220.dp),
    )
}
