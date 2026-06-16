package ru.mastermitsu.cvtapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object NetworkUtils {
    
    private val client = OkHttpClient()
    
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    fun sendPushToken(token: String) {
        val json = JSONObject().apply {
            put("token", token)
            put("platform", "android")
            put("app_version", "1.0.0")
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = json.toString().toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("https://mastermitsu.ru/api/push_token.php")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code $response")
            }
        }
    }
    
    fun checkServerHealth(): Boolean {
        val request = Request.Builder()
            .url("https://mastermitsu.ru/api/health.php")
            .get()
            .build()
        
        return try {
            client.newCall(request).execute().use { response ->
                response.isSuccessful && 
                response.body?.string()?.contains("\"status\":\"ok\"") == true
            }
        } catch (e: Exception) {
            false
        }
    }
}
