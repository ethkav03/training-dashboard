package com.momentum.android.ui.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.momentum.android.auth.TokenStore
import com.momentum.android.data.TrainingRepository
import com.momentum.android.network.ApiClient
import com.momentum.android.network.dto.CreateGoalRequest
import com.momentum.android.network.dto.GoalDirection
import com.momentum.android.network.dto.GoalType
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumModalSheet
import java.time.LocalDate
import java.time.ZoneId

private data class GoalTypeOption(val type: GoalType, val label: String, val defaultUnit: String)

private val TYPE_OPTIONS = listOf(
    GoalTypeOption(GoalType.BODY_WEIGHT, "Body weight", "kg"),
    GoalTypeOption(GoalType.CALORIE_INTAKE, "Calorie intake", "kcal/day"),
    GoalTypeOption(GoalType.PROTEIN_INTAKE, "Protein intake", "g/day"),
    GoalTypeOption(GoalType.TRAINING_FREQUENCY, "Training frequency", "sessions/wk"),
    GoalTypeOption(GoalType.EXERCISE_PERFORMANCE, "Exercise performance", "kg"),
    GoalTypeOption(GoalType.SPORT_PERFORMANCE, "Sport performance", "rating"),
    GoalTypeOption(GoalType.SLEEP_RECOVERY, "Sleep & recovery", "hours"),
    GoalTypeOption(GoalType.CUSTOM, "Custom", ""),
)

/** Mirrors frontend/src/components/forms/GoalForm.tsx -- create-only, no edit mode on web either. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalForm(onDismiss: () -> Unit, onCreate: (CreateGoalRequest) -> Unit) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(TYPE_OPTIONS.first()) }
    var typeMenuOpen by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var targetValue by remember { mutableStateOf("") }
    var targetUnit by remember { mutableStateOf(TYPE_OPTIONS.first().defaultUnit) }
    var direction by remember { mutableStateOf(GoalDirection.DECREASE) }
    var targetDate by remember { mutableStateOf("") }
    var relatedExerciseName by remember { mutableStateOf("") }
    var exerciseMenuOpen by remember { mutableStateOf(false) }
    var exerciseNames by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        val repository = TrainingRepository(ApiClient.create(TokenStore(context)))
        runCatching { repository.exerciseNames() }.onSuccess { exerciseNames = it }
    }

    MomentumModalSheet(title = "New goal", onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("Goal type", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { typeMenuOpen = true }) { Text(selectedType.label) }
                DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    TYPE_OPTIONS.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = {
                                selectedType = option
                                targetUnit = option.defaultUnit
                                typeMenuOpen = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                placeholder = { Text("Reach 80kg") },
                modifier = Modifier.fillMaxWidth(),
            )

            if (selectedType.type == GoalType.EXERCISE_PERFORMANCE) {
                Column {
                    Text("Exercise", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { exerciseMenuOpen = true }) {
                        Text(relatedExerciseName.ifBlank { "Select an exercise..." })
                    }
                    DropdownMenu(expanded = exerciseMenuOpen, onDismissRequest = { exerciseMenuOpen = false }) {
                        exerciseNames.forEach { name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { relatedExerciseName = name; exerciseMenuOpen = false })
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { targetValue = it },
                    label = { Text("Target value") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = targetUnit,
                    onValueChange = { targetUnit = it },
                    label = { Text("Unit") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Direction", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { direction = GoalDirection.DECREASE }) {
                            Text(if (direction == GoalDirection.DECREASE) "• Decrease" else "Decrease")
                        }
                        TextButton(onClick = { direction = GoalDirection.INCREASE }) {
                            Text(if (direction == GoalDirection.INCREASE) "• Increase" else "Increase")
                        }
                    }
                }
                // No date picker component yet (same trim as WeightEntryForm/
                // TrainingSessionForm) -- plain ISO text entry still works
                // for anyone who wants to set one.
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { targetDate = it },
                    label = { Text("Target date (YYYY-MM-DD, optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val titleValid = title.isNotBlank()
            val exerciseValid = selectedType.type != GoalType.EXERCISE_PERFORMANCE || relatedExerciseName.isNotBlank()
            MomentumButton(
                text = "Create goal",
                enabled = titleValid && exerciseValid,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (titleValid && exerciseValid) {
                        onCreate(
                            CreateGoalRequest(
                                type = selectedType.type,
                                title = title,
                                targetValue = targetValue.toDoubleOrNull(),
                                targetUnit = targetUnit.ifBlank { null },
                                direction = direction,
                                targetDate = targetDate.takeIf { it.isNotBlank() }?.let {
                                    runCatching {
                                        LocalDate.parse(it).atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
                                    }.getOrNull()
                                },
                                relatedExerciseName = if (selectedType.type == GoalType.EXERCISE_PERFORMANCE) relatedExerciseName else null,
                            ),
                        )
                    }
                },
            )
        }
    }
}
