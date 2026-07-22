package com.momentum.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.momentum.android.ui.cards.InsightCard
import com.momentum.android.ui.theme.MomentumTheme

/** Mirrors frontend/src/pages/InsightsPage.tsx -- read-only, no forms. */
@Composable
fun InsightsScreen() {
    val context = LocalContext.current
    val viewModel: InsightsViewModel = viewModel(factory = remember { InsightsViewModel.Factory(context) })
    val uiState by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Insights", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Simple, explainable trends from your own data -- every insight shows the numbers behind it.",
            style = MaterialTheme.typography.bodyMedium,
            color = MomentumTheme.colors.textSecondary,
        )

        uiState.errorMessage?.let { Text(it, color = MomentumTheme.colors.statusCritical) }

        if (!uiState.isLoading && uiState.insights.isEmpty()) {
            Text(
                "Not enough history yet to surface a trend. Keep logging and check back in a couple of weeks.",
                style = MaterialTheme.typography.bodyMedium,
                color = MomentumTheme.colors.textMuted,
            )
        }

        uiState.insights.forEach { insight -> InsightCard(insight) }
    }
}
