package com.adda.browser.navigation;

import android.webkit.WebView;

/** Small navigation operations shared by browser UI controls. */
public final class NavigationController {
    private NavigationController() {}

    public static boolean goBack(WebView web) {
        if (web == null || !web.canGoBack()) return false;
        web.goBack();
        return true;
    }

    public static boolean goForward(WebView web) {
        if (web == null || !web.canGoForward()) return false;
        web.goForward();
        return true;
    }

    public static void loadHome(WebView web, String baseUrl, String html) {
        if (web != null) web.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null);
    }

    public static void reloadOrHome(WebView web, String homeUrl, String homeHtml) {
        if (web == null) return;
        String url = web.getUrl();
        if (url != null && url.startsWith(homeUrl)) loadHome(web, homeUrl, homeHtml);
        else web.reload();
    }
}