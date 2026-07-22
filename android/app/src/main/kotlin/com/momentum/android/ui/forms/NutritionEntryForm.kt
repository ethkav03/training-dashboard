package com.momentum.android.ui.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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
import com.momentum.android.network.dto.CreateNutritionEntryRequest
import com.momentum.android.network.dto.MealType
import com.momentum.android.network.dto.NutritionEntryDto
import com.momentum.android.network.dto.UpdateNutritionEntryRequest
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumModalSheet
import java.time.LocalTime

private val MEAL_TYPE_LABELS = mapOf(
    MealType.BREAKFAST to "Breakfast",
    MealType.LUNCH to "Lunch",
    MealType.DINNER to "Dinner",
    MealType.SNACKS to "Snacks",
)

/** Guesses the meal category from the current time, MyFitnessPal-style -- mirrors NutritionEntryForm.tsx's suggestMealType(). */
private fun suggestMealType(): MealType {
    val hour = LocalTime.now().hour
    return when {
        hour < 11 -> MealType.BREAKFAST
        hour < 15 -> MealType.LUNCH
        hour < 21 -> MealType.DINNER
        else -> MealType.SNACKS
    }
}

/** Mirrors frontend/src/components/forms/NutritionEntryForm.tsx. Same component for create and edit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionEntryForm(
    entry: NutritionEntryDto?,
    defaultMealType: MealType?,
    onDismiss: () -> Unit,
    onCreate: (CreateNutritionEntryRequest) -> Unit,
    onUpdate: (String, UpdateNutritionEntryRequest) -> Unit,
) {
    var mealType by remember { mutableStateOf(entry?.mealType ?: defaultMealType ?: suggestMealType()) }
    var mealName by remember { mutableStateOf(entry?.mealName ?: "") }
    var calories by remember { mutableStateOf(entry?.calories?.toString() ?: "") }
    var proteinG by remember { mutableStateOf(entry?.proteinG?.toString() ?: "") }
    var carbsG by remember { mutableStateOf(entry?.carbsG?.toString() ?: "") }
    var fatG by remember { mutableStateOf(entry?.fatG?.toString() ?: "") }

    MomentumModalSheet(title = if (entry != null) "Edit meal" else "Log meal", onDismiss = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text("Meal", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MEAL_TYPE_LABELS.forEach { (type, label) ->
                        FilterChip(selected = mealType == type, onClick = { mealType = type }, label = { Text(label) })
                    }
                }
            }
            OutlinedTextField(
                value = mealName,
                onValueChange = { mealName = it },
                label = { Text("Food (optional)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text("Calories") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = proteinG,
                    onValueChange = { proteinG = it },
                    label = { Text("Protein (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = carbsG,
                    onValueChange = { carbsG = it },
                    label = { Text("Carbs (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = fatG,
                    onValueChange = { fatG = it },
                    label = { Text("Fat (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            val caloriesValue = calories.toIntOrNull()
            MomentumButton(
                text = if (entry != null) "Save changes" else "Save",
                enabled = caloriesValue != null,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (caloriesValue != null) {
                        if (entry != null) {
                            onUpdate(
                                entry.id,
                                UpdateNutritionEntryRequest(
                                    mealType = mealType,
                                    mealName = mealName.ifBlank { null },
                                    calories = caloriesValue,
                                    proteinG = proteinG.toDoubleOrNull(),
                                    carbsG = carbsG.toDoubleOrNull(),
                                    fatG = fatG.toDoubleOrNull(),
                                ),
                            )
                        } else {
                            onCreate(
                                CreateNutritionEntryRequest(
                                    date = java.time.Instant.now().toString(),
                                    mealType = mealType,
                                    mealName = mealName.ifBlank { null },
                                    calories = caloriesValue,
                                    proteinG = proteinG.toDoubleOrNull(),
                                    carbsG = carbsG.toDoubleOrNull(),
                                    fatG = fatG.toDoubleOrNull(),
                                ),
                            )
                        }
                    }
                },
            )
        }
    }
}
