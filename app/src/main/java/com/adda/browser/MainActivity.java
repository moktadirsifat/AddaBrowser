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
    private SharedPreferences prefs;
    private boolean dark, desktop, javascript;
    private int bg, surface, text, muted, accent;
    private static final String HOME_URL = "https://sifat.local/";
    private static final String HOME_HTML =
        "<!doctype html><html><head><meta name='viewport' content='width=device-width, initial-scale=1, maximum-scale=1'>" +
        "<style>" +
        "*{box-sizing:border-box}html,body{margin:0;width:100%;height:100%;font-family:Roboto,'Noto Sans Bengali',Arial,sans-serif;color:#fff}" +
        "body{min-height:100vh;overflow:auto;background:#263b42}" +
        ".scene{position:fixed;inset:0;overflow:hidden;background:linear-gradient(180deg,#8fc5df 0%,#d5d9ce 43%,#4d9db5 64%,#15536d 100%)}.scene svg{width:100%;height:100%;display:block}" +
        ".shade{position:fixed;inset:0;background:linear-gradient(180deg,rgba(16,40,52,.04),rgba(16,40,52,.10) 55%,rgba(10,34,43,.24))}" +
        ".content{position:relative;z-index:1;min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:30px 22px 28px}" +
        ".brand{font-size:46px;font-weight:900;letter-spacing:5px;line-height:1.15;background:linear-gradient(180deg,#fff 8%,#e7edf0 48%,#aab9c0 92%);-webkit-background-clip:text;background-clip:text;color:transparent;text-shadow:0 2px 18px rgba(255,255,255,.16);filter:drop-shadow(0 3px 5px rgba(0,0,0,.35))}" +
        ".tag{font-size:15px;margin-top:12px;color:rgba(255,255,255,.94);letter-spacing:.2px;text-shadow:0 2px 7px rgba(0,0,0,.55)}" +
        ".search{width:100%;max-width:440px;margin-top:42px;background:rgba(255,255,255,.91);border:1px solid rgba(255,255,255,.65);border-radius:18px;padding:7px 8px 7px 15px;display:flex;align-items:center;box-shadow:0 9px 28px rgba(0,0,0,.16)}" +
        ".g{font-size:22px;font-weight:800;background:conic-gradient(from -45deg,#4285f4 0 25%,#34a853 25% 48%,#fbbc05 48% 70%,#ea4335 70% 88%,#4285f4 88%);-webkit-background-clip:text;background-clip:text;color:transparent;margin-right:10px}" +
        ".search input{flex:1;min-width:0;border:0;outline:0;background:transparent;color:#27343a;font-size:15px;padding:11px 2px}" +
        ".search button{border:0;border-radius:13px;background:#dce6e8;color:#34464c;font-size:18px;width:43px;height:42px}" +
        ".shortcuts{display:flex;gap:13px;flex-wrap:wrap;justify-content:center;margin-top:31px;max-width:430px}" +
        ".shortcut{width:67px;text-decoration:none;color:#fff;text-align:center;font-size:11px;text-shadow:0 1px 5px rgba(0,0,0,.8)}" +
        ".bubble{width:48px;height:48px;margin:0 auto 8px;border-radius:16px;background:rgba(245,249,249,.88);color:#34464c;display:flex;align-items:center;justify-content:center;font-size:19px;font-weight:700;box-shadow:0 5px 15px rgba(0,0,0,.12)}" +
        ".foot{margin-top:24px;text-align:center;line-height:1.55;letter-spacing:.8px;font-size:9px;color:rgba(231,239,237,.78)}" +
        ".foot .by{display:block;color:#d5e1df;font-size:9px;font-weight:500;letter-spacing:1.8px;text-transform:uppercase}" +
        ".foot .name{display:block;color:#c9b99a;font-size:11px;font-weight:700;letter-spacing:.7px;text-shadow:0 1px 5px rgba(0,0,0,.35)}" +
        "@media(max-height:620px){.content{justify-content:flex-start;padding-top:22px}.search{margin-top:22px}.shortcuts{margin-top:16px}}" +
        "</style></head><body><div class='scene'><svg viewBox='0 0 900 1600' preserveAspectRatio='xMidYMid slice' xmlns='http://www.w3.org/2000/svg'><defs><linearGradient id='sky' x2='0' y2='1'><stop stop-color='#79b8d9'/><stop offset='.55' stop-color='#f4d7a4'/><stop offset='1' stop-color='#80b6c1'/></linearGradient><linearGradient id='water' x2='0' y2='1'><stop stop-color='#53a8bc'/><stop offset='1' stop-color='#164d68'/></linearGradient><linearGradient id='hill' x2='0' y2='1'><stop stop-color='#477d70'/><stop offset='1' stop-color='#183f43'/></linearGradient></defs><rect width='900' height='1600' fill='url(#sky)'/><circle cx='170' cy='530' r='105' fill='#ffe4a4' opacity='.92'/><path d='M0 780 Q130 650 250 735 T500 690 T900 710 V1040 H0Z' fill='#799c9a'/><path d='M0 900 L180 700 290 820 460 590 640 840 760 690 900 820 V1100 H0Z' fill='#527d79'/><path d='M0 960 Q130 860 260 940 T520 900 T900 930 V1600 H0Z' fill='url(#hill)'/><path d='M0 1010 Q180 950 360 1020 T720 980 T900 1020 V1600 H0Z' fill='url(#water)'/><path d='M0 1110 Q220 1060 430 1120 T900 1100' stroke='#b9d9d2' stroke-width='8' opacity='.45' fill='none'/><path d='M0 1220 Q220 1170 450 1230 T900 1210' stroke='#d4e4d9' stroke-width='5' opacity='.28' fill='none'/><path d='M0 1370 Q180 1320 360 1375 T720 1360 T900 1380' stroke='#b9d9d2' stroke-width='4' opacity='.2' fill='none'/><path d='M0 920 Q85 820 165 900 Q240 850 300 950 L300 1040 H0Z' fill='#254e43'/><path d='M0 930 Q70 855 125 900 M35 950 Q95 870 160 930 M100 930 Q170 850 220 920' stroke='#8eaa78' stroke-width='12' fill='none' opacity='.8'/></svg></div><div class='shade'></div><main class='content'>" +
        "<div class='brand'>SIFAT</div><div class='tag'>সিফাতের সাথে, চলো অজানায়।</div>" +
        "<form class='search' onsubmit='event.preventDefault();goSearch()'><span class='g'>G</span><input id='q' type='search' placeholder='Google-এ খুঁজুন...' autocomplete='off'><button type='submit' aria-label='Search'>⌕</button></form>" +
        "<nav class='shortcuts'>" +
        "<a class='shortcut' href='https://www.youtube.com'><span class='bubble'>▶</span>YouTube</a>" +
        "<a class='shortcut' href='https://www.facebook.com'><span class='bubble'>f</span>Facebook</a>" +
        "<a class='shortcut' href='https://www.wikipedia.org'><span class='bubble'>W</span>Wikipedia</a>" +
        "<a class='shortcut' href='https://mail.google.com'><span class='bubble'>✉</span>Gmail</a>" +
        "</nav><div class='foot'><span class='by'>Powered by-</span><span class='name'>Dr. Muktadir Sifat</span></div></main>" +
        "<script>function goSearch(){var q=document.getElementById('q').value.trim();if(q)location.href='https://www.google.com/search?q='+encodeURIComponent(q)}</script></body></html>";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("adda_browser", MODE_PRIVATE);
        dark = prefs.getBoolean("dark", false);
        desktop = prefs.getBoolean("desktop", false);
        javascript = prefs.getBoolean("javascript", true);
        applyPalette();
        buildInterface();
        if (state == null) showHome();
        else if (web.restoreState(state) == null) showHome();
    }

    private void applyPalette() {
        bg = dark ? Color.rgb(17, 24, 29) : Color.rgb(238, 242, 242);
        surface = dark ? Color.rgb(35, 46, 51) : Color.rgb(248, 250, 249);
        text = dark ? Color.rgb(241, 244, 244) : Color.rgb(42, 54, 58);
        muted = dark ? Color.rgb(164, 176, 179) : Color.rgb(111, 124, 127);
        accent = Color.rgb(105, 139, 143);
        getWindow().setStatusBarColor(Color.rgb(27, 42, 47));
        getWindow().setNavigationBarColor(Color.rgb(27, 42, 47));
        getWindow().getDecorView().setSystemUiVisibility(0);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(radius); return d;
    }

    private TextView label(String value, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(size); v.setTextColor(text);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private void buildInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(bg);

        LinearLayout addressBar = new LinearLayout(this);
        addressBar.setGravity(Gravity.CENTER_VERTICAL);
        addressBar.setPadding(13, 5, 8, 5);
        addressBar.setBackground(rounded(surface, 24));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(-1, 58);
        barParams.setMargins(14, 12, 14, 9);

        TextView secure = label("◈", 18, true);
        secure.setTextColor(accent);
        addressBar.addView(secure, new LinearLayout.LayoutParams(34, -1));

        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextSize(15);
        address.setTextColor(text);
        address.setHintTextColor(muted);
        address.setHint("Search or enter website address");
        address.setSingleLine(true);
        address.setSelectAllOnFocus(true);
        address.setBackgroundColor(Color.TRANSPARENT);
        address.setPadding(4, 0, 4, 0);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.addView(address, new LinearLayout.LayoutParams(0, -1, 1));

        TextView reload = label("↻", 23, false);
        reload.setTextColor(muted);
        addressBar.addView(reload, new LinearLayout.LayoutParams(44, -1));
        reload.setOnClickListener(v -> {
            if (web.getUrl() != null && web.getUrl().startsWith(HOME_URL)) showHome();
            else web.reload();
        });
        root.addView(addressBar, barParams);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(accent));
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, 2));

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
        web.setBackgroundColor(Color.rgb(34, 53, 59));
        web.setVerticalScrollBarEnabled(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                if (url != null && url.startsWith(HOME_URL)) address.setText("");
                else if (!address.hasFocus()) {
                    String pageTitle = view.getTitle();
                    address.setText(pageTitle == null || pageTitle.trim().isEmpty() ? url : pageTitle);
                }
                addHistory(url);
            }
        });
        address.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && web.getUrl() != null && !web.getUrl().startsWith(HOME_URL)) {
                address.setText(web.getUrl());
                address.setSelection(address.getText().length());
            } else if (!hasFocus && web.getUrl() != null && !web.getUrl().startsWith(HOME_URL)) {
                String pageTitle = web.getTitle();
                address.setText(pageTitle == null || pageTitle.trim().isEmpty() ? web.getUrl() : pageTitle);
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
        bottom.setPadding(8, 6, 8, 6);
        bottom.setBackground(rounded(Color.argb(235, 28, 48, 55), 30));
        LinearLayout.LayoutParams bottomParams = new LinearLayout.LayoutParams(-1, 68);
        bottomParams.setMargins(14, 5, 14, 10);
        String[] icons = {"⌂", "‹", "›", "☆", "⋮"};
        String[] labels = {"Home", "Back", "Forward", "Saved", "More"};
        for (int i = 0; i < icons.length; i++) {
            final int action = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            TextView icon = label(icons[i], 22, true);
            icon.setTextColor(i == 0 ? Color.rgb(206, 220, 218) : Color.rgb(230, 236, 235));
            TextView caption = label(labels[i], 10, false);
            caption.setTextColor(Color.rgb(180, 195, 194));
            item.addView(icon, new LinearLayout.LayoutParams(-1, 34));
            item.addView(caption, new LinearLayout.LayoutParams(-1, 18));
            bottom.addView(item, new LinearLayout.LayoutParams(0, 56, 1));
            item.setOnClickListener(v -> {
                if (action == 0) showHome();
                else if (action == 1) { if (web.canGoBack()) web.goBack(); }
                else if (action == 2) { if (web.canGoForward()) web.goForward(); }
                else if (action == 3) showSavedItems("bookmarks", "Saved pages");
                else showMenu();
            });
        }
        root.addView(bottom, bottomParams);
        setContentView(root);

        address.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_GO || action == EditorInfo.IME_ACTION_DONE) {
                openAddress(); return true;
            }
            return false;
        });
    }

    private void showHome() {
        if (web == null) return;
        address.setText("");
        web.loadDataWithBaseURL(HOME_URL, HOME_HTML, "text/html", "UTF-8", null);
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
        if ("sifat://home".equalsIgnoreCase(value)) { showHome(); return; }
        String url;
        if (value.matches("(?i)^(https?://)?[a-z0-9-]+(\\.[a-z0-9-]+)+(:[0-9]+)?(/.*)?$")) {
            url = value.matches("(?i)^https?://.*") ? value : "https://" + value;
        } else {
            try { url = "https://www.google.com/search?q=" + URLEncoder.encode(value, "UTF-8"); }
            catch (Exception e) { url = "https://www.google.com"; }
        }
        web.loadUrl(url);
        web.requestFocus();
    }

    private void saveBookmark() {
        String url = web.getUrl();
        if (url == null || url.startsWith(HOME_URL)) {
            Toast.makeText(this, "Open a page to save it", Toast.LENGTH_SHORT).show(); return;
        }
        Set<String> items = new LinkedHashSet<>(prefs.getStringSet("bookmarks", new LinkedHashSet<>()));
        items.add(url);
        prefs.edit().putStringSet("bookmarks", items).apply();
        Toast.makeText(this, "Saved ★", Toast.LENGTH_SHORT).show();
    }

    private void addHistory(String url) {
        if (url == null || url.startsWith("about:") || url.startsWith(HOME_URL)) return;
        ArrayList<String> list = new ArrayList<>(prefs.getStringSet("history", new LinkedHashSet<>()));
        list.remove(url); list.add(0, url);
        while (list.size() > 50) list.remove(list.size() - 1);
        prefs.edit().putStringSet("history", new LinkedHashSet<>(list)).apply();
    }

    private void showMenu() {
        String[] options = {"Bookmarks", "History", "Settings", "Share page"};
        new AlertDialog.Builder(this).setTitle("SIFAT Browser")
            .setItems(options, (dialog, which) -> {
                if (which == 0) showSavedItems("bookmarks", "Bookmarks");
                else if (which == 1) showSavedItems("history", "Recent history");
                else if (which == 2) showSettings();
                else sharePage();
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
            "About SIFAT Browser"
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
                new AlertDialog.Builder(this).setTitle("SIFAT Browser v1.9")
                    .setMessage("সিফাতের সাথে, চলো অজানায়।")
                    .setPositiveButton("OK", null).show();
            }
        }).show();
    }

    private void sharePage() {
        if (web.getUrl() == null || web.getUrl().startsWith(HOME_URL)) return;
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
