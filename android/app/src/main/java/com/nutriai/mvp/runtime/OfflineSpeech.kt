package com.nutriai.mvp.runtime

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Never falls back to a cloud recognizer or starts a model download. */
@Composable
fun rememberOfflineSpeech(onText: (String) -> Unit, onError: (String) -> Unit): () -> Unit {
    val context=LocalContext.current
    val latestText by rememberUpdatedState(onText)
    val latestError by rememberUpdatedState(onError)
    var recognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    DisposableEffect(Unit) { onDispose { recognizer?.destroy() } }
    fun start() {
        if(Build.VERSION.SDK_INT<31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            latestError("Локальный голосовой ввод недоступен на этом телефоне. Используйте текст или справочник.")
            return
        }
        recognizer?.destroy()
        try {
            recognizer=SpeechRecognizer.createOnDeviceSpeechRecognizer(context).also { speech ->
                speech.setRecognitionListener(object: RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) = Unit
                    override fun onBeginningOfSpeech() = Unit
                    override fun onRmsChanged(rmsdB: Float) = Unit
                    override fun onBufferReceived(buffer: ByteArray?) = Unit
                    override fun onEndOfSpeech() = Unit
                    override fun onPartialResults(partialResults: Bundle?) = Unit
                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                    override fun onError(error: Int) { latestError("Не удалось распознать речь локально. Проверьте установленный русский языковой пакет или введите текст.") }
                    override fun onResults(results: Bundle?) {
                        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let(latestText)
                    }
                })
                speech.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ru-RU")
                    .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true))
            }
        } catch(e: Exception) { latestError("Локальный голосовой ввод недоступен. Введите текст.") }
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if(granted) start() else latestError("Для голосового ввода нужен доступ к микрофону. Текстовый ввод доступен всегда.")
    }
    return {
        if(Build.VERSION.SDK_INT<31 || !SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            latestError("Локальный голосовой ввод недоступен на этом телефоне. Используйте текст или справочник.")
        } else if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) start()
        else permission.launch(Manifest.permission.RECORD_AUDIO)
    }
}
