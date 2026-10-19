package com.nutriai.mvp.data

data class Macros(
    val kcal: Int,
    val protein: Int,
    val fat: Int,
    val carbs: Int,
) {
    fun formattedShort(): String = "$kcal kcal · Б$protein Ж$fat У$carbs"
}

data class DiaryRow(
    val meal: String,
    val macros: Macros,
)

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
)

data class DiaryEntryCard(
    val title: String,
    val portionGrams: Int,
    val macros: Macros,
)

data class MealRecommendation(
    val title: String,
    val mealLabel: String,
    val description: String,
    val macros: Macros,
    val detail: String,
)

object MockData {
    const val userName = "Антон"
    const val goalLabel = "−5 кг"
    const val weightKg = 82
    const val heightCm = 178
    const val gender = "муж"

    val dailyTargetKcal = 2200
    val dailyCurrentKcal = 1840

    val chatMessages = listOf(
        ChatMessage(isUser = false, text = "Что вы ели сегодня? Можно текстом, голосом или фото."),
        ChatMessage(isUser = true, text = "Овсянка с бананом на завтрак"),
    )

    val entryCard = DiaryEntryCard(
        title = "Овсянка с бананом",
        portionGrams = 250,
        macros = Macros(kcal = 420, protein = 24, fat = 8, carbs = 72),
    )

    val weeklyKcal = listOf(1950, 2100, 1840, 2050, 2200, 1780, 1900)
    val weekDays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    val diaryRows = listOf(
        DiaryRow("Завтрак", Macros(420, 24, 8, 72)),
        DiaryRow("Обед", Macros(650, 38, 22, 58)),
        DiaryRow("Перекус", Macros(180, 6, 8, 12)),
        DiaryRow("Ужин", Macros(590, 32, 18, 45)),
    )

    val macroTotals = Macros(kcal = 1840, protein = 100, fat = 56, carbs = 187)

    val recommendations = listOf(
        MealRecommendation(
            mealLabel = "Обед",
            title = "Куриный салат + гречка",
            description = "Лёгкий белковый обед с медленными углеводами",
            macros = Macros(520, 42, 14, 48),
            detail = "150 г куриной грудки, салат, 120 г гречки. Подходит под ваш остаток калорий и белка на сегодня.",
        ),
        MealRecommendation(
            mealLabel = "Ужин",
            title = "Творог 5% + ягоды",
            description = "Медленный белок на вечер",
            macros = Macros(280, 28, 8, 22),
            detail = "200 г творога и 100 г ягод. Низкий glycemic load, помогает закрыть норму белка.",
        ),
    )
}
