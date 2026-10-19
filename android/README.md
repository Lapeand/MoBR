# Android NutriAI

Откройте эту папку в Android Studio. Требования: JDK 17, SDK 35, minSdk 26.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

По умолчанию debug-клиент обращается к локальному API через `http://10.0.2.2:8000/` (Android Emulator). Другой сервер: `-PAPI_BASE_URL=https://your-api.example/`. Для телефона по USB: `adb reverse tcp:8000 tcp:8000` и `-PAPI_BASE_URL=http://127.0.0.1:8000/`.

Рабочие экраны находятся в `runtime/`, общие компоненты и исходные дизайн-превью в `ui/`. Авторизация не имеет встроенных демонстрационных паролей: зарегистрируйте пользователя на своём backend.

Полные инструкции и ограничения: [README репозитория](../README.md).
