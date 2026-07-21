package com.momentum.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.momentum.android.auth.AuthViewModel
import com.momentum.android.healthconnect.HealthConnectViewModel
import com.momentum.android.ui.LoginScreen
import com.momentum.android.ui.SyncScreen
import com.momentum.android.ui.theme.MomentumTheme

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels { AuthViewModel.Factory(applicationContext) }
    private val healthConnectViewModel: HealthConnectViewModel by viewModels {
        HealthConnectViewModel.Factory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MomentumTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val authState by authViewModel.state.collectAsState()
                    if (authState.token == null) {
                        LoginScreen(viewModel = authViewModel)
                    } else {
                        SyncScreen(authViewModel = authViewModel, healthConnectViewModel = healthConnectViewModel)
                    }
                }
            }
        }
    }
}
