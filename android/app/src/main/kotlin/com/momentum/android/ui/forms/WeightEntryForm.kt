package com.momentum.android.ui.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.momentum.android.network.dto.CreateWeightEntryRequest
import com.momentum.android.network.dto.UpdateWeightEntryRequest
import com.momentum.android.network.dto.WeightEntryDto
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumModalSheet
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Mirrors frontend/src/components/forms/WeightEntryForm.tsx. Same component for create and edit -- pass `entry` to edit. */
@Composable
fun WeightEntryForm(
    entry: WeightEntryDto?,
    onDismiss: () -> Unit,
    onCreate: (CreateWeightEntryRequest) -> Unit,
    onUpdate: (String, UpdateWeightEntryRequest) -> Unit,
) {
    var weightKg by remember { mutableStateOf(entry?.weightKg?.toString() ?: "") }
    var note by remember { mutableStateOf(entry?.note ?: "") }
    val date = remember {
        entry?.date?.let { Instant.parse(it).atZone(ZoneId.systemDefault()).toLocalDate() } ?: LocalDate.now()
    }

    MomentumModalSheet(title = if (entry != null) "Edit weight entry" else "Log weight", onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = weightKg,
                onValueChange = { weightKg = it },
                label = { Text("Weight (kg)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            val weightValue = weightKg.toDoubleOrNull()
            MomentumButton(
                text = if (entry != null) "Save changes" else "Save",
                enabled = weightValue != null,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (weightValue != null) {
                        val isoDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
                        if (entry != null) {
                            onUpdate(entry.id, UpdateWeightEntryRequest(date = isoDate, weightKg = weightValue, note = note.ifBlank { null }))
                        } else {
                            onCreate(CreateWeightEntryRequest(date = isoDate, weightKg = weightValue, note = note.ifBlank { null }))
                        }
                    }
                },
            )
        }
    }
}
