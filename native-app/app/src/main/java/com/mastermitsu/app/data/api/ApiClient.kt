package com.mastermitsu.app.data.api

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import com.mastermitsu.app.data.models.*
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

interface MasterApi {
    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse
    
    @GET("api/user/profile")
    suspend fun getProfile(@Header("Authorization") token: String): ApiResponse<User>
    
    @GET("api/news")
    suspend fun getNews(): ApiResponse<List<NewsItem>>
    
    @GET("api/forum/topics")
    suspend fun getForumTopics(): ApiResponse<List<ForumTopic>>
    
    @GET("api/forum/messages")
    suspend fun getForumMessages(@Query("cat_id") catId: Int): ApiResponse<List<ForumMessage>>
    
    @POST("api/forum/send")
    suspend fun sendForumMessage(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): ApiResponse<Any>
    
    @GET("api/chat/dialogs")
    suspend fun getChatDialogs(@Header("Authorization") token: String): ApiResponse<List<ChatDialog>>
    
    @GET("api/chat/messages")
    suspend fun getChatMessages(
        @Header("Authorization") token: String,
        @Query("user_id") userId: Int
    ): ApiResponse<List<ChatMessage>>
    
    @POST("api/chat/send")
    suspend fun sendChatMessage(
        @Header("Authorization") token: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): ApiResponse<Any>
    
    @GET("api/market")
    suspend fun getMarketItems(): ApiResponse<List<MarketItem>>
}

object ApiClient {
    
    private const val BASE_URL = "https://mastermitsu.ru/"
    
    // Gson с lenient режимом для нестандартного JSON
    private val gson = GsonBuilder()
        .setLenient()
        .create()
    
    private fun getClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor { Log.d("API", it) }
        logging.level = HttpLoggingInterceptor.Level.BODY
        
        return try {
            val trustAll = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(c: Array<X509Certificate>, a: String) {}
                override fun checkServerTrusted(c: Array<X509Certificate>, a: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })
            val ssl = SSLContext.getInstance("TLS")
            ssl.init(null, trustAll, SecureRandom())
            
            OkHttpClient.Builder()
                .sslSocketFactory(ssl.socketFactory, trustAll[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .build()
        }
    }
    
    val instance: MasterApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(getClient())
            .addConverterFactory(GsonConverterFactory.create(gson))  // Используем lenient Gson
            .build()
            .create(MasterApi::class.java)
    }
}
