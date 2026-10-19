package com.nutriai.mvp

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nutriai.mvp.runtime.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.time.LocalDate

/** Full local workflow. Run with emulator Wi-Fi/mobile data disabled; no API process is used. */
@RunWith(AndroidJUnit4::class)
class DiaryFlowTest {
    @get:Rule val compose=createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val context get()=InstrumentationRegistry.getInstrumentation().targetContext
    @Before fun freshLocalApp() {
        context.deleteDatabase("nutriai_local.db")
        context.getSharedPreferences("nutriai_secure",Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("nutriai_config",Context.MODE_PRIVATE).edit().clear().commit()
        context.getSharedPreferences("nutriai_ai",Context.MODE_PRIVATE).edit().clear().putBoolean("enabled",false).commit()
        scenario=ActivityScenario.launch(MainActivity::class.java)
    }
    @After fun close() { scenario.close() }
    private fun awaitText(value: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty() }
    }
    private fun setupProfile() {
        awaitText("Ваша цель")
        compose.waitUntil(15_000) { compose.onAllNodes(hasText("Далее") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Далее").performClick()
        awaitText("О вас")
        compose.onNodeWithText("Далее").performScrollTo().performClick()
        awaitText("Активность")
        compose.onNodeWithText("Сохранить").performScrollTo().performClick()
        awaitText("Что вы ели?")
        compose.waitUntil(15_000) { compose.onAllNodes(hasText("Что вы ели?") and isEnabled()).fetchSemanticsNodes().isNotEmpty() }
    }
    /** Opt-in only: -Pandroid.testInstrumentationRunnerArguments.liveOpenRouter=true */
    @Test fun liveAiParsesNaturalLanguageThenAndroidCalculatesAndStores() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveOpenRouter")=="true")
        setupProfile()
        lateinit var vm: NutriViewModel
        scenario.onActivity { activity ->
            vm=androidx.lifecycle.ViewModelProvider(activity)[NutriViewModel::class.java]
            vm.enableAi(true)
        }
        compose.waitUntil(15_000) { !vm.state.busy && vm.state.aiEnabled }
        compose.onNodeWithText("Что вы ели?").performTextInput("На обед съел тарелку борща 300 граммов и хлеб 40 граммов.")
        compose.onNodeWithContentDescription("Отправить").performClick()
        compose.waitUntil(100_000) { !vm.state.busy }
        assertNull(vm.state.error)
        assertTrue(vm.state.messages.last().text,vm.state.messages.last().text.startsWith("ИИ · Nemotron"))
        assertEquals(247.0,Nutrition.totals(vm.state.pending!!.items).kcal,.001)
        compose.onNodeWithText("Сохранить в дневник").performScrollTo().performClick()
        compose.waitUntil(15_000) { !vm.state.busy && vm.state.pending==null }
        assertEquals(247.0,Nutrition.totals(vm.state.entries.flatMap { it.items }).kcal,.001)
        scenario.onActivity { vm.send("Сколько калорий осталось на сегодня? Не предлагай продукты, только ответь.",null) }
        compose.waitUntil(100_000) { !vm.state.busy }
        assertTrue(vm.state.messages.last().text,vm.state.messages.last().text.startsWith("ИИ · Nemotron"))
        assertNull(vm.state.pending)
        assertEquals(1,vm.state.entries.size)
    }
    @Test fun unavailableAiFallsBackToLocalCalculatorWithoutLosingTheMeal() {
        Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("liveOpenRouter")!="true")
        setupProfile()
        lateinit var vm: NutriViewModel
        scenario.onActivity { activity ->
            vm=androidx.lifecycle.ViewModelProvider(activity)[NutriViewModel::class.java]
            vm.enableAi(true)
        }
        compose.waitUntil(15_000) { !vm.state.busy && vm.state.aiEnabled }
        scenario.onActivity { vm.send("борщ 300 г, хлеб 40 г",null) }
        compose.waitUntil(100_000) { !vm.state.busy }
        assertTrue(vm.state.messages.last().text,vm.state.messages.last().text.contains("Локальный помощник:"))
        assertEquals(247.0,Nutrition.totals(vm.state.pending!!.items).kcal,.001)
        scenario.onActivity { vm.saveEntry(vm.state.pending!!) }
        compose.waitUntil(15_000) { !vm.state.busy && vm.state.pending==null }
        assertEquals(247.0,vm.state.entries.single().totals.kcal,.001)
    }
    @Test fun replacementKeyIsEncryptedAndModeSurvivesReopening() {
        val testKey="sk-or-v1-"+"test".repeat(12)
        AiSettings(context).replaceKey(testKey)
        val reopened=AiSettings(context)
        assertEquals(testKey,reopened.apiKey())
        assertTrue(reopened.enabled())
        assertFalse(context.getSharedPreferences("nutriai_ai",Context.MODE_PRIVATE).getString("key","")!!.contains(testKey))
        reopened.enable(false)
        assertFalse(AiSettings(context).enabled())
    }
    private fun chartScreenshot(name: String) {
        if(InstrumentationRegistry.getArguments().getString("captureCharts")!="true") return
        compose.waitForIdle()
        val descriptor=InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("screencap -p /data/local/tmp/nutriai-$name.png")
        android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
    }
    @Test fun chartsReflectDiaryAndShowMissingDaysWithoutInventedValues() {
        setupProfile()
        val today=LocalDate.now()
        Repository(context).use { db ->
            db.saveEntry(Entry(date=today.toString(),meal="Обед",items=listOf(Nutrition.foods.first { it.name=="Борщ" }.portion(300.0))))
            db.saveEntry(Entry(date=today.toString(),meal="Перекус",items=listOf(Nutrition.foods.first { it.name=="Хлеб" }.portion(40.0))))
            db.saveEntry(Entry(date=today.minusDays(1).toString(),meal="Завтрак",items=listOf(Nutrition.foods.first { it.name=="Банан" }.portion(100.0))))
            (2L..6L).forEach { offset ->
                db.saveEntry(Entry(date=today.minusDays(offset).toString(),meal="Обед",items=listOf(Nutrition.foods.first { it.name=="Плов" }.portion(300.0+offset*80))))
            }
        }
        lateinit var vm: NutriViewModel
        scenario.onActivity { activity ->
            vm=androidx.lifecycle.ViewModelProvider(activity)[NutriViewModel::class.java]
            vm.refresh()
        }
        compose.waitUntil(15_000) { !vm.state.busy && vm.state.entries.size==2 }
        compose.onNode(hasText("Аналитика") and hasClickAction()).performClick()
        awaitText("247 / 2370 ккал")
        chartScreenshot("charts-day")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Распределение БЖУ"))
        compose.onNodeWithContentDescription("Круговая диаграмма БЖУ: ${Nutrition.totals(vm.state.entries.flatMap { it.items }).label()}").assertExists()
        chartScreenshot("charts-donut")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("По приёмам пищи"))
        compose.onAllNodesWithText("147 ккал · 60%").onFirst().assertExists()
        compose.onAllNodesWithText("100 ккал · 40%").onFirst().assertExists()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithText("За неделю").performClick()
        awaitText("Итоги за 7 дней")
        compose.onNodeWithText("Дней с записями: 7 из 7").assertExists()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Калории по дням"))
        val yesterday=today.minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("dd.MM"))
        compose.onNodeWithContentDescription("Калории $yesterday: 89 ккал").performClick()
        compose.onNodeWithText("$yesterday · 89 ккал").assertExists()
        chartScreenshot("charts-week")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("БЖУ по дням"))
        val todayLabel=today.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM"))
        compose.onNodeWithContentDescription("БЖУ $todayLabel: 252 ккал из БЖУ").assertExists()
        chartScreenshot("charts-week-macros")
        scenario.onActivity { vm.selectDate(today.minusDays(10)) }
        compose.waitUntil(15_000) { !vm.state.busy && vm.state.date==today.minusDays(10) }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        awaitText("Дней с записями: 0 из 7")
        compose.onNodeWithText("Среднее появится после первой записи").assertExists()
        compose.onNodeWithText("За день").performClick()
        awaitText("0 / 2370 ккал")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Распределение БЖУ"))
        compose.onNodeWithContentDescription("Нет данных для диаграммы БЖУ").assertExists()
    }
    @Test fun offlineDiarySurvivesColdRestartAndCanBeDeleted() {
        assertFalse(AiSettings(context).enabled())
        setupProfile()
        compose.onNodeWithText("Что вы ели?").performTextInput("борщ 300 г, хлеб 40 г")
        compose.onNodeWithContentDescription("Отправить").performClick()
        awaitText("Проверьте запись")
        compose.onNodeWithText("Сохранить в дневник").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Проверьте запись").fetchSemanticsNodes().isEmpty() }
        compose.onNode(hasText("Аналитика") and hasClickAction()).performClick()
        awaitText("247 / 2370 ккал")
        // Close Activity and its ViewModel, reopen SQLite from a new repository instance.
        scenario.close()
        Repository(context).use { db ->
            db.initialize()
            assertTrue(db.profile().onboarded)
            val entries=db.entries(LocalDate.now(),LocalDate.now())
            assertEquals(1,entries.size)
            assertEquals(247.0,entries.single().totals.kcal,.001)
        }
        scenario=ActivityScenario.launch(MainActivity::class.java)
        awaitText("Что вы ели?")
        compose.onNode(hasText("Аналитика") and hasClickAction()).performClick()
        awaitText("247 / 2370 ккал")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Удалить"))
        compose.onNodeWithText("Удалить").performClick()
        awaitText("Удалить запись?")
        compose.onNodeWithText("Да, удалить").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        awaitText("0 / 2370 ккал")
    }
    @Test fun localCatalogDraftIsRestoredAndWritesAreIdempotent() {
        setupProfile()
        compose.onNodeWithContentDescription("Справочник продуктов").performClick()
        compose.onNodeWithText("Поиск продукта").performTextInput("греч")
        compose.onNodeWithText("Гречка варёная").performClick()
        awaitText("Проверьте запись")
        scenario.recreate()
        awaitText("Проверьте запись")
        compose.onNodeWithText("Сохранить в дневник").performScrollTo().performClick()
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Проверьте запись").fetchSemanticsNodes().isEmpty() }
        scenario.close()
        Repository(context).use { db ->
            db.initialize()
            val entry=db.entries(LocalDate.now(),LocalDate.now()).single()
            db.saveEntry(entry)
            assertEquals(1,db.entries(LocalDate.now(),LocalDate.now()).size)
            assertTrue(Nutrition.csv(listOf(entry)).contains("Гречка варёная"))
            db.erase()
            assertTrue(db.entries(LocalDate.now(),LocalDate.now()).isEmpty())
            assertFalse(db.profile().onboarded)
        }
    }
    @Test fun photoAndCsvAreStoredOnlyOnTheDevice() {
        setupProfile()
        val source=java.io.File(context.cacheDir,"offline-food.png")
        android.graphics.Bitmap.createBitmap(64,32,android.graphics.Bitmap.Config.ARGB_8888).let { image ->
            source.outputStream().use { image.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it) };image.recycle()
        }
        lateinit var vm: NutriViewModel
        scenario.onActivity { activity ->
            vm=androidx.lifecycle.ViewModelProvider(activity)[NutriViewModel::class.java]
            vm.send("банан 100 г",android.net.Uri.fromFile(source))
        }
        awaitText("Проверьте запись")
        compose.waitUntil(15_000) { !vm.state.busy }
        val path=vm.state.pending!!.photoPath!!
        assertTrue(java.io.File(path).isFile)
        assertTrue(path.startsWith(context.filesDir.absolutePath))
        source.delete()
        compose.onNodeWithText("Сохранить в дневник").performScrollTo().performClick()
        compose.waitUntil(15_000) { !vm.state.busy && vm.state.pending==null }
        val csv=java.io.File(context.cacheDir,"offline-export.csv")
        scenario.onActivity { vm.export(android.net.Uri.fromFile(csv)) }
        compose.waitUntil(15_000) { !vm.state.busy && csv.exists() }
        assertTrue(csv.readText().contains("Банан"))
        scenario.close()
        Repository(context).use { db ->
            db.initialize()
            val entry=db.entries(LocalDate.now(),LocalDate.now()).single()
            assertEquals(path,entry.photoPath)
            assertTrue(java.io.File(entry.photoPath!!).exists())
            assertEquals(89.0,entry.totals.kcal,.001)
        }
    }
}
