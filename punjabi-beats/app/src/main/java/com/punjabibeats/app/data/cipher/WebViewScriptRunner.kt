package com.punjabibeats.app.data.cipher

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Headless WebView that executes small JavaScript snippets extracted from the
 * live YouTube player script — the zemer-cipher "runtime execution" model.
 *
 * Thread-Safe & Concurrent:
 * Protected by a coroutine [evalMutex] to ensure that concurrent decipher requests
 * (e.g. pre-resolving multiple tracks in parallel) do not race or overwrite the single
 * JavaScript bridge callback, completely eliminating 4-second timeout freezes.
 */
@Singleton
class WebViewScriptRunner @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var webView: WebView? = null
    private val bridge = JsBridge()
    private val evalMutex = Mutex()

    /** Runs [script], which must end by calling `PbBridge.result(value)`. */
    suspend fun evaluate(script: String): String? = evalMutex.withLock {
        withTimeoutOrNull(EVAL_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                mainHandler.post {
                    val view = obtainWebView()
                    bridge.setCallback { value ->
                        if (continuation.isActive) continuation.resume(value)
                    }
                    runCatching {
                        view.evaluateJavascript(script, null)
                    }.onFailure {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }
        }
    }

    /** Convenience: evaluate an expression and resolve it via the bridge. */
    suspend fun evaluateExpression(expression: String): String? =
        evaluate("(function(){ try { PbBridge.result(String($expression)); } catch (e) { PbBridge.result(\"\"); } })()")

    @SuppressLint("SetJavaScriptEnabled")
    private fun obtainWebView(): WebView {
        webView?.let { return it }
        val view = WebView(context)
        view.settings.javaScriptEnabled = true
        view.settings.domStorageEnabled = false
        view.settings.blockNetworkImage = true
        view.settings.loadsImagesAutomatically = false
        view.addJavascriptInterface(bridge, BRIDGE_NAME)
        webView = view
        return view
    }

    fun release() {
        mainHandler.post {
            webView?.destroy()
            webView = null
        }
    }

    private class JsBridge {
        @Volatile
        private var callback: ((String?) -> Unit)? = null

        fun setCallback(callback: (String?) -> Unit) {
            this.callback = callback
        }

        @JavascriptInterface
        fun result(value: String?) {
            val cb = callback
            callback = null
            cb?.invoke(value)
        }
    }

    private companion object {
        const val BRIDGE_NAME = "PbBridge"
        const val EVAL_TIMEOUT_MS = 4_000L
    }
}
