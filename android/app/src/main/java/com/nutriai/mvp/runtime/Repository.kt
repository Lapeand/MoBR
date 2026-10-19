package com.nutriai.mvp.runtime

import android.content.Context
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.time.LocalDate
import java.util.UUID

val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** One-time import of the last diary snapshot from the former network build. */
private class LegacySecureStore(context: Context) {
    private val prefs=context.getSharedPreferences("nutriai_secure",Context.MODE_PRIVATE)
    fun read(name: String): String? = runCatching {
        prefs.getString(name,null)?.let { encoded ->
            val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val key=store.getKey("nutriai",null) as SecretKey
            val bytes=Base64.decode(encoded,Base64.NO_WRAP)
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,key,GCMParameterSpec(128,bytes.copyOfRange(0,12)))
            String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)),Charsets.UTF_8)
        }
    }.getOrNull()
    fun clear() { check(prefs.edit().clear().commit()) }
}

/** Local source of truth for the diary. The optional AI never writes to this database directly. */
class Repository(private val context: Context) : SQLiteOpenHelper(context, "nutriai_local.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE metadata (key TEXT PRIMARY KEY, value TEXT NOT NULL)")
        db.execSQL("CREATE TABLE entries (id TEXT PRIMARY KEY, request_id TEXT NOT NULL UNIQUE, day TEXT NOT NULL, payload TEXT NOT NULL)")
        db.execSQL("CREATE INDEX entries_day ON entries(day)")
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, day TEXT NOT NULL, payload TEXT NOT NULL)")
        db.execSQL("CREATE INDEX messages_day ON messages(day)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    private fun read(key: String): String? = readableDatabase.rawQuery("SELECT value FROM metadata WHERE key=?", arrayOf(key)).use { if(it.moveToFirst()) it.getString(0) else null }
    private fun write(key: String, value: String) { writableDatabase.insertWithOnConflict("metadata", null, ContentValues().apply { put("key",key);put("value",value) }, SQLiteDatabase.CONFLICT_REPLACE) }
    fun initialize() {
        if(read("profile") != null) return
        val legacy = LegacySecureStore(context).read("snapshot")?.let { runCatching { json.decodeFromString<Snapshot>(it) }.getOrNull() }
        writableDatabase.beginTransaction()
        try {
            val profile = legacy?.profile ?: Profile()
            write("profile",json.encodeToString(profile.copy(targets=Nutrition.targets(profile))))
            legacy?.entries?.forEach { saveEntry(it) }
            legacy?.messages?.forEach { appendMessage(legacy.date,it) }
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }
    fun profile(): Profile = json.decodeFromString<Profile>(read("profile") ?: error("Профиль не создан"))
    fun saveProfile(profile: Profile) { write("profile",json.encodeToString(profile.copy(targets=Nutrition.targets(profile)))) }
    fun entries(from: LocalDate, to: LocalDate): List<Entry> = readableDatabase.rawQuery(
        "SELECT payload FROM entries WHERE day>=? AND day<=? ORDER BY day,id", arrayOf(from.toString(),to.toString())
    ).use { cursor -> buildList { while(cursor.moveToNext()) add(json.decodeFromString<Entry>(cursor.getString(0))) } }
    fun saveEntry(entry: Entry): Entry {
        require(entry.items.isNotEmpty() && entry.meal.isNotBlank()) { "Заполните блюдо и приём пищи" }
        LocalDate.parse(entry.date)
        entry.items.forEach { item ->
            require(item.name.isNotBlank() && item.weight_g in 1.0..3000.0 && listOf(item.kcal,item.protein,item.fat,item.carbs).all { it.isFinite() && it in 0.0..30000.0 }) { "Проверьте порцию и КБЖУ" }
        }
        readableDatabase.rawQuery("SELECT payload FROM entries WHERE request_id=?",arrayOf(entry.request_id)).use {
            if(it.moveToFirst()) return json.decodeFromString(it.getString(0))
        }
        val saved = entry.copy(id=entry.id.ifEmpty { UUID.randomUUID().toString() }, totals=Nutrition.totals(entry.items))
        writableDatabase.insertOrThrow("entries",null,ContentValues().apply {
            put("id",saved.id);put("request_id",saved.request_id);put("day",saved.date);put("payload",json.encodeToString(saved))
        })
        return saved
    }
    fun deleteEntry(id: String) { writableDatabase.delete("entries","id=?",arrayOf(id)) }
    fun appendMessage(day: String, message: Message) { writableDatabase.insertOrThrow("messages",null,ContentValues().apply {
        put("day",day);put("payload",json.encodeToString(message))
    }) }
    fun messages(day: LocalDate): List<Message> = readableDatabase.rawQuery("SELECT payload FROM messages WHERE day=? ORDER BY id",arrayOf(day.toString())).use { c ->
        buildList { while(c.moveToNext()) add(json.decodeFromString<Message>(c.getString(0))) }
    }
    fun pending(): Entry? = read("pending")?.takeIf { it.isNotEmpty() }?.let { json.decodeFromString(it) }
    fun pending(entry: Entry?) { write("pending",entry?.let { json.encodeToString(it) } ?: "") }
    fun saveConversation(day: LocalDate, user: Message, reply: Message, draft: Entry?) {
        writableDatabase.beginTransaction()
        try {
            appendMessage(day.toString(),user);appendMessage(day.toString(),reply);pending(draft)
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }
    fun confirm(entry: Entry) {
        writableDatabase.beginTransaction()
        try { saveEntry(entry);pending(null);writableDatabase.setTransactionSuccessful() }
        finally { writableDatabase.endTransaction() }
    }
    fun erase() {
        writableDatabase.beginTransaction()
        try {
            writableDatabase.delete("entries",null,null);writableDatabase.delete("messages",null,null);writableDatabase.delete("metadata",null,null)
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
        java.io.File(context.filesDir,"meal_photos").deleteRecursively()
        java.io.File(context.cacheDir,"photos").deleteRecursively()
        LegacySecureStore(context).clear()
        context.getSharedPreferences("nutriai_config",Context.MODE_PRIVATE).edit().clear().commit()
        initialize()
    }
}
