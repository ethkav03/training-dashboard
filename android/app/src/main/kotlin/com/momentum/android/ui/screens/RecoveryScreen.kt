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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.momentum.android.ui.cards.ReadinessBadge
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumButtonSize
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.components.MomentumCardTitle
import com.momentum.android.ui.forms.RecoveryEntryForm
import com.momentum.android.ui.theme.MomentumTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** Mirrors frontend/src/pages/progress/RecoveryTab.tsx. */
@Composable
fun RecoveryScreen() {
    val context = LocalContext.current
    val viewModel: RecoveryViewModel = viewModel(factory = remember { RecoveryViewModel.Factory(context) })
    val uiState by viewModel.state.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            MomentumButton(
                text = if (uiState.today != null) "Update today's recovery" else "Log recovery",
                size = MomentumButtonSize.Small,
                onClick = { showForm = true },
            )
        }

        uiState.errorMessage?.let { Text(it, color = MomentumTheme.colors.statusCritical) }

        MomentumCard {
            MomentumCardTitle("Today's readiness")
            val today = uiState.today
            if (today == null) {
                if (!uiState.isLoading) {
                    Text("No recovery data logged today yet.", color = MomentumTheme.colors.textMuted)
                }
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Recovery", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                        Text("${today.readinessScore}", style = MaterialTheme.typography.headlineSmall)
                        ReadinessBadge(today.readinessLevel)
                    }
                    Column {
                        Text("Sleep", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                        Text("${today.sleepScore ?: "—"}", style = MaterialTheme.typography.headlineSmall)
                        today.sleepHours?.let { Text("${it}h", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textMuted) }
                    }
                    Column {
                        Text("Yesterday's strain", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                        Text(today.strain?.let { "%.1f".format(it) } ?: "—", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Text(today.recommendation, style = MaterialTheme.typography.bodyMedium)
            }
        }

        MomentumCard {
            MomentumCardTitle("History")
            if (uiState.history.isEmpty()) {
                Text("No history yet.", color = MomentumTheme.colors.textMuted)
            } else {
                uiState.history.forEach { record ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(formatDate(record.date), style = MaterialTheme.typography.bodyMedium)
                            val subtitle = buildString {
                                record.sleepHours?.let { append("${it}h sleep") }
                                record.sleepQuality?.let { append(" · quality $it/5") }
                                record.soreness?.let { append(" · soreness $it/5") }
                            }
                            if (subtitle.isNotEmpty()) {
                                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text("Sleep", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textMuted)
                                Text("${record.sleepScore ?: "—"}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text("Strain", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textMuted)
                                Text(record.strain?.let { "%.1f".format(it) } ?: "—", style = MaterialTheme.typography.bodyMedium)
                            }
                            Column {
                                Text("${record.readinessScore}", style = MaterialTheme.typography.bodyMedium)
                                ReadinessBadge(record.readinessLevel)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        RecoveryEntryForm(
            onDismiss = { showForm = false },
            onSave = { body -> viewModel.logRecovery(body) { showForm = false } },
        )
    }
}

private fun formatDate(iso: String): String =
    runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).format(DATE_FORMATTER) }.getOrDefault(iso)
