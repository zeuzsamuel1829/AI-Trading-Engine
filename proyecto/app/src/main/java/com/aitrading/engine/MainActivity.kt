package com.aitrading.engine

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = false
        webView.settings.allowFileAccess = true
        webView.settings.allowContentAccess = false
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.setBackgroundColor(android.graphics.Color.rgb(8, 16, 25))

        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onDestroy() {
        (window.decorView.findViewById<WebView>(android.R.id.content))?.destroy()
        super.onDestroy()
    }
}
