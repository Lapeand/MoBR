package com.nutriai.mvp.runtime

import java.util.Locale

data class CalorieGroup(val name: String, val kcal: Double)
data class WeekSummary(val total: Nutrients, val loggedDays: Int, val averageKcal: Double?)

object AnalyticsData {
    fun macroEnergy(total: Nutrients) = listOf(total.protein * 4, total.fat * 9, total.carbs * 4)
    /** Largest-remainder rounding keeps the displayed pie labels at exactly 100%. */
    fun macroPercentages(total: Nutrients): List<Int> {
        val energies=macroEnergy(total)
        val sum=energies.sum()
        if(sum<=0) return listOf(0,0,0)
        val raw=energies.map { it/sum*100 }
        val percentages=raw.map { it.toInt() }.toMutableList()
        raw.indices.sortedByDescending { raw[it]-percentages[it] }.take(100-percentages.sum()).forEach { percentages[it]++ }
        return percentages
    }
    fun meals(entries: List<Entry>): List<CalorieGroup> = entries.groupBy { it.meal.trim() }
        .map { (name, meals) -> CalorieGroup(name, Nutrition.totals(meals.flatMap { it.items }).kcal) }
        .sortedByDescending { it.kcal }
    fun foods(entries: List<Entry>): List<CalorieGroup> = entries.flatMap { it.items }
        .groupBy { it.name.trim().lowercase(Locale.ROOT) }
        .map { (_, foods) -> CalorieGroup(foods.first().name.trim(), foods.sumOf { it.kcal }) }
        .sortedByDescending { it.kcal }
    fun week(days: List<DayStats>): WeekSummary {
        val total=Nutrients(days.sumOf { it.totals.kcal },days.sumOf { it.totals.protein },days.sumOf { it.totals.fat },days.sumOf { it.totals.carbs })
        val logged=days.count { it.entryCount>0 }
        return WeekSummary(total,logged,if(logged==0) null else total.kcal/logged)
    }
}
