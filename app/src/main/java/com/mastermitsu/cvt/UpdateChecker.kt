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
    
    fun checkForUpdate(context: Context, showDialog: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastCheck = prefs.getLong(LAST_CHECK_KEY, 0)
        val currentTime = System.currentTimeMillis()
        val weekInMillis = 7L * 24 * 60 * 60 * 1000
        
        if (!showDialog && (currentTime - lastCheck) < weekInMillis) {
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
                val latestVersion = json.getString("tag_name")
                val assets = json.getJSONArray("assets")
                
                if (assets.length() > 0) {
                    val downloadUrl = assets.getJSONObject(0).getString("browser_download_url")
                    val currentVersion = "v1.0.0"
                    
                    if (latestVersion != currentVersion) {
                        Handler(Looper.getMainLooper()).post {
                            showUpdateDialog(context, latestVersion, downloadUrl)
                        }
                    } else {
                        if (showDialog) {
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(context, "У вас последняя версия", Toast.LENGTH_SHORT).show()
                            }
                        }
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
    
    private fun showUpdateDialog(context: Context, version: String, downloadUrl: String) {
        try {
            AlertDialog.Builder(context)
                .setTitle("Доступна новая версия")
                .setMessage("Версия $version готова к установке.\n\nСкачать и установить сейчас?")
                .setPositiveButton("Обновить") { _, _ ->
                    downloadAndInstall(context, downloadUrl)
                }
                .setNegativeButton("Позже", null)
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка отображения диалога", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun downloadAndInstall(context: Context, downloadUrl: String) {
        try {
            val fileName = "Master-Update.apk"
            val file = File(context.externalCacheDir, fileName)
            
            if (file.exists()) file.delete()
            
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadUrl))
                .setTitle("Скачивание обновления")
                .setDescription("Загрузка $fileName...")
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
            
            Toast.makeText(context, "Загрузка началась...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка загрузки: ${e.message}", Toast.LENGTH_LONG).show()
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
