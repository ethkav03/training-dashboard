package com.momentum.android.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import com.momentum.android.auth.AuthViewModel
import com.momentum.android.healthconnect.HealthConnectManager
import com.momentum.android.healthconnect.HealthConnectViewModel
import com.momentum.android.network.dto.IntegrationProvider
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumButtonVariant
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.components.MomentumCardTitle
import com.momentum.android.ui.theme.MomentumTheme
import com.momentum.android.ui.theme.ThemePreference
import com.momentum.android.ui.theme.ThemeViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val SYNCED_AT_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

/**
 * Mirrors frontend/src/pages/SettingsPage.tsx's Profile + Integrations
 * sections -- absorbs what used to be the standalone SyncScreen (Health
 * Connect permission/sync UI). WHOOP connect/disconnect stay web-only (see
 * docs/architecture.md's Native Android app section), but syncing an
 * already-connected account needs no OAuth flow, so that's included here.
 * Units, energy baseline, and data controls land in Sprint 21.
 */
@Composable
fun SettingsScreen(
    authViewModel: AuthViewModel,
    healthConnectViewModel: HealthConnectViewModel,
    themeViewModel: ThemeViewModel,
) {
    val authState by authViewModel.state.collectAsState()
    val hcState by healthConnectViewModel.state.collectAsState()
    val themePreference by themeViewModel.preference.collectAsState()
    val context = LocalContext.current
    val integrationsViewModel: IntegrationsViewModel = viewModel(factory = remember { IntegrationsViewModel.Factory(context) })
    val integrationsState by integrationsViewModel.state.collectAsState()
    val whoop = integrationsState.connections.find { it.provider == IntegrationProvider.WHOOP }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = remember { healthConnectViewModel.requestPermissionsContract() },
    ) { granted -> healthConnectViewModel.onPermissionsResult(granted) }

    // Re-checks status every time this screen is entered -- e.g. after the
    // user grants permissions in the system sheet, or installs/updates
    // Health Connect from the Play Store and comes back.
    LaunchedEffect(Unit) {
        healthConnectViewModel.refreshStatus()
        integrationsViewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)

        MomentumCard {
            MomentumCardTitle("Profile")
            Text(authState.user?.name ?: "...", style = MaterialTheme.typography.bodyLarge)
            Text(
                authState.user?.email ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MomentumTheme.colors.textSecondary,
            )
        }

        MomentumCard {
            MomentumCardTitle("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOptionButton(
                    label = "System",
                    selected = themePreference == ThemePreference.SYSTEM,
                    onClick = { themeViewModel.setPreference(ThemePreference.SYSTEM) },
                )
                ThemeOptionButton(
                    label = "Light",
                    selected = themePreference == ThemePreference.LIGHT,
                    onClick = { themeViewModel.setPreference(ThemePreference.LIGHT) },
                )
                ThemeOptionButton(
                    label = "Dark",
                    selected = themePreference == ThemePreference.DARK,
                    onClick = { themeViewModel.setPreference(ThemePreference.DARK) },
                )
            }
        }

        MomentumCard {
            MomentumCardTitle("Health Connect")
            Text(
                "Weight, workouts and sleep from your phone",
                style = MaterialTheme.typography.bodySmall,
                color = MomentumTheme.colors.textSecondary,
            )

            when (hcState.availability) {
                HealthConnectManager.Availability.NOT_INSTALLED, HealthConnectManager.Availability.UPDATE_REQUIRED -> {
                    Text(
                        if (hcState.availability == HealthConnectManager.Availability.UPDATE_REQUIRED) {
                            "Health Connect needs an update before Momentum can sync."
                        } else {
                            "Install Health Connect to sync weight, workouts, and sleep."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    MomentumButton(
                        text = "Open Play Store",
                        onClick = { context.startActivity(healthConnectViewModel.installOrUpdateIntent()) },
                        variant = MomentumButtonVariant.Secondary,
                    )
                }

                HealthConnectManager.Availability.AVAILABLE -> {
                    if (!hcState.permissionsGranted) {
                        Text(
                            "Momentum needs permission to read weight, exercise, and sleep from Health Connect.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        MomentumButton(
                            text = "Grant Health Connect access",
                            onClick = { permissionLauncher.launch(healthConnectViewModel.permissions) },
                        )
                    } else {
                        MomentumButton(
                            text = if (hcState.isSyncing) "Syncing..." else "Sync now",
                            onClick = healthConnectViewModel::syncNow,
                            enabled = !hcState.isSyncing,
                            variant = MomentumButtonVariant.Secondary,
                        )
                        if (hcState.isSyncing) CircularProgressIndicator()
                        hcState.lastResult?.let { result ->
                            Text(
                                "Synced ${result.weightRecordsSynced} weigh-ins, " +
                                    "${result.trainingSessionsSynced} sessions, " +
                                    "${result.recoveryRecordsSynced} nights of sleep.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            val skipped = result.weightRecordsSkippedManualEdit +
                                result.trainingSessionsSkippedManualEdit +
                                result.recoveryRecordsSkippedManualEdit
                            if (skipped > 0) {
                                Text(
                                    "$skipped day(s) skipped -- already manually edited.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MomentumTheme.colors.textMuted,
                                )
                            }
                        }
                        hcState.errorMessage?.let { message ->
                            Text(
                                message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MomentumTheme.colors.statusCritical,
                            )
                        }
                    }
                }

                null -> Unit
            }
        }

        MomentumCard {
            MomentumCardTitle("WHOOP")
            Text(
                "Recovery, sleep and workout data",
                style = MaterialTheme.typography.bodySmall,
                color = MomentumTheme.colors.textSecondary,
            )

            if (whoop?.connected == true) {
                Text(
                    whoop.lastSyncAt?.let { "Last synced ${formatSyncedAt(it)}" } ?: "Connected — not synced yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MomentumTheme.colors.textSecondary,
                )
                MomentumButton(
                    text = if (integrationsState.isSyncingWhoop) "Syncing..." else "Sync now",
                    onClick = integrationsViewModel::syncWhoopNow,
                    enabled = !integrationsState.isSyncingWhoop,
                    variant = MomentumButtonVariant.Secondary,
                )
                if (integrationsState.isSyncingWhoop) CircularProgressIndicator()
                integrationsState.lastWhoopResult?.let { result ->
                    Text(
                        "Synced ${result.recoveryRecordsSynced} recovery day(s) and " +
                            "${result.trainingSessionsSynced} workout(s).",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                integrationsState.errorMessage?.let { message ->
                    Text(message, style = MaterialTheme.typography.bodySmall, color = MomentumTheme.colors.statusCritical)
                }
            } else {
                Text(
                    "Not connected yet — connect WHOOP from Settings on the web app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MomentumTheme.colors.textMuted,
                )
            }
        }

        MomentumButton(
            text = "Sign out",
            onClick = authViewModel::signOut,
            modifier = Modifier.fillMaxWidth(),
            variant = MomentumButtonVariant.Secondary,
        )
    }
}

private fun formatSyncedAt(iso: String): String =
    runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).format(SYNCED_AT_FORMATTER) }.getOrDefault(iso)

/** Mirrors web's Appearance section -- three buttons, the selected one filled rather than outlined. */
@Composable
private fun ThemeOptionButton(label: String, selected: Boolean, onClick: () -> Unit) {
    MomentumButton(
        text = label,
        onClick = onClick,
        variant = if (selected) MomentumButtonVariant.Primary else MomentumButtonVariant.Secondary,
    )
}
