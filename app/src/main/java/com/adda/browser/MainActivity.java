package com.adda.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import com.adda.browser.history.HistoryRepository;
import com.adda.browser.tabs.TabManager;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends Activity {
    private WebView web;
    private FrameLayout webContainer;
    private final TabManager tabManager = new TabManager();
    private EditText address;
    private ProgressBar progress;
    private TextView tabCount;
    private View menuAnchor;
    private SharedPreferences prefs;
    private HistoryRepository historyRepository;
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
        ".birds{position:fixed;z-index:2;top:8%;left:0;width:100%;height:34%;pointer-events:none;overflow:hidden}.bird{position:absolute;width:25px;height:15px;opacity:.8;filter:drop-shadow(0 1px 2px rgba(20,50,60,.25));animation:fly-left 15s linear infinite}.bird path{fill:none;stroke:#fff;stroke-width:2.8;stroke-linecap:round;stroke-linejoin:round}.bird.b2{top:23%;animation-duration:19s;animation-delay:-7s;transform:scale(.72)}.bird.b3{top:48%;animation-duration:17s;animation-delay:-12s;transform:scale(.55)}.bird.b4{top:12%;animation-duration:22s;animation-delay:-4s;transform:scale(.65)}@keyframes fly-left{0%{left:108%;transform:translateY(-4px) scale(.8)}45%{transform:translateY(13px) scale(1)}100%{left:-12%;transform:translateY(32px) scale(.75)}}.content{position:relative;z-index:1;min-height:100vh;display:flex;flex-direction:column;align-items:center;justify-content:center;padding:30px 22px 76px}" +
        ".logo-stage{position:relative;display:inline-block;margin:0 auto 34px;padding-bottom:30px;isolation:isolate}" +
        ".brand{position:relative;font-family:'Arial Black',Roboto,sans-serif;font-size:52px;font-style:italic;font-weight:900;letter-spacing:7px;line-height:1.2;background:linear-gradient(35deg,#e52b36 4%,#ff8a24 30%,#fff 57%,#3d8ff5 88%);-webkit-background-clip:text;background-clip:text;color:transparent;-webkit-text-fill-color:transparent;filter:drop-shadow(0 4px 3px rgba(14,42,55,.55)) drop-shadow(0 0 9px rgba(255,255,255,.24))}" +
        ".brand:after{content:attr(data-text);position:absolute;left:0;right:0;top:calc(100% + 1px);transform:scaleY(-1);transform-origin:top;background:linear-gradient(35deg,rgba(229,43,54,.58) 4%,rgba(255,138,36,.56) 30%,rgba(255,255,255,.52) 57%,rgba(61,143,245,.58) 88%);-webkit-background-clip:text;background-clip:text;color:transparent;-webkit-text-fill-color:transparent;opacity:.48;filter:blur(.7px);-webkit-mask-image:linear-gradient(to bottom,rgba(0,0,0,.7),transparent 82%);mask-image:linear-gradient(to bottom,rgba(0,0,0,.7),transparent 82%);animation:water-shimmer 3.8s ease-in-out infinite}" +
        ".waterline{position:absolute;left:-8%;right:-8%;bottom:12px;height:12px;border-radius:50%;background:linear-gradient(90deg,transparent,rgba(223,249,255,.58),rgba(255,255,255,.82),rgba(184,235,246,.55),transparent);filter:blur(3px);opacity:.75;animation:ripple 3.8s ease-in-out infinite}" +
        "@keyframes water-shimmer{0%,100%{opacity:.35;transform:scaleY(-1) skewX(0deg)}50%{opacity:.52;transform:scaleY(-1) skewX(2deg) translateY(2px)}}@keyframes ripple{0%,100%{transform:scaleX(.88);opacity:.45}50%{transform:scaleX(1.08);opacity:.85}}" +
        ".tag{font-family:'Noto Serif Bengali','Noto Sans Bengali',serif;font-size:17px;font-weight:700;margin-top:15px;padding:5px 2px;color:#fff;letter-spacing:.15px;text-align:center;background:transparent;border:0;box-shadow:none;text-shadow:0 2px 7px rgba(10,38,48,.72),0 0 13px rgba(255,255,255,.3)}" +
        ".search{width:100%;max-width:440px;margin-top:42px;background:rgba(255,255,255,.91);border:1px solid rgba(255,255,255,.65);border-radius:18px;padding:7px 8px 7px 15px;display:flex;align-items:center;box-shadow:0 9px 28px rgba(0,0,0,.16)}" +
        ".g{font-size:22px;font-weight:800;background:conic-gradient(from -45deg,#4285f4 0 25%,#34a853 25% 48%,#fbbc05 48% 70%,#ea4335 70% 88%,#4285f4 88%);-webkit-background-clip:text;background-clip:text;color:transparent;margin-right:10px}" +
        ".search input{flex:1;min-width:0;border:0;outline:0;background:transparent;color:#27343a;font-size:15px;padding:11px 2px}" +
        ".search button{border:0;border-radius:13px;background:#dce6e8;color:#34464c;font-size:18px;width:43px;height:42px}" +
        ".shortcuts{display:flex;gap:13px;flex-wrap:wrap;justify-content:center;margin-top:31px;max-width:430px}" +
        ".shortcut{width:67px;text-decoration:none;color:#fff;text-align:center;font-size:11px;text-shadow:0 1px 5px rgba(0,0,0,.8)}" +
        ".bubble{width:48px;height:48px;margin:0 auto 8px;border-radius:16px;background:rgba(255,255,255,.96);color:#34464c;display:flex;align-items:center;justify-content:center;font-size:19px;font-weight:700;box-shadow:0 5px 15px rgba(0,0,0,.12)}.bubble svg{width:29px;height:29px;display:block}.bubble.youtube{color:#ff0033}.bubble.facebook{color:#0866ff}.bubble.wiki{color:#202124}.bubble.gmail{color:#ea4335}" +
        ".foot{position:fixed;right:17px;bottom:15px;z-index:3;margin:0;text-align:right;line-height:1.45;letter-spacing:.5px;font-size:9px;color:#fff;text-shadow:0 1px 5px rgba(0,0,0,.55)}" +
        ".foot .by{display:block;color:#fff;font-family:Roboto,Arial,sans-serif;font-size:9px;font-weight:600;letter-spacing:1.4px;text-transform:uppercase;text-shadow:0 1px 5px rgba(0,0,0,.65)}" +
        ".foot .name{display:block;color:#fff;font-family:Roboto,Arial,sans-serif;font-size:10px;font-style:italic;font-weight:700;letter-spacing:.7px;text-shadow:0 1px 5px rgba(0,0,0,.65)}" +
        "@media(max-height:620px){.content{justify-content:flex-start;padding-top:22px}.search{margin-top:22px}.shortcuts{margin-top:16px}}" +
        "</style></head><body><div class='scene'><svg viewBox='0 0 900 1600' preserveAspectRatio='xMidYMid slice' xmlns='http://www.w3.org/2000/svg'><defs><linearGradient id='sky' x2='0' y2='1'><stop stop-color='#79b8d9'/><stop offset='.55' stop-color='#f4d7a4'/><stop offset='1' stop-color='#80b6c1'/></linearGradient><linearGradient id='water' x2='0' y2='1'><stop stop-color='#53a8bc'/><stop offset='1' stop-color='#164d68'/></linearGradient><linearGradient id='hill' x2='0' y2='1'><stop stop-color='#477d70'/><stop offset='1' stop-color='#183f43'/></linearGradient></defs><rect width='900' height='1600' fill='url(#sky)'/><circle cx='170' cy='530' r='105' fill='#ffe4a4' opacity='.92'/><path d='M0 780 Q130 650 250 735 T500 690 T900 710 V1040 H0Z' fill='#799c9a'/><path d='M0 900 L180 700 290 820 460 590 640 840 760 690 900 820 V1100 H0Z' fill='#527d79'/><path d='M0 960 Q130 860 260 940 T520 900 T900 930 V1600 H0Z' fill='url(#hill)'/><path d='M0 1010 Q180 950 360 1020 T720 980 T900 1020 V1600 H0Z' fill='url(#water)'/><path d='M0 1110 Q220 1060 430 1120 T900 1100' stroke='#b9d9d2' stroke-width='8' opacity='.45' fill='none'/><path d='M0 1220 Q220 1170 450 1230 T900 1210' stroke='#d4e4d9' stroke-width='5' opacity='.28' fill='none'/><path d='M0 1370 Q180 1320 360 1375 T720 1360 T900 1380' stroke='#b9d9d2' stroke-width='4' opacity='.2' fill='none'/><path d='M0 920 Q85 820 165 900 Q240 850 300 950 L300 1040 H0Z' fill='#254e43'/><path d='M0 930 Q70 855 125 900 M35 950 Q95 870 160 930 M100 930 Q170 850 220 920' stroke='#8eaa78' stroke-width='12' fill='none' opacity='.8'/></svg></div><div class='shade'></div><div class='birds' aria-hidden='true'><svg class='bird' viewBox='0 0 30 18'><path d='M2 5 Q8 -1 15 8 Q22 -1 28 5'/></svg><svg class='bird b2' viewBox='0 0 30 18'><path d='M2 5 Q8 -1 15 8 Q22 -1 28 5'/></svg><svg class='bird b3' viewBox='0 0 30 18'><path d='M2 5 Q8 -1 15 8 Q22 -1 28 5'/></svg><svg class='bird b4' viewBox='0 0 30 18'><path d='M2 5 Q8 -1 15 8 Q22 -1 28 5'/></svg></div><main class='content'>" +
        "<div class='logo-stage'><div class='brand' data-text='SIFAT'>SIFAT</div><div class='waterline'></div></div><div class='tag'>চলো অজানায়, দিগন্তের পথে।</div>" +
        "<form class='search' onsubmit='event.preventDefault();goSearch()'><span class='g'>G</span><input id='q' type='search' placeholder='Google-এ খুঁজুন...' autocomplete='off'><button type='submit' aria-label='Search'>⌕</button></form>" +
        "<nav class='shortcuts'>" +
        "<a class='shortcut' href='https://www.youtube.com'><span class='bubble youtube'><svg viewBox='0 0 48 34' aria-hidden='true'><rect x='1' y='1' width='46' height='32' rx='9' fill='#ff0033'/><path d='M20 9 L33 17 L20 25Z' fill='white'/></svg></span>YouTube</a>" +
        "<a class='shortcut' href='https://www.facebook.com'><span class='bubble facebook'><svg viewBox='0 0 32 32' aria-hidden='true'><path fill='#0866ff' d='M18.5 30V17.4h4.2l.6-4.9h-4.8V9.4c0-1.4.4-2.3 2.4-2.3h2.6V2.7c-.5-.1-2.1-.2-4-.2-4 0-6.7 2.4-6.7 6.9v3.1H8.5v4.9h4.3V30z'/></svg></span>Facebook</a>" +
        "<a class='shortcut' href='https://www.wikipedia.org'><span class='bubble wiki'><svg viewBox='0 0 40 32' aria-hidden='true'><path fill='#202124' d='M2 5h8v2H7l5 16 5-13-2-3h-2V5h9v2h-2l5 16 5-16h-3V5h8v2h-2l-7 21h-2L19 13l-6 15h-2L4 7H2z'/></svg></span>Wikipedia</a>" +
        "<a class='shortcut' href='https://mail.google.com'><span class='bubble gmail'><svg viewBox='0 0 40 30' aria-hidden='true'><path fill='#4285f4' d='M3 27V5l5 4v18z'/><path fill='#34a853' d='M32 27V9l5-4v22z'/><path fill='#ea4335' d='M3 5l5-4 12 10L32 1l5 4-17 14z'/><path fill='#c5221f' d='M3 5l5 4v18H3z'/><path fill='#fbbc04' d='M32 9l5-4v22h-5z'/></svg></span>Gmail</a>" +
        "</nav><div class='foot'><span class='by'>Powered by-</span><span class='name'>Dr. Muktadir Sifat</span></div></main>" +
        "<script>function goSearch(){var q=document.getElementById('q').value.trim();if(q)location.href='https://www.google.com/search?q='+encodeURIComponent(q)}</script></body></html>";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("adda_browser", MODE_PRIVATE);
        historyRepository = new HistoryRepository(prefs);
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
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
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
        root.setFitsSystemWindows(true);

        // Compact browser toolbar: Home | page title/address | New tab | tab count | Menu
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setPadding(8, 5, 8, 5);
        LinearLayout.LayoutParams topRowParams = new LinearLayout.LayoutParams(-1, 88);
        topRowParams.setMargins(6, 5, 6, 5);

        TextView homeTop = label("⌂", 25, true);
        homeTop.setTextColor(text);
        homeTop.setBackground(rounded(surface, 16));
        homeTop.setOnClickListener(v -> showHome());
        topRow.addView(homeTop, new LinearLayout.LayoutParams(42, -1));

        LinearLayout addressBar = new LinearLayout(this);
        addressBar.setGravity(Gravity.CENTER_VERTICAL);
        addressBar.setPadding(11, 3, 8, 3);
        addressBar.setBackground(rounded(surface, 20));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(0, -1, 1);
        barParams.setMargins(7, 0, 5, 0);

        address = new EditText(this);
        address.setSingleLine(true);
        address.setTextSize(14);
        address.setTextColor(text);
        address.setHintTextColor(muted);
        address.setHint("Page title or web address");
        address.setSelectAllOnFocus(true);
        address.setBackgroundColor(Color.TRANSPARENT);
        address.setPadding(3, 0, 3, 0);
        address.setImeOptions(EditorInfo.IME_ACTION_GO);
        addressBar.addView(address, new LinearLayout.LayoutParams(0, -1, 1));

        TextView reload = label("↻", 21, true);
        reload.setTextColor(muted);
        reload.setOnClickListener(v -> {
            if (web.getUrl() != null && web.getUrl().startsWith(HOME_URL)) showHome();
            else web.reload();
        });
        addressBar.addView(reload, new LinearLayout.LayoutParams(34, -1));
        topRow.addView(addressBar, barParams);

        TextView addTab = label("+", 27, false);
        addTab.setTextColor(text);
        addTab.setOnClickListener(v -> createTab());
        topRow.addView(addTab, new LinearLayout.LayoutParams(34, -1));

        tabCount = label("1", 14, true);
        tabCount.setTextColor(text);
        tabCount.setBackground(rounded(surface, 12));
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(30, 32);
        countParams.setMargins(3, 0, 3, 0);
        tabCount.setOnClickListener(v -> showTabs());
        topRow.addView(tabCount, countParams);

        TextView menuButton = label("⋮", 25, true);
        menuButton.setTextColor(text);
        menuButton.setOnClickListener(v -> showMenu());
        topRow.addView(menuButton, new LinearLayout.LayoutParams(34, -1));
        menuAnchor = menuButton;

        root.addView(topRow, topRowParams);

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
        final float[] touchStartY = new float[1];
        web.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) touchStartY[0] = event.getY();
            else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (web.getScrollY() == 0 && event.getY() - touchStartY[0] > dp(110)) web.reload();
            }
            return false;
        });
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                if (url != null && url.startsWith(HOME_URL)) address.setText("");
                else if (!address.hasFocus()) {
                    String pageTitle = view.getTitle();
                    address.setText(pageTitle == null || pageTitle.trim().isEmpty() ? url : pageTitle);
                }
                addHistory(url, view.getTitle());
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
        webContainer = new FrameLayout(this);
        webContainer.addView(web, new FrameLayout.LayoutParams(-1, -1));
        root.addView(webContainer, new LinearLayout.LayoutParams(-1, 0, 1));
        tabManager.add(web);
        updateTabCount();

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

    private void addHistory(String url, String pageTitle) {
        historyRepository.add(url, pageTitle);
    }

    private void showHistory() {
        ArrayList<HistoryRepository.Entry> entries = historyRepository.getEntries();
        if (entries.isEmpty()) {
            new AlertDialog.Builder(this).setTitle("Recent history")
                .setMessage("Nothing here yet.").setPositiveButton("OK", null).show();
            return;
        }
        String[] labels = new String[entries.size()];
        for (int i = 0; i < entries.size(); i++) {
            HistoryRepository.Entry entry = entries.get(i);
            String when = entry.time > 0
                ? DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(entry.time))
                : "";
            labels[i] = entry.title + (when.isEmpty() ? "" : "\n" + when) + "\n" + entry.url;
        }
        new AlertDialog.Builder(this).setTitle("Recent history")
            .setItems(labels, (d, which) -> {
                address.setText(entries.get(which).url);
                openAddress();
            })
            .setNeutralButton("Clear", (d, w) -> {
                historyRepository.clear();
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Close", null).show();
    }

    private void updateTabCount() {
        if (tabCount != null) tabCount.setText(String.valueOf(tabManager.size()));
    }

    private String tabLabel(WebView view, int index) {
        String title = view.getTitle();
        if (title == null || title.trim().isEmpty() ||
            (view.getUrl() != null && view.getUrl().startsWith(HOME_URL))) title = "SIFAT Browser Home";
        return (index + 1) + ". " + title;
    }

    private void switchToTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        if (web != null) web.setVisibility(View.GONE);
        tabManager.setActive(index);
        web = tabManager.get(index);
        web.setVisibility(View.VISIBLE);
        String url = web.getUrl();
        if (url == null || url.startsWith(HOME_URL)) address.setText("");
        else address.setText(web.getTitle() == null ? url : web.getTitle());
        updateTabCount();
    }

    private void createTab() {
        WebView next = new WebView(this);
        WebSettings settings = next.getSettings();
        settings.setJavaScriptEnabled(javascript);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        String agent = WebSettings.getDefaultUserAgent(this);
        if (desktop) {
            agent = agent.replaceAll("; wv", "").replaceAll("Version/4\\.0 ", "");
            agent = agent.replaceAll("Android [^;]+; ", "").replaceAll("Mobile ", "");
        }
        settings.setUserAgentString(agent);
        settings.setUseWideViewPort(desktop);
        settings.setLoadWithOverviewMode(desktop);
        next.setBackgroundColor(Color.rgb(34, 53, 59));
        next.setVerticalScrollBarEnabled(false);
        final float[] startY = new float[1];
        next.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) startY[0] = event.getY();
            else if (event.getAction() == MotionEvent.ACTION_UP &&
                     next.getScrollY() == 0 && event.getY() - startY[0] > dp(110)) {
                next.reload();
            }
            return false;
        });
        next.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                addHistory(url, view.getTitle());
                if (view == web) {
                    if (url != null && url.startsWith(HOME_URL)) address.setText("");
                    else if (!address.hasFocus()) address.setText(view.getTitle() == null ? url : view.getTitle());
                }
            }
        });
        next.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                if (view == web) {
                    progress.setProgress(value);
                    progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
                }
            }
        });
        webContainer.addView(next, new FrameLayout.LayoutParams(-1, -1));
        next.setVisibility(View.GONE);
        tabManager.add(next);
        switchToTab(tabs.size() - 1);
        next.loadDataWithBaseURL(HOME_URL, HOME_HTML, "text/html", "UTF-8", null);
    }

    private void closeCurrentTab() {
        if (tabs.size() <= 1) {
            showHome();
            return;
        }
        int index = tabManager.indexOf(web);
        WebView closing = web;
        tabManager.remove(index);
        webContainer.removeView(closing);
        closing.stopLoading();
        closing.destroy();
        switchToTab(Math.max(0, Math.min(index, tabs.size() - 1)));
    }

    private void showTabs() {
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < tabs.size(); i++) labels.add(tabLabel(tabManager.get(i), i));
        new AlertDialog.Builder(this).setTitle("Tabs · " + tabs.size())
            .setItems(labels.toArray(new String[0]), (d, which) -> switchToTab(which))
            .setPositiveButton("New tab", (d, w) -> createTab())
            .setNeutralButton("Close current", (d, w) -> closeCurrentTab())
            .setNegativeButton("Done", null).show();
    }

    private void showMenu() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(6, 7, 6, 7);
        panel.setBackground(rounded(surface, 16));
        panel.setElevation(12f);
        PopupWindow popup = new PopupWindow(panel, dp(196), -2, true);
        popup.setBackgroundDrawable(rounded(surface, 16));
        popup.setOutsideTouchable(true);
        popup.setElevation(12f);

        addPopupItem(panel, "←   Back", () -> { if (web.canGoBack()) web.goBack(); });
        addPopupItem(panel, "→   Forward", () -> { if (web.canGoForward()) web.goForward(); });
        addPopupDivider(panel);
        addPopupItem(panel, "★   Bookmarks", () -> showSavedItems("bookmarks", "Bookmarks"));
        addPopupItem(panel, "◷   History", this::showHistory);
        addPopupItem(panel, "⚙   Settings", this::showSettings);
        addPopupItem(panel, "↗   Share page", this::sharePage);
        if (menuAnchor != null) popup.showAsDropDown(menuAnchor, -dp(150), 0, Gravity.END);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void addPopupItem(LinearLayout panel, String title, Runnable action) {
        TextView item = label(title, 15, true);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(dp(14), 0, dp(10), 0);
        item.setTextColor(text);
        panel.addView(item, new LinearLayout.LayoutParams(-1, dp(46)));
        item.setOnClickListener(v -> action.run());
    }

    private void addPopupDivider(LinearLayout panel) {
        View divider = new View(this);
        divider.setBackgroundColor(dark ? Color.rgb(65, 76, 80) : Color.rgb(220, 226, 226));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(1));
        params.setMargins(dp(8), dp(3), dp(8), dp(3));
        panel.addView(divider, params);
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
                historyRepository.clear();
                Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
            } else {
                new AlertDialog.Builder(this).setTitle("SIFAT Browser v" + BuildConfig.VERSION_NAME)
                    .setMessage("চলো অজানায়, দিগন্তের পথে।")
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
        for (WebView tab : tabManager.all()) { tab.stopLoading(); tab.destroy(); }
        tabManager.clear();
        super.onDestroy();
    }
}
