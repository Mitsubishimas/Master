package ru.mastermitsu.cvtapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {
    
    private lateinit var webView: WebView
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var retryButton: MaterialButton
    
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    
    private val mainScope = CoroutineScope(Dispatchers.Main + Job())
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        retryButton = findViewById(R.id.retryButton)
        
        setupWebView()
        setupRetryButton()
        requestPermissions()
        setupFirebase()
        
        loadWebsite()
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            setSupportZoom(false)
            builtInZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 CVT-App"
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowUniversalAccessFromFileURLs = true
        }
        
        webView.webViewClient = CVTWebViewClient(progressBar, retryButton, webView)
        webView.webChromeClient = CVTWebChromeClient(progressBar, this) { callback ->
            filePathCallback = callback
        }
        
        webView.setDownloadListener { url, _, _, _, _ ->
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Не удалось открыть файл", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun setupRetryButton() {
        retryButton.setOnClickListener {
            retryButton.visibility = android.view.View.GONE
            webView.visibility = android.view.View.VISIBLE
            loadWebsite()
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Некоторые функции могут быть недоступны", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun setupFirebase() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                sendTokenToServer(token)
            }
        }
        
        if (intent.hasExtra("url")) {
            val url = intent.getStringExtra("url")
            url?.let {
                webView.loadUrl(it)
            }
        }
    }
    
    private fun sendTokenToServer(token: String) {
        mainScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    NetworkUtils.sendPushToken(token)
                }
            } catch (e: Exception) {
                delay(30000)
                sendTokenToServer(token)
            }
        }
    }
    
    private fun loadWebsite() {
        if (NetworkUtils.isNetworkAvailable(this)) {
            webView.loadUrl("https://mastermitsu.ru")
        } else {
            showNoInternetDialog()
        }
    }
    
    private fun showNoInternetDialog() {
        AlertDialog.Builder(this)
            .setTitle("Нет подключения")
            .setMessage("Проверьте подключение к интернету")
            .setPositiveButton("Повторить") { _, _ -> loadWebsite() }
            .setNegativeButton("Выйти") { _, _ -> finish() }
            .show()
    }
    
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            if (filePathCallback != null) {
                val results = if (resultCode == RESULT_OK && data != null) {
                    arrayOf(data.data!!)
                } else {
                    null
                }
                filePathCallback?.onReceiveValue(results)
                filePathCallback = null
            }
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent?.hasExtra("url") == true) {
            val url = intent.getStringExtra("url")
            url?.let {
                webView.loadUrl(it)
            }
        }
    }
    
    override fun onDestroy() {
        mainScope.cancel()
        webView.destroy()
        super.onDestroy()
    }
}
