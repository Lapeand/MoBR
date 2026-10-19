package com.nutriai.mvp.runtime

import androidx.activity.compose.BackHandler
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutriai.mvp.ui.components.ChatBubble
import com.nutriai.mvp.ui.components.NutriTopBar
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

@Composable
fun NutriApp(vm: NutriViewModel = viewModel()) {
    val state = vm.state
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val savedTabs = rememberSaveableStateHolder()
    var page by rememberSaveable { mutableStateOf("") }
    BackHandler(enabled = page.isNotEmpty()) { page = "" }
    LaunchedEffect(state.profile?.onboarded) { if(state.profile?.onboarded == false) { page="";tab=0;(0..2).forEach { savedTabs.removeState(it) } } }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { error ->
                Surface(color = MaterialTheme.colorScheme.errorContainer) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(error, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                        IconButton(onClick = vm::dismissError) { Icon(Icons.Default.Close, "Закрыть ошибку") }
                    }
                }
            }
            Box(Modifier.weight(1f)) {
                when {
                    state.profile == null -> Column(Modifier.padding(24.dp)) {
                        Text("Загружаем профиль…")
                        Button(onClick = vm::refresh, enabled = !state.busy) { Text("Повторить") }
                    }
                    !state.profile.onboarded -> ProfileEditor(state.profile, true, state.busy, { vm.saveProfile(it) })
                    page == "edit" -> ProfileEditor(state.profile, false, state.busy, {
                        vm.saveProfile(it) { page = "" }
                    }, { page = "" })
                    page == "settings" -> SettingsPage(vm, { page = "" })
                    else -> Scaffold(bottomBar = {
                        NavigationBar {
                            listOf("Агент", "Аналитика", "Профиль").forEachIndexed { index, label ->
                                NavigationBarItem(selected = index == tab, onClick = { tab = index }, label = { Text(label) },
                                    icon = { Icon(listOf(Icons.Default.SmartToy, Icons.Default.ShowChart, Icons.Default.Person)[index], label) })
                            }
                        }
                    }) { inner ->
                        Box(Modifier.padding(inner).consumeWindowInsets(inner)) {
                            savedTabs.SaveableStateProvider(tab) { when(tab) {
                                0 -> AgentPage(vm)
                                1 -> AnalyticsPage(vm)
                                else -> ProfilePage(vm, { page = "edit" }, { page = "settings" })
                            } }
                        }
                    }
                }
            }
        }
    }
    state.pending?.let { entry -> ConfirmEntry(entry, state.busy, vm::saveEntry, vm::cancelPending) }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit, number: Boolean = false) {
    OutlinedTextField(value, onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = if(number) KeyboardType.Decimal else KeyboardType.Text))
}

