package com.momentum.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentum.android.auth.AuthViewModel

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Momentum", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text("See your progress. Build your momentum.", style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = viewModel::signIn, enabled = !state.isLoading) {
            Text(if (state.isLoading) "Signing in..." else "Continue with Google")
        }

        state.errorMessage?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
