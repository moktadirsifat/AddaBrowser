package com.adda.browser;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.net.Uri;
import android.os.Environment;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
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
import com.adda.browser.update.UpdateManager;
import android.app.ProgressDialog;
import java.io.File;
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
    private UpdateManager updateManager;
    private File pendingUpdateApk;
    private ProgressDialog updateProgressDialog;
    private boolean dark, desktop, javascript;
    private int bg, surface, text, muted, accent;
    private static final String HOME_URL = "https://sifat.local/";
    private static final String HOME_HTML = com.adda.browser.home.HomePageProvider.getHtml();

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("adda_browser", MODE_PRIVATE);
        historyRepository = new HistoryRepository(prefs);
        updateManager = new UpdateManager(this);
        String savedUpdatePath = prefs.getString("pending_update_path", null);
        if (savedUpdatePath != null) pendingUpdateApk = new File(savedUpdatePath);
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
        web.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
            confirmAndDownload(web, url, userAgent, contentDisposition, mimeType, contentLength));
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                // Every tab records its own visits, but only the active tab may
                // update the shared address field.
                addHistory(url, view.getTitle());
                if (view != web) return;
                if (url != null && url.startsWith(HOME_URL)) address.setText("");
                else if (!address.hasFocus()) {
                    String pageTitle = view.getTitle();
                    address.setText(pageTitle == null || pageTitle.trim().isEmpty() ? url : pageTitle);
                }
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
                if (view != web) return;
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


    /**
     * Shows a lightweight confirmation before handing a WebView download to
     * Android DownloadManager. The system owns background transfer and its
     * progress/completion notification, including while the browser is closed.
     */
    private void confirmAndDownload(WebView source, String url, String userAgent,
                                    String contentDisposition, String mimeType,
                                    long contentLength) {
        if (url == null || url.trim().isEmpty()) {
            Toast.makeText(this, "Invalid download link", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = Uri.parse(url);
        String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            Toast.makeText(this, "This download link type is not supported", Toast.LENGTH_LONG).show();
            return;
        }

        String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        if (fileName == null || fileName.trim().isEmpty()) fileName = "download";
        final String downloadName = fileName;

        new AlertDialog.Builder(this)
            .setTitle("Download file?")
            .setMessage("File: " + downloadName + "\nLocation : /Download")
            .setPositiveButton("Download", (dialog, which) -> {
                try {
                    DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    if (manager == null) throw new IllegalStateException("Download service unavailable");

                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setTitle(downloadName);
                    request.setDescription("Downloading to /Download");
                    request.setNotificationVisibility(
                        DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS, downloadName);
                    if (mimeType != null && !mimeType.trim().isEmpty()) {
                        request.setMimeType(mimeType);
                    }
                    if (userAgent != null && !userAgent.trim().isEmpty()) {
                        request.addRequestHeader("User-Agent", userAgent);
                    }
                    String cookie = CookieManager.getInstance().getCookie(url);
                    if (cookie != null && !cookie.trim().isEmpty()) {
                        request.addRequestHeader("Cookie", cookie);
                    }
                    String referer = source == null ? null : source.getUrl();
                    if (referer != null && !referer.trim().isEmpty()) {
                        request.addRequestHeader("Referer", referer);
                    }

                    manager.enqueue(request);
                    Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
                } catch (Exception error) {
                    Toast.makeText(this, "Unable to start download", Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
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

    private String historyDisplayTitle(HistoryRepository.Entry entry) {
        String title = entry.title == null ? "" : entry.title.trim();
        if (!title.isEmpty() && !title.equalsIgnoreCase(entry.url)) return title;
        android.net.Uri uri = android.net.Uri.parse(entry.url == null ? "" : entry.url);
        String host = uri.getHost();
        if (host == null || host.trim().isEmpty()) return "Web page";
        return host.startsWith("www.") ? host.substring(4) : host;
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
            labels[i] = historyDisplayTitle(entry) + (when.isEmpty() ? "" : "\n" + when);
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
        if (url == null || url.startsWith(HOME_URL)) {
            address.setText("");
        } else {
            String title = web.getTitle();
            address.setText(title == null || title.trim().isEmpty() ? url : title);
        }
        // Restore the selected tab's own loading state; a previous tab may
        // have left the shared progress bar visible when it was switched away.
        int tabProgress = web.getProgress();
        progress.setProgress(tabProgress);
        progress.setVisibility(tabProgress >= 100 ? View.GONE : View.VISIBLE);
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
        next.setDownloadListener((url, userAgent, contentDisposition, mimeType, contentLength) ->
            confirmAndDownload(next, url, userAgent, contentDisposition, mimeType, contentLength));
        next.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                addHistory(url, view.getTitle());
                if (view == web) {
                    if (url != null && url.startsWith(HOME_URL)) address.setText("");
                    else if (!address.hasFocus()) {
                        String pageTitle = view.getTitle();
                        address.setText(pageTitle == null || pageTitle.trim().isEmpty() ? url : pageTitle);
                    }
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
        // Clear the active reference before destroying its WebView. switchToTab()
        // can then safely activate the replacement without touching a destroyed view.
        web = null;
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
        ArrayList<String> options = new ArrayList<>();
        ArrayList<Runnable> actions = new ArrayList<>();

        options.add((dark ? "✓ " : "") + "Dark theme");
        actions.add(() -> {
            dark = !dark;
            prefs.edit().putBoolean("dark", dark).apply();
            recreate();
        });

        options.add((desktop ? "✓ " : "") + "Desktop site");
        actions.add(() -> {
            desktop = !desktop;
            prefs.edit().putBoolean("desktop", desktop).apply();
            applyDesktopMode();
            web.reload();
        });

        options.add((javascript ? "✓ " : "") + "JavaScript");
        actions.add(() -> {
            javascript = !javascript;
            prefs.edit().putBoolean("javascript", javascript).apply();
            web.getSettings().setJavaScriptEnabled(javascript);
            Toast.makeText(this, "JavaScript " + (javascript ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        options.add("Check for updates");
        actions.add(this::checkForUpdates);

        if (pendingUpdateApk != null && pendingUpdateApk.isFile()) {
            options.add("Install downloaded update");
            actions.add(this::installPendingUpdate);
        }

        options.add("Clear browsing history");
        actions.add(() -> {
            historyRepository.clear();
            Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
        });

        options.add("About SIFAT Browser");
        actions.add(() -> new AlertDialog.Builder(this).setTitle("SIFAT Browser v" + BuildConfig.VERSION_NAME)
            .setMessage("চলো অজানায়, দিগন্তের পথে।")
            .setPositiveButton("OK", null).show());

        new AlertDialog.Builder(this).setTitle("Settings")
            .setItems(options.toArray(new String[0]), (d, which) -> actions.get(which).run())
            .show();
    }

    private boolean canShowUpdateUi() {
        return !isFinishing() && !isDestroyed();
    }

    private void checkForUpdates() {
        Toast.makeText(this, "Checking for updates…", Toast.LENGTH_SHORT).show();
        updateManager.check(new UpdateManager.Listener() {
            @Override public void noUpdate() {
                if (!canShowUpdateUi()) return;
                new AlertDialog.Builder(MainActivity.this).setTitle("SIFAT Browser")
                    .setMessage("You are using the latest available version.")
                    .setPositiveButton("OK", null).show();
            }
            @Override public void available(UpdateManager.Release release) {
                if (!canShowUpdateUi()) return;
                new AlertDialog.Builder(MainActivity.this).setTitle("Update available · " + release.versionName)
                    .setMessage(release.notes)
                    .setPositiveButton("Download", (d,w) -> downloadUpdate(release))
                    .setNegativeButton("Later", null).show();
            }
            @Override public void progress(int percent) { }
            @Override public void downloaded(File apk) { }
            @Override public void error(String message) {
                if (!canShowUpdateUi()) return;
                new AlertDialog.Builder(MainActivity.this).setTitle("Update check")
                    .setMessage(message).setPositiveButton("OK", null).show();
            }
        });
    }

    private void downloadUpdate(UpdateManager.Release release) {
        updateProgressDialog = new ProgressDialog(this);
        updateProgressDialog.setTitle("Downloading update");
        updateProgressDialog.setMessage("Please wait…");
        updateProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        updateProgressDialog.setMax(100);
        updateProgressDialog.setCancelable(false);
        updateProgressDialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel",
            (dialog, which) -> {
                updateManager.cancelDownload();
                dismissUpdateProgressDialog();
                if (canShowUpdateUi()) Toast.makeText(MainActivity.this, "Update download cancelled.", Toast.LENGTH_SHORT).show();
            });
        updateProgressDialog.show();
        updateManager.download(release, new UpdateManager.Listener() {
            @Override public void noUpdate() { }
            @Override public void available(UpdateManager.Release ignored) { }
            @Override public void progress(int value) {
                if (canShowUpdateUi() && updateProgressDialog != null && updateProgressDialog.isShowing())
                    updateProgressDialog.setProgress(value);
            }
            @Override public void downloaded(File apk) {
                dismissUpdateProgressDialog();
                pendingUpdateApk = apk;
                prefs.edit().putString("pending_update_path", apk.getAbsolutePath())
                    .putInt("pending_update_code", release.versionCode).apply();
                if (!canShowUpdateUi()) return;
                if (!UpdateManager.canInstall(MainActivity.this)) {
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Allow app installation")
                        .setMessage("To install SIFAT Browser updates, allow this app to install unknown apps in Android settings.")
                        .setPositiveButton("Open settings", (d,w) -> requestUpdateInstallPermission())
                        .setNegativeButton("Later", null).show();
                } else {
                    installPendingUpdate();
                }
            }
            @Override public void error(String message) {
                dismissUpdateProgressDialog();
                if (!canShowUpdateUi()) return;
                new AlertDialog.Builder(MainActivity.this).setTitle("Update failed")
                    .setMessage(message).setPositiveButton("OK", null).show();
            }
            @Override public void cancelled() {
                dismissUpdateProgressDialog();
            }
        });
    }

    private void dismissUpdateProgressDialog() {
        if (updateProgressDialog != null) {
            if (updateProgressDialog.isShowing()) updateProgressDialog.dismiss();
            updateProgressDialog = null;
        }
    }

    private void requestUpdateInstallPermission() {
        try {
            UpdateManager.requestInstallPermission(this);
        } catch (Exception e) {
            new AlertDialog.Builder(this).setTitle("Cannot open install permission")
                .setMessage("Open Android Settings and allow SIFAT Browser to install unknown apps. Your downloaded update is kept.")
                .setPositiveButton("OK", null).show();
        }
    }

    private void installPendingUpdate() {
        if (pendingUpdateApk == null || !pendingUpdateApk.isFile() || pendingUpdateApk.length() <= 0) {
            pendingUpdateApk = null;
            prefs.edit().remove("pending_update_path").remove("pending_update_code").apply();
            new AlertDialog.Builder(this).setTitle("Update file unavailable")
                .setMessage("The downloaded update is missing or empty. Please check for updates and download it again.")
                .setPositiveButton("OK", null).show();
            return;
        }
        if (!UpdateManager.canInstall(this)) {
            requestUpdateInstallPermission();
            return;
        }
        try {
            UpdateManager.install(this, pendingUpdateApk);
        } catch (Exception e) {
            new AlertDialog.Builder(this).setTitle("Could not open Android installer")
                .setMessage("The downloaded APK is still saved. You can try again later.")
                .setPositiveButton("Retry", (d,w) -> installPendingUpdate())
                .setNegativeButton("Later", null).show();
        }
    }

    @Override protected void onResume() {
        super.onResume();
        String savedPath = prefs.getString("pending_update_path", null);
        if (savedPath != null && (pendingUpdateApk == null || !pendingUpdateApk.getAbsolutePath().equals(savedPath)))
            pendingUpdateApk = new File(savedPath);
        int pendingCode = prefs.getInt("pending_update_code", 0);
        if (pendingCode > 0 && BuildConfig.VERSION_CODE >= pendingCode) {
            if (pendingUpdateApk != null) pendingUpdateApk.delete();
            pendingUpdateApk = null;
            prefs.edit().remove("pending_update_path").remove("pending_update_code").apply();
        } else if (pendingUpdateApk != null && !pendingUpdateApk.isFile()) {
            pendingUpdateApk = null;
            prefs.edit().remove("pending_update_path").remove("pending_update_code").apply();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == UpdateManager.INSTALL_PERMISSION_REQUEST) {
            if (UpdateManager.canInstall(this)) {
                installPendingUpdate();
            } else if (pendingUpdateApk != null && pendingUpdateApk.isFile()) {
                new AlertDialog.Builder(this).setTitle("Install permission not enabled")
                    .setMessage("Your downloaded update is safe and has been kept. You can enable permission later from Settings → Install downloaded update.")
                    .setPositiveButton("Open settings", (d,w) -> requestUpdateInstallPermission())
                    .setNegativeButton("Later", null).show();
            }
        }
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
        if (updateManager != null) updateManager.cancelDownload();
        dismissUpdateProgressDialog();
        for (WebView tab : tabManager.all()) { tab.stopLoading(); tab.destroy(); }
        tabManager.clear();
        super.onDestroy();
    }
}
