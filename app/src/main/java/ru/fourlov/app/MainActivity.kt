package ru.fourlov.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback

/**
 * 4LOV Android shell.
 *
 * All listing/category/detail/publication data is rendered by the live 4lov.ru
 * PHP application. This deliberately avoids a second, incomplete API contract
 * and never substitutes demo listings for real server data.
 */
class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var homeTab: TextView
    private lateinit var searchTab: TextView
    private lateinit var publishTab: TextView
    private var fileChooserCallback: ValueCallback<Array<Uri>>? = null

    private val homeUrl = "https://4lov.ru/"
    private val searchUrl = "https://4lov.ru/#listings"
    private val publishUrl = "https://4lov.ru/create.php"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(247, 250, 245)
        window.navigationBarColor = Color.WHITE
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(247, 250, 245))
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(10), dp(18), dp(10))
            setBackgroundColor(Color.WHITE)
        }

        val mark = TextView(this).apply {
            text = "4♡"
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(43, 145, 91))
            typeface = android.graphics.Typeface.create("sans-serif-rounded", android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(dp(46), dp(46))
        }
        val brand = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }
        brand.addView(TextView(this).apply {
            text = "4LOV"
            textSize = 19f
            setTextColor(Color.rgb(36, 66, 48))
            typeface = android.graphics.Typeface.create("sans-serif-rounded", android.graphics.Typeface.BOLD)
        })
        brand.addView(TextView(this).apply {
            text = "Делимся добром"
            textSize = 12f
            setTextColor(Color.rgb(112, 133, 117))
        })
        header.addView(mark)
        header.addView(brand, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        root.addView(header)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            visibility = View.GONE
            progressTintList = android.content.res.ColorStateList.valueOf(Color.rgb(43, 145, 91))
        }
        root.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(3)))

        val webContainer = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(247, 250, 245))
        }
        webView = WebView(this)
        webView.setBackgroundColor(Color.rgb(247, 250, 245))
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.loadsImagesAutomatically = true
        webView.settings.javaScriptCanOpenWindowsAutomatically = false
        webView.settings.allowFileAccess = false
        webView.settings.allowContentAccess = true
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                if (uri.scheme == "https" && uri.host == "4lov.ru") return false
                if (uri.scheme == "http" && uri.host == "4lov.ru") {
                    view.loadUrl(uri.buildUpon().scheme("https").build().toString())
                    return true
                }
                return try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    true
                } catch (_: ActivityNotFoundException) {
                    true
                }
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                progress.progress = newProgress
                progress.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                fileChooserCallback?.onReceiveValue(null)
                fileChooserCallback = filePathCallback
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "image/*"
                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                }
                return try {
                    startActivityForResult(Intent.createChooser(intent, "Выберите фотографии"), FILE_CHOOSER_REQUEST)
                    true
                } catch (_: ActivityNotFoundException) {
                    fileChooserCallback = null
                    false
                }
            }
        }

        webContainer.addView(webView, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        root.addView(webContainer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            elevation = dp(10).toFloat()
            setPadding(dp(8), dp(7), dp(8), dp(7))
        }
        homeTab = navItem("⌂", "Главная")
        searchTab = navItem("⌕", "Поиск")
        publishTab = navItem("+", "Разместить")
        listOf(homeTab, searchTab, publishTab).forEach { item ->
            nav.addView(item, LinearLayout.LayoutParams(0, dp(54), 1f))
        }
        homeTab.setOnClickListener { open(homeUrl, 0) }
        searchTab.setOnClickListener { open(searchUrl, 1) }
        publishTab.setOnClickListener { open(publishUrl, 2) }
        root.addView(nav)

        setContentView(root)
        updateTabs(0)

        if (savedInstanceState == null) {
            webView.loadUrl(homeUrl)
        } else {
            webView.restoreState(savedInstanceState)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack() else finish()
            }
        })
    }

    private fun navItem(symbol: String, label: String): TextView {
        return TextView(this).apply {
            text = "$symbol\n$label"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(112, 133, 117))
            typeface = android.graphics.Typeface.create("sans-serif-rounded", android.graphics.Typeface.NORMAL)
        }
    }

    private fun open(url: String, selected: Int) {
        updateTabs(selected)
        if (webView.url != url) webView.loadUrl(url)
        else if (selected == 1) webView.evaluateJavascript(
            "document.getElementById('searchInput')?.focus();", null
        )
    }

    private fun updateTabs(selected: Int) {
        val items = listOf(homeTab, searchTab, publishTab)
        items.forEachIndexed { index, item ->
            item.setTextColor(if (index == selected) Color.rgb(36, 132, 78) else Color.rgb(112, 133, 117))
            item.typeface = android.graphics.Typeface.create(
                "sans-serif-rounded",
                if (index == selected) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != FILE_CHOOSER_REQUEST) return

        val callback = fileChooserCallback ?: return
        fileChooserCallback = null
        val results = if (resultCode == Activity.RESULT_OK) {
            val clip = data?.clipData
            when {
                clip != null -> Array(clip.itemCount) { index -> clip.getItemAt(index).uri }
                data?.data != null -> arrayOf(data.data!!)
                else -> null
            }
        } else null
        callback.onReceiveValue(results)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        fileChooserCallback?.onReceiveValue(null)
        webView.stopLoading()
        webView.destroy()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val FILE_CHOOSER_REQUEST = 4107
    }
}
