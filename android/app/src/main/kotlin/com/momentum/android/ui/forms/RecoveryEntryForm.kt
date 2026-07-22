package com.momentum.android.ui.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import com.momentum.android.network.dto.UpsertRecoveryRecordRequest
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumModalSheet

/** Mirrors frontend/src/components/forms/RecoveryEntryForm.tsx -- upsert-only, no separate edit mode (logging again for today just updates it). */
@Composable
fun RecoveryEntryForm(onDismiss: () -> Unit, onSave: (UpsertRecoveryRecordRequest) -> Unit) {
    var sleepHours by remember { mutableStateOf("") }
    var sleepQuality by remember { mutableStateOf("3") }
    var restingHr by remember { mutableStateOf("") }
    var hrv by remember { mutableStateOf("") }
    var soreness by remember { mutableStateOf("3") }
    var energy by remember { mutableStateOf("3") }
    var sleepScore by remember { mutableStateOf("") }
    var strain by remember { mutableStateOf("") }

    MomentumModalSheet(title = "Log today's recovery", onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sleepScore,
                    onValueChange = { sleepScore = it },
                    label = { Text("Sleep score /100 (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = strain,
                    onValueChange = { strain = it },
                    label = { Text("Yesterday's strain (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            OutlinedTextField(
                value = sleepHours,
                onValueChange = { sleepHours = it },
                label = { Text("Sleep duration (hours)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = sleepQuality,
                    onValueChange = { sleepQuality = it },
                    label = { Text("Sleep quality (1-5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = soreness,
                    onValueChange = { soreness = it },
                    label = { Text("Soreness (1-5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = energy,
                    onValueChange = { energy = it },
                    label = { Text("Energy (1-5)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = restingHr,
                    onValueChange = { restingHr = it },
                    label = { Text("Resting HR (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = hrv,
                    onValueChange = { hrv = it },
                    label = { Text("HRV (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            MomentumButton(
                text = "Save",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onSave(
                        UpsertRecoveryRecordRequest(
                            sleepHours = sleepHours.toDoubleOrNull(),
                            sleepQuality = sleepQuality.toIntOrNull(),
                            restingHr = restingHr.toIntOrNull(),
                            hrv = hrv.toDoubleOrNull(),
                            soreness = soreness.toIntOrNull(),
                            energy = energy.toIntOrNull(),
                            sleepScore = sleepScore.toIntOrNull(),
                            strain = strain.toDoubleOrNull(),
                        ),
                    )
                },
            )
        }
    }
}
