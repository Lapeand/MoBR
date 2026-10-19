package com.nutriai.mvp.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.nutriai.mvp.ui.auth.LoginScreen
import com.nutriai.mvp.ui.auth.RegisterScreen
import com.nutriai.mvp.ui.auth.WelcomeScreen
import com.nutriai.mvp.ui.entry.EntryConfirmSheet
import com.nutriai.mvp.ui.main.MainScreen
import com.nutriai.mvp.ui.main.MainTab
import com.nutriai.mvp.ui.onboarding.OnboardingScreen
import com.nutriai.mvp.ui.onboarding.OnboardingStep
import com.nutriai.mvp.ui.profile.EditProfileScreen
import com.nutriai.mvp.ui.profile.ProfileTabScreen
import com.nutriai.mvp.ui.profile.RecommendationsDetailScreen
import com.nutriai.mvp.ui.profile.SettingsScreen
import com.nutriai.mvp.ui.splash.SplashScreen

/**
 * Галерея всех MVP-превью — удобно для скриншотов к ВКР (Split view на этом файле).
 */
@Preview(name = "Gallery — Welcome", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun GalleryWelcome() {
    NutriPreviewSurface { WelcomeScreen() }
}

@Preview(name = "Gallery — Main Agent", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun GalleryMain() {
    NutriPreviewSurface { MainScreen(selectedTab = MainTab.Agent) }
}

@Preview(name = "Gallery — Onboarding", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun GalleryOnboarding() {
    NutriPreviewSurface { OnboardingScreen(step = OnboardingStep.Goal) }
}

@Preview(name = "Gallery — Entry sheet", showBackground = true, widthDp = 411, heightDp = 400)
@Composable
private fun GalleryEntry() {
    NutriPreviewSurface { EntryConfirmSheet() }
}

@Preview(name = "Gallery — Profile", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun GalleryProfile() {
    NutriPreviewSurface { ProfileTabScreen() }
}

@Preview(name = "Gallery — Splash", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun GallerySplash() {
    NutriPreviewSurface { SplashScreen() }
}

@Preview(name = "Gallery — Login", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun GalleryLogin() {
    NutriPreviewSurface { LoginScreen(onBack = {}, onLoginSuccess = {}) }
}

@Preview(name = "Gallery — Register", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun GalleryRegister() {
    NutriPreviewSurface { RegisterScreen(onBack = {}, onRegisterSuccess = {}) }
}

@Preview(name = "Gallery — Edit profile", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun GalleryEditProfile() {
    NutriPreviewSurface { EditProfileScreen(onBack = {}) }
}

@Preview(name = "Gallery — Reco detail", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun GalleryReco() {
    NutriPreviewSurface { RecommendationsDetailScreen(onBack = {}) }
}

@Preview(name = "Gallery — Settings", showBackground = true, widthDp = 411, heightDp = 891)
@Composable
private fun GallerySettings() {
    NutriPreviewSurface { SettingsScreen(onBack = {}) }
}
