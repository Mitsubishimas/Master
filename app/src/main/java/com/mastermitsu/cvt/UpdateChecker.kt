package com.mastermitsu.cvt

import android.app.DownloadManager
import android.app.PendingIntent
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
    private const val CURRENT_VERSION = "v2.10.1"
    
    fun checkForUpdate(context: Context, showDialog: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(LAST_CHECK_KEY, 0)
        val currentTime = System.currentTimeMillis()
        val weekInMillis = 7L * 24 * 60 * 60 * 1000
        
        if (!showDialog && (currentTime - lastCheck) < weekInMillis) return
        
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
                
                if (serverVersion != CURRENT_VERSION && assets.length() > 0) {
                    val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                    if (isVersionNewer(CURRENT_VERSION, serverVersion)) {
                        Handler(Looper.getMainLooper()).post {
                            showUpdateDialog(context, serverVersion, downloadUrl)
                        }
                    }
                } else if (showDialog) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "У вас последняя версия ($CURRENT_VERSION)", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                if (showDialog) {
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(context, "Не удалось проверить обновления", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }.start()
    }
    
    private fun isVersionNewer(current: String, server: String): Boolean {
        try {
            val c = current.replace("v", "").split(".").map { it.toInt() }
            val s = server.replace("v", "").split(".").map { it.toInt() }
            for (i in 0 until minOf(c.size, s.size)) {
                if (s[i] > c[i]) return true
                if (s[i] < c[i]) return false
            }
            return s.size > c.size
        } catch (e: Exception) { return false }
    }
    
    private fun showUpdateDialog(context: Context, version: String, downloadUrl: String) {
        AlertDialog.Builder(context)
            .setTitle("Доступно обновление")
            .setMessage("Новая версия $version\nТекущая: $CURRENT_VERSION\n\nСтарая версия будет удалена автоматически.\nПродолжить?")
            .setPositiveButton("Обновить") { _, _ ->
                downloadAndInstall(context, downloadUrl)
            }
            .setNegativeButton("Позже", null)
            .setCancelable(false)
            .show()
    }
    
    private fun downloadAndInstall(context: Context, downloadUrl: String) {
        try {
            val file = File(context.externalCacheDir, "Master-Update.apk")
            if (file.exists()) file.delete()
            
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Скачивание обновления Master")
                .setDescription("Загрузка новой версии...")
                .setDestinationUri(Uri.fromFile(file))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
            
            val downloadId = dm.enqueue(request)
            
            val onComplete = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (id == downloadId) {
                        ctx.unregisterReceiver(this)
                        installAndRemoveOld(ctx, file)
                    }
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            }
            
            Toast.makeText(context, "Загрузка началась...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun installAndRemoveOld(context: Context, file: File) {
        try {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }
            
            // Создаём Intent для установки с флагом удаления старой версии
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Флаг для замены существующего приложения
                putExtra(Intent.EXTRA_REPLACING, true)
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                putExtra(Intent.EXTRA_INSTALLER_PACKAGE_NAME, context.packageName)
            }
            
            context.startActivity(installIntent)
            
            // Показываем инструкцию
            Handler(Looper.getMainLooper()).postDelayed({
                Toast.makeText(context, "Подтвердите установку обновления", Toast.LENGTH_LONG).show()
            }, 1000)
            
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка установки: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
