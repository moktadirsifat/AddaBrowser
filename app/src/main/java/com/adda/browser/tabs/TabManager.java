package com.adda.browser.tabs;

import android.webkit.WebView;
import java.util.ArrayList;
import java.util.List;

/** Owns the ordered set of browser tabs and their active selection. */
public final class TabManager {
    private final ArrayList<WebView> tabs = new ArrayList<>();
    private int activeIndex = -1;

    public void add(WebView tab) {
        if (tab == null) throw new IllegalArgumentException("Tab cannot be null");
        tabs.add(tab);
        activeIndex = tabs.size() - 1;
    }

    public int size() { return tabs.size(); }
    public boolean isEmpty() { return tabs.isEmpty(); }
    public WebView get(int index) { return tabs.get(index); }
    public List<WebView> all() { return new ArrayList<>(tabs); }
    public int indexOf(WebView tab) { return tabs.indexOf(tab); }
    public WebView getActive() {
        return activeIndex >= 0 && activeIndex < tabs.size() ? tabs.get(activeIndex) : null;
    }

    public void setActive(int index) {
        if (index < 0 || index >= tabs.size()) return;
        activeIndex = index;
    }

    public int getActiveIndex() { return activeIndex; }

    public WebView remove(int index) {
        if (index < 0 || index >= tabs.size()) return null;
        WebView removed = tabs.remove(index);
        if (tabs.isEmpty()) activeIndex = -1;
        else if (activeIndex > index) activeIndex--;
        else if (activeIndex >= tabs.size()) activeIndex = tabs.size() - 1;
        return removed;
    }

    public void clear() {
        tabs.clear();
        activeIndex = -1;
    }
}
