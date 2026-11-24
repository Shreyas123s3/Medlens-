package com.medlens.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.medlens.app.ui.MedLensApp
import com.medlens.app.ui.MedLensViewModel
import com.medlens.app.ui.theme.MedLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val medLensViewModel: MedLensViewModel = viewModel()
            val state by medLensViewModel.state.collectAsStateWithLifecycle()
            MedLensTheme(darkTheme = state.darkMode) {
                MedLensApp(medLensViewModel)
            }
        }
    }
}
