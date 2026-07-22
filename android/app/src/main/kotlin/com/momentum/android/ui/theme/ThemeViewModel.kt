package com.momentum.android.ui.theme

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Held at the Activity level (alongside AuthViewModel/HealthConnectViewModel)
 * so MainActivity can resolve the actual dark/light boolean MomentumTheme
 * needs, and SettingsScreen can offer the same System/Light/Dark choice web
 * has in its Appearance section.
 */
class ThemeViewModel(private val store: ThemePreferenceStore) : ViewModel() {
    private val _preference = MutableStateFlow(ThemePreference.SYSTEM)
    val preference: StateFlow<ThemePreference> = _preference.asStateFlow()

    init {
        viewModelScope.launch { store.preference.collect { _preference.value = it } }
    }

    fun setPreference(preference: ThemePreference) {
        _preference.value = preference
        viewModelScope.launch { store.setPreference(preference) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ThemeViewModel(ThemePreferenceStore(context.applicationContext)) as T
        }
    }
}
