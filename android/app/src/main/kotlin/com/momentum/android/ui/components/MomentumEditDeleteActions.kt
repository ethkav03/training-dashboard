package com.momentum.android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentum.android.ui.theme.MomentumTheme

/**
 * Pen + trash icon buttons for the "Edit" / "Remove" row action pattern
 * repeated across Body/Fuel/Training's entry lists -- icons over text
 * per the user's request, and pulled into one place since the exact same
 * pair of actions shows up on every entry list in the app.
 */
@Composable
fun MomentumEditDeleteActions(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = MomentumTheme.colors.textSecondary)
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = MomentumTheme.colors.statusCritical)
        }
    }
}

/** A lone trash icon for "remove this row" actions with no paired edit action -- e.g. removing a dynamic form row. */
@Composable
fun MomentumDeleteIconButton(onClick: () -> Unit, modifier: Modifier = Modifier, contentDescription: String = "Remove") {
    IconButton(onClick = onClick, modifier = modifier.size(32.dp)) {
        Icon(imageVector = Icons.Default.Delete, contentDescription = contentDescription, tint = MomentumTheme.colors.statusCritical)
    }
}
