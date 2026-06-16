package com.mastermitsu.cvt

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import com.google.android.material.button.MaterialButton

class CVTWebViewClient(
    private val progressBar: ProgressBar,
    private val retryButton: MaterialButton,
    private val webView: WebView
) : WebViewClient() {
    
    private var isRedirected = false
    
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        progressBar.visibility = android.view.View.VISIBLE
        isRedirected = false
    }
    
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        progressBar.visibility = android.view.View.GONE
    }
    
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url.toString()
        
        if (!isMastermitsuUrl(url)) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                view?.context?.startActivity(intent)
                return true
            } catch (e: Exception) {
                return false
            }
        }
        
        if (request?.isRedirect == true && !isRedirected) {
            isRedirected = true
            view?.loadUrl(url)
            return true
        }
        
        return false
    }
    
    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        if (request?.isForMainFrame == true) {
            progressBar.visibility = android.view.View.GONE
            webView.visibility = android.view.View.GONE
            retryButton.visibility = android.view.View.VISIBLE
        }
    }
    
    private fun isMastermitsuUrl(url: String): Boolean {
        return url.startsWith("https://mastermitsu.ru") || 
               url.startsWith("http://mastermitsu.ru") ||
               url.startsWith("https://www.mastermitsu.ru") ||
               url.startsWith("http://www.mastermitsu.ru") ||
               url.startsWith("file://") ||
               url.startsWith("data:")
    }
}
