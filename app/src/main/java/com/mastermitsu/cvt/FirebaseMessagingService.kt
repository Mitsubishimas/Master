package com.mastermitsu.cvt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class FirebaseMessagingService : FirebaseMessagingService() {
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        sendTokenToServer(token)
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        val title = message.notification?.title ?: message.data["title"] ?: "CVT"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val url = message.data["url"] ?: "https://mastermitsu.ru"
        
        sendNotification(title, body, url)
    }
    
    private fun sendNotification(title: String, body: String, url: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("url", url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSound)
            .setContentIntent(pendingIntent)
            .setColor(getColor(android.R.color.holo_blue_dark))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    private fun sendTokenToServer(token: String) {
        Thread {
            try {
                val json = JSONObject().apply {
                    put("token", token)
                    put("platform", "android")
                    put("app_version", "2.2.0")
                }
                
                val client = OkHttpClient()
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://mastermitsu.ru/api/push_token.php")
                    .post(body)
                    .build()
                
                val response = client.newCall(request).execute()
                Log.d("FCM", "Token sent to server: ${response.isSuccessful}")
            } catch (e: Exception) {
                Log.e("FCM", "Failed to send token", e)
            }
        }.start()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CVT Уведомления",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Push-уведомления от CVT"
                enableLights(true)
                lightColor = android.graphics.Color.parseColor("#00D2FF")
                enableVibration(true)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }
    
    companion object {
        private const val CHANNEL_ID = "cvt_channel"
    }
}