@Composable
private fun Choices(options: Map<String, String>, selected: String, onSelect: (String) -> Unit) {
    options.forEach { (key, label) ->
        FilterChip(selected == key, { onSelect(key) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ProfileEditor(profile: Profile, onboarding: Boolean, busy: Boolean, save: (Profile) -> Unit, back: () -> Unit = {}) {
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name) }
    var weight by rememberSaveable(profile.weight) { mutableStateOf(profile.weight.toString()) }
    var height by rememberSaveable(profile.height) { mutableStateOf(profile.height.toString()) }
    var age by rememberSaveable(profile.age) { mutableStateOf(profile.age.toString()) }
    var sex by rememberSaveable(profile.sex) { mutableStateOf(profile.sex) }
    var goal by rememberSaveable(profile.goal) { mutableStateOf(profile.goal) }
    var activity by rememberSaveable(profile.activity) { mutableStateOf(profile.activity) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    val w = weight.replace(',', '.').toDoubleOrNull()
    val h = height.replace(',', '.').toDoubleOrNull()
    val a = age.toIntOrNull()
    val valid = name.isNotBlank() && w != null && w in 30.0..300.0 && h != null && h in 120.0..230.0 && a != null && a in 18..100
    Scaffold(topBar = { if(!onboarding) NutriTopBar("Изменить профиль", onBack = back) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if(onboarding) {
                LinearProgressIndicator(progress = { (step+1)/3f }, modifier = Modifier.fillMaxWidth())
                Text("Шаг ${step+1} из 3", style = MaterialTheme.typography.labelLarge)
            }
            if(!onboarding || step == 0) {
                Text("Ваша цель", style = MaterialTheme.typography.headlineSmall)
                Choices(goals, goal, { goal = it })
            }
            if(!onboarding || step == 1) {
                Text("О вас", style = MaterialTheme.typography.headlineSmall)
                Field("Имя", name, { name = it })
                Field("Вес, кг · 30–300", weight, { weight = it }, true)
                Field("Рост, см · 120–230", height, { height = it }, true)
                Field("Возраст · 18–100", age, { age = it }, true)
                Choices(mapOf("male" to "Мужской", "female" to "Женский"), sex, { sex = it })
                if(!valid) Text("Проверьте имя, вес, рост и возраст.", color = MaterialTheme.colorScheme.error)
            }
            if(!onboarding || step == 2) {
                Text("Активность", style = MaterialTheme.typography.headlineSmall)
                Choices(activities, activity, { activity = it })
                Text("Норма рассчитывается по формуле Миффлина — Сан Жеора. Ориентир для дневника, а не медицинское назначение.", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = {
                if(onboarding && step < 2) step++ else if(valid) save(profile.copy(name = name.trim(), weight = w!!, height = h!!, age = a!!,
                    sex = sex, goal = goal, activity = activity, onboarded = true))
            }, enabled = !busy && (if(onboarding && step == 0) true else valid), modifier = Modifier.fillMaxWidth()) {
                Text(if(onboarding && step < 2) "Далее" else "Сохранить")
            }
            if(onboarding && step > 0) TextButton(onClick = { step-- }, enabled = !busy) { Text("Назад") }
        }
    }
}

@Composable
private fun DateControl(vm: NutriViewModel) {
    val state = vm.state
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        TextButton({ vm.selectDate(state.date.minusDays(1)) }, enabled = !state.busy) { Text("‹ День") }
        Text(state.date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
        TextButton({ vm.selectDate(state.date.plusDays(1)) }, enabled = !state.busy && state.date < LocalDate.now()) { Text("День ›") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgentPage(vm: NutriViewModel) {
    val state = vm.state
    val context = LocalContext.current
    var text by rememberSaveable { mutableStateOf("") }
    var photo by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    var mediaMenu by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(false) }
    var catalog by remember { mutableStateOf(false) }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { photo = it?.toString() }
    val capture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if(ok) photo = cameraUri }
    val speech = rememberOfflineSpeech({ text=it },vm::reportError)
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) { if(state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.size + 1) }
    var sending by remember { mutableStateOf(false) }
    LaunchedEffect(state.busy) {
        if(sending && !state.busy) { if(state.error == null) { text = ""; photo = null }; sending = false }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("NutriAI") }, actions = {
        IconButton({ manual = true }, enabled = !state.busy) { Icon(Icons.Default.Add, "Добавить вручную") }
        IconButton({ catalog = true }, enabled = !state.busy) { Icon(Icons.Default.MenuBook, "Справочник продуктов") }
    }) }, bottomBar = {
        Column(Modifier.imePadding()) {
            photo?.let { uri ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LocalPhoto(uri, "Фото блюда", Modifier.size(72.dp).padding(8.dp))
                    TextButton({ photo = null }, enabled = !state.busy) { Text("Убрать фото") }
                }
            }
            Row(Modifier.fillMaxWidth().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    IconButton({ mediaMenu = true }, enabled = !state.busy) { Icon(Icons.Default.CameraAlt, "Фото") }
                    DropdownMenu(mediaMenu, { mediaMenu = false }) {
                        DropdownMenuItem(text = { Text("Из галереи") }, onClick = { mediaMenu = false; pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) })
                        DropdownMenuItem(text = { Text("Сделать фото") }, onClick = {
                            mediaMenu = false
                            val dir = File(context.cacheDir, "photos").apply { mkdirs() }
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", File.createTempFile("meal", ".jpg", dir))
                            cameraUri = uri.toString()
                            try { capture.launch(uri) } catch(e: Exception) { vm.reportError("На устройстве нет приложения камеры") }
                        })
                    }
                }
                OutlinedTextField(text, { text = it }, placeholder = { Text("Что вы ели?") }, modifier = Modifier.weight(1f), maxLines = 3, enabled = !state.busy)
                IconButton(speech, enabled = !state.busy) { Icon(Icons.Default.Mic, "Голос") }
                IconButton({ sending = true; vm.send(text, photo?.let(Uri::parse)) }, enabled = !state.busy && (text.isNotBlank() || photo != null)) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Отправить")
                }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp), state = listState, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { DateControl(vm) }
            item { Text("Здравствуйте, ${state.profile?.name}", style = MaterialTheme.typography.titleMedium) }
            item { Text(if(state.aiEnabled) "ИИ через интернет · дневник на телефоне" else "Локальный помощник · без интернета", style = MaterialTheme.typography.labelLarge) }
            item { ChatBubble(if(state.aiEnabled) "Расскажите, что съели, или спросите, что приготовить. ИИ разберёт описание, приложение рассчитает КБЖУ. Текст чата и суточный баланс отправляются в OpenRouter; фото остаются на телефоне. Запись сохраняется только после подтверждения."
                else "Опишите еду: «борщ 300 г, хлеб 40 г» или выберите продукт в справочнике. Фото — только вложение, блюдо нужно указать текстом. Расчёт работает локально. ИИ через интернет можно включить в настройках.", false) }
            if(state.busy) item { Text(if(state.aiEnabled) "ИИ обрабатывает сообщение…" else "Обрабатываю…",style=MaterialTheme.typography.bodySmall) }
            items(state.messages) { message ->
                Column { message.photoPath?.let { LocalPhoto(it, "Фото блюда в чате", Modifier.fillMaxWidth().height(160.dp)) };ChatBubble(message.text,message.is_user) }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
    if(manual) ManualEntry(state.date, { vm.propose(it); manual = false }, { manual = false })
    if(catalog) FoodCatalog({ food -> vm.propose(Entry(date=state.date.toString(),items=listOf(food.portion(food.defaultGrams))));catalog=false }, { catalog=false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmEntry(entry: Entry, busy: Boolean, save: (Entry) -> Unit, cancel: () -> Unit) {
    var items by remember(entry.request_id) { mutableStateOf(entry.items) }
    var meal by remember(entry.request_id) { mutableStateOf(entry.meal) }
    ModalBottomSheet(onDismissRequest = { if(!busy) cancel() }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Проверьте запись", style = MaterialTheme.typography.headlineSmall)
            Text(entry.date)
            entry.photoPath?.let { LocalPhoto(it, "Фото блюда", Modifier.fillMaxWidth().height(150.dp)) }
            Field("Приём пищи", meal, { meal = it })
            items.forEachIndexed { index, food ->
                Text(food.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ items = items.mapIndexed { i, item -> if(i == index) item.scaled((item.weight_g - 25).coerceAtLeast(5.0)) else item } }, enabled = !busy) { Icon(Icons.Default.Remove, "Уменьшить порцию") }
                    Text("${food.weight_g.roundToInt()} г", Modifier.weight(1f))
                    IconButton({ items = items.mapIndexed { i, item -> if(i == index) item.scaled((item.weight_g + 25).coerceAtMost(3000.0)) else item } }, enabled = !busy) { Icon(Icons.Default.Add, "Увеличить порцию") }
                }
                Text(food.label())
                Text("Источник: ${food.source}", style = MaterialTheme.typography.bodySmall)
            }
            Text("КБЖУ — приблизительная оценка; состав и способ приготовления влияют на результат.", style = MaterialTheme.typography.bodySmall)
            Button({ save(entry.copy(meal = meal, items = items)) }, enabled = !busy && meal.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Сохранить в дневник") }
            TextButton(cancel, enabled = !busy) { Text("Отмена") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualEntry(date: LocalDate, done: (Entry) -> Unit, cancel: () -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var grams by rememberSaveable { mutableStateOf("100") }
    var kcal by rememberSaveable { mutableStateOf("") }
    var protein by rememberSaveable { mutableStateOf("") }
    var fat by rememberSaveable { mutableStateOf("") }
    var carbs by rememberSaveable { mutableStateOf("") }
    val values = listOf(grams, kcal, protein, fat, carbs).map { it.replace(',', '.').toDoubleOrNull() }
    val valid = name.isNotBlank() && values.all { it != null && it.isFinite() && it >= 0 } && (values[0] ?: 0.0) in 1.0..3000.0 && values.drop(1).all { it!! <= 3000 }
    ModalBottomSheet(onDismissRequest = cancel) {
        Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState()).imePadding(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ручная запись", style = MaterialTheme.typography.headlineSmall)
            Text("Введите КБЖУ на всю порцию, не на 100 г.")
            Field("Блюдо", name, { name = it })
            Field("Порция, г", grams, { grams = it }, true)
            Field("Ккал на порцию", kcal, { kcal = it }, true)
            Field("Белки, г", protein, { protein = it }, true)
            Field("Жиры, г", fat, { fat = it }, true)
            Field("Углеводы, г", carbs, { carbs = it }, true)
            Button({ done(Entry(date = date.toString(), items = listOf(FoodItem(name.trim(), values[0]!!, values[1]!!, values[2]!!, values[3]!!, values[4]!!)))) }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Проверить запись") }
        }
    }
}

@Composable
private fun AnalyticsPage(vm: NutriViewModel) {
    val state = vm.state
    val total = Nutrients(state.entries.sumOf { it.totals.kcal }, state.entries.sumOf { it.totals.protein }, state.entries.sumOf { it.totals.fat }, state.entries.sumOf { it.totals.carbs })
    val target = state.profile?.targets?.kcal ?: 1.0
    val color = MaterialTheme.colorScheme.primary
    var delete by remember { mutableStateOf<Entry?>(null) }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Аналитика", style = MaterialTheme.typography.headlineSmall); DateControl(vm) }
        item { Card { Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("${total.kcal.roundToInt()} / ${target.roundToInt()} ккал", style = MaterialTheme.typography.headlineSmall)
            LinearProgressIndicator(progress = { (total.kcal / target.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
            Text(total.label()); Text("Норма: ${state.profile?.targets?.label()}", style = MaterialTheme.typography.bodySmall)
        } } }
        item { Text("Последние 7 дней", style = MaterialTheme.typography.titleMedium) }
        item {
            Canvas(Modifier.fillMaxWidth().height(140.dp)) {
                val max = state.weekly.maxOfOrNull { it.totals.kcal }?.coerceAtLeast(1.0) ?: 1.0
                val width = size.width / 14
                state.weekly.forEachIndexed { i, day ->
                    val height = (day.totals.kcal / max * size.height).toFloat()
                    drawRect(color, Offset(i * width * 2 + width / 2, size.height-height), Size(width, height))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                state.weekly.forEach { Text(LocalDate.parse(it.date).format(DateTimeFormatter.ofPattern("dd.MM")), style = MaterialTheme.typography.labelSmall) }
            }
        }
        item { Text("Баланс БЖУ по калориям", style = MaterialTheme.typography.titleMedium)
            val energies = listOf(total.protein*4, total.fat*9, total.carbs*4)
            val sum = energies.sum().coerceAtLeast(1.0)
            listOf("Белки", "Жиры", "Углеводы").forEachIndexed { i, label ->
                Text("$label · ${(energies[i]/sum*100).roundToInt()}%")
                LinearProgressIndicator(progress = { (energies[i]/sum).toFloat() }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
            }
        }
        item { Text("Дневник", style = MaterialTheme.typography.titleMedium) }
        if(state.entries.isEmpty()) item { Text("За этот день записей пока нет. Добавьте еду во вкладке «Агент».") }
        items(state.entries, key = { it.id }) { entry -> Card { Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(entry.meal, style = MaterialTheme.typography.titleMedium)
            entry.photoPath?.let { LocalPhoto(it, "Фото записи", Modifier.fillMaxWidth().height(140.dp)) }
            entry.items.forEach { Text("${it.name} · ${it.weight_g.roundToInt()} г") }
            Text(entry.totals.label())
            TextButton({ delete = entry }, enabled = !state.busy) { Text("Удалить") }
        } } }
        item { OutlinedButton(vm::refresh, enabled = !state.busy) { Text("Обновить") } }
    }
    delete?.let { entry -> AlertDialog(onDismissRequest = { delete = null }, title = { Text("Удалить запись?") },
        text = { Text(entry.items.joinToString { it.name }) }, confirmButton = { TextButton({ vm.deleteEntry(entry.id); delete = null }) { Text("Да, удалить") } },
        dismissButton = { TextButton({ delete = null }) { Text("Отмена") } }) }
}

@Composable
private fun ProfilePage(vm: NutriViewModel, edit: () -> Unit, settings: () -> Unit) {
    val state = vm.state
    val profile = state.profile ?: return
    var detail by remember { mutableStateOf<Recommendation?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Профиль", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
            IconButton(settings) { Icon(Icons.Default.Settings, "Настройки") }
        }
        Text(profile.name, style = MaterialTheme.typography.titleLarge)
        Text("${profile.weight} кг · ${profile.height} см · ${profile.age} лет")
        Text(goals[profile.goal] ?: "")
        Text(activities[profile.activity] ?: "")
        Text("Дневная норма: ${profile.targets.label()}")
        OutlinedButton(edit, enabled = !state.busy) { Text("Изменить профиль") }
        Text("Рекомендации на ${state.date.format(DateTimeFormatter.ofPattern("dd.MM"))}", style = MaterialTheme.typography.titleMedium)
        if(state.recommendations.isEmpty()) Text("Нет подходящего варианта для текущего остатка калорий.")
        state.recommendations.forEach { rec -> Card { Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(rec.meal, style = MaterialTheme.typography.labelLarge)
            Text(rec.title, style = MaterialTheme.typography.titleMedium)
            Text(rec.description); Text(rec.totals.label())
            TextButton({ detail = rec }) { Text("Подробнее") }
        } } }
    }
    detail?.let { rec -> AlertDialog(onDismissRequest = { detail = null }, title = { Text(rec.title) }, text = {
        Column { rec.items.forEach { Text("${it.name} · ${it.weight_g.roundToInt()} г") }; Text(rec.totals.label()) }
    }, confirmButton = { TextButton({ vm.propose(Entry(date = state.date.toString(), meal = rec.meal, items = rec.items)); detail = null }, enabled = !state.busy) { Text("Добавить в дневник") } },
        dismissButton = { TextButton({ detail = null }) { Text("Закрыть") } }) }
}

@Composable
private fun SettingsPage(vm: NutriViewModel, back: () -> Unit) {
    var info by remember { mutableStateOf(false) }
    var erase by remember { mutableStateOf(false) }
    var keyDialog by remember { mutableStateOf(false) }
    var key by remember { mutableStateOf("") }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let(vm::export) }
    Scaffold(topBar = { NutriTopBar("Настройки", onBack = back) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("NutriAI 2.1 · ИИ и локальный дневник",style=MaterialTheme.typography.titleMedium)
            Text("Дневник и фотографии хранятся на телефоне. ИИ подключается к OpenRouter напрямую; собственный сервер не нужен.")
            Row(verticalAlignment=Alignment.CenterVertically) {
                Text("ИИ через интернет",Modifier.weight(1f))
                Switch(checked=vm.state.aiEnabled,onCheckedChange=vm::enableAi,enabled=!vm.state.busy)
            }
            Text("Nemotron 3 Super · free",style=MaterialTheme.typography.labelLarge)
            Text(if(vm.state.aiConfigured) "Ключ настроен" else "Для ИИ добавьте ключ OpenRouter",style=MaterialTheme.typography.bodySmall)
            Text("В OpenRouter отправляются текст запроса, последние 6 сообщений и суточные суммы КБЖУ. Фото, имя и параметры тела из профиля не отправляются. Бесплатная модель может быть перегружена; при ошибке включается локальный разбор еды.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton({ key="";keyDialog=true },enabled=!vm.state.busy) { Text("Заменить ключ OpenRouter") }
            TextButton({ info = true }) { Text("Как работает приложение") }
            OutlinedButton({ export.launch("nutriai-${vm.state.date}.csv") }, enabled = !vm.state.busy) { Text("Экспорт последних 7 дней в CSV") }
            Text("Сохраняйте CSV перед удалением приложения: данные не синхронизируются с облаком.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton({ erase=true },enabled=!vm.state.busy) { Text("Стереть все данные") }
        }
    }
    if(keyDialog) AlertDialog(onDismissRequest={ key="";keyDialog=false },title={ Text("Ключ OpenRouter") },text={
        OutlinedTextField(key,{ key=it },label={ Text("Новый API-ключ") },visualTransformation=PasswordVisualTransformation(),singleLine=true)
    },confirmButton={ TextButton({ vm.replaceAiKey(key);key="";keyDialog=false },enabled=key.trim().matches(Regex("sk-or-[A-Za-z0-9_-]{20,}"))) { Text("Сохранить ключ") } },dismissButton={ TextButton({ key="";keyDialog=false }) { Text("Отмена") } })
    if(info) AlertDialog(onDismissRequest = { info = false }, title = { Text("Как работает NutriAI") }, text = {
        Text("ИИ разбирает описание еды и подбирает порции. Для знакомых продуктов КБЖУ берутся из встроенного справочника; для новых блюд используются явно отмеченные приблизительные оценки ИИ. Арифметика выполняется в приложении. Всегда проверяйте порции. Без интернета доступны дневник, справочник, ручной ввод, аналитика, CSV и локальные рекомендации. Фото — вложения, без распознавания. Голос требует локального распознавателя и русского языкового пакета Android.")
    }, confirmButton = { TextButton({ info = false }) { Text("Понятно") } })
    if(erase) AlertDialog(onDismissRequest={ erase=false },title={ Text("Стереть все данные?") },text={ Text("Будут удалены профиль, записи, история чата и фотографии. Сначала сохраните CSV, если хотите оставить дневник.") },
        confirmButton={ TextButton({ erase=false;vm.erase() }) { Text("Стереть") } },dismissButton={ TextButton({ erase=false }) { Text("Отмена") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodCatalog(select: (LocalFood) -> Unit, close: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val found=remember(query) { Nutrition.search(query) }
    ModalBottomSheet(onDismissRequest=close) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.85f).padding(16.dp)) {
            Text("Справочник продуктов",style=MaterialTheme.typography.headlineSmall)
            Text("${Nutrition.foods.size} продуктов и блюд · КБЖУ на 100 г",style=MaterialTheme.typography.bodySmall)
            Field("Поиск продукта",query,{ query=it })
            LazyColumn(Modifier.weight(1f)) {
                if(found.isEmpty()) item { Text("Не нашли продукт? Закройте справочник и добавьте блюдо вручную кнопкой +.",Modifier.padding(16.dp)) }
                items(found,key={ it.name }) { food ->
                    TextButton({ select(food) },modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) { Text(food.name);Text(food.nutrients.label(),style=MaterialTheme.typography.bodySmall) }
                    }
                }
            }
            TextButton(close) { Text("Закрыть") }
        }
    }
}
