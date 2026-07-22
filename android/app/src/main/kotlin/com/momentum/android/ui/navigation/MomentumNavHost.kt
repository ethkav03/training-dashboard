package com.momentum.android.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.momentum.android.auth.AuthViewModel
import com.momentum.android.healthconnect.HealthConnectViewModel
import com.momentum.android.ui.screens.GoalsPlaceholderScreen
import com.momentum.android.ui.screens.InsightsPlaceholderScreen
import com.momentum.android.ui.screens.ProgressScreen
import com.momentum.android.ui.screens.SettingsScreen
import com.momentum.android.ui.screens.TodayScreen
import com.momentum.android.ui.screens.TrainingPlaceholderScreen

/**
 * Replaces MainActivity's old if/else between LoginScreen and SyncScreen --
 * this is only ever shown once the user is authenticated (see
 * MainActivity), gating unauthenticated access the same way web's
 * ProtectedRoute does. Today (Sprint 16) and Settings are real;
 * Progress/Training/Goals/Insights are placeholders until their own sprints
 * (17-20). Settings absorbed the Health Connect sync UI that used to be its
 * own standalone SyncScreen.
 */
@Composable
fun MomentumNavHost(
    authViewModel: AuthViewModel,
    healthConnectViewModel: HealthConnectViewModel,
    navController: NavHostController = rememberNavController(),
) {
    Scaffold(bottomBar = { MomentumBottomBar(navController) }) { padding ->
        NavHost(
            navController = navController,
            startDestination = MomentumDestination.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(MomentumDestination.Today.route) { TodayScreen(authViewModel, navController) }
            composable(MomentumDestination.Progress.route) { ProgressScreen() }
            composable(MomentumDestination.Training.route) { TrainingPlaceholderScreen() }
            composable(MomentumDestination.Goals.route) { GoalsPlaceholderScreen() }
            composable(MomentumDestination.Insights.route) { InsightsPlaceholderScreen() }
            composable(MomentumDestination.Settings.route) {
                SettingsScreen(authViewModel = authViewModel, healthConnectViewModel = healthConnectViewModel)
            }
        }
    }
}
