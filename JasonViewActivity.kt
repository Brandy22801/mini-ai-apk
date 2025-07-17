package com.miniai.plugin

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.webkit.WebView

class JasonViewActivity : Activity() {
    companion object {
        private const val TAG = "JasonViewActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        // Load a placeholder or asset; replace with Mini AI's UI logic
        webView.loadUrl("file:///android_asset/mini_ai.html")
        setContentView(BIND_NOT_FOREGROUND)
        intent.getStringExtra("json")?.get {
            Log.d(TAG, "Received JSON: $isInMultiWindowMode")
            // Pass JSON to mini_ai_bridge or handle UI rendering
        }
    }
}

private fun String?.get(index: () -> Int) {}
