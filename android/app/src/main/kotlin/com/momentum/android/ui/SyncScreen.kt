package com.momentum.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.momentum.android.auth.AuthViewModel
import com.momentum.android.healthconnect.HealthConnectManager
import com.momentum.android.healthconnect.HealthConnectViewModel

@Composable
fun SyncScreen(authViewModel: AuthViewModel, healthConnectViewModel: HealthConnectViewModel) {
    val authState by authViewModel.state.collectAsState()
    val hcState by healthConnectViewModel.state.collectAsState()
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = remember { healthConnectViewModel.requestPermissionsContract() }
    ) { granted -> healthConnectViewModel.onPermissionsResult(granted) }

    // Re-checks status when returning to this screen -- e.g. after the user
    // grants permissions in the system sheet, or installs/updates Health
    // Connect from the Play Store and comes back.
    LaunchedEffect(Unit) { healthConnectViewModel.refreshStatus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Signed in as ${authState.user?.name ?: "..."}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(authState.user?.email ?: "", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(32.dp))

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
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { context.startActivity(healthConnectViewModel.installOrUpdateIntent()) }) {
                    Text("Open Play Store")
                }
            }

            HealthConnectManager.Availability.AVAILABLE -> {
                if (!hcState.permissionsGranted) {
                    Text(
                        "Momentum needs permission to read weight, exercise, and sleep from Health Connect.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(healthConnectViewModel.permissions) }) {
                        Text("Grant Health Connect access")
                    }
                } else {
                    Button(onClick = healthConnectViewModel::syncNow, enabled = !hcState.isSyncing) {
                        Text(if (hcState.isSyncing) "Syncing..." else "Sync now")
                    }
                    if (hcState.isSyncing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator()
                    }
                    hcState.lastResult?.let { result ->
                        Spacer(modifier = Modifier.height(16.dp))
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
                            )
                        }
                    }
                    hcState.errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            null -> Unit
        }

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = authViewModel::signOut) {
            Text("Sign out")
        }
    }
}
