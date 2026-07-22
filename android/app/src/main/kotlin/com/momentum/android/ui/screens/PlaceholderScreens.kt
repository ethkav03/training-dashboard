package com.momentum.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentum.android.ui.theme.MomentumTheme

// Every tab here gets its own real screen in a later sprint (see
// docs/roadmap.md / the Android-parity plan) -- these exist only so the
// bottom-nav shell is fully navigable from Sprint 15 onward.
@Composable
private fun ComingSoonScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            "Coming in a later sprint.",
            style = MaterialTheme.typography.bodyMedium,
            color = MomentumTheme.colors.textSecondary,
        )
    }
}

@Composable
fun TodayPlaceholderScreen() = ComingSoonScreen("Today")

@Composable
fun ProgressPlaceholderScreen() = ComingSoonScreen("Progress")

@Composable
fun TrainingPlaceholderScreen() = ComingSoonScreen("Training")

@Composable
fun GoalsPlaceholderScreen() = ComingSoonScreen("Goals")

@Composable
fun InsightsPlaceholderScreen() = ComingSoonScreen("Insights")
