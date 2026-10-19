package com.nutriai.mvp.runtime

import android.app.Application
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
import java.io.File
import java.util.UUID

class NutriViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)
    private val aiSettings = AiSettings(application)
    var state by mutableStateOf(AppState(busy=true))
        private set
    init {
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { repository.initialize() }; load() }
            catch(e: Exception) { state=state.copy(error="Не удалось открыть локальную базу. ${e.message.orEmpty()}") }
            finally { state=state.copy(busy=false) }
        }
    }
    fun dismissError() { state=state.copy(error=null) }
    fun reportError(message: String) { state=state.copy(error=message) }
    private fun run(action: suspend () -> Unit) {
        if(state.busy) return
        state=state.copy(busy=true,error=null)
        viewModelScope.launch {
            try { action() }
            catch(e: CancellationException) { throw e }
            catch(e: Exception) { state=state.copy(error=e.message ?: "Не удалось сохранить данные на телефоне. Проверьте свободное место.") }
            finally { state=state.copy(busy=false) }
        }
    }
    private suspend fun load() {
        val day=state.date
        val data=withContext(Dispatchers.IO) {
            val profile=repository.profile()
            val week=repository.entries(day.minusDays(6),day)
            val entries=week.filter { it.date==day.toString() }
            val weekly=(0..6).map { n ->
                val d=day.minusDays(6L-n)
                val records=week.filter { it.date==d.toString() }
                DayStats(d.toString(),Nutrition.totals(records.flatMap { it.items }),records.size)
            }
            Snapshot(profile,entries,repository.messages(day),weekly,Nutrition.recommendations(profile,Nutrition.totals(entries.flatMap { it.items })),day.toString()) to repository.pending()
        }
        val (snapshot,pending)=data
        val ai = withContext(Dispatchers.IO) { aiSettings.enabled() to aiSettings.apiKey().isNotBlank() }
        state=state.copy(profile=snapshot.profile,entries=snapshot.entries,messages=snapshot.messages,weekly=snapshot.weekly,recommendations=snapshot.recommendations,pending=pending,aiEnabled=ai.first,aiConfigured=ai.second)
    }
    fun refresh()=run { load() }
    fun enableAi(enabled: Boolean)=run { withContext(Dispatchers.IO) { aiSettings.enable(enabled) };load() }
    fun replaceAiKey(key: String)=run { withContext(Dispatchers.IO) { aiSettings.replaceKey(key) };load() }
    fun saveProfile(profile: Profile, onSaved: () -> Unit = {})=run { withContext(Dispatchers.IO) { repository.saveProfile(profile) };load();onSaved() }
    fun selectDate(date: LocalDate) {
        if(state.busy || date==state.date) return
        state=state.copy(date=date,entries=emptyList(),weekly=emptyList(),recommendations=emptyList())
        refresh()
    }
    private suspend fun savePhoto(uri: Uri): String = withContext(Dispatchers.IO) {
        val context=getApplication<Application>()
        val resolver=context.contentResolver
        val options=BitmapFactory.Options().apply { inJustDecodeBounds=true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,options) }
        require(options.outWidth>0 && options.outHeight>0) { "Выберите поддерживаемую фотографию" }
        var sample=1
        while(options.outWidth/sample>1600 || options.outHeight/sample>1600) sample*=2
        val bitmap=resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply { inSampleSize=sample }) }
            ?: error("Не удалось прочитать фото")
        val orientation=runCatching { resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION,ExifInterface.ORIENTATION_NORMAL) } }.getOrNull()
        val matrix=Matrix().apply { when(orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f,1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f,-1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f);postScale(-1f,1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f);postScale(-1f,1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
        } }
        val upright=if(matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap,0,0,bitmap.width,bitmap.height,matrix,true)
        val dir=File(context.filesDir,"meal_photos").apply { mkdirs() }
        val file=File(dir,UUID.randomUUID().toString()+".jpg")
        try { file.outputStream().use { check(upright.compress(Bitmap.CompressFormat.JPEG,85,it)) };file.absolutePath }
        catch(e: Exception) { file.delete();throw e }
        finally { if(upright!==bitmap) upright.recycle();bitmap.recycle() }
    }
    fun send(text: String, uri: Uri?)=run {
        require(text.isNotBlank() || uri!=null) { "Добавьте описание еды" }
        require(text.length<=4000) { "Сократите сообщение до 4000 символов" }
        val photo=uri?.let { savePhoto(it) }
        val result=if(text.isBlank()) AgentResult("Фото сохранено только на телефоне. Автоматического распознавания фото в этой версии нет. Напишите название блюда и массу или выберите продукт в справочнике.")
        else if(state.aiEnabled) {
            withContext(Dispatchers.IO) {
                try { OpenRouterAgent(aiSettings.apiKey()).respond(text,state.messages,requireNotNull(state.profile),state.entries) }
                catch(e: AgentUnavailable) {
                    val fallback=Nutrition.parse(text)
                    fallback.copy(reply="${e.message}\nЛокальный помощник: ${fallback.reply}")
                }
            }
        } else Nutrition.parse(text)
        val reply=if(photo!=null && result.items.isNotEmpty()) result.reply+" Фото прикреплено как иллюстрация, расчёт сделан по тексту." else result.reply
        withContext(Dispatchers.IO) {
            repository.saveConversation(state.date,Message(true,text.ifBlank { "Фото блюда" },photo),Message(false,reply),
                result.items.takeIf { it.isNotEmpty() }?.let { Entry(date=state.date.toString(),items=it,photoPath=photo) })
        }
        load()
    }
    fun propose(entry: Entry)=run { withContext(Dispatchers.IO) { repository.pending(entry) };load() }
    fun cancelPending()=run { withContext(Dispatchers.IO) { repository.pending(null) };load() }
    fun saveEntry(entry: Entry)=run { withContext(Dispatchers.IO) { repository.confirm(entry) };load() }
    fun deleteEntry(id: String)=run { withContext(Dispatchers.IO) { repository.deleteEntry(id) };load() }
    fun export(uri: Uri)=run {
        withContext(Dispatchers.IO) {
            val csv=Nutrition.csv(repository.entries(state.date.minusDays(6),state.date))
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray(Charsets.UTF_8)) } ?: error("Не удалось создать файл")
        }
    }
    fun erase()=run {
        withContext(Dispatchers.IO) { repository.erase();aiSettings.clear() }
        state=AppState(busy=true)
        load()
    }
    override fun onCleared() { repository.close();super.onCleared() }
}
