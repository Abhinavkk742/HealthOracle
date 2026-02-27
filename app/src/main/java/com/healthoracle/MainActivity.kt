package com.healthoracle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.healthoracle.core.navigation.HealthOracleNavGraph
import com.healthoracle.core.navigation.Screen
import com.healthoracle.core.ui.theme.HealthOracleTheme
import com.healthoracle.core.util.OnboardingUtils
import com.healthoracle.core.util.ThemeMode
import com.healthoracle.core.util.ThemePreferences
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // NEW: Initialize the theme preference manager
        ThemePreferences.init(this)

        val startDestination = if (!OnboardingUtils.isOnboardingComplete(this)) {
            Screen.Onboarding.route
        } else if (FirebaseAuth.getInstance().currentUser != null) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }

        setContent {
            // NEW: Observe the theme state dynamically
            val themeMode by ThemePreferences.themeMode.collectAsState()
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            HealthOracleTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    HealthOracleNavGraph(
                        navController = navController,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}