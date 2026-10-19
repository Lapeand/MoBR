package com.nutriai.mvp.runtime

import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

/** Reference values per 100 g of prepared food. Estimates, never inferred from a photograph. */
data class LocalFood(val name: String, val aliases: List<String>, val nutrients: Nutrients, val defaultGrams: Double = 200.0) {
    fun portion(grams: Double) = FoodItem(name, grams, nutrients.kcal*grams/100, nutrients.protein*grams/100,
        nutrients.fat*grams/100, nutrients.carbs*grams/100, "Локальный справочник · оценка")
}
object Nutrition {
    private fun food(name: String, aliases: String, kcal: Double, p: Double, f: Double, c: Double, grams: Double = 200.0) =
        LocalFood(name, aliases.split('|'), Nutrients(kcal,p,f,c), grams)
    val foods = listOf(
        food("Борщ", "борщ",49.0,2.0,2.0,6.0,300.0),
        food("Хлеб", "хлеб|батон",250.0,8.0,3.0,49.0,40.0),
        food("Овсяная каша", "овсян|овсяная|овсяную",88.0,3.0,1.7,15.0),
        food("Банан", "банан",89.0,1.1,0.3,23.0,120.0),
        food("Куриная грудка", "курин|куриц|грудк",165.0,31.0,3.6,0.0,150.0),
        food("Гречка варёная", "греч",110.0,4.2,1.1,21.0,150.0),
        food("Рис варёный", "рис",130.0,2.7,0.3,28.0,150.0),
        food("Творог 5%", "творог",121.0,17.0,5.0,1.8),
        food("Яйцо", "яйц|яиц",155.0,13.0,11.0,1.1,60.0),
        food("Яблоко", "яблок",52.0,0.3,0.2,14.0,150.0),
        food("Овощной салат", "салат",35.0,1.0,1.0,5.0,150.0),
        food("Кефир 2,5%", "кефир",53.0,3.0,2.5,4.0,250.0),
        food("Картофель варёный", "картоф|картош",82.0,2.0,0.4,17.0),
        food("Котлета", "котлет",220.0,15.0,14.0,9.0,100.0),
        food("Макароны варёные", "макарон|паста|спагетти",131.0,5.0,1.1,25.0),
        food("Говядина", "говядин",187.0,26.0,9.0,0.0,150.0),
        food("Индейка", "индейк",135.0,29.0,1.6,0.0,150.0),
        food("Лосось", "лосос|семг|сёмг",208.0,20.0,13.0,0.0,150.0),
        food("Треска", "треск",82.0,18.0,0.7,0.0,150.0),
        food("Тунец", "тунец|тунц",116.0,26.0,1.0,0.0,120.0),
        food("Молоко 2,5%", "молок",52.0,2.8,2.5,4.7,250.0),
        food("Йогурт натуральный", "йогурт",60.0,4.0,2.5,5.0,150.0),
        food("Сыр", "сыр",350.0,25.0,27.0,0.0,30.0),
        food("Сметана 15%", "сметан",162.0,2.6,15.0,3.6,30.0),
        food("Сливочное масло", "сливочное масло|сливочного масла",748.0,0.5,82.0,0.8,10.0),
        food("Растительное масло", "оливков|подсолнечн|растительное масло",899.0,0.0,99.9,0.0,10.0),
        food("Огурец", "огур",15.0,0.8,0.1,2.8,100.0),
        food("Помидор", "помидор|томат",20.0,1.1,0.2,3.7,100.0),
        food("Морковь", "морков",35.0,1.3,0.1,6.9,100.0),
        food("Капуста", "капуст",27.0,1.8,0.1,4.7,100.0),
        food("Брокколи", "брокколи",34.0,2.8,0.4,7.0,150.0),
        food("Авокадо", "авокадо",160.0,2.0,14.7,8.5,100.0),
        food("Апельсин", "апельсин",47.0,0.9,0.1,12.0,150.0),
        food("Груша", "груш",57.0,0.4,0.1,15.0,150.0),
        food("Клубника", "клубник",32.0,0.7,0.3,7.7,100.0),
        food("Ягоды", "ягод",45.0,0.8,0.4,10.0,100.0),
        food("Миндаль", "миндал",579.0,21.0,50.0,22.0,30.0),
        food("Грецкий орех", "грецк|орех",654.0,15.0,65.0,14.0,30.0),
        food("Шоколад", "шоколад",540.0,6.0,32.0,57.0,25.0),
        food("Мёд", "мед|мёд",304.0,0.3,0.0,82.0,20.0),
        food("Сахар", "сахар",400.0,0.0,0.0,100.0,10.0),
        food("Чечевица варёная", "чечевиц",116.0,9.0,0.4,20.0,150.0),
        food("Фасоль варёная", "фасол",127.0,8.7,0.5,23.0,150.0),
        food("Плов", "плов",198.0,6.0,8.0,26.0,250.0),
        food("Пельмени", "пельмен",275.0,11.0,12.0,30.0),
        food("Сырники", "сырник",230.0,12.0,10.0,23.0,150.0),
        food("Блин", "блин",190.0,5.0,8.0,25.0,100.0),
        food("Пицца", "пицц",266.0,11.0,10.0,33.0,150.0),
        food("Вода", "вода|воды|воду",0.0,0.0,0.0,0.0,250.0),
        food("Чай без сахара", "чай|чая",1.0,0.0,0.0,0.0,250.0),
        food("Кофе без сахара", "кофе",2.0,0.2,0.0,0.0,200.0),
    )
    fun targets(p: Profile): Nutrients {
        require(p.weight in 30.0..300.0 && p.height in 120.0..230.0 && p.age in 18..100 && p.name.isNotBlank()) { "Проверьте параметры профиля" }
        require(p.sex in listOf("male","female") && p.goal in goals && p.activity in activities)
        val bmr = 10*p.weight + 6.25*p.height - 5*p.age + if(p.sex == "male") 5 else -161
        val kcal = (bmr * mapOf("low" to 1.2,"moderate" to 1.375,"high" to 1.725).getValue(p.activity) * mapOf("lose" to .85,"maintain" to 1.0,"gain" to 1.1).getValue(p.goal)).roundToInt().toDouble()
        val protein = (p.weight*1.6).roundToInt().toDouble()
        val fat = (kcal*.3/9).roundToInt().toDouble()
        return Nutrients(kcal, protein, fat, ((kcal-protein*4-fat*9)/4).roundToInt().coerceAtLeast(0).toDouble())
    }
    fun totals(items: List<FoodItem>) = Nutrients(items.sumOf { it.kcal },items.sumOf { it.protein },items.sumOf { it.fat },items.sumOf { it.carbs })
    fun search(query: String) = foods.filter { query.isBlank() || it.name.contains(query,true) || it.aliases.any { a -> a.contains(query,true) } }
    fun parse(text: String): AgentResult {
        val normalized = text.lowercase(Locale.ROOT).replace('ё','е')
        val clauses = normalized.split(Regex("[,;\\n+](?!\\d)|\\s+и\\s+|\\s+с\\s+" )).map(String::trim).filter(String::isNotBlank)
        val items = mutableListOf<FoodItem>()
        for(clause in clauses) {
            val candidates = foods.flatMap { food -> food.aliases.flatMap { alias ->
                Regex("(?<![а-яa-z])"+Regex.escape(alias.replace('ё','е'))).findAll(clause).map { food to it.range }.toList()
            } }
            val matches = candidates.filter { (_, range) -> candidates.none { (_, other) ->
                other != range && other.first <= range.first && other.last >= range.last
            } }.map { it.first }.distinct()
            // Reject ambiguous / partly unknown clauses instead of silently dropping food.
            if(matches.size != 1) return AgentResult("Не удалось однозначно разобрать: «$clause». Разделите продукты запятыми или выберите их в справочнике.")
            val food = matches.single()
            val amount = Regex("(\\d+(?:[.,]\\d+)?)\\s*(кг|kg|г(?:рамм[а-я]*)?|g|мл|ml)\\b").find(clause)
            val pieces = Regex("(\\d+)\\s*(?:шт|штук[а-я]*)\\b").find(clause)
            val bareNumber = Regex("\\d").containsMatchIn(clause)
            val grams = when {
                amount != null -> amount.groupValues[1].replace(',','.').toDouble() * if(amount.groupValues[2] in listOf("кг","kg")) 1000 else 1
                pieces != null -> pieces.groupValues[1].toDouble()*food.defaultGrams
                bareNumber -> return AgentResult("Укажите единицы массы: например «${food.name} 150 г».")
                else -> food.defaultGrams
            }
            if(!grams.isFinite() || grams !in 1.0..3000.0) return AgentResult("Порция должна быть от 1 до 3000 г. Проверьте массу.")
            // Multiple known foods without separators must not become a single portion.
            items += food.portion(grams)
        }
        if(items.isEmpty()) return AgentResult("Напишите блюдо и массу, например «борщ 300 г, хлеб 40 г», или откройте справочник.")
        return AgentResult("Посчитал по локальному справочнику. Проверьте продукты и порции — без указанной массы используется стандартная порция.", items)
    }
    fun recommendations(profile: Profile, eaten: Nutrients): List<Recommendation> {
        val target = targets(profile)
        val remaining = listOf(target.kcal-eaten.kcal,target.protein-eaten.protein,target.fat-eaten.fat,target.carbs-eaten.carbs).map { it.coerceAtLeast(0.0) }
        val recipes = listOf(
            Triple("Обед","Курица с гречкой",listOf("Куриная грудка" to 120.0,"Гречка варёная" to 150.0,"Овощной салат" to 150.0)),
            Triple("Ужин","Творог с бананом",listOf("Творог 5%" to 200.0,"Банан" to 100.0)),
            Triple("Перекус","Йогурт с яблоком",listOf("Йогурт натуральный" to 150.0,"Яблоко" to 150.0)),
            Triple("Обед","Треска с рисом",listOf("Треска" to 150.0,"Рис варёный" to 150.0,"Брокколи" to 100.0)),
        )
        return recipes.mapNotNull { (meal,title,parts) ->
            val base = parts.map { (name,g) -> foods.first { it.name == name }.portion(g) }
            val t = totals(base)
            val nutrients = listOf(t.kcal,t.protein,t.fat,t.carbs)
            val scale = (listOf(1.0)+nutrients.indices.filter { nutrients[it]>0 }.map { remaining[it]/nutrients[it] }).min()
            if(scale < .15 || remaining[0]<100) null else {
                val items = base.map { it.scaled(it.weight_g*scale) }
                Recommendation(title,meal,"Остаток на день: ${remaining[0].roundToInt()} ккал. Порции учитывают остаток БЖУ.",items,totals(items))
            }
        }
    }
    fun csv(entries: List<Entry>): String {
        fun cell(value: String): String {
            val safe = if(value.trimStart().startsWithAnyFormula()) "'$value" else value
            return "\"${safe.replace("\"","\"\"")}\""
        }
        return "\uFEFFdate,meal,food,grams,kcal,protein,fat,carbs\n" + entries.flatMap { e -> e.items.map { f ->
            listOf(e.date,e.meal,f.name,f.weight_g.toString(),f.kcal.toString(),f.protein.toString(),f.fat.toString(),f.carbs.toString()).joinToString(",",transform=::cell)
        } }.joinToString("\n")
    }
    private fun String.startsWithAnyFormula() = firstOrNull() in listOf('=','+','-','@')
}
