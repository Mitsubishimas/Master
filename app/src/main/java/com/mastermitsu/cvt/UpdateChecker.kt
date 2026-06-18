package com.mastermitsu.cvt

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object UpdateChecker {
    
    private const val PREFS_NAME = "cvt_update_prefs"
    private const val LAST_CHECK_KEY = "last_update_check"
    private const val GITHUB_API = "https://api.github.com/repos/Mitsubishimas/Master/releases/latest"
    private const val CURRENT_VERSION = "v2.6.0"  // Менять при каждом релизе!
    
    fun checkForUpdate(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(LAST_CHECK_KEY, 0)
        val currentTime = System.currentTimeMillis()
        val weekInMillis = 7L * 24 * 60 * 60 * 1000
        
        // Проверяем раз в неделю
        if ((currentTime - lastCheck) < weekInMillis) {
            return
        }
        
        prefs.edit().putLong(LAST_CHECK_KEY, currentTime).apply()
        
        Thread {
            try {
                val url = URL(GITHUB_API)
                val connection = url.openConnection() as HttpURLConnection
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()
                
                val json = JSONObject(response)
                val serverVersion = json.getString("tag_name")
                val assets = json.getJSONArray("assets")
                
                // Сравниваем версии
                if (serverVersion != CURRENT_VERSION && assets.length() > 0) {
                    val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                    
                    // Проверяем, что серверная версия новее
                    if (isVersionNewer(CURRENT_VERSION, serverVersion)) {
                        Handler(Looper.getMainLooper()).post {
                            showUpdateDialog(context, serverVersion, downloadUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                // Тихая ошибка — без интернета или сервер недоступен
            }
        }.start()
    }
    
    private fun isVersionNewer(current: String, server: String): Boolean {
        try {
            val currentParts = current.replace("v", "").split(".")
            val serverParts = server.replace("v", "").split(".")
            
            for (i in 0 until minOf(currentParts.size, serverParts.size)) {
                val currentNum = currentParts[i].toInt()
                val serverNum = serverParts[i].toInt()
                
                if (serverNum > currentNum) return true
                if (serverNum < currentNum) return false
            }
            
            // Если все части равны, но серверная версия длиннее — она новее
            return serverParts.size > currentParts.size
        } catch (e: Exception) {
            return false
        }
    }
    
    private fun showUpdateDialog(context: Context, version: String, downloadUrl: String) {
        try {
            AlertDialog.Builder(context)
                .setTitle("Доступно обновление")
                .setMessage("Новая версия $version доступна для установки.\n\nТекущая версия: $CURRENT_VERSION\n\nОбновить сейчас?")
                .setPositiveButton("Скачать и установить") { _, _ ->
                    downloadAndInstall(context, downloadUrl)
                }
                .setNegativeButton("Позже") { dialog, _ ->
                    dialog.dismiss()
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            // Активность уже закрыта
        }
    }
    
    private fun downloadAndInstall(context: Context, downloadUrl: String) {
        try {
            val fileName = "Master-Update.apk"
            val file = File(context.externalCacheDir, fileName)
            
            if (file.exists()) file.delete()
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Скачивание обновления Master")
                .setDescription("Загрузка новой версии...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(file))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            val downloadId = downloadManager.enqueue(request)
            
            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        installApk(ctx, file)
                        ctx.unregisterReceiver(this)
                    }
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }
            
            Toast.makeText(context, "Загрузка обновления...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка загрузки", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun installApk(context: Context, file: File) {
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(intent)
            
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка установки", Toast.LENGTH_LONG).show()
        }
    }
}
