package com.healthoracle.core.navigation

sealed class Screen(val route: String) {

    // ── Onboarding ────────────────────────────────────────────────────────────
    data object Onboarding : Screen("onboarding")

    // ── Auth ──────────────────────────────────────────────────────────────────
    data object Login  : Screen("login")
    data object SignUp : Screen("signup")

    // ── Main patient screens (bottom nav) ────────────────────────────────────
    data object Home        : Screen("home")
    data object WalkTracker : Screen("walk_tracker")
    data object Calendar    : Screen("calendar")
    data object Forum       : Screen("forum")
    data object Profile     : Screen("profile")

    // ── Diagnostics ───────────────────────────────────────────────────────────
    data object SkinDisease : Screen("skin_disease")
    data object Diabetes    : Screen("diabetes")
    data object AiSuggestion : Screen("ai_suggestion/{conditionName}/{conditionSource}") {
        fun createRoute(conditionName: String, conditionSource: String) =
            "ai_suggestion/$conditionName/$conditionSource"
    }

    // ── My Data ───────────────────────────────────────────────────────────────
    data object History : Screen("history")
    data object Todo    : Screen("todo")
    data object MyPosts : Screen("my_posts")
    data object Settings : Screen("settings")

    // ── Prescriptions ─────────────────────────────────────────────────────────
    data object Prescriptions : Screen("prescriptions/{patientId}/{doctorId}") {
        fun createRoute(patientId: String, doctorId: String) =
            "prescriptions/$patientId/$doctorId"
    }

    // ── Forum ─────────────────────────────────────────────────────────────────
    data object CreatePost : Screen("create_post")
    data object PostDetail : Screen("post_detail/{postId}") {
        fun createRoute(postId: String) = "post_detail/$postId"
    }

    // ── Doctor ────────────────────────────────────────────────────────────────
    data object DoctorDashboard : Screen("doctor_dashboard")
    data object Chat : Screen("chat/{patientId}/{doctorId}/{contactName}") {
        fun createRoute(patientId: String, doctorId: String, contactName: String) =
            "chat/$patientId/$doctorId/$contactName"
    }
    data object PatientTasks : Screen("patient_tasks/{patientId}/{patientName}") {
        fun createRoute(patientId: String, patientName: String) =
            "patient_tasks/$patientId/$patientName"
    }

    // ── Fitness ───────────────────────────────────────────────────────────────
    data object WalkHistoryDetail : Screen("walk_history_detail/{sessionId}") {
        fun createRoute(sessionId: Long) = "walk_history_detail/$sessionId"
    }
}
