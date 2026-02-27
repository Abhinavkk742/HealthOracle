package com.healthoracle.core.navigation

sealed class Screen(val route: String) {
    // Phase 1: Auth & Profile Screens
    data object Login : Screen("login")
    data object SignUp : Screen("signup")
    data object Profile : Screen("profile")

    // Phase 2: History Screen
    data object History : Screen("history")

    // Phase 3: Community Forum Screens
    data object Forum : Screen("forum")
    data object CreatePost : Screen("create_post")
    data object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String): String = "post_detail/$postId"
    }

    // Existing Diagnostics Screens
    data object Home : Screen("home")
    data object SkinDisease : Screen("skin_disease")
    data object Diabetes : Screen("diabetes")
    data object AiSuggestion : Screen("ai_suggestion/{conditionName}/{conditionSource}") {
        fun createRoute(conditionName: String, conditionSource: String): String =
            "ai_suggestion/$conditionName/$conditionSource"
    }
}