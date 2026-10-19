package com.nutriai.mvp.runtime

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.nutriai.mvp.BuildConfig
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** User override is encrypted with a non-exportable Android Keystore key. No key is shown in UI. */
class AiSettings(context: Context) {
    private val prefs = context.getSharedPreferences("nutriai_ai", Context.MODE_PRIVATE)
    fun apiKey(): String {
        val stored = prefs.getString("key", null) ?: return BuildConfig.OPENROUTER_API_KEY
        return runCatching {
            val bytes=Base64.decode(stored,Base64.NO_WRAP)
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,secretKey(),GCMParameterSpec(128,bytes.copyOfRange(0,12)))
            String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8)
        }.getOrDefault("")
    }
    fun enabled() = prefs.getBoolean("enabled", apiKey().isNotBlank())
    fun enable(value: Boolean) { check(prefs.edit().putBoolean("enabled",value).commit()) }
    fun clear() { check(prefs.edit().clear().putBoolean("enabled",false).commit()) }
    fun replaceKey(value: String) {
        val key=value.trim()
        require(key.matches(Regex("sk-or-[A-Za-z0-9_-]{20,}"))) { "Проверьте формат ключа OpenRouter" }
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,secretKey())
        val encoded=Base64.encodeToString(cipher.iv+cipher.doFinal(key.toByteArray(Charsets.UTF_8)),Base64.NO_WRAP)
        check(prefs.edit().putString("key",encoded).putBoolean("enabled",true).commit())
    }
    private fun secretKey(): SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("nutriai_openrouter",null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("nutriai_openrouter",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}
