package com.healthoracle.core.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.healthoracle.presentation.aisuggestion.AiSuggestionScreen
import com.healthoracle.presentation.auth.LoginScreen
import com.healthoracle.presentation.auth.SignUpScreen
import com.healthoracle.presentation.diabetes.DiabetesScreen
import com.healthoracle.presentation.forum.ForumScreen
import com.healthoracle.presentation.home.HomeScreen
import com.healthoracle.presentation.skin.SkinDiseaseScreen

@Composable
fun HealthOracleNavGraph(
    navController: NavHostController
) {
    // Smart Start Destination: If logged in, go to Home. Otherwise, Login.
    val currentUser = FirebaseAuth.getInstance().currentUser
    val startDestination = if (currentUser != null) Screen.Home.route else Screen.Login.route

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
    ) {

        // --- AUTH SCREENS ---
        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true } // Removes login from backstack
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

        // --- MAIN APP SCREENS ---
        composable(route = Screen.Home.route) {
            HomeScreen(
                onNavigateToSkinDisease = { navController.navigate(Screen.SkinDisease.route) },
                onNavigateToDiabetes = { navController.navigate(Screen.Diabetes.route) },
                onNavigateToForum = { navController.navigate(Screen.Forum.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) } // NEW LINE
            )
        }

        composable(route = Screen.Profile.route) {
            com.healthoracle.presentation.profile.ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true } // Clears entire backstack on logout
                    }
                }
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
                onNavigateToPostDetail = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                }
            )
        }

        composable(
            route = Screen.PostDetail.route,
            arguments = listOf(
                navArgument("postId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            // PostDetailScreen placeholder
        }
    }
}