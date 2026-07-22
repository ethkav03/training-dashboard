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
import androidx.compose.runtime.remember
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
import com.momentum.android.ui.navigation.MomentumDestination
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
 * Mirrors frontend/src/pages/TodayPage.tsx. "Quick actions" navigates to the
 * relevant tab for now rather than opening an inline log form -- those
 * forms don't exist yet (Sprints 17-19 build Body/Fuel/Training/Recovery/
 * Goals), so this is an honest interim behavior, not the final one.
 */
@Composable
fun TodayScreen(authViewModel: AuthViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val viewModel: TodayViewModel = viewModel(factory = remember { TodayViewModel.Factory(context) })
    val uiState by viewModel.state.collectAsState()
    val authState by authViewModel.state.collectAsState()

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
                            onClick = { navController.navigate(MomentumDestination.Progress.route) },
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
                MomentumCardTitle("Today's timeline")
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
                    MomentumButton("Log weight", size = MomentumButtonSize.Small, variant = MomentumButtonVariant.Secondary, onClick = { navController.navigate(MomentumDestination.Progress.route) })
                    MomentumButton("Log meal", size = MomentumButtonSize.Small, variant = MomentumButtonVariant.Secondary, onClick = { navController.navigate(MomentumDestination.Progress.route) })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MomentumButton("Log workout", size = MomentumButtonSize.Small, variant = MomentumButtonVariant.Secondary, onClick = { navController.navigate(MomentumDestination.Training.route) })
                    MomentumButton("New goal", size = MomentumButtonSize.Small, variant = MomentumButtonVariant.Secondary, onClick = { navController.navigate(MomentumDestination.Goals.route) })
                }
            }
        }
    }
}
