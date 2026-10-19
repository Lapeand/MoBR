package com.nutriai.mvp.runtime

import org.junit.Assert.*
import org.junit.Test

class AnalyticsDataTest {
    private fun item(name: String, kcal: Double)=FoodItem(name,100.0,kcal,1.0,1.0,1.0)
    @Test fun breakdownsReconcileToDiaryDespiteStaleEntryTotalsAndRepeatedProductNames() {
        val entries=listOf(
            Entry(meal="Завтрак",items=listOf(item(" Хлеб ",100.0),item("Банан",89.0))),
            Entry(meal="Обед",items=listOf(item("хлеб",50.0),item("Борщ",147.0)))
        )
        val meals=AnalyticsData.meals(entries)
        val foods=AnalyticsData.foods(entries)
        assertEquals(386.0,meals.sumOf { it.kcal },.001)
        assertEquals(386.0,foods.sumOf { it.kcal },.001)
        assertEquals(2,meals.size)
        assertEquals(3,foods.size)
        assertEquals("Хлеб",foods.first().name)
        assertEquals(150.0,foods.first().kcal,.001)
    }
    @Test fun unloggedDaysAreExcludedButRecordedZeroCalorieDaysCountInAverage() {
        val summary=AnalyticsData.week(listOf(
            DayStats("2026-10-17",Nutrients()),
            DayStats("2026-10-18",Nutrients(247.0,3.0,4.0,5.0),2),
            DayStats("2026-10-19",Nutrients(),1)))
        assertEquals(2,summary.loggedDays)
        assertEquals(123.5,summary.averageKcal!!,.001)
        assertEquals(247.0,summary.total.kcal,.001)
        assertEquals(3.0,summary.total.protein,.001)
    }
    @Test fun emptyPeriodHasNoFabricatedAverageOrGroups() {
        assertNull(AnalyticsData.week(listOf(DayStats("2026-10-19",Nutrients()))).averageKcal)
        assertTrue(AnalyticsData.meals(emptyList()).isEmpty())
        assertTrue(AnalyticsData.foods(emptyList()).isEmpty())
    }
    @Test fun piePercentagesTotalOneHundredDespiteRoundingAndZeroEnergyIsEmpty() {
        val percentages=AnalyticsData.macroPercentages(Nutrients(247.0,9.2,7.2,37.6))
        assertEquals(100,percentages.sum())
        assertEquals(listOf(14,26,60),percentages)
        assertEquals(listOf(0,0,0),AnalyticsData.macroPercentages(Nutrients()))
        assertEquals(listOf(0,100,0),AnalyticsData.macroPercentages(Nutrients(fat=5.0)))
    }
}
