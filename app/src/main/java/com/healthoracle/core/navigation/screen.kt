package com.healthoracle.core.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object SkinDisease : Screen("skin_disease")
    data object Diabetes : Screen("diabetes")
    data object AiSuggestion : Screen("ai_suggestion/{conditionName}/{conditionSource}") {
        fun createRoute(conditionName: String, conditionSource: String): String =
            "ai_suggestion/$conditionName/$conditionSource"
    }
    data object Forum : Screen("forum")
    data object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String): String = "post_detail/$postId"
    }
}
