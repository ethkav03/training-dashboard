package com.momentum.android.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private val PROGRESS_TABS = listOf("Body", "Fuel", "Recovery")

/**
 * Mirrors web's Progress page, which hosts Body/Fuel/Recovery as sub-tabs
 * under one top-level nav destination. Recovery lands in Sprint 19; it's
 * listed here now so the tab strip doesn't reshuffle positions later.
 */
@Composable
fun ProgressScreen() {
    var selectedTab by remember { mutableIntStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            PROGRESS_TABS.forEachIndexed { index, label ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(label) })
            }
        }
        when (selectedTab) {
            0 -> BodyScreen()
            1 -> FuelScreen()
            else -> RecoveryPlaceholderTab()
        }
    }
}

@Composable
private fun RecoveryPlaceholderTab() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("Recovery is coming in a later sprint.")
    }
}
