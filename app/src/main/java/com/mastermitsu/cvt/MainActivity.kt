package com.mastermitsu.cvt

import android.Manifest
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var connectionStatus: TextView
    private lateinit var btnBack: LinearLayout
    private lateinit var btnHome: LinearLayout
    private lateinit var btnForward: LinearLayout
    private lateinit var btnRefresh: LinearLayout
    private lateinit var btnSettings: LinearLayout
    
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var isOnline = true
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        errorText = findViewById(R.id.errorText)
        connectionStatus = findViewById(R.id.connectionStatus)
        btnBack = findViewById(R.id.btnBack)
        btnHome = findViewById(R.id.btnHome)
        btnForward = findViewById(R.id.btnForward)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnSettings = findViewById(R.id.btnSettings)
        
        // СРАЗУ запрашиваем ВСЕ разрешения
        requestAllPermissions()
        
        setupWebView()
        setupNavigation()
        checkConnection()
        setupFirebase()
        
        val url = intent.getStringExtra("url") ?: "https://mastermitsu.ru"
        webView.loadUrl(url)
    }
    
    private fun requestAllPermissions() {
        val permissions = mutableListOf<String>()
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.CAMERA)
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.RECORD_AUDIO)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 200)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            AlertDialog.Builder(this)
                .setTitle("Установка приложений")
                .setMessage("Для обновлений разрешите установку")
                .setPositiveButton("Настройки") { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply { data = Uri.parse("package:$packageName") })
                }
                .setNegativeButton("Позже", null).show()
        }
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 CVT-App"
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
        }
        
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.visibility = ProgressBar.VISIBLE
                errorText.visibility = TextView.GONE
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = ProgressBar.GONE
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (!url.contains("mastermitsu.ru")) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    catch (e: Exception) { Toast.makeText(this@MainActivity, "Ошибка ссылки", Toast.LENGTH_SHORT).show() }
                    return true
                }
                return false
            }
        }
        
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progressBar.progress = newProgress
                if (newProgress == 100) progressBar.visibility = ProgressBar.GONE
            }
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                this@MainActivity.filePathCallback = filePathCallback
                try { startActivityForResult(Intent.createChooser(fileChooserParams?.createIntent(), "Выберите"), 1001) }
                catch (e: Exception) { filePathCallback?.onReceiveValue(null) }
                return true
            }
        }
        
        webView.setDownloadListener { url, _, _, _, _ ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    dm.enqueue(DownloadManager.Request(Uri.parse(url))
                        .setTitle("Скачивание")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.substringAfterLast("/")))
                    Toast.makeText(this, "Загрузка...", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Ошибка загрузки", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupNavigation() {
        btnBack.setOnClickListener { if (webView.canGoBack()) webView.goBack() else Toast.makeText(this, "Главная", Toast.LENGTH_SHORT).show() }
        btnHome.setOnClickListener { webView.loadUrl("https://mastermitsu.ru") }
        btnForward.setOnClickListener { if (webView.canGoForward()) webView.goForward() else Toast.makeText(this, "Нет страниц", Toast.LENGTH_SHORT).show() }
        btnRefresh.setOnClickListener { if (isOnline) webView.reload() else Toast.makeText(this, "Нет сети", Toast.LENGTH_SHORT).show() }
        btnSettings.setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }
    
    private fun checkConnection() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        isOnline = cm.activeNetwork?.let { cm.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } == true
        connectionStatus.setBackgroundResource(if (isOnline) R.drawable.status_dot_online else R.drawable.status_dot_offline)
    }
    
    private fun setupFirebase() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) android.util.Log.d("FCM", task.result)
        }
    }
    
    override fun onResume() {
        super.onResume()
        checkConnection()
        UpdateChecker.checkForUpdate(this)
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            filePathCallback?.onReceiveValue(if (resultCode == RESULT_OK && data?.data != null) arrayOf(data.data!!) else null)
            filePathCallback = null
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getStringExtra("url")?.let { webView.loadUrl(it) }
    }
}
