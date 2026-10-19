# NutriAI

Дневник питания с ИИ-агентом — проект для защиты ВКР.

## Структура репозитория

| Папка | Содержимое |
|-------|------------|
| [`docs/`](docs/) | План: стек, архитектура, задание 2 (экраны, навигация) |
| [`android/`](android/) | Android-приложение (Kotlin, Jetpack Compose, MVP UI + `@Preview`) |
| `backend/` | *(планируется)* FastAPI, LLM, PostgreSQL |

## Быстрый старт

### Документация

- Полный план и ТЗ по интерфейсу: [`docs/PLAN.md`](docs/PLAN.md)

### Android (превью экранов)

1. Открой в **Android Studio** папку [`android/`](android/) (именно её, не корень `MobR`).
2. Gradle Sync → любой экран → **Split / Design** для `@Preview`.
3. Подробнее: [`android/README.md`](android/README.md)

## Команда

| Участник | Роль |
|----------|------|
| Куприй Антон | Лидер / продукт |
| Габуния Гурам | AI / Backend |
| Берус Никита | Frontend / Mobile |
| Кирьяков Данил | Data / UX / QA |

## Стек (кратко)

**Клиент:** Kotlin, Jetpack Compose, Material 3, Hilt, Retrofit, Room.  
**Сервер:** Python, FastAPI, PostgreSQL, MinIO, JWT, LLM/Vision.

Подробности — в [`docs/PLAN.md`](docs/PLAN.md).
