package com.momentum.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentum.android.auth.AuthViewModel

/**
 * Post-login placeholder for this sprint -- Health Connect permission
 * request, manual "Sync now", and background sync all land in the next
 * two sprints. This screen exists to prove sign-in end to end first.
 */
@Composable
fun SyncScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Signed in as ${state.user?.name ?: "..."}", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(state.user?.email ?: "", style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Health Connect sync arrives in the next build.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedButton(onClick = viewModel::signOut) {
            Text("Sign out")
        }
    }
}
