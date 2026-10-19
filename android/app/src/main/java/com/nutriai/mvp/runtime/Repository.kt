package com.nutriai.mvp.runtime

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.nutriai.mvp.BuildConfig
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.security.KeyStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.HttpException
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

/** All session and health cache values are encrypted by an Android Keystore AES-GCM key. */
class SecureStore(context: Context) {
    private val prefs = context.getSharedPreferences("nutriai_secure", Context.MODE_PRIVATE)
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (store.getKey("nutriai", null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("nutriai", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun read(name: String): String? = runCatching {
        prefs.getString(name, null)?.let {
            val bytes = Base64.decode(it, Base64.NO_WRAP)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8)
        }
    }.getOrNull()
    fun write(name: String, value: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        prefs.edit().putString(name, Base64.encodeToString(cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)).apply()
    }
    fun clear() { prefs.edit().clear().apply() }
}

interface NutriApi {
    @POST("auth/register") suspend fun register(@Body body: Credentials): Tokens
    @POST("auth/login") suspend fun login(@Body body: Credentials): Tokens
    @POST("auth/refresh") suspend fun refresh(@Body body: RefreshBody): Tokens
    @POST("auth/logout") suspend fun logout(@Body body: RefreshBody)
    @POST("debug/plan") suspend fun demoPlan(@Body body: Map<String, Boolean>): Profile
    @GET("users/me") suspend fun profile(): Profile
    @PATCH("users/me") suspend fun profile(@Body profile: Profile): Profile
    @GET("diary") suspend fun diary(@Query("from") from: String, @Query("to") to: String): List<Entry>
    @POST("diary/entries") suspend fun save(@Body entry: Entry): Entry
    @DELETE("diary/entries/{id}") suspend fun delete(@Path("id") id: String)
    @GET("chat") suspend fun chat(): List<Message>
    @GET("stats/weekly") suspend fun weekly(@Query("date") date: String): List<DayStats>
    @GET("recommendations/meals") suspend fun recommendations(@Query("date") date: String): List<Recommendation>
    @POST("agent/message") suspend fun agent(@Body body: AgentRequest): AgentResult
    @Multipart @POST("media/upload") suspend fun upload(@Part file: MultipartBody.Part): MediaResult
    @GET("foods/barcode/{barcode}") suspend fun barcode(@Path("barcode") barcode: String): FoodItem
    @GET("diary/export") suspend fun export(@Query("from") from: String, @Query("to") to: String): ResponseBody
}

class Repository(context: Context) {
    private val config = context.getSharedPreferences("nutriai_config", Context.MODE_PRIVATE)
    var baseUrl: String = config.getString("base_url", BuildConfig.API_BASE_URL) ?: BuildConfig.API_BASE_URL
        private set
    val store = SecureStore(context)
    private var tokens = store.read("tokens")?.let { runCatching { json.decodeFromString<Tokens>(it) }.getOrNull() } ?: Tokens()
    val hasSession get() = tokens.refresh_token.isNotEmpty()
    private val http = OkHttpClient.Builder().callTimeout(90, TimeUnit.SECONDS).addInterceptor { chain ->
        chain.proceed(chain.request().newBuilder().apply {
            if (tokens.access_token.isNotEmpty()) header("Authorization", "Bearer ${tokens.access_token}")
        }.build())
    }.build()
    private fun createApi() = Retrofit.Builder().baseUrl(baseUrl).client(http)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType())).build().create(NutriApi::class.java)
    var api = createApi()
        private set
    fun changeServer(value: String) {
        val url = (value.trim().trimEnd('/') + "/").toHttpUrlOrNull()
        require(url != null && url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null) { "Введите адрес API, например https://api.example.org/" }
        require(BuildConfig.DEBUG || url.scheme == "https") { "Для release-сборки требуется HTTPS" }
        clear()
        baseUrl = url.toString()
        config.edit().putString("base_url", baseUrl).apply()
        api = createApi()
    }
    fun setTokens(value: Tokens) { tokens = value; store.write("tokens", json.encodeToString(value)) }
    suspend fun authenticate(register: Boolean, body: Credentials) {
        val value = if (register) api.register(body) else api.login(body)
        store.clear()
        setTokens(value)
    }
    suspend fun <T> authorized(action: suspend () -> T): T {
        try { return action() } catch (e: HttpException) {
            if (e.code() != 401 || !hasSession) throw e
            try { setTokens(api.refresh(RefreshBody(tokens.refresh_token))) }
            catch (refreshError: HttpException) { if (refreshError.code() == 401) clear(); throw refreshError }
            return action()
        }
    }
    suspend fun logout() {
        try { if (hasSession) api.logout(RefreshBody(tokens.refresh_token)) } finally { clear() }
    }
    fun clear() { tokens = Tokens(); store.clear() }
    fun cached(): Snapshot? = store.read("snapshot")?.let { runCatching { json.decodeFromString<Snapshot>(it) }.getOrNull() }
    fun cache(value: Snapshot) { store.write("snapshot", json.encodeToString(value)) }
    suspend fun upload(bytes: ByteArray): String = authorized {
        api.upload(MultipartBody.Part.createFormData("file", "meal.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))).image_id
    }
}

fun errorMessage(error: Throwable): String {
    if(error is IllegalArgumentException) return error.message ?: "Проверьте введённые данные."
    if (error is HttpException) {
        val detail = runCatching { json.parseToJsonElement(error.response()?.errorBody()?.string() ?: "{}").jsonObject["detail"]?.jsonPrimitive?.content }.getOrNull()
        return detail ?: "Сервер отклонил запрос (${error.code()}). Проверьте данные."
    }
    return "Не удалось связаться с сервером. Проверьте подключение и адрес API."
}
