package com.momentum.android.ui.cards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import com.momentum.android.network.dto.InsightDto
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.theme.MomentumTheme

private val TREND_ARROW = mapOf("up" to "↑", "down" to "↓", "flat" to "→")

/** Mirrors frontend/src/components/cards/InsightCard.tsx. */
@Composable
fun InsightCard(insight: InsightDto) {
    MomentumCard {
        Row(verticalAlignment = Alignment.Top) {
            Text(TREND_ARROW[insight.trend] ?: "→", color = MomentumTheme.colors.textMuted)
            Column {
                Text(insight.headline, style = MaterialTheme.typography.bodyMedium)
                Text(insight.detail, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
            }
        }
    }
}
