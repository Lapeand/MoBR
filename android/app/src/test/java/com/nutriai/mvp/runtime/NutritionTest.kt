package com.nutriai.mvp.runtime

import org.junit.Assert.*
import org.junit.Test

class NutritionTest {
    @Test fun referenceMealIsCalculatedPerPortion() {
        val result=Nutrition.parse("съел борщ 300 г, хлеб 40 г")
        assertEquals(2,result.items.size)
        assertEquals(247.0,Nutrition.totals(result.items).kcal,.001)
    }
    @Test fun decimalCommaAndKilogramsAreSupported() {
        val result=Nutrition.parse("рис 0,15 кг, курица 150 г")
        assertEquals(2,result.items.size)
        assertEquals(150.0,result.items[0].weight_g,.001)
    }
    @Test fun ambiguousOrUnknownFoodNeedsClarification() {
        listOf("борщ 200 г, неизвестный продукт", "неизвестное блюдо", "рис 9000 г", "рис 150", "курица рис 200 г").forEach {
            assertTrue(it,Nutrition.parse(it).items.isEmpty())
        }
    }
    @Test fun specificProductWinsOverPrefix() {
        assertEquals("Сырники",Nutrition.parse("сырники 150 г").items.single().name)
        assertEquals("Сыр",Nutrition.parse("сыр 30 г").items.single().name)
    }
    @Test fun defaultPortionAndPiecesAreExplicit() {
        assertEquals(300.0,Nutrition.parse("борщ").items.single().weight_g,.001)
        assertEquals(120.0,Nutrition.parse("яйцо 2 шт").items.single().weight_g,.001)
    }
    @Test fun tdeeMatchesProfileFormula() {
        assertEquals(2370.0,Nutrition.targets(Profile()).kcal,.001)
        val p=Profile(weight=82.0,height=178.0,age=25,goal="lose")
        assertEquals(2118.0,Nutrition.targets(p).kcal,.001)
        assertTrue(Nutrition.targets(p.copy(sex="female")).kcal<Nutrition.targets(p).kcal)
    }
    @Test fun recommendationsFitEveryRemainingNutrient() {
        val p=Profile()
        val eaten=Nutrients(1900.0,90.0,65.0,240.0)
        val target=Nutrition.targets(p)
        val result=Nutrition.recommendations(p,eaten)
        assertTrue(result.isNotEmpty())
        result.forEach {
            assertTrue(it.totals.kcal<=target.kcal-eaten.kcal+.001)
            assertTrue(it.totals.protein<=target.protein-eaten.protein+.001)
            assertTrue(it.totals.fat<=target.fat-eaten.fat+.001)
            assertTrue(it.totals.carbs<=target.carbs-eaten.carbs+.001)
        }
        assertTrue(Nutrition.recommendations(p,target).isEmpty())
    }
    @Test fun csvEscapesCellsAndSpreadsheetFormulas() {
        val csv=Nutrition.csv(listOf(Entry(date="2026-10-19",meal="=2+2",items=listOf(FoodItem("Суп, домашний",100.0,40.0,1.0,2.0,3.0)))))
        assertTrue(csv.startsWith("\uFEFFdate,"))
        assertTrue(csv.contains("\"'=2+2\""))
        assertTrue(csv.contains("\"Суп, домашний\""))
    }
}
