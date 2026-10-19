package com.nutriai.mvp.runtime

import org.junit.Assert.*
import org.junit.Test
import kotlinx.serialization.decodeFromString

class FoodItemTest {
    @Test fun portionScalesAllNutrients() {
        val food = FoodItem("Борщ", 300.0, 147.0, 6.0, 6.0, 18.0)
        val scaled = food.scaled(150.0)
        assertEquals(73.5, scaled.kcal, 0.01)
        assertEquals(3.0, scaled.protein, 0.01)
        assertEquals(3.0, scaled.fat, 0.01)
        assertEquals(9.0, scaled.carbs, 0.01)
        assertEquals(food, scaled.scaled(300.0))
    }
    @Test fun apiEntryContractDecodes() {
        val entry = json.decodeFromString<Entry>("""{"id":"one","request_id":"r","date":"2026-10-19","meal":"Обед","items":[{"name":"Борщ","weight_g":300,"kcal":147,"protein":6,"fat":6,"carbs":18}],"totals":{"kcal":147,"protein":6,"fat":6,"carbs":18}}""")
        assertEquals(147.0, entry.totals.kcal, 0.01)
        assertEquals(300.0, entry.items.single().weight_g, 0.01)
    }
}
