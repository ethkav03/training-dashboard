package com.momentum.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.momentum.android.auth.AuthViewModel
import com.momentum.android.ui.cards.GoalCard
import com.momentum.android.ui.cards.InsightCard
import com.momentum.android.ui.cards.ReadinessBadge
import com.momentum.android.ui.components.MomentumButton
import com.momentum.android.ui.components.MomentumButtonSize
import com.momentum.android.ui.components.MomentumButtonVariant
import com.momentum.android.ui.components.MomentumCard
import com.momentum.android.ui.components.MomentumCardTitle
import com.momentum.android.ui.forms.GoalForm
import com.momentum.android.ui.forms.NutritionEntryForm
import com.momentum.android.ui.forms.RecoveryEntryForm
import com.momentum.android.ui.forms.TrainingSessionForm
import com.momentum.android.ui.forms.WeightEntryForm
import com.momentum.android.ui.navigation.MomentumDestination
import com.momentum.android.ui.navigation.TIMELINE_ROUTE
import com.momentum.android.ui.theme.MomentumTheme
import com.momentum.android.ui.timeline.TimelineEntryItem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.round

private val ACHIEVEMENT_DATE_FORMATTER = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun greeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }
}

/**
 * Mirrors frontend/src/pages/TodayPage.tsx, including its `activeModal`
 * pattern -- "Quick actions" (and the empty-readiness-state "Log recovery"
 * button) open the same entry-form bottom sheets each pillar's own screen
 * uses, rather than navigating away from Today, exactly like web's inline
 * modals. Saving refreshes this screen's own dashboard aggregate afterward
 * (each form's owning ViewModel only refreshes its own resource), so the new
 * entry shows up on Today immediately without a manual pull-to-refresh.
 */
