package com.nutriai.mvp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nutriai.mvp.ui.auth.LoginScreen
import com.nutriai.mvp.ui.auth.RegisterScreen
import com.nutriai.mvp.ui.auth.WelcomeScreen
import com.nutriai.mvp.ui.main.MainScreen
import com.nutriai.mvp.ui.main.MainTab
import com.nutriai.mvp.ui.onboarding.OnboardingScreen
import com.nutriai.mvp.ui.onboarding.OnboardingStep
import com.nutriai.mvp.ui.profile.EditProfileScreen
import com.nutriai.mvp.ui.profile.ProfileTabScreen
import com.nutriai.mvp.ui.profile.RecommendationsDetailScreen
import com.nutriai.mvp.ui.profile.SettingsScreen

@Composable
fun NutriNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Routes.WELCOME,
    ) {
        composable(Routes.WELCOME) {
            WelcomeScreen(
                onLoginClick = { navController.navigate(Routes.LOGIN) },
                onRegisterClick = { navController.navigate(Routes.REGISTER) },
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onBack = { navController.popBackStack() },
                onLoginSuccess = { navController.navigate(Routes.ONBOARDING) },
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegisterSuccess = { navController.navigate(Routes.ONBOARDING) },
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingFlow(
                onComplete = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.WELCOME) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.MAIN) {
            var selectedTab by rememberSaveable { mutableStateOf(MainTab.Agent) }
            MainScreen(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                profileContent = {
                    ProfileTabScreen(
                        onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                        onEditProfileClick = { navController.navigate(Routes.EDIT_PROFILE) },
                        onRecommendationDetailClick = { navController.navigate(Routes.RECOMMENDATIONS) },
                    )
                },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Routes.WELCOME) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(Routes.EDIT_PROFILE) {
            EditProfileScreen(
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
            )
        }

        composable(Routes.RECOMMENDATIONS) {
            RecommendationsDetailScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun OnboardingFlow(onComplete: () -> Unit) {
    var step by remember { mutableStateOf(OnboardingStep.Goal) }

    fun advance() {
        when (step) {
            OnboardingStep.Goal -> step = OnboardingStep.Body
            OnboardingStep.Body -> step = OnboardingStep.Activity
            OnboardingStep.Activity -> onComplete()
        }
    }

    OnboardingScreen(
        step = step,
        onNext = { advance() },
        onSkip = onComplete,
    )
}
