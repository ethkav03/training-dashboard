package com.momentum.android.ui.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.momentum.android.network.dto.ActivityType
import com.momentum.android.network.dto.MatchDetailDto
import com.momentum.android.network.dto.MatchResult
import com.momentum.android.network.dto.TrainingSessionDto
import com.momentum.android.network.dto.TrainingSessionWriteDto
import com.momentum.android.network.dto.WorkoutDto
import com.momentum.android.network.dto.WorkoutExerciseDto
import com.momentum.android.network.dto.WorkoutSetDto
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumModalSheet
import com.momentum.android.ui.theme.MomentumTheme
import java.time.Instant

private val ACTIVITY_LABELS = mapOf(
    ActivityType.GYM to "Gym / strength",
    ActivityType.RUNNING to "Running",
    ActivityType.TEAM_SPORT_TRAINING to "Team sport training",
    ActivityType.MATCH to "Match / competition",
    ActivityType.CYCLING to "Cycling",
    ActivityType.WALKING to "Walking",
    ActivityType.RECOVERY_SESSION to "Recovery session",
    ActivityType.OTHER to "Other",
)

private val RESULT_LABELS = mapOf(MatchResult.WIN to "Win", MatchResult.LOSS to "Loss", MatchResult.DRAW to "Draw")

// State-holder classes -- the Compose equivalent of react-hook-form's
// useFieldArray: each holds its own mutableStateOf fields so editing one
// input only recomposes that row, not the whole dynamic list.
private class SetFormState(reps: String = "8", weightKg: String = "20", rpe: String = "", isWarmup: Boolean = false) {
    var reps by mutableStateOf(reps)
    var weightKg by mutableStateOf(weightKg)
    var rpe by mutableStateOf(rpe)
    var isWarmup by mutableStateOf(isWarmup)
}

private class ExerciseFormState(name: String = "", sets: List<SetFormState> = listOf(SetFormState())) {
    var name by mutableStateOf(name)
    val sets = mutableStateListOf(*sets.toTypedArray())
}

private class KeyStatFormState(key: String = "", value: String = "") {
    var key by mutableStateOf(key)
    var value by mutableStateOf(value)
}

