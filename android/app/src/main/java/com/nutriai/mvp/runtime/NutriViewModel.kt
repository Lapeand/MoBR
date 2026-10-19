package com.nutriai.mvp.runtime

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import java.time.LocalDate

class NutriViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)
    var state by mutableStateOf(AppState(signedIn = repository.hasSession))
        private set
    var baseUrl by mutableStateOf(repository.baseUrl)
        private set
    fun changeServer(value: String) {
        if(state.busy || state.signedIn) return
        try {
            repository.changeServer(value)
            baseUrl = repository.baseUrl
            state = AppState()
        } catch(e: IllegalArgumentException) { reportError(e.message ?: "Неверный адрес API") }
    }
    init {
        repository.cached()?.takeIf { repository.hasSession }?.let {
            state = state.copy(profile = it.profile, entries = it.entries, messages = it.messages,
                weekly = it.weekly, recommendations = it.recommendations, date = LocalDate.parse(it.date), offline = true)
        }
        if (repository.hasSession) refresh()
    }
    fun dismissError() { state = state.copy(error = null) }
    fun reportError(message: String) { state = state.copy(error = message) }
    private fun run(action: suspend () -> Unit) {
        if (state.busy) return
        state = state.copy(busy = true, error = null)
        viewModelScope.launch {
            try { action() }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                state = if (!repository.hasSession) AppState(error = errorMessage(e)) else state.copy(error = errorMessage(e), offline = e is java.io.IOException)
            } finally { state = state.copy(busy = false) }
        }
    }
    private suspend fun load() {
        val api = repository.api
        val profile = repository.authorized { api.profile() }
        // Keep the authenticated profile if a later request fails.
        state = state.copy(signedIn = true, profile = profile)
        val day = state.date.toString()
        val entries = repository.authorized { api.diary(day, day) }
        val messages = repository.authorized { api.chat() }
        val weekly = repository.authorized { api.weekly(day) }
        val recommendations = repository.authorized { api.recommendations(day) }
        state = state.copy(entries = entries, messages = messages, weekly = weekly, recommendations = recommendations, offline = false)
        repository.cache(Snapshot(profile, entries, messages, weekly, recommendations, day))
    }
    fun refresh() = run { load() }
    fun authenticate(register: Boolean, email: String, password: String, name: String) = run {
        repository.authenticate(register, Credentials(email.trim(), password, name.trim().ifEmpty { "Пользователь" }))
        state = AppState(signedIn = true, busy = true)
        load()
    }
    fun saveProfile(profile: Profile) = run {
        val saved = repository.authorized { repository.api.profile(profile) }
        state = state.copy(profile = saved)
        load()
    }
    fun selectDate(date: LocalDate) {
        if (state.busy || date == state.date) return
        state = state.copy(date = date, entries = emptyList(), weekly = emptyList(), recommendations = emptyList(), pending = null)
        refresh()
    }
    fun send(text: String, uri: Uri?) = run {
        val bytes = uri?.let { withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openInputStream(it)?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (output.size() <= 8 * 1024 * 1024) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    output.write(buffer, 0, count)
                }
                val data = output.toByteArray()
                require(data.size <= 8 * 1024 * 1024) { "Фото больше 8 МБ" }
                data
            } ?: error("Не удалось прочитать фото")
        } }
        val imageId = bytes?.let { repository.upload(it) }
        val result = repository.authorized { repository.api.agent(AgentRequest(text, imageId, state.date.toString())) }
        state = state.copy(messages = state.messages + Message(true, text.ifBlank { "[Фото блюда]" }) + Message(false, result.reply),
            pending = result.items.takeIf { it.isNotEmpty() }?.let { Entry(date = state.date.toString(), items = it) })
        load()
    }
    fun propose(entry: Entry) { if (!state.busy) state = state.copy(pending = entry) }
    fun cancelPending() { if (!state.busy) state = state.copy(pending = null) }
    fun saveEntry(entry: Entry) = run {
        repository.authorized { repository.api.save(entry) }
        state = state.copy(pending = null)
        load()
    }
    fun deleteEntry(id: String) = run {
        repository.authorized { repository.api.delete(id) }
        load()
    }
    fun barcode(code: String) = run {
        val food = repository.authorized { repository.api.barcode(code) }
        state = state.copy(pending = Entry(date = state.date.toString(), items = listOf(food)))
    }
    fun export(uri: Uri) = run {
        val csv = repository.authorized { repository.api.export(state.date.minusDays(6).toString(), state.date.toString()).bytes() }
        withContext(Dispatchers.IO) {
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(csv) } ?: error("Не удалось создать файл")
        }
    }
    fun demoPlan(premium: Boolean) = run {
        repository.authorized { repository.api.demoPlan(mapOf("premium" to premium)) }; load()
    }
    fun logout() = run {
        try { repository.logout() } finally {
            withContext(Dispatchers.IO) { java.io.File(getApplication<Application>().cacheDir, "photos").deleteRecursively() }
            state = AppState()
        }
    }
}
