package com.momentum.android.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.momentum.android.ui.theme.MomentumTheme

enum class MomentumButtonVariant { Primary, Secondary, Ghost, Danger }
enum class MomentumButtonSize { Small, Medium }

/** Mirrors frontend/src/components/ui/Button.tsx's variant/size matrix. */
@Composable
fun MomentumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: MomentumButtonVariant = MomentumButtonVariant.Primary,
    size: MomentumButtonSize = MomentumButtonSize.Medium,
    enabled: Boolean = true,
) {
    val padding = if (size == MomentumButtonSize.Small) {
        PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    } else {
        PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    }

    when (variant) {
        MomentumButtonVariant.Primary -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentPadding = padding,
            colors = ButtonDefaults.buttonColors(containerColor = MomentumTheme.colors.series1),
        ) { Text(text) }

        MomentumButtonVariant.Secondary -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentPadding = padding,
        ) { Text(text) }

        MomentumButtonVariant.Ghost -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentPadding = padding,
        ) { Text(text) }

        MomentumButtonVariant.Danger -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            contentPadding = padding,
            colors = ButtonDefaults.buttonColors(containerColor = MomentumTheme.colors.statusCritical),
        ) { Text(text) }
    }
}
