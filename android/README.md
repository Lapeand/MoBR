# NutriAI Android · 2.0 local

Полностью автономная версия. Сервер, аккаунт, API URL, ключи и интернет не нужны.

Откройте папку в Android Studio. JDK 17, compileSdk 35, minSdk 26.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Для проверки на эмуляторе отключите Wi-Fi/мобильную сеть и выполните `./gradlew connectedDebugAndroidTest`. Тест проверяет отсутствие разрешения INTERNET, онбординг, расчёт КБЖУ, SQLite, повторный запуск, удаление и справочник.

Снимок последнего дневника из старой версии переносится однократно при доступном ключе Android Keystore. Полное описание функций и ограничений: [README](../README.md).
