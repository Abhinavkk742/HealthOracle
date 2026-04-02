package com.healthoracle.core.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.healthoracle.core.ui.components.HealthOracleBottomBar
import com.healthoracle.core.ui.components.NavItem
import com.healthoracle.presentation.aisuggestion.AiSuggestionScreen
import com.healthoracle.presentation.auth.LoginScreen
import com.healthoracle.presentation.auth.SignUpScreen
import com.healthoracle.presentation.calendar.CalendarScreen
import com.healthoracle.presentation.chat.ChatScreen
import com.healthoracle.presentation.diabetes.DiabetesScreen
import com.healthoracle.presentation.doctor.DoctorDashboardScreen
import com.healthoracle.presentation.doctor.PatientTasksScreen
import com.healthoracle.presentation.doctor.PrescriptionScreen
import com.healthoracle.presentation.forum.CreatePostScreen
import com.healthoracle.presentation.forum.ForumScreen
import com.healthoracle.presentation.forum.PostDetailScreen
import com.healthoracle.presentation.history.HistoryScreen
import com.healthoracle.presentation.home.HomeScreen
import com.healthoracle.presentation.onboarding.OnboardingScreen
import com.healthoracle.presentation.profile.ProfileScreen
import com.healthoracle.presentation.settings.SettingsScreen
import com.healthoracle.presentation.skin.SkinDiseaseScreen
import com.healthoracle.presentation.todo.TodoScreen
import com.healthoracle.presentation.walktracker.WalkHistoryDetailScreen
import com.healthoracle.presentation.walktracker.WalkTrackerScreen

// Bottom nav is shown on these routes only
private val bottomNavRoutes = setOf(
    Screen.Home.route,
    Screen.Forum.route,
    Screen.WalkTracker.route,
    Screen.Profile.route,
    Screen.Calendar.route
)

private val bottomNavItems = listOf(
    NavItem(Icons.Filled.Home,          "Home",      Screen.Home.route),
    NavItem(Icons.Filled.DirectionsWalk,"Activity",  Screen.WalkTracker.route),
    NavItem(Icons.Filled.DateRange,     "Calendar",  Screen.Calendar.route),
    NavItem(Icons.Filled.Forum,         "Forum",     Screen.Forum.route),
    NavItem(Icons.Filled.Person,        "Profile",   Screen.Profile.route)
)

