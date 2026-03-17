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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.healthoracle.core.navigation.HealthOracleNavGraph
import com.healthoracle.core.navigation.Screen
import com.healthoracle.core.ui.theme.HealthOracleTheme
import com.healthoracle.core.util.OnboardingUtils
import com.healthoracle.core.util.ThemeMode
import com.healthoracle.core.util.ThemePreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // NEW: A state variable to hold our destination once we figure it out
    private var startDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // NEW: Tell the Android Splash Screen to stay visible until we know where to go!
        splashScreen.setKeepOnScreenCondition { startDestination == null }

        ThemePreferences.init(this)

        // NEW: Determine the starting screen asynchronously
        lifecycleScope.launch {
            if (!OnboardingUtils.isOnboardingComplete(this@MainActivity)) {
                startDestination = Screen.Onboarding.route
            } else {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    try {
                        // Ask Firestore for the user's role
                        val document = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(user.uid)
                            .get()
                            .await()

                        val role = document.getString("role")

                        // Route them based on the role!
                        startDestination = if (role == "doctor") {
                            Screen.DoctorDashboard.route
                        } else {
                            Screen.Home.route
                        }
                    } catch (e: Exception) {
                        // If there is no internet or an error, default to the normal Home screen
                        startDestination = Screen.Home.route
                    }
                } else {
                    startDestination = Screen.Login.route
                }
            }
        }

        setContent {
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
                    // Only build the NavGraph once we have finished calculating the destination
                    startDestination?.let { destination ->
                        val navController = rememberNavController()
                        HealthOracleNavGraph(
                            navController = navController,
                            startDestination = destination
                        )
                    }
                }
            }
        }
    }
}