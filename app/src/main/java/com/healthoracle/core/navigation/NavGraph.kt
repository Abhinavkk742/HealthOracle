package com.healthoracle.core.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.healthoracle.presentation.aisuggestion.AiSuggestionScreen
import com.healthoracle.presentation.auth.LoginScreen
import com.healthoracle.presentation.auth.SignUpScreen
import com.healthoracle.presentation.calendar.CalendarScreen
import com.healthoracle.presentation.chat.ChatScreen
import com.healthoracle.presentation.chat.ChatViewModel
import com.healthoracle.presentation.diabetes.DiabetesScreen
import com.healthoracle.presentation.forum.ForumScreen
import com.healthoracle.presentation.home.HomeScreen
import com.healthoracle.presentation.onboarding.OnboardingScreen
import com.healthoracle.presentation.skin.SkinDiseaseScreen

@Composable
fun HealthOracleNavGraph(
    navController: NavHostController,
    startDestination: String // Passed in dynamically
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
    ) {

        // --- ONBOARDING ---
        composable(route = Screen.Onboarding.route) {
            OnboardingScreen(
                onFinishOnboarding = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                // FIXED: Added the routing for the Doctor Dashboard
                onNavigateToDoctorDashboard = {
                    navController.navigate(Screen.DoctorDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
            )
        }

        composable(route = Screen.SignUp.route) {
            SignUpScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.SignUp.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToSkinDisease = { navController.navigate(Screen.SkinDisease.route) },
                onNavigateToDiabetes = { navController.navigate(Screen.Diabetes.route) },
                onNavigateToForum = { navController.navigate(Screen.Forum.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.route) },
                onNavigateToCalendar = { navController.navigate(Screen.Calendar.route) },
                // NEW: Triggers the navigation
                onNavigateToChat = { patientId, doctorId, contactName ->
                    navController.navigate(Screen.Chat.createRoute(patientId, doctorId, contactName))
                }
            )
        }

        composable(route = Screen.Profile.route) {
            com.healthoracle.presentation.profile.ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onNavigateToMyPosts = { navController.navigate("my_posts") },
                // NEW: Triggers the navigation
                onNavigateToChat = { patientId, doctorId, contactName ->
                    navController.navigate(Screen.Chat.createRoute(patientId, doctorId, contactName))
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Settings.route) {
            com.healthoracle.presentation.settings.SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true } // Clear the entire app backstack!
                    }
                }
            )
        }

        composable(route = Screen.History.route) {
            com.healthoracle.presentation.history.HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Calendar.route) {
            CalendarScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.SkinDisease.route) {
            SkinDiseaseScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAiSuggestion = { conditionName ->
                    navController.navigate(
                        Screen.AiSuggestion.createRoute(conditionName, "skin")
                    )
                }
            )
        }

        composable(route = Screen.Diabetes.route) {
            DiabetesScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAiSuggestion = { conditionName ->
                    navController.navigate(
                        Screen.AiSuggestion.createRoute(conditionName, "diabetes")
                    )
                }
            )
        }

        composable(
            route = Screen.AiSuggestion.route,
            arguments = listOf(
                navArgument("conditionName") { type = NavType.StringType },
                navArgument("conditionSource") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conditionName = backStackEntry.arguments?.getString("conditionName") ?: ""
            val conditionSource = backStackEntry.arguments?.getString("conditionSource") ?: ""
            AiSuggestionScreen(
                conditionName = conditionName,
                conditionSource = conditionSource,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = Screen.Forum.route) {
            ForumScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCreatePost = { navController.navigate(Screen.CreatePost.route) },
                onNavigateToPostDetail = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                }
            )
        }

        composable(route = Screen.CreatePost.route) {
            com.healthoracle.presentation.forum.CreatePostScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: return@composable

            com.healthoracle.presentation.forum.PostDetailScreen(
                postId = postId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = "my_posts") {
            com.healthoracle.presentation.profile.MyPostsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPostDetail = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                }
            )
        }

        // Tele-health Chat Screen
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("patientId") { type = NavType.StringType },
                navArgument("doctorId") { type = NavType.StringType },
                navArgument("contactName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val contactName = backStackEntry.arguments?.getString("contactName") ?: "Doctor"

            // Initialize the ViewModel (Hilt handles injecting the repository and SavedStateHandle)
            val chatViewModel: ChatViewModel = hiltViewModel()
            val messages by chatViewModel.messages.collectAsState()

            ChatScreen(
                contactName = contactName,
                currentUserId = chatViewModel.currentUserId,
                messages = messages,
                onSendMessage = { text -> chatViewModel.sendMessage(text) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Doctor Dashboard Screen
        composable(route = Screen.DoctorDashboard.route) {
            com.healthoracle.presentation.doctor.DoctorDashboardScreen(
                onNavigateToChat = { patientId, doctorId, patientName ->
                    navController.navigate(Screen.Chat.createRoute(patientId, doctorId, patientName))
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true } // Clear backstack and go to login
                    }
                }
            )
        }
    }
}