/**
 * Mirrors frontend/src/components/forms/TrainingSessionForm.tsx -- the
 * largest form in the app. Same component for create and edit. Date/time
 * isn't editable yet (defaults to now for new sessions, or the existing
 * session's own date for edits) -- same trim already accepted for
 * WeightEntryForm, not wired to a picker in this pass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrainingSessionForm(
    session: TrainingSessionDto?,
    onDismiss: () -> Unit,
    onCreate: (TrainingSessionWriteDto) -> Unit,
    onUpdate: (String, TrainingSessionWriteDto) -> Unit,
) {
    var type by remember { mutableStateOf(session?.type ?: ActivityType.GYM) }
    var typeMenuOpen by remember { mutableStateOf(false) }
    var durationMin by remember { mutableStateOf(session?.durationMin?.toString() ?: "60") }
    var intensity by remember { mutableStateOf(session?.intensity?.toString() ?: "6") }
    var caloriesBurned by remember { mutableStateOf(session?.caloriesBurned?.toString() ?: "") }
    var notes by remember { mutableStateOf(session?.notes ?: "") }

    val exercises = remember {
        val existing = session?.workout?.exercises
        if (!existing.isNullOrEmpty()) {
            mutableStateListOf(
                *existing.map { ex ->
                    ExerciseFormState(
                        name = ex.name,
                        sets = ex.sets.map { s ->
                            SetFormState(
                                reps = s.reps.toString(),
                                weightKg = s.weightKg.toString(),
                                rpe = s.rpe?.toString() ?: "",
                                isWarmup = s.isWarmup,
                            )
                        },
                    )
                }.toTypedArray(),
            )
        } else {
            mutableStateListOf(ExerciseFormState())
        }
    }

    val match = session?.matchDetail
    var opponent by remember { mutableStateOf(match?.opponent ?: "") }
    var competition by remember { mutableStateOf(match?.competition ?: "") }
    var position by remember { mutableStateOf(match?.position ?: "") }
    var minutesPlayed by remember { mutableStateOf(match?.minutesPlayed?.toString() ?: "") }
    var result by remember { mutableStateOf(match?.result) }
    var resultMenuOpen by remember { mutableStateOf(false) }
    var performanceRating by remember { mutableStateOf(match?.performanceRating?.toString() ?: "") }
    var injuryNotes by remember { mutableStateOf(match?.injuryNotes ?: "") }
    var reflection by remember { mutableStateOf(match?.reflection ?: "") }
    val keyStats = remember {
        val entries = match?.keyStats?.map { (k, v) -> KeyStatFormState(k, v.toString()) } ?: emptyList()
        mutableStateListOf(*entries.toTypedArray())
    }

    MomentumModalSheet(title = if (session != null) "Edit training session" else "Log training session", onDismiss = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text("Activity type", style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { typeMenuOpen = true }) { Text(ACTIVITY_LABELS[type] ?: type.name) }
                DropdownMenu(expanded = typeMenuOpen, onDismissRequest = { typeMenuOpen = false }) {
                    ACTIVITY_LABELS.forEach { (value, label) ->
                        DropdownMenuItem(text = { Text(label) }, onClick = { type = value; typeMenuOpen = false })
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = durationMin,
                    onValueChange = { durationMin = it },
                    label = { Text("Duration (min)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = intensity,
                    onValueChange = { intensity = it },
                    label = { Text("Intensity (1-10)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = caloriesBurned,
                onValueChange = { caloriesBurned = it },
                label = { Text("Calories burned (optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            if (type == ActivityType.GYM) {
                Text("Exercises", style = MaterialTheme.typography.labelMedium)
                exercises.forEachIndexed { exIndex, exercise ->
                    ExerciseEditor(exercise = exercise, onRemoveExercise = { exercises.removeAt(exIndex) })
                }
                TextButton(onClick = { exercises.add(ExerciseFormState()) }) { Text("+ Add exercise") }
            }

            if (type == ActivityType.MATCH) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = opponent, onValueChange = { opponent = it }, label = { Text("Opponent") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = competition, onValueChange = { competition = it }, label = { Text("Competition") }, modifier = Modifier.fillMaxWidth())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = position, onValueChange = { position = it }, label = { Text("Position") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = minutesPlayed,
                        onValueChange = { minutesPlayed = it },
                        label = { Text("Minutes played") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column {
                    Text("Result", style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { resultMenuOpen = true }) { Text(result?.let { RESULT_LABELS[it] } ?: "—") }
                    DropdownMenu(expanded = resultMenuOpen, onDismissRequest = { resultMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("—") }, onClick = { result = null; resultMenuOpen = false })
                        RESULT_LABELS.forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { result = value; resultMenuOpen = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = performanceRating,
                    onValueChange = { performanceRating = it },
                    label = { Text("Performance rating (1-10)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("Key stats", style = MaterialTheme.typography.labelMedium)
                keyStats.forEachIndexed { index, stat ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = stat.key,
                            onValueChange = { stat.key = it },
                            label = { Text("e.g. goals") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = stat.value,
                            onValueChange = { stat.value = it },
                            label = { Text("value") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(onClick = { keyStats.removeAt(index) }) {
                            Text("✕", color = MomentumTheme.colors.statusCritical)
                        }
                    }
                }
                TextButton(onClick = { keyStats.add(KeyStatFormState()) }) { Text("+ Add stat") }

                OutlinedTextField(value = injuryNotes, onValueChange = { injuryNotes = it }, label = { Text("Injury / pain notes") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = reflection, onValueChange = { reflection = it }, label = { Text("Post-match reflection") }, modifier = Modifier.fillMaxWidth())
            }

            OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth())

            val durationValue = durationMin.toIntOrNull()
            val intensityValue = intensity.toIntOrNull()
            MomentumButton(
                text = if (session != null) "Save changes" else "Save session",
                enabled = durationValue != null && intensityValue != null,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (durationValue != null && intensityValue != null) {
                        val workout = if (type == ActivityType.GYM) {
                            WorkoutDto(
                                exercises = exercises
                                    .filter { it.name.isNotBlank() }
                                    .mapIndexed { index, exercise ->
                                        WorkoutExerciseDto(
                                            name = exercise.name,
                                            orderIndex = index,
                                            sets = exercise.sets.mapIndexed { setIndex, set ->
                                                WorkoutSetDto(
                                                    setNumber = setIndex + 1,
                                                    reps = set.reps.toIntOrNull() ?: 0,
                                                    weightKg = set.weightKg.toDoubleOrNull() ?: 0.0,
                                                    rpe = set.rpe.toDoubleOrNull(),
                                                    isWarmup = set.isWarmup,
                                                )
                                            },
                                        )
                                    },
                            )
                        } else null

                        val matchDetail = if (type == ActivityType.MATCH) {
                            MatchDetailDto(
                                opponent = opponent.ifBlank { null },
                                competition = competition.ifBlank { null },
                                position = position.ifBlank { null },
                                minutesPlayed = minutesPlayed.toIntOrNull(),
                                result = result,
                                performanceRating = performanceRating.toIntOrNull(),
                                keyStats = keyStats.filter { it.key.isNotBlank() }
                                    .associate { it.key to (it.value.toDoubleOrNull() ?: 0.0) }
                                    .ifEmpty { null },
                                injuryNotes = injuryNotes.ifBlank { null },
                                reflection = reflection.ifBlank { null },
                            )
                        } else null

                        val body = TrainingSessionWriteDto(
                            type = type,
                            date = session?.date ?: Instant.now().toString(),
                            durationMin = durationValue,
                            intensity = intensityValue,
                            caloriesBurned = caloriesBurned.toIntOrNull(),
                            notes = notes.ifBlank { null },
                            workout = workout,
                            matchDetail = matchDetail,
                        )

                        if (session != null) onUpdate(session.id, body) else onCreate(body)
                    }
                },
            )
        }
    }
}

@Composable
private fun ExerciseEditor(exercise: ExerciseFormState, onRemoveExercise: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = exercise.name,
                onValueChange = { exercise.name = it },
                label = { Text("Exercise name") },
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = onRemoveExercise) { Text("✕", color = MomentumTheme.colors.statusCritical) }
        }
        exercise.sets.forEachIndexed { setIndex, set ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${setIndex + 1}", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textMuted)
                OutlinedTextField(
                    value = set.reps,
                    onValueChange = { set.reps = it },
                    label = { Text("Reps") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = set.weightKg,
                    onValueChange = { set.weightKg = it },
                    label = { Text("kg") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = set.rpe,
                    onValueChange = { set.rpe = it },
                    label = { Text("RPE") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Checkbox(checked = set.isWarmup, onCheckedChange = { set.isWarmup = it })
                TextButton(onClick = { exercise.sets.removeAt(setIndex) }) { Text("✕") }
            }
        }
        TextButton(onClick = { exercise.sets.add(SetFormState()) }) { Text("+ Add set") }
    }
}
