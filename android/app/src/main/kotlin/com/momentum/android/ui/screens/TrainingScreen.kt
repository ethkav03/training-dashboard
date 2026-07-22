package com.momentum.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import com.momentum.android.network.dto.ActivityType
import com.momentum.android.network.dto.TrainingSessionDto
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumButtonSize
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.components.MomentumCardTitle
import com.momentum.android.ui.components.MomentumEditDeleteActions
import com.momentum.android.ui.forms.TrainingSessionForm
import com.momentum.android.ui.theme.MomentumTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val ACTIVITY_LABELS = mapOf(
    ActivityType.GYM to "Gym",
    ActivityType.RUNNING to "Running",
    ActivityType.TEAM_SPORT_TRAINING to "Team sport training",
    ActivityType.MATCH to "Match",
    ActivityType.CYCLING to "Cycling",
    ActivityType.WALKING to "Walking",
    ActivityType.RECOVERY_SESSION to "Recovery session",
    ActivityType.OTHER to "Other",
)

private val DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** Mirrors frontend/src/pages/TrainingPage.tsx. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrainingScreen(onOpenExercise: (String) -> Unit) {
    val context = LocalContext.current
    val viewModel: TrainingViewModel = viewModel(factory = remember { TrainingViewModel.Factory(context) })
    val uiState by viewModel.state.collectAsState()
    var showForm by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<TrainingSessionDto?>(null) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Training", style = MaterialTheme.typography.headlineSmall)
            MomentumButton(text = "Log session", size = MomentumButtonSize.Small, onClick = { showForm = true })
        }

        uiState.errorMessage?.let { Text(it, color = MomentumTheme.colors.statusCritical) }

        uiState.loadSummary?.let { summary ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("Weekly load")
                    Text("${summary.weeklyLoad}", style = MaterialTheme.typography.titleMedium)
                }
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("Sessions this week")
                    Text("${summary.sessionsThisWeek}", style = MaterialTheme.typography.titleMedium)
                }
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("Load ratio (7d:28d)")
                    Text(summary.acwr?.toString() ?: "—", style = MaterialTheme.typography.titleMedium)
                    if ((summary.acwr ?: 0.0) > 1.5) {
                        Text("Load rising quickly vs. recent average", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.statusSerious)
                    }
                }
            }
        }

        if (uiState.exerciseNames.isNotEmpty()) {
            MomentumCard {
                MomentumCardTitle("Exercise progression")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.exerciseNames.forEach { name ->
                        OutlinedButton(onClick = { onOpenExercise(name) }) { Text(name) }
                    }
                }
            }
        }

        MomentumCard {
            MomentumCardTitle("Recent sessions")
            if (uiState.sessions.isEmpty()) {
                Text("No sessions logged yet.", color = MomentumTheme.colors.textMuted)
            } else {
                uiState.sessions.forEach { session ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(ACTIVITY_LABELS[session.type] ?: session.type.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${formatDate(session.date)} · ${session.durationMin} min · intensity ${session.intensity}/10 · load ${session.trainingLoad}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MomentumTheme.colors.textMuted,
                                )
                            }
                            MomentumEditDeleteActions(
                                onEdit = { editingSession = session },
                                onDelete = { viewModel.delete(session.id) },
                            )
                        }
                        session.workout?.exercises?.forEach { exercise ->
                            val setsSummary = exercise.sets.joinToString(", ") { "${it.reps}x${it.weightKg}kg" }
                            Text(
                                "${exercise.name}: $setsSummary",
                                style = MaterialTheme.typography.labelSmall,
                                color = MomentumTheme.colors.textSecondary,
                            )
                        }
                        session.matchDetail?.let { match ->
                            val parts = buildList {
                                match.opponent?.let { add("vs $it") }
                                match.result?.let { add(it.name) }
                                match.performanceRating?.let { add("rating $it/10") }
                            }
                            if (parts.isNotEmpty()) {
                                Text(parts.joinToString(" · "), style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                            }
                        }
                        session.notes?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary) }
                    }
                }
            }
        }
    }

    if (showForm) {
        TrainingSessionForm(
            session = null,
            onDismiss = { showForm = false },
            onCreate = { body -> viewModel.create(body) { showForm = false } },
            onUpdate = { _, _ -> },
        )
    }
    editingSession?.let { session ->
        TrainingSessionForm(
            session = session,
            onDismiss = { editingSession = null },
            onCreate = {},
            onUpdate = { id, body -> viewModel.replace(id, body) { editingSession = null } },
        )
    }
}

private fun formatDate(iso: String): String =
    runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER) }.getOrDefault(iso)
