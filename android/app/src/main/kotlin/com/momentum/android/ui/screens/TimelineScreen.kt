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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumButtonSize
import com.momentum.android.ui.components.MomentumButtonVariant
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.theme.MomentumTheme
import com.momentum.android.ui.timeline.TimelineEntryItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val RANGE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM d")
private val DAY_HEADER_FORMATTER = DateTimeFormatter.ofPattern("EEEE, MMM d")

/** Mirrors frontend/src/pages/TimelinePage.tsx -- week-paged, grouped-by-day cross-pillar feed. */
@Composable
fun TimelineScreen() {
    val context = LocalContext.current
    val viewModel: TimelineViewModel = viewModel(factory = remember { TimelineViewModel.Factory(context) })
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    val (rangeStart, rangeEnd) = viewModel.weekRange(uiState.weekOffset)
    val groupedByDay = remember(uiState.entries) {
        uiState.entries
            .mapNotNull { entry ->
                val date = runCatching { Instant.parse(entry.date).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
                date?.let { it to entry }
            }
            .groupBy({ it.first }, { it.second })
            .toSortedMap(compareByDescending { it })
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Timeline", style = MaterialTheme.typography.headlineSmall)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MomentumButton(
                text = "← Previous week",
                size = MomentumButtonSize.Small,
                variant = MomentumButtonVariant.Secondary,
                onClick = viewModel::goToPreviousWeek,
            )
            Text(
                "${rangeStart.format(RANGE_LABEL_FORMATTER)} – ${rangeEnd.format(RANGE_LABEL_FORMATTER)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MomentumTheme.colors.textSecondary,
            )
            MomentumButton(
                text = "Next week →",
                size = MomentumButtonSize.Small,
                variant = MomentumButtonVariant.Secondary,
                onClick = viewModel::goToNextWeek,
            )
            if (uiState.weekOffset != 0L) {
                MomentumButton(
                    text = "Today",
                    size = MomentumButtonSize.Small,
                    variant = MomentumButtonVariant.Ghost,
                    onClick = viewModel::goToToday,
                )
            }
        }

        uiState.errorMessage?.let { Text(it, color = MomentumTheme.colors.statusCritical) }

        if (!uiState.isLoading && groupedByDay.isEmpty()) {
            Text("Nothing logged in this range.", color = MomentumTheme.colors.textMuted, style = MaterialTheme.typography.bodyMedium)
        }

        groupedByDay.forEach { (day, dayEntries) ->
            MomentumCard {
                Text(
                    day.format(DAY_HEADER_FORMATTER),
                    style = MaterialTheme.typography.labelLarge,
                    color = MomentumTheme.colors.textSecondary,
                )
                dayEntries.forEach { entry -> TimelineEntryItem(entry) }
            }
        }
    }
}
