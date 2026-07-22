package com.momentum.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Mirrors frontend/src/components/ui/Modal.tsx -- title + close button, a
 * bottom sheet (which is what the web version already becomes on mobile
 * viewports, so this is the more-faithful default here, not a compromise).
 * Every entry form (weight/meal/training/recovery/goal) is built on this.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentumModalSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                // Plain "✕" text, matching the web Modal's own literal
                // close glyph, rather than pulling in the Material Icons
                // library for one button.
                TextButton(onClick = onDismiss) { Text("✕") }
            }
            content()
        }
    }
}
