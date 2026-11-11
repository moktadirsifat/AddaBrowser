package com.adda.browser;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    private WebView web;
    private EditText address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(8, 8, 8, 8);

        address = new EditText(this);
        address.setSingleLine(true);
        address.setHint("Search or enter website");
        address.setTextSize(15);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        bar.addView(address, new LinearLayout.LayoutParams(0, 48, 1));

        Button go = new Button(this);
        go.setText("Go");
        bar.addView(go);
        root.addView(bar);

        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                address.setText(url);
            }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));

        go.setOnClickListener(v -> openAddress());
        address.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                openAddress();
                return true;
            }
            return false;
        });

        setContentView(root);
        web.loadUrl("https://www.google.com");
    }

    private void openAddress() {
        String value = address.getText().toString().trim();
        if (value.isEmpty()) return;
        String url;
        if (value.contains(".") && !value.contains(" ")) {
            url = value.startsWith("http://") || value.startsWith("https://")
                    ? value : "https://" + value;
        } else {
            try {
                url = "https://www.google.com/search?q=" +
                        java.net.URLEncoder.encode(value, "UTF-8");
            } catch (Exception e) {
                url = "https://www.google.com";
            }
        }
        web.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
