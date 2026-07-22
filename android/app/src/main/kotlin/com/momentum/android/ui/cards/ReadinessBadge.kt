package com.momentum.android.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentum.android.network.dto.ReadinessLevel
import com.momentum.android.ui.theme.MomentumTheme

/** Mirrors frontend/src/components/cards/ReadinessBadge.tsx. */
@Composable
fun ReadinessBadge(level: ReadinessLevel) {
    val (color, label) = when (level) {
        ReadinessLevel.HIGH -> MomentumTheme.colors.statusGood to "High readiness"
        ReadinessLevel.MODERATE -> MomentumTheme.colors.statusWarning to "Moderate readiness"
        ReadinessLevel.LOW -> MomentumTheme.colors.statusCritical to "Low readiness"
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(label, color = color, style = MaterialTheme.typography.labelMedium)
    }
}
