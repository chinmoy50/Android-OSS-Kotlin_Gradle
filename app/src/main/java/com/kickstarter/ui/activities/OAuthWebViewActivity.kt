package com.kickstarter.ui.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.HttpAuthHandler
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewDatabase
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.kickstarter.libs.utils.extensions.getEnvironment
import com.kickstarter.libs.utils.extensions.isDarkModeEnabled
import com.kickstarter.ui.IntentKey
import com.kickstarter.ui.compose.designsystem.KickstarterApp
import com.kickstarter.viewmodels.OAuthViewModel

/**
 * Will be used for OAuth when default Browser is not Chrome
 * with other browsers (Firefox, Opera, Arc, Duck Duck Go ... etc) even based in Chromium
 * the redirection was not triggered on `LoginToutActivity.onNewIntent`, and the customTabInstance was never killed.
 */
class OAuthWebViewActivity : ComponentActivity() {
    val callback: (String) -> Unit = { inputString ->
        val intent = Intent()
            .putExtra(IntentKey.OAUTH_REDIRECT_URL, inputString)
        this.setResult(Activity.RESULT_OK, intent)
        this.finish()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(IntentKey.URL) ?: ""
        this.getEnvironment()?.let { env ->
            setContent {
                KickstarterApp(useDarkTheme = this.isDarkModeEnabled(env = env)) {
                    WebView(url, this, callback)
                }
            }
        }
    }

    @Composable
    private fun WebView(url: String, context: Context, callback: (String) -> Unit) {
        AndroidView(factory = {
            WebView(it).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                this.webViewClient = CustomWebViewClient(context = context, callback)
                this.settings.allowFileAccess = true
            }
        }, update = {
                it.loadUrl(url)
            })
    }
}

class CustomWebViewClient(private val context: Context, private val callback: (String) -> Unit) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        request?.url?.let {
            if (OAuthViewModel.isAfterRedirectionStep(it)) {
                callback(it.toString())
            }
        } ?: callback("")
        return false
    }

    /**
     * Only used on staging environments for basic http authentication
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceivedHttpAuthRequest(
        view: WebView?,
        handler: HttpAuthHandler?,
        host: String?,
        realm: String?
    ) {
        val webDatabase = WebViewDatabase.getInstance(context)
        webDatabase.setHttpAuthUsernamePassword(host, realm, "creative", "studyingatewinterfunny")
        handler?.proceed("creative", "studyingatewinterfunny")
        return super.onReceivedHttpAuthRequest(view, handler, host, realm)
    }
}