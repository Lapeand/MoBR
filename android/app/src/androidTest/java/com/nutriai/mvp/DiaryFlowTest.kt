package com.nutriai.mvp

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Integration smoke against the dev API on the emulator host (10.0.2.2:8000). */
@RunWith(AndroidJUnit4::class)
class DiaryFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun awaitText(value: String) {
        compose.waitUntil(30_000) { compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun registerConfirmMealAndRestoreSession() {
        awaitText("Создать аккаунт")
        compose.onNodeWithText("Создать аккаунт").performClick()
        compose.onNodeWithText("Имя").performTextInput("Демо")
        compose.onNodeWithText("Email").performTextInput("android${System.currentTimeMillis()}@example.com")
        compose.onNodeWithText("Пароль · минимум 8 символов").performTextInput("TestPass1234")
        compose.onNodeWithText("Зарегистрироваться").performClick()
        awaitText("Ваша цель")
        compose.waitUntil(30_000) { compose.onAllNodes(hasText("Далее") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Далее").performClick()
        awaitText("О вас")
        compose.onNodeWithText("Далее").performScrollTo().performClick()
        awaitText("Активность")
        compose.onNodeWithText("Сохранить").performScrollTo().performClick()
        awaitText("Что вы ели?")
        compose.waitUntil(30_000) { compose.onAllNodes(hasText("Что вы ели?") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Что вы ели?").performTextInput("борщ 300 г, хлеб 40 г")
        compose.onNodeWithContentDescription("Отправить").performClick()
        awaitText("Проверьте запись")
        compose.waitUntil(30_000) { compose.onAllNodes(hasText("Сохранить в дневник") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Сохранить в дневник").performScrollTo().performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Проверьте запись").fetchSemanticsNodes().isEmpty() }
        compose.onNode(hasText("Аналитика") and hasClickAction()).performClick()
        awaitText("247 / 2370 ккал")
        compose.activityRule.scenario.recreate()
        awaitText("Аналитика")
        compose.onNode(hasText("Аналитика") and hasClickAction()).performClick()
        awaitText("247 / 2370 ккал")
    }
}
