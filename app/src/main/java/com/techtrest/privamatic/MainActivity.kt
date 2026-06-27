package com.techtrest.privamatic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.techtrest.privamatic.data.OnboardingPreferences
import com.techtrest.privamatic.ui.screens.MainScreen
import com.techtrest.privamatic.ui.screens.onboarding.OnboardingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        SnapshotReceiver.scheduleMidnightSnapshot(this)

        setContent {
            PrivacyWidgetTheme {
                val onboardingPrefs = remember { OnboardingPreferences(applicationContext) }
                var onboardingComplete by remember { mutableStateOf(onboardingPrefs.isComplete()) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (onboardingComplete) {
                        MainScreen()
                    } else {
                        OnboardingScreen(
                            onComplete = {
                                onboardingPrefs.setComplete()
                                onboardingComplete = true
                            }
                        )
                    }
                }
            }
        }
    }
}