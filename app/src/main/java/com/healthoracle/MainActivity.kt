package com.healthoracle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.healthoracle.core.navigation.HealthOracleNavGraph
import com.healthoracle.core.navigation.Screen
import com.healthoracle.core.ui.theme.HealthOracleTheme
import com.healthoracle.core.util.OnboardingUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- SMART ROUTING ---
        // 1. Did they finish the tutorial?
        // 2. Are they logged in?
        val startDestination = if (!OnboardingUtils.isOnboardingComplete(this)) {
            Screen.Onboarding.route
        } else if (FirebaseAuth.getInstance().currentUser != null) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }

        setContent {
            HealthOracleTheme {
                val navController = rememberNavController()
                HealthOracleNavGraph(
                    navController = navController,
                    startDestination = startDestination
                )
            }
        }
    }
}