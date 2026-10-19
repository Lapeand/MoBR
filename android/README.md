# NutriAI — MVP UI (Jetpack Compose)

Статичные экраны и `@Preview` для защиты ВКР (задание 2). Без API, ViewModel и бизнес-логики.

Документация проекта: [`../README.md`](../README.md) · план и архитектура: [`../docs/PLAN.md`](../docs/PLAN.md)

## Как открыть

1. **Android Studio** (Ladybug или новее) → *Open* → **эта папка** (`MobR/android`).
2. Дождитесь Gradle Sync.
3. Откройте любой файл экрана (например `ui/main/MainScreen.kt`) и вкладку **Split** / **Design** для превью.

## Карта превью (имена в Android Studio)

| ID | Preview name | Файл |
|----|----------------|------|
| S0 | S0 Splash | `ui/splash/SplashScreen.kt` |
| S1–S3 | Welcome, Login, Register | `ui/auth/AuthScreens.kt` |
| S4 | Onboarding (3 превью) | `ui/onboarding/OnboardingScreen.kt` |
| S5 | Main — Agent / Analytics / Profile | `ui/main/MainScreen.kt` |
| S5a | S5a Agent Tab | `ui/agent/AgentTabScreen.kt` |
| S5b | S5b Analytics Tab | `ui/analytics/AnalyticsTabScreen.kt` |
| S5c | S5c Profile Tab | `ui/profile/ProfileScreens.kt` |
| S6 | Entry Confirm Sheet | `ui/entry/EntryConfirmSheet.kt` |
| S7–S9 | Edit Profile, Recommendations, Settings | `ui/profile/ProfileScreens.kt` |

Галерея: `ui/preview/NutriAiPreviewGallery.kt`

Mock-данные: `app/src/main/java/com/nutriai/mvp/data/MockData.kt`

## Запуск на устройстве

Сборка из этой папки: `gradlew :app:assembleDebug`

## Навигация на устройстве (demo flow)

1. **Welcome** → «Войти» или «Создать аккаунт»
2. **Login / Register** → кнопка входа/регистрации (без проверки полей)
3. **Onboarding** (3 шага) → «Далее» / «Начать» или «Пропустить»
4. **Main** — вкладки Агент / Аналитика / Профиль
5. В профиле: настройки, редактирование, «Подробнее» у рекомендаций; **Выйти** возвращает на Welcome

Граф: `ui/navigation/NutriNavHost.kt`

## Стек модуля

Kotlin, Compose BOM, Material 3, Navigation Compose (подключён для следующего этапа).
