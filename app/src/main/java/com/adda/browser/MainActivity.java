package com.adda.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private WebView web;
    private EditText address;
    private ProgressBar progress;
    private LinearLayout root;
    private SharedPreferences prefs;
    private boolean dark, desktop, javascript;
    private int bg, surface, text, muted, accent;
    private static final String HOME = "https://www.google.com";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("adda_browser", MODE_PRIVATE);
        dark = prefs.getBoolean("dark", false);
        desktop = prefs.getBoolean("desktop", false);
        javascript = prefs.getBoolean("javascript", true);
        applyPalette();
        buildInterface();
        if (state == null) web.loadUrl(HOME);
    }

    private void applyPalette() {
        bg = dark ? Color.rgb(17, 24, 39) : Color.rgb(246, 248, 252);
        surface = dark ? Color.rgb(31, 41, 55) : Color.WHITE;
        text = dark ? Color.rgb(243, 244, 246) : Color.rgb(31, 41, 55);
        muted = dark ? Color.rgb(156, 163, 175) : Color.rgb(107, 114, 128);
        accent = Color.rgb(99, 102, 241);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(dark ? 0 : View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius); return d;
    }

    private TextView label(String value, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(text);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER_VERTICAL);
        return v;
    }

    private Button button(String title) {
        Button b = new Button(this);
        b.setText(title); b.setTextSize(13); b.setAllCaps(false);
        b.setTextColor(text);
        return b;
    }

    private void buildInterface() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(16, 12, 16, 8);
        TextView brand = label("◉  Adda Browser", 21, true);
        brand.setTextColor(accent);
        header.addView(brand, new LinearLayout.LayoutParams(0, 42, 1));
        TextView version = label("v1.5", 12, false);
        version.setTextColor(muted);
        header.addView(version);
        root.addView(header);

        LinearLayout search = new LinearLayout(this);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(12, 5, 12, 5);
        search.setBackground(rounded(surface, 28));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, 52);
        searchParams.setMargins(12, 3, 12, 8);
        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextSize(14);
        address.setTextColor(text);
        address.setHintTextColor(muted);
        address.setHint("Search or enter website");
        address.setBackgroundColor(Color.TRANSPARENT);
        address.setPadding(8, 0, 4, 0);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        search.addView(address, new LinearLayout.LayoutParams(0, -1, 1));
        Button go = button("Go");
        go.setTextColor(Color.WHITE);
        go.setBackground(rounded(accent, 24));
        search.addView(go, new LinearLayout.LayoutParams(64, 42));
        root.addView(search, searchParams);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, 3));

        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(javascript);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        applyDesktopMode();
        web.setBackgroundColor(bg);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                address.setText(url);
                addHistory(url);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout bottom = new LinearLayout(this);
        bottom.setGravity(Gravity.CENTER);
        bottom.setPadding(4, 5, 4, 7);
        bottom.setBackgroundColor(surface);
        String[] names = {"‹", "›", "⌂", "★", "•••"};
        for (int i = 0; i < names.length; i++) {
            final int action = i;
            Button b = button(names[i]);
            b.setTextSize(i == 4 ? 18 : 23);
            b.setBackgroundColor(Color.TRANSPARENT);
            b.setTextColor(i == 3 ? accent : text);
            bottom.addView(b, new LinearLayout.LayoutParams(0, 48, 1));
            b.setOnClickListener(v -> {
                if (action == 0 && web.canGoBack()) web.goBack();
                else if (action == 1 && web.canGoForward()) web.goForward();
                else if (action == 2) web.loadUrl(HOME);
                else if (action == 3) saveBookmark();
                else showMenu();
            });
        }
        root.addView(bottom);
        setContentView(root);

        go.setOnClickListener(v -> openAddress());
        address.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                openAddress(); return true;
            }
            return false;
        });
    }

    private void applyDesktopMode() {
        if (web == null) return;
        String agent = WebSettings.getDefaultUserAgent(this);
        if (desktop) {
            agent = agent.replaceAll("; wv", "").replaceAll("Version/4\\.0 ", "");
            agent = agent.replaceAll("Android [^;]+; ", "");
            agent = agent.replaceAll("Mobile ", "");
        }
        web.getSettings().setUserAgentString(agent);
        web.getSettings().setUseWideViewPort(desktop);
        web.getSettings().setLoadWithOverviewMode(desktop);
    }

    private void openAddress() {
        String value = address.getText().toString().trim();
        if (value.isEmpty()) return;
        String url;
        if (value.matches("(?i)^(https?://)?[a-z0-9-]+(\\.[a-z0-9-]+)+(:[0-9]+)?(/.*)?$")) {
            url = value.matches("(?i)^https?://.*") ? value : "https://" + value;
        } else {
            try { url = "https://www.google.com/search?q=" + URLEncoder.encode(value, "UTF-8"); }
            catch (Exception e) { url = HOME; }
        }
        web.loadUrl(url);
        web.requestFocus();
    }

    private void saveBookmark() {
        String url = web.getUrl();
        if (url == null || url.isEmpty()) return;
        Set<String> items = new LinkedHashSet<>(prefs.getStringSet("bookmarks", new LinkedHashSet<>()));
        items.add(url);
        prefs.edit().putStringSet("bookmarks", items).apply();
        Toast.makeText(this, "Bookmark saved ★", Toast.LENGTH_SHORT).show();
    }

    private void addHistory(String url) {
        if (url == null || url.startsWith("about:")) return;
        ArrayList<String> list = new ArrayList<>(prefs.getStringSet("history", new LinkedHashSet<>()));
        list.remove(url); list.add(0, url);
        while (list.size() > 50) list.remove(list.size() - 1);
        prefs.edit().putStringSet("history", new LinkedHashSet<>(list)).apply();
    }

    private void showMenu() {
        String[] options = {"Bookmarks", "History", "Settings", "Share page"};
        new AlertDialog.Builder(this).setTitle("Adda Browser")
            .setItems(options, (dialog, which) -> {
                if (which == 0) showSavedItems("bookmarks", "Bookmarks");
                else if (which == 1) showSavedItems("history", "Recent history");
                else if (which == 2) showSettings();
                else if (which == 3) sharePage();
            }).show();
    }

    private void showSavedItems(String key, String title) {
        ArrayList<String> items = new ArrayList<>(prefs.getStringSet(key, new LinkedHashSet<>()));
        if (items.isEmpty()) {
            new AlertDialog.Builder(this).setTitle(title).setMessage("Nothing here yet.")
                .setPositiveButton("OK", null).show(); return;
        }
        new AlertDialog.Builder(this).setTitle(title).setItems(items.toArray(new String[0]),
            (d, which) -> { address.setText(items.get(which)); openAddress(); })
            .setNeutralButton("Clear", (d, w) -> {
                prefs.edit().remove(key).apply();
                Toast.makeText(this, title + " cleared", Toast.LENGTH_SHORT).show();
            }).setNegativeButton("Close", null).show();
    }

    private void showSettings() {
        String[] options = {
            (dark ? "✓ " : "") + "Dark theme",
            (desktop ? "✓ " : "") + "Desktop site",
            (javascript ? "✓ " : "") + "JavaScript",
            "Clear browsing history",
            "About Adda Browser"
        };
        new AlertDialog.Builder(this).setTitle("Settings").setItems(options, (d, which) -> {
            if (which == 0) {
                dark = !dark; prefs.edit().putBoolean("dark", dark).apply(); recreate();
            } else if (which == 1) {
                desktop = !desktop; prefs.edit().putBoolean("desktop", desktop).apply();
                applyDesktopMode(); web.reload();
            } else if (which == 2) {
                javascript = !javascript; prefs.edit().putBoolean("javascript", javascript).apply();
                web.getSettings().setJavaScriptEnabled(javascript);
                Toast.makeText(this, "JavaScript " + (javascript ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
            } else if (which == 3) {
                prefs.edit().remove("history").apply();
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
            } else {
                new AlertDialog.Builder(this).setTitle("Adda Browser v1.5")
                    .setMessage("A simple, lightweight browser made for everyday browsing.")
                    .setPositiveButton("OK", null).show();
            }
        }).show();
    }

    private void sharePage() {
        if (web.getUrl() == null) return;
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(android.content.Intent.EXTRA_TEXT, web.getUrl());
        startActivity(android.content.Intent.createChooser(intent, "Share page"));
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (web != null) web.saveState(out);
    }

    @Override protected void onDestroy() {
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }
}
