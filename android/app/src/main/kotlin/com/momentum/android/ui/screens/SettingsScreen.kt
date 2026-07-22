package com.momentum.android.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.momentum.android.auth.AuthViewModel
import com.momentum.android.healthconnect.HealthConnectManager
import com.momentum.android.healthconnect.HealthConnectViewModel
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumButtonVariant
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.components.MomentumCardTitle
import com.momentum.android.ui.theme.MomentumTheme

/**
 * Mirrors frontend/src/pages/SettingsPage.tsx's Profile + Integrations
 * sections -- absorbs what used to be the standalone SyncScreen (Health
 * Connect permission/sync UI). Units, appearance, energy baseline, WHOOP
 * status, and data controls all land in Sprint 21; this sprint only moves
 * what already existed into its new home.
 */
@Composable
fun SettingsScreen(authViewModel: AuthViewModel, healthConnectViewModel: HealthConnectViewModel) {
    val authState by authViewModel.state.collectAsState()
    val hcState by healthConnectViewModel.state.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = remember { healthConnectViewModel.requestPermissionsContract() },
    ) { granted -> healthConnectViewModel.onPermissionsResult(granted) }

    // Re-checks status every time this screen is entered -- e.g. after the
    // user grants permissions in the system sheet, or installs/updates
    // Health Connect from the Play Store and comes back.
    LaunchedEffect(Unit) { healthConnectViewModel.refreshStatus() }

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

        MomentumButton(
            text = "Sign out",
            onClick = authViewModel::signOut,
            modifier = Modifier.fillMaxWidth(),
            variant = MomentumButtonVariant.Secondary,
        )
    }
}
