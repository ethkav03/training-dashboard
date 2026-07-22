package com.momentum.android.ui.navigation

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

/**
 * Plain-text emoji stand in for icons rather than pulling in the Material
 * Icons Extended library for six glyphs -- avoids one more dependency whose
 * exact current version I'd otherwise be guessing at.
 */
@Composable
fun MomentumBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route

    NavigationBar {
        MomentumDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                // No saveState/restoreState: web's dashboard/summary queries
                // use refetchOnMount:"always" specifically so a write made
                // in one tab (e.g. logging a meal in Fuel) shows up
                // immediately when you switch back to Today, rather than
                // showing stale cached data. Matching that here means each
                // tab switch fully recomposes the destination -- so every
                // screen's own LaunchedEffect(Unit) load naturally re-fires
                // on every visit. Trade-off: scroll position within a tab
                // isn't preserved across switches, which is a minor loss
                // next to staying correct.
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
                icon = { Text(destination.icon) },
                label = { Text(destination.label) },
            )
        }
    }
}
