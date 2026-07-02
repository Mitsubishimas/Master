package com.mastermitsu.cvt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class FirebaseMessagingService : FirebaseMessagingService() {
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        Log.d("FCM", "FirebaseMessagingService создан")
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Новый токен: $token")
        sendTokenToServer(token)
    }
    
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d("FCM", "Сообщение получено от: ${message.from}")
        
        wakeUpScreen()
        
        val title = message.notification?.title ?: message.data["title"] ?: "Master"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val url = message.data["url"] ?: "https://mastermitsu.ru"
        val type = message.data["type"] ?: "general"
        
        val channelId = when (type) {
            "new_message" -> CHANNEL_MESSAGES
            "order_status" -> CHANNEL_ORDERS
            "calibration_ready" -> CHANNEL_CALIBRATION
            else -> CHANNEL_GENERAL
        }
        
        sendNotification(title, body, url, channelId)
    }
    
    private fun wakeUpScreen() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Master:FCM"
        )
        wakeLock.acquire(3000)
    }
    
    private fun sendNotification(title: String, body: String, url: String, channelId: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("url", url)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setLights(0xFF00D2FF.toInt(), 500, 1000)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
        
        Log.d("FCM", "Уведомление отправлено: $title")
    }
    
    private fun sendTokenToServer(token: String) {
        Thread {
            try {
                val json = JSONObject()
                json.put("token", token)
                json.put("platform", "android")
                json.put("app_version", "2.10.2")
                
                val client = OkHttpClient()
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://mastermitsu.ru/api/push_token.php")
                    .post(body)
                    .build()
                
                client.newCall(request).execute()
                Log.d("FCM", "Токен отправлен на сервер")
            } catch (e: Exception) {
                Log.e("FCM", "Ошибка отправки токена", e)
            }
        }.start()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            
            listOf(
                CHANNEL_MESSAGES to "Сообщения",
                CHANNEL_ORDERS to "Заказы",
                CHANNEL_CALIBRATION to "Калибровка",
                CHANNEL_GENERAL to "Общие"
            ).forEach { (id, name) ->
                val channel = NotificationChannel(id, name, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Уведомления Master"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 200, 500)
                    setSound(soundUri, audioAttrs)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
    
    companion object {
        const val CHANNEL_MESSAGES = "cvt_messages"
        const val CHANNEL_ORDERS = "cvt_orders"
        const val CHANNEL_CALIBRATION = "cvt_calibration"
        const val CHANNEL_GENERAL = "cvt_general"
    }
}
