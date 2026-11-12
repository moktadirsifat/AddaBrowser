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
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import com.adda.browser.history.HistoryRepository;
import com.adda.browser.tabs.TabManager;
import com.adda.browser.refresh.PullToRefreshController;
import com.adda.browser.navigation.NavigationController;
import com.adda.browser.home.HomePageProvider;
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
    private static final String HOME_HTML = com.adda.browser.home.HomePageProvider.getHtml();

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
            NavigationController.reloadOrHome(web, HOME_URL, HOME_HTML);
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
        web.setOnTouchListener(new PullToRefreshController(dp(110)).createListener(web));
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
        NavigationController.loadHome(web, HOME_URL, HOME_HTML);
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
        if (index < 0 || index >= tabManager.size()) return;
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
        next.setOnTouchListener(new PullToRefreshController(dp(110)).createListener(next));
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
        switchToTab(tabManager.size() - 1);
        next.loadDataWithBaseURL(HOME_URL, HOME_HTML, "text/html", "UTF-8", null);
    }

    private void closeCurrentTab() {
        if (tabManager.size() <= 1) {
            showHome();
            return;
        }
        int index = tabManager.indexOf(web);
        WebView closing = web;
        tabManager.remove(index);
        webContainer.removeView(closing);
        closing.stopLoading();
        closing.destroy();
        switchToTab(Math.max(0, Math.min(index, tabManager.size() - 1)));
    }

    private void showTabs() {
        ArrayList<String> labels = new ArrayList<>();
        for (int i = 0; i < tabManager.size(); i++) labels.add(tabLabel(tabManager.get(i), i));
        new AlertDialog.Builder(this).setTitle("Tabs · " + tabManager.size())
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

        addPopupItem(panel, "←   Back", () -> NavigationController.goBack(web));
        addPopupItem(panel, "→   Forward", () -> NavigationController.goForward(web));
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
        if (!NavigationController.goBack(web)) super.onBackPressed();
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
