package com.mastermitsu.cvt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
    private var cameraPermissionRequested = false
    
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
        
        setupWebView()
        setupNavigation()
        checkConnection()
        setupFirebase()
        requestAllPermissions()
        
        val url = intent.getStringExtra("url") ?: "https://mastermitsu.ru"
        webView.loadUrl(url)
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
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
                webView.visibility = WebView.VISIBLE
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = ProgressBar.GONE
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (!url.contains("mastermitsu.ru")) {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    catch (e: Exception) { Toast.makeText(this@MainActivity, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show() }
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
                // Автоматически разрешаем WebView-запросы (камера, микрофон)
                request?.grant(request.resources)
            }
            
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean {
                // Проверяем разрешение камеры перед открытием
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) 
                    != PackageManager.PERMISSION_GRANTED) {
                    requestCameraPermission()
                    filePathCallback?.onReceiveValue(null)
                    return true
                }
                
                this@MainActivity.filePathCallback = filePathCallback
                try { 
                    val intent = fileChooserParams?.createIntent()
                    startActivityForResult(Intent.createChooser(intent, "Выберите файл"), 1001)
                }
                catch (e: Exception) { filePathCallback?.onReceiveValue(null) }
                return true
            }
        }
        
        webView.setDownloadListener { url, _, _, _, _ ->
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            catch (e: Exception) { Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_SHORT).show() }
        }
    }
    
    private fun setupNavigation() {
        btnBack.setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
            else Toast.makeText(this, "Главная страница", Toast.LENGTH_SHORT).show()
        }
        btnHome.setOnClickListener { webView.loadUrl("https://mastermitsu.ru") }
        btnForward.setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
            else Toast.makeText(this, "Нет следующей страницы", Toast.LENGTH_SHORT).show()
        }
        btnRefresh.setOnClickListener {
            if (isOnline) webView.reload()
            else Toast.makeText(this, "Нет подключения", Toast.LENGTH_SHORT).show()
        }
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }
    
    private fun checkConnection() {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val capabilities = network?.let { cm.getNetworkCapabilities(it) }
        isOnline = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        connectionStatus.visibility = View.VISIBLE
        connectionStatus.setBackgroundResource(if (isOnline) R.drawable.status_dot_online else R.drawable.status_dot_offline)
    }
    
    private fun setupFirebase() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) android.util.Log.d("FCM_TOKEN", task.result)
        }
    }
    
    private fun requestAllPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Проверка уведомлений (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        // Проверка камеры
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA)
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 200)
        }
        
        // Разрешение на установку (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!packageManager.canRequestPackageInstalls()) {
                AlertDialog.Builder(this)
                    .setTitle("Разрешите установку")
                    .setMessage("Для автообновлений нужно разрешить установку приложений")
                    .setPositiveButton("Настройки") { _, _ -> 
                        startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply { 
                            data = Uri.parse("package:$packageName") 
                        }) 
                    }
                    .setNegativeButton("Отмена", null)
                    .show()
            }
        }
    }
    
    private fun requestCameraPermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
            AlertDialog.Builder(this)
                .setTitle("Доступ к камере")
                .setMessage("Для сканирования QR-кодов и загрузки файлов нужен доступ к камере")
                .setPositiveButton("Разрешить") { _, _ ->
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 201)
                }
                .setNegativeButton("Отмена", null)
                .show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 201)
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200 || requestCode == 201) {
            for (i in permissions.indices) {
                if (permissions[i] == Manifest.permission.CAMERA && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Камера разрешена", Toast.LENGTH_SHORT).show()
                }
            }
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
            val results = if (resultCode == RESULT_OK && data?.data != null) arrayOf(data.data!!) else null
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.getStringExtra("url")?.let { webView.loadUrl(it) }
    }
}
