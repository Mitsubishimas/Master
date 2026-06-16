package com.mastermitsu.cvt

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://mastermitsu.ru")
        
        setContentView(webView)
    }
    
    override fun onBackPressed() {
        val wv = findViewById<android.view.View>(android.R.id.content) as? WebView
        if (wv?.canGoBack() == true) wv.goBack() else super.onBackPressed()
    }
}
