package aaravgupta.youtubesdk.shared.ui

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout

/**
 * Embedded Google login view for YouTube cookie capture.
 */
class GoogleLoginView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {

    var onLoginSuccess: ((String) -> Unit)? = null

    private var hasSentCookies = false

    private val webView = WebView(context).apply {
        layoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )

        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"

        webChromeClient = WebChromeClient()
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                collectCookiesIfAvailable()
            }
        }
    }

    init {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        addView(webView)
        startLogin()
    }

    fun startLogin() {
        resetSession()
        val loginUrl = "https://accounts.google.com/ServiceLogin?service=youtube&passive=true&continue=https://www.youtube.com"
        webView.loadUrl(loginUrl)
    }

    fun resetSession() {
        hasSentCookies = false
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookies(null)
        cookieManager.flush()
    }

    fun stop() {
        webView.stopLoading()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        webView.stopLoading()
        webView.destroy()
    }

    private fun collectCookiesIfAvailable() {
        if (hasSentCookies) return

        val cookieManager = CookieManager.getInstance()
        val youtubeCookies = cookieManager.getCookie("https://www.youtube.com").orEmpty()
        val accountsCookies = cookieManager.getCookie("https://accounts.google.com").orEmpty()

        val merged = listOf(youtubeCookies, accountsCookies)
            .filter { it.isNotBlank() }
            .joinToString(separator = "; ")
            .trim()

        if (merged.isBlank()) return

        // Session cookies of interest become available only after successful sign-in.
        val likelyAuthenticated = merged.contains("SAPISID") ||
            merged.contains("__Secure-3PSID") ||
            merged.contains("SID=")

        if (likelyAuthenticated) {
            hasSentCookies = true
            cookieManager.flush()
            onLoginSuccess?.invoke(merged)
        }
    }
}
