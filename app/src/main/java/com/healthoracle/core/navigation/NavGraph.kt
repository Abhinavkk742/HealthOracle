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

        composable(route = Screen.Login.route) {
            LoginScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
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
                onNavigateToHistory = { navController.navigate(Screen.History.route) } // NEW LINE
            )
        }

        composable(route = Screen.Profile.route) {
            com.healthoracle.presentation.profile.ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // NEW: History Route Integration
        composable(route = Screen.History.route) {
            com.healthoracle.presentation.history.HistoryScreen(
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
            val postId = backStackEntry.arguments?.getString("postId") ?: ""
            // PostDetailScreen placeholder
        }
    }
}