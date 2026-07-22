package com.momentum.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.momentum.android.auth.AuthViewModel
import com.momentum.android.network.dto.GoalDirection
import com.momentum.android.network.dto.GoalType
import com.momentum.android.network.dto.OnboardingRequest
import com.momentum.android.network.dto.PrimaryGoalRequest
import com.momentum.android.network.dto.UnitSystem
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumButtonVariant
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.components.MomentumCardTitle
import com.momentum.android.ui.theme.MomentumTheme

/**
 * Mirrors frontend/src/pages/OnboardingPage.tsx -- one single form (not a
 * multi-step wizard), shown once by MainActivity's gating when
 * `authState.user.onboardingStatus == PENDING`. Saving or skipping both
 * flip the status server-side and push the fresh UserDto back into
 * AuthViewModel, so the gating check in MainActivity naturally falls
 * through to the main nav graph on the next recomposition -- no explicit
 * navigation call needed here.
 */
private enum class PrimaryGoalOption(val label: String) {
    LOSE_WEIGHT("Lose weight"),
    MAINTAIN("Maintain"),
    GAIN_MUSCLE("Gain muscle"),
    IMPROVE_PERFORMANCE("Improve performance"),
    CUSTOM("Custom"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(authViewModel: AuthViewModel) {
    val context = LocalContext.current
    val viewModel: OnboardingViewModel = viewModel(factory = remember { OnboardingViewModel.Factory(context) })
    val uiState by viewModel.state.collectAsState()

    var unitSystem by remember { mutableStateOf(UnitSystem.METRIC) }
    var heightCm by remember { mutableStateOf("") }
    var currentWeightKg by remember { mutableStateOf("") }
    var primaryGoalOption by remember { mutableStateOf(PrimaryGoalOption.LOSE_WEIGHT) }
    var targetWeightKg by remember { mutableStateOf("") }
    var trainingFrequency by remember { mutableStateOf("4") }

    val wantsWeightGoal = primaryGoalOption == PrimaryGoalOption.LOSE_WEIGHT || primaryGoalOption == PrimaryGoalOption.GAIN_MUSCLE

    fun buildRequest(): OnboardingRequest {
        val targetWeightValue = targetWeightKg.toDoubleOrNull()
        val primaryGoal = if (wantsWeightGoal && targetWeightValue != null) {
            PrimaryGoalRequest(
                type = GoalType.BODY_WEIGHT,
                title = if (primaryGoalOption == PrimaryGoalOption.LOSE_WEIGHT) "Reach target weight" else "Gain muscle weight",
                targetValue = targetWeightValue,
                targetUnit = "kg",
                direction = if (primaryGoalOption == PrimaryGoalOption.LOSE_WEIGHT) GoalDirection.DECREASE else GoalDirection.INCREASE,
            )
        } else {
            null
        }
        return OnboardingRequest(
            heightCm = heightCm.toDoubleOrNull(),
            currentWeightKg = currentWeightKg.toDoubleOrNull(),
            unitSystem = unitSystem,
            trainingFrequencyPerWeek = trainingFrequency.toIntOrNull(),
            primaryGoal = primaryGoal,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Welcome to Momentum", style = MaterialTheme.typography.headlineSmall)
        Text(
            "A few quick details help personalize your dashboard -- everything here is optional and can be changed later in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MomentumTheme.colors.textSecondary,
        )

        MomentumCard {
            MomentumCardTitle("Units")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MomentumButton(
                    text = "Metric (kg/cm)",
                    variant = if (unitSystem == UnitSystem.METRIC) MomentumButtonVariant.Primary else MomentumButtonVariant.Secondary,
                    onClick = { unitSystem = UnitSystem.METRIC },
                )
                MomentumButton(
                    text = "Imperial (lb/in)",
                    variant = if (unitSystem == UnitSystem.IMPERIAL) MomentumButtonVariant.Primary else MomentumButtonVariant.Secondary,
                    onClick = { unitSystem = UnitSystem.IMPERIAL },
                )
            }
        }

        MomentumCard {
            MomentumCardTitle("About you")
            OutlinedTextField(
                value = heightCm,
                onValueChange = { heightCm = it },
                label = { Text("Height (cm, optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = currentWeightKg,
                onValueChange = { currentWeightKg = it },
                label = { Text("Current weight (kg, optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = trainingFrequency,
                onValueChange = { trainingFrequency = it },
                label = { Text("Typical training sessions/week") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        MomentumCard {
            MomentumCardTitle("Primary goal")
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PrimaryGoalOption.entries.forEach { option ->
                    FilterChip(
                        selected = primaryGoalOption == option,
                        onClick = { primaryGoalOption = option },
                        label = { Text(option.label) },
                    )
                }
            }
            if (wantsWeightGoal) {
                OutlinedTextField(
                    value = targetWeightKg,
                    onValueChange = { targetWeightKg = it },
                    label = { Text("Target weight (kg, optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        uiState.errorMessage?.let { message ->
            Text(message, color = MomentumTheme.colors.statusCritical)
        }

        MomentumButton(
            text = if (uiState.isSaving) "Saving..." else "Get started",
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.complete(buildRequest()) { user -> authViewModel.setUser(user) } },
        )
        MomentumButton(
            text = "Skip for now",
            enabled = !uiState.isSaving,
            variant = MomentumButtonVariant.Ghost,
            modifier = Modifier.fillMaxWidth(),
            onClick = { viewModel.skip { user -> authViewModel.setUser(user) } },
        )
    }
}
