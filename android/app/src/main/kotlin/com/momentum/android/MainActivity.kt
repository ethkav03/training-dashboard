package com.momentum.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.momentum.android.auth.AuthViewModel
import com.momentum.android.healthconnect.HealthConnectViewModel
import com.momentum.android.ui.LoginScreen
import com.momentum.android.ui.navigation.MomentumNavHost
import com.momentum.android.ui.theme.MomentumTheme
import com.momentum.android.ui.theme.ThemePreference
import com.momentum.android.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels { AuthViewModel.Factory(applicationContext) }
    private val healthConnectViewModel: HealthConnectViewModel by viewModels {
        HealthConnectViewModel.Factory(applicationContext)
    }
    private val themeViewModel: ThemeViewModel by viewModels { ThemeViewModel.Factory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val themePreference by themeViewModel.preference.collectAsState()
            val darkTheme = when (themePreference) {
                ThemePreference.SYSTEM -> isSystemInDarkTheme()
                ThemePreference.LIGHT -> false
                ThemePreference.DARK -> true
            }

            MomentumTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val authState by authViewModel.state.collectAsState()
                    if (authState.token == null) {
                        LoginScreen(viewModel = authViewModel)
                    } else {
                        MomentumNavHost(
                            authViewModel = authViewModel,
                            healthConnectViewModel = healthConnectViewModel,
                            themeViewModel = themeViewModel,
                        )
                    }
                }
            }
        }
    }
}
