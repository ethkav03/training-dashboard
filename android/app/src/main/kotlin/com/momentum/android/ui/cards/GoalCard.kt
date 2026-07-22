package com.momentum.android.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentum.android.network.dto.GoalDto
import com.momentum.android.network.dto.GoalStatus
import com.momentum.android.network.dto.GoalType
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.theme.MomentumTheme
import kotlin.math.round

private val TYPE_LABELS = mapOf(
    GoalType.BODY_WEIGHT to "Body weight",
    GoalType.CALORIE_INTAKE to "Calorie intake",
    GoalType.PROTEIN_INTAKE to "Protein intake",
    GoalType.TRAINING_FREQUENCY to "Training frequency",
    GoalType.EXERCISE_PERFORMANCE to "Exercise performance",
    GoalType.SPORT_PERFORMANCE to "Sport performance",
    GoalType.SLEEP_RECOVERY to "Sleep & recovery",
    GoalType.CUSTOM to "Custom",
)

/**
 * Mirrors frontend/src/components/cards/GoalCard.tsx. Pause/resume and
 * delete are plain callbacks here rather than inline mutations -- the
 * screen's ViewModel owns the actual network call, this stays presentation-only.
 */
@Composable
fun GoalCard(
    goal: GoalDto,
    onTogglePause: () -> Unit,
    onDelete: () -> Unit,
) {
    val (statusColor, statusLabel) = when (goal.status) {
        GoalStatus.ON_TRACK -> MomentumTheme.colors.statusGood to "On track"
        GoalStatus.AT_RISK -> MomentumTheme.colors.statusWarning to "At risk"
        GoalStatus.ACHIEVED -> MomentumTheme.colors.statusGood to "Achieved"
        GoalStatus.PAUSED -> MomentumTheme.colors.textMuted to "Paused"
    }
    val clampedProgress = goal.progressPercent?.coerceIn(0.0, 100.0)

    MomentumCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(
                    TYPE_LABELS[goal.type] ?: goal.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MomentumTheme.colors.textMuted,
                )
                Text(goal.title, style = MaterialTheme.typography.bodyMedium)
            }
            Text(statusLabel, style = MaterialTheme.typography.labelSmall, color = statusColor)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(MomentumTheme.colors.page, RoundedCornerShape(4.dp)),
            ) {
                if (clampedProgress != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction = (clampedProgress / 100.0).toFloat())
                            .height(8.dp)
                            .background(statusColor, RoundedCornerShape(4.dp)),
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val current = goal.currentValue?.let { round(it * 10) / 10 }
                val currentText = buildString {
                    append(current?.toString() ?: "—")
                    if (goal.targetValue != null) append(" / ${goal.targetValue}")
                    if (!goal.targetUnit.isNullOrBlank()) append(" ${goal.targetUnit}")
                }
                Text(currentText, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                Text(
                    if (clampedProgress != null) "${clampedProgress.toInt()}%" else "no data yet",
                    style = MaterialTheme.typography.labelSmall,
                    color = MomentumTheme.colors.textSecondary,
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (goal.status != GoalStatus.ACHIEVED) {
                TextButton(onClick = onTogglePause) {
                    Text(if (goal.status == GoalStatus.PAUSED) "Resume" else "Pause")
                }
            }
            TextButton(onClick = onDelete) {
                Text("Delete", color = MomentumTheme.colors.statusCritical)
            }
        }
    }
}
