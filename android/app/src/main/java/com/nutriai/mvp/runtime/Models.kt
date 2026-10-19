package com.nutriai.mvp.runtime

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt
import java.time.LocalDate
import java.util.UUID

@Serializable
data class Nutrients(val kcal: Double = 0.0, val protein: Double = 0.0, val fat: Double = 0.0, val carbs: Double = 0.0) {
    fun label() = "${kcal.roundToInt()} ккал · Б ${protein.roundToInt()} · Ж ${fat.roundToInt()} · У ${carbs.roundToInt()}"
}
@Serializable
data class Profile(
    val name: String = "Пользователь", val sex: String = "male", val age: Int = 25,
    val weight: Double = 75.0, val height: Double = 175.0, val goal: String = "maintain",
    val activity: String = "moderate", val onboarded: Boolean = false,
    val targets: Nutrients = Nutrients(),
)
@Serializable
data class FoodItem(val name: String, val weight_g: Double, val kcal: Double, val protein: Double, val fat: Double, val carbs: Double, val source: String = "manual") {
    fun scaled(grams: Double) = copy(weight_g = grams, kcal = kcal * grams / weight_g,
        protein = protein * grams / weight_g, fat = fat * grams / weight_g, carbs = carbs * grams / weight_g)
    fun label() = Nutrients(kcal, protein, fat, carbs).label()
}
@Serializable
data class Entry(val id: String = "", val request_id: String = UUID.randomUUID().toString(),
    val date: String = LocalDate.now().toString(), val meal: String = "Приём пищи", val items: List<FoodItem>, val totals: Nutrients = Nutrients(), val photoPath: String? = null)
@Serializable
data class Message(val is_user: Boolean, val text: String, val photoPath: String? = null)
@Serializable
data class AgentResult(val reply: String, val items: List<FoodItem> = emptyList())
@Serializable
data class DayStats(val date: String, val totals: Nutrients)
@Serializable
data class Recommendation(val title: String, val meal: String, val description: String, val items: List<FoodItem>, val totals: Nutrients)
@Serializable
data class Snapshot(val profile: Profile, val entries: List<Entry>, val messages: List<Message>, val weekly: List<DayStats>, val recommendations: List<Recommendation>, val date: String)

data class AppState(
    val profile: Profile? = null, val busy: Boolean = false,
    val error: String? = null, val date: LocalDate = LocalDate.now(),
    val entries: List<Entry> = emptyList(), val messages: List<Message> = emptyList(),
    val weekly: List<DayStats> = emptyList(), val recommendations: List<Recommendation> = emptyList(),
    val pending: Entry? = null,
)
val goals = linkedMapOf("lose" to "Похудение", "maintain" to "Поддержание формы", "gain" to "Набор массы")
val activities = linkedMapOf("low" to "Мало двигаюсь", "moderate" to "1–3 тренировки в неделю", "high" to "Ежедневный спорт")
