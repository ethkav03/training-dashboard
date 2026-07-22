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

// Not a bottom-nav tab -- reached via Today's "Full timeline" link, same as
// web's <Link to="/timeline">, so it's a plain pushed destination rather
// than one of MomentumDestination's six entries. Public (unlike
// MomentumNavHost's other route constants) since TodayScreen needs it too.
const val TIMELINE_ROUTE = "timeline"
