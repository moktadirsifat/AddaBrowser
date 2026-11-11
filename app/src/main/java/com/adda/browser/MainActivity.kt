package com.adda.browser

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.inputmethod.InputMethodManager
import android.content.Context

class MainActivity : Activity() {
    private lateinit var web: WebView
    private lateinit var address: EditText
    private lateinit var progress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(18, 20, 30))
        }
        val title = TextView(this).apply {
            text = "adda"
            textSize = 26f
            setTextColor(Color.WHITE)
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(18, 12, 18, 8)
        }
        root.addView(title, LinearLayout.LayoutParams(-1, 54))
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 4, 10, 8)
        }
        address = EditText(this).apply {
            hint = "Search or enter website"
            textSize = 14f
            singleLine = true
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setPadding(14, 0, 14, 0)
            background = rounded(Color.rgb(43, 47, 62), 22)
            imeOptions = EditorInfo.IME_ACTION_GO
        }
        row.addView(address, LinearLayout.LayoutParams(0, 46, 1f))
        val go = makeButton("GO")
        row.addView(go)
        root.addView(row)
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        root.addView(progress, LinearLayout.LayoutParams(-1, 3))
        web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadsImagesAutomatically = true
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    progress.visibility = View.VISIBLE
                    if (url != null) address.setText(url)
                }
                override fun onPageFinished(view: WebView?, url: String?) {
                    progress.visibility = View.GONE
                    if (url != null) address.setText(url)
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    progress.progress = newProgress
                    progress.visibility = if (newProgress >= 100) View.GONE else View.VISIBLE
                }
            }
        }
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))
        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(6, 6, 6, 8)
        }
        listOf("‹", "›", "⌂", "↻").forEachIndexed { index, label ->
            val button = makeButton(label)
            controls.addView(button, LinearLayout.LayoutParams(0, 48, 1f))
            button.setOnClickListener {
                when (index) {
                    0 -> if (web.canGoBack()) web.goBack()
                    1 -> if (web.canGoForward()) web.goForward()
                    2 -> load("https://www.google.com")
                    3 -> web.reload()
                }
            }
        }
        root.addView(controls)
        setContentView(root)
        fun submit() {
            val raw = address.text.toString().trim()
            if (raw.isNotEmpty()) {
                val url = if (raw.contains(".") && !raw.contains(" ")) {
                    if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
                } else {
                    "https://www.google.com/search?q=" + java.net.URLEncoder.encode(raw, "UTF-8")
                }
                load(url)
                (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(address.windowToken, 0)
            }
        }
        go.setOnClickListener { submit() }
        address.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                submit(); true
            } else false
        }
        load("https://www.google.com")
    }
    private fun load(url: String) { web.loadUrl(url) }
    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
    }
    private fun makeButton(label: String) = Button(this).apply {
        text = label
        textSize = 14f
        setTextColor(Color.WHITE)
        isAllCaps = false
        background = rounded(Color.rgb(79, 70, 180), 18)
        minWidth = 48
        setPadding(8, 0, 8, 0)
    }
    @Deprecated("Deprecated in Android")
    override fun onBackPressed() {
        if (::web.isInitialized && web.canGoBack()) web.goBack() else super.onBackPressed()
    }
    override fun onDestroy() {
        if (::web.isInitialized) web.destroy()
        super.onDestroy()
    }
}
