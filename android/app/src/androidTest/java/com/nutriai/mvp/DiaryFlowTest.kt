package com.nutriai.mvp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
    @Test fun offlineDiarySurvivesColdRestartAndCanBeDeleted() {
        assertEquals(PackageManager.PERMISSION_DENIED,context.packageManager.checkPermission(Manifest.permission.INTERNET,context.packageName))
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
        compose.onNodeWithText("Удалить").performScrollTo().performClick()
        awaitText("Удалить запись?")
        compose.onNodeWithText("Да, удалить").performClick()
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
