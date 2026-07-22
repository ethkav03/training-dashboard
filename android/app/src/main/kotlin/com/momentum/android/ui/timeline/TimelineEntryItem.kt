package com.momentum.android.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.momentum.android.network.dto.TimelineEntryDto
import com.momentum.android.network.dto.TimelineEntryKind
import com.momentum.android.ui.theme.MomentumTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private data class KindStyle(val color: Color, val label: String)

private val TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a")

/** Mirrors frontend/src/components/timeline/TimelineEntryItem.tsx -- shared by Today's preview and the full Timeline screen (Sprint 20). */
@Composable
fun TimelineEntryItem(entry: TimelineEntryDto) {
    val style = when (entry.kind) {
        TimelineEntryKind.WEIGHT -> KindStyle(MomentumTheme.colors.series4, "Body")
        TimelineEntryKind.MEAL -> KindStyle(MomentumTheme.colors.series2, "Fuel")
        TimelineEntryKind.TRAINING -> KindStyle(MomentumTheme.colors.series1, "Training")
        TimelineEntryKind.RECOVERY -> KindStyle(MomentumTheme.colors.series3, "Recovery")
        TimelineEntryKind.ACHIEVEMENT -> KindStyle(MomentumTheme.colors.series5, "Achievement")
    }
    val time = runCatching {
        Instant.parse(entry.date).atZone(ZoneId.systemDefault()).format(TIME_FORMATTER)
    }.getOrDefault("")

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp, end = 12.dp)
                .size(8.dp)
                .background(style.color, CircleShape),
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(entry.title, style = MaterialTheme.typography.bodyMedium)
                Text(time, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textMuted)
            }
            entry.subtitle?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
            }
        }
    }
}