@Composable
fun HealthOracleNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                HealthOracleBottomBar(
                    items        = bottomNavItems,
                    currentRoute = currentRoute,
                    onNavigate   = { route ->
                        navController.navigate(route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = startDestination,
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition   = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition  = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition   = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            // ── Onboarding ──────────────────────────────────────────────────
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onFinishOnboarding = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }

            // ── Auth ────────────────────────────────────────────────────────
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToDoctorDashboard = {
                        navController.navigate(Screen.DoctorDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) }
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onNavigateToHome = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    },
                    onNavigateToDoctorDashboard = {
                        navController.navigate(Screen.DoctorDashboard.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = { navController.popBackStack() }
                )
            }

            // ── Home / Dashboard ────────────────────────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToSkinDisease     = { navController.navigate(Screen.SkinDisease.route) },
                    onNavigateToDiabetes        = { navController.navigate(Screen.Diabetes.route) },
                    onNavigateToForum           = { navController.navigate(Screen.Forum.route) },
                    onNavigateToProfile         = { navController.navigate(Screen.Profile.route) },
                    onNavigateToHistory         = { navController.navigate(Screen.History.route) },
                    onNavigateToCalendar        = { navController.navigate(Screen.Calendar.route) },
                    onNavigateToWalkTracker     = { navController.navigate(Screen.WalkTracker.route) },
                    onNavigateToTodo            = { navController.navigate(Screen.Todo.route) },
                    onNavigateToPrescriptions   = { patientId, doctorId ->
                        navController.navigate(Screen.Prescriptions.createRoute(patientId, doctorId))
                    },
                    onNavigateToChat            = { patientId, doctorId, name ->
                        navController.navigate(Screen.Chat.createRoute(patientId, doctorId, name))
                    }
                )
            }

            // ── Diagnostics ─────────────────────────────────────────────────
            composable(Screen.SkinDisease.route) {
                SkinDiseaseScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAiSuggestion = { conditionName ->
                        navController.navigate(Screen.AiSuggestion.createRoute(conditionName, "skin"))
                    }
                )
            }
            composable(Screen.Diabetes.route) {
                DiabetesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAiSuggestion = { conditionName ->
                        navController.navigate(Screen.AiSuggestion.createRoute(conditionName, "diabetes"))
                    }
                )
            }
            composable(
                route     = Screen.AiSuggestion.route,
                arguments = listOf(
                    navArgument("conditionName")   { type = NavType.StringType },
                    navArgument("conditionSource") { type = NavType.StringType }
                )
            ) { back ->
                AiSuggestionScreen(
                    conditionName   = back.arguments?.getString("conditionName") ?: "",
                    conditionSource = back.arguments?.getString("conditionSource") ?: "",
                    onNavigateBack  = { navController.popBackStack() }
                )
            }

            // ── Profile & Settings ──────────────────────────────────────────
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateBack       = { navController.popBackStack() },
                    onNavigateToLogin    = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToMyPosts  = { navController.navigate(Screen.MyPosts.route) },
                    onNavigateToChat     = { patientId, doctorId, name ->
                        navController.navigate(Screen.Chat.createRoute(patientId, doctorId, name))
                    }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack    = { navController.popBackStack() },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.History.route) {
                HistoryScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Calendar & Tasks ────────────────────────────────────────────
            composable(Screen.Calendar.route) {
                CalendarScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Todo.route) {
                TodoScreen(onNavigateBack = { navController.popBackStack() })
            }

            // ── Forum ───────────────────────────────────────────────────────
            composable(Screen.Forum.route) {
                ForumScreen(
                    onNavigateToCreatePost = { navController.navigate(Screen.CreatePost.route) },
                    onNavigateToPostDetail = { postId ->
                        navController.navigate(Screen.PostDetail.createRoute(postId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.CreatePost.route) {
                CreatePostScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route     = Screen.PostDetail.route,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { back ->
                PostDetailScreen(
                    postId         = back.arguments?.getString("postId") ?: "",
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.MyPosts.route) {
                com.healthoracle.presentation.profile.MyPostsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPostDetail = { postId ->
                        navController.navigate(Screen.PostDetail.createRoute(postId))
                    }
                )
            }

            // ── Walk Tracker ────────────────────────────────────────────────
            composable(Screen.WalkTracker.route) {
                WalkTrackerScreen(
                    onNavigateToWalkDetail = { sessionId ->
                        navController.navigate(Screen.WalkHistoryDetail.createRoute(sessionId))
                    }
                )
            }
            composable(
                route     = Screen.WalkHistoryDetail.route,
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType })
            ) { back ->
                WalkHistoryDetailScreen(
                    sessionId      = back.arguments?.getLong("sessionId") ?: 0L,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ── Doctor flows ────────────────────────────────────────────────
            composable(Screen.DoctorDashboard.route) {
                DoctorDashboardScreen(
                    onNavigateToChat = { patientId, doctorId, name ->
                        navController.navigate(Screen.Chat.createRoute(patientId, doctorId, name))
                    },
                    onNavigateToForum = { navController.navigate(Screen.Forum.route) },
                    onNavigateToPatientTasks = { patientId, name ->
                        navController.navigate(Screen.PatientTasks.createRoute(patientId, name))
                    },
                    onNavigateToPrescriptions = { patientId, _ ->
                        val doctorId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                        navController.navigate(Screen.Prescriptions.createRoute(patientId, doctorId))
                    },
                    onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
            composable(
                route     = Screen.Chat.route,
                arguments = listOf(
                    navArgument("patientId")    { type = NavType.StringType },
                    navArgument("doctorId")     { type = NavType.StringType },
                    navArgument("contactName")  { type = NavType.StringType }
                )
            ) { back ->
                val chatViewModel: com.healthoracle.presentation.chat.ChatViewModel = hiltViewModel()
                val messages by chatViewModel.messages.collectAsState()
                val contactProfileUrl by chatViewModel.contactProfileUrl.collectAsState()

                ChatScreen(
                    contactName = back.arguments?.getString("contactName") ?: "",
                    contactProfileUrl = contactProfileUrl,
                    currentUserId = chatViewModel.currentUserId,
                    messages = messages,
                    onSendMessage = { text, imageUri -> chatViewModel.sendMessage(text, imageUri) },
                    onNavigateBack = { navController.popBackStack() },
                    viewModel = chatViewModel
                )
            }
            composable(
                route     = Screen.PatientTasks.route,
                arguments = listOf(
                    navArgument("patientId")   { type = NavType.StringType },
                    navArgument("patientName") { type = NavType.StringType }
                )
            ) { back ->
                PatientTasksScreen(
                    patientId   = back.arguments?.getString("patientId") ?: "",
                    patientName = back.arguments?.getString("patientName") ?: "",
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route     = Screen.Prescriptions.route,
                arguments = listOf(
                    navArgument("patientId") { type = NavType.StringType },
                    navArgument("doctorId")  { type = NavType.StringType }
                )
            ) { back ->
                PrescriptionScreen(
                    patientId = back.arguments?.getString("patientId") ?: "",
                    doctorId = back.arguments?.getString("doctorId") ?: "",
                    patientName = "",
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
