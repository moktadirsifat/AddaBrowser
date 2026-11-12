package com.adda.browser.history;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

/** Owns browser history persistence and legacy-history migration reads. */
public final class HistoryRepository {
    public static final class Entry {
        public final String title;
        public final String url;
        public final long time;

        Entry(String title, String url, long time) {
            this.title = title;
            this.url = url;
            this.time = time;
        }
    }

    private static final String HISTORY_KEY = "history_v2";
    private static final String LEGACY_HISTORY_KEY = "history";
    private static final int MAX_ENTRIES = 50;
    private final SharedPreferences preferences;

    public HistoryRepository(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    public void add(String url, String pageTitle) {
        if (url == null || url.startsWith("about:") || url.startsWith("https://sifat.local/")) return;
        try {
            JSONArray old = new JSONArray(preferences.getString(HISTORY_KEY, "[]"));
            JSONArray result = new JSONArray();
            JSONObject entry = new JSONObject();
            String title = pageTitle == null || pageTitle.trim().isEmpty() ? url : pageTitle.trim();
            entry.put("title", title);
            entry.put("url", url);
            entry.put("time", System.currentTimeMillis());
            result.put(entry);
            for (int i = 0; i < old.length() && result.length() < MAX_ENTRIES; i++) {
                JSONObject item = old.optJSONObject(i);
                if (item != null && !url.equals(item.optString("url"))) result.put(item);
            }
            preferences.edit().putString(HISTORY_KEY, result.toString()).apply();
        } catch (Exception ignored) { }
    }

    public ArrayList<Entry> getEntries() {
        ArrayList<Entry> entries = new ArrayList<>();
        try {
            JSONArray list = new JSONArray(preferences.getString(HISTORY_KEY, "[]"));
            for (int i = 0; i < list.length(); i++) {
                JSONObject item = list.optJSONObject(i);
                if (item == null) continue;
                String url = item.optString("url", "");
                String title = item.optString("title", url);
                entries.add(new Entry(title, url, item.optLong("time", 0)));
            }
        } catch (Exception ignored) { }

        // Merge legacy URL-only entries even when history_v2 already contains
        // newer visits. Otherwise adding one new visit would hide the entire
        // user's pre-v1.16 history.
        Set<String> knownUrls = new LinkedHashSet<>();
        for (Entry entry : entries) knownUrls.add(entry.url);
        Set<String> legacy = preferences.getStringSet(LEGACY_HISTORY_KEY, new LinkedHashSet<>());
        for (String url : legacy) {
            if (url != null && !url.isEmpty() && !knownUrls.contains(url) && entries.size() < MAX_ENTRIES) {
                entries.add(new Entry(url, url, 0));
                knownUrls.add(url);
            }
        }
        return entries;
    }

    public void clear() {
        preferences.edit().remove(HISTORY_KEY).remove(LEGACY_HISTORY_KEY).apply();
    }
}
