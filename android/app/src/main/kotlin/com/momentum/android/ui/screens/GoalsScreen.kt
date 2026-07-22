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
import com.momentum.android.ui.cards.GoalCard
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumButtonSize
import com.momentum.android.ui.forms.GoalForm
import com.momentum.android.ui.theme.MomentumTheme

/** Mirrors frontend/src/pages/GoalsPage.tsx. */
@Composable
fun GoalsScreen() {
    val context = LocalContext.current
    val viewModel: GoalsViewModel = viewModel(factory = remember { GoalsViewModel.Factory(context) })
    val uiState by viewModel.state.collectAsState()
    var showForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Goals", style = MaterialTheme.typography.headlineSmall)
            MomentumButton(text = "New goal", size = MomentumButtonSize.Small, onClick = { showForm = true })
        }

        uiState.errorMessage?.let { Text(it, color = MomentumTheme.colors.statusCritical) }

        if (!uiState.isLoading && uiState.goals.isEmpty()) {
            Text(
                "No goals yet — set one to turn your logging into progress you can track.",
                color = MomentumTheme.colors.textMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        uiState.goals.forEach { goal ->
            GoalCard(
                goal = goal,
                onTogglePause = { viewModel.togglePause(goal) },
                onDelete = { viewModel.delete(goal.id) },
            )
        }
    }

    if (showForm) {
        GoalForm(
            onDismiss = { showForm = false },
            onCreate = { body -> viewModel.create(body) { showForm = false } },
        )
    }
}
