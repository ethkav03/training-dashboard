package com.momentum.android.ui.navigation

/** Mirrors frontend/src/components/layout/navItems.ts's NAV_ITEMS exactly, same order. */
enum class MomentumDestination(val route: String, val label: String, val icon: String) {
    Today("today", "Today", "📅"),
    Progress("progress", "Progress", "📈"),
    Training("training", "Training", "🏋"),
    Goals("goals", "Goals", "🎯"),
    Insights("insights", "Insights", "💡"),
    Settings("settings", "Settings", "⚙"),
}
