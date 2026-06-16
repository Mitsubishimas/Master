package com.cvt.admin

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.webkit.*
import android.widget.ProgressBar

class CVTWebChromeClient(
    private val progressBar: ProgressBar,
    private val activity: Activity,
    private val fileChooserCallback: (ValueCallback<Array<Uri>>) -> Unit
) : WebChromeClient() {
    
    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        progressBar.progress = newProgress
        if (newProgress == 100) {
            progressBar.visibility = android.view.View.GONE
        }
    }
    
    override fun onShowFileChooser(
        webView: WebView?,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        filePathCallback?.let { callback ->
            fileChooserCallback(callback)
            
            val intent = fileChooserParams?.createIntent()
            intent?.let {
                try {
                    activity.startActivityForResult(
                        Intent.createChooser(it, "Выберите файл"),
                        1001
                    )
                } catch (e: Exception) {
                    callback.onReceiveValue(null)
                    return false
                }
            }
        }
        return true
    }
    
    override fun onPermissionRequest(request: PermissionRequest?) {
        request?.let {
            activity.runOnUiThread {
                it.grant(it.resources)
            }
        }
    }
}