@Composable
fun TodayScreen(authViewModel: AuthViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: TodayViewModel = viewModel(factory = remember { TodayViewModel.Factory(context) })
    val uiState by viewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()

    var activeModal by remember { mutableStateOf<String?>(null) }
    val bodyViewModel: BodyViewModel = viewModel(factory = remember { BodyViewModel.Factory(context) })
    val fuelViewModel: FuelViewModel = viewModel(factory = remember { FuelViewModel.Factory(context) })
    val trainingViewModel: TrainingViewModel = viewModel(factory = remember { TrainingViewModel.Factory(context) })
    val recoveryViewModel: RecoveryViewModel = viewModel(factory = remember { RecoveryViewModel.Factory(context) })
    val goalsViewModel: GoalsViewModel = viewModel(factory = remember { GoalsViewModel.Factory(context) })

    fun closeModalAndRefresh() {
        activeModal = null
        viewModel.refresh()
    }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "${greeting()}, ${authState.user?.name?.substringBefore(" ") ?: ""}",
            style = MaterialTheme.typography.headlineSmall,
        )

        if (uiState.isLoading && uiState.data == null) {
            Text("Loading your day...", color = MomentumTheme.colors.textSecondary)
        }

        uiState.errorMessage?.let { message ->
            Text(message, color = MomentumTheme.colors.statusCritical)
        }

        val data = uiState.data
        if (data != null) {
            MomentumCard {
                MomentumCardTitle("Readiness")
                val readiness = data.readiness
                if (readiness != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column {
                            Text("Recovery", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                            Text("${readiness.readinessScore}", style = MaterialTheme.typography.headlineMedium)
                            ReadinessBadge(readiness.readinessLevel)
                        }
                        Column {
                            Text("Sleep", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                            Text("${readiness.sleepScore ?: "—"}", style = MaterialTheme.typography.headlineMedium)
                        }
                        Column {
                            Text(
                                "Yesterday's strain",
                                style = MaterialTheme.typography.labelSmall,
                                color = MomentumTheme.colors.textSecondary,
                            )
                            val strainText = readiness.strain?.let { "%.1f".format(it) } ?: "—"
                            Text(strainText, style = MaterialTheme.typography.headlineMedium)
                        }
                    }
                    Text(readiness.recommendation, style = MaterialTheme.typography.bodyMedium)
                } else {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("No recovery data logged today yet.", color = MomentumTheme.colors.textMuted)
                        MomentumButton(
                            text = "Log recovery",
                            size = MomentumButtonSize.Small,
                            variant = MomentumButtonVariant.Secondary,
                            onClick = { activeModal = "recovery" },
                        )
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("Weight")
                    Text(
                        data.weightSummary.latestWeightKg?.let { "$it kg" } ?: "—",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    data.weightSummary.rateOfChangeKgPerWeek?.let {
                        Text("${"%.2f".format(it)} kg/wk", style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textMuted)
                    }
                }
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("Calories")
                    Text("${data.nutritionSummary.totalCalories}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        data.nutritionSummary.targetCalories?.let { "of $it target" } ?: "consumed today",
                        style = MaterialTheme.typography.labelSmall,
                        color = MomentumTheme.colors.textMuted,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("Training load")
                    Text("${round(data.trainingLoadSummary.weeklyLoad).toInt()}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${data.trainingLoadSummary.sessionsThisWeek} sessions this week",
                        style = MaterialTheme.typography.labelSmall,
                        color = MomentumTheme.colors.textMuted,
                    )
                }
                MomentumCard(modifier = Modifier.fillMaxWidth()) {
                    MomentumCardTitle("Streak")
                    Text("${data.gamification.loggingStreakDays}d", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${data.gamification.weeklyCompletionScore}% of last 7 days logged",
                        style = MaterialTheme.typography.labelSmall,
                        color = MomentumTheme.colors.textMuted,
                    )
                }
            }

            if (data.goalsStrip.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Active goals", style = MaterialTheme.typography.titleSmall)
                        MomentumButton(
                            text = "View all",
                            size = MomentumButtonSize.Small,
                            variant = MomentumButtonVariant.Ghost,
                            onClick = { navController.navigate(MomentumDestination.Goals.route) },
                        )
                    }
                    data.goalsStrip.forEach { goal ->
                        GoalCard(goal = goal, onTogglePause = {}, onDelete = {})
                    }
                }
            }

            if (data.topInsights.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Insights", style = MaterialTheme.typography.titleSmall)
                        MomentumButton(
                            text = "View all",
                            size = MomentumButtonSize.Small,
                            variant = MomentumButtonVariant.Ghost,
                            onClick = { navController.navigate(MomentumDestination.Insights.route) },
                        )
                    }
                    data.topInsights.forEach { InsightCard(it) }
                }
            }

            if (data.gamification.recentAchievements.isNotEmpty()) {
                MomentumCard {
                    MomentumCardTitle("Recent achievements")
                    data.gamification.recentAchievements.forEach { achievement ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(achievement.title, style = MaterialTheme.typography.bodyMedium)
                                achievement.description?.let {
                                    Text(it, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textSecondary)
                                }
                            }
                            val date = runCatching {
                                Instant.parse(achievement.achievedAt).atZone(ZoneId.systemDefault()).format(ACHIEVEMENT_DATE_FORMATTER)
                            }.getOrDefault("")
                            Text(date, style = MaterialTheme.typography.labelSmall, color = MomentumTheme.colors.textMuted)
                        }
                    }
                }
            }

            MomentumCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MomentumCardTitle("Today's timeline")
                    MomentumButton(
                        text = "Full timeline",
                        size = MomentumButtonSize.Small,
                        variant = MomentumButtonVariant.Ghost,
                        onClick = { navController.navigate(TIMELINE_ROUTE) },
                    )
                }
                if (data.timelineToday.isEmpty()) {
                    Text("Nothing logged yet today.", color = MomentumTheme.colors.textMuted)
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        data.timelineToday.forEach { entry -> TimelineEntryItem(entry) }
                    }
                }
            }

            MomentumCard {
                MomentumCardTitle("Quick actions")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MomentumButton("Log weight", size = MomentumButtonSize.Small, variant = MomentumButtonVariant.Secondary, onClick = { activeModal = "weight" })
                    MomentumButton("Log meal", size = MomentumButtonSize.Small, variant = MomentumButtonVariant.Secondary, onClick = { activeModal = "nutrition" })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MomentumButton("Log workout", size = MomentumButtonSize.Small, variant = MomentumButtonVariant.Secondary, onClick = { activeModal = "training" })
                    MomentumButton("Log recovery", size = MomentumButtonSize.Small, variant = MomentumButtonVariant.Secondary, onClick = { activeModal = "recovery" })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MomentumButton("New goal", size = MomentumButtonSize.Small, variant = MomentumButtonVariant.Secondary, onClick = { activeModal = "goal" })
                }
            }
        }
    }

    when (activeModal) {
        "weight" -> WeightEntryForm(
            entry = null,
            onDismiss = { activeModal = null },
            onCreate = { body -> bodyViewModel.create(body) { closeModalAndRefresh() } },
            onUpdate = { _, _ -> },
        )
        "nutrition" -> NutritionEntryForm(
            entry = null,
            defaultMealType = null,
            onDismiss = { activeModal = null },
            onCreate = { body -> fuelViewModel.create(body) { closeModalAndRefresh() } },
            onUpdate = { _, _ -> },
        )
        "training" -> TrainingSessionForm(
            session = null,
            onDismiss = { activeModal = null },
            onCreate = { body -> trainingViewModel.create(body) { closeModalAndRefresh() } },
            onUpdate = { _, _ -> },
        )
        "recovery" -> RecoveryEntryForm(
            onDismiss = { activeModal = null },
            onSave = { body -> recoveryViewModel.logRecovery(body) { closeModalAndRefresh() } },
        )
        "goal" -> GoalForm(
            onDismiss = { activeModal = null },
            onCreate = { body -> goalsViewModel.create(body) { closeModalAndRefresh() } },
        )
    }
}
