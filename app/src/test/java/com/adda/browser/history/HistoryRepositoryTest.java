package com.adda.browser.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.HashSet;
import java.util.Set;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class HistoryRepositoryTest {
    private SharedPreferences preferences;
    private HistoryRepository repository;

    @Before
    public void setUp() {
        preferences = ApplicationProvider.getApplicationContext()
                .getSharedPreferences("history-test", 0);
        preferences.edit().clear().commit();
        repository = new HistoryRepository(preferences);
    }

    @Test
    public void addStoresNewestVisitFirstAndUsesUrlWhenTitleIsBlank() {
        repository.add("https://example.com/first", "First page");
        repository.add("https://example.com/second", "  ");

        assertEquals(2, repository.getEntries().size());
        assertEquals("https://example.com/second", repository.getEntries().get(0).title);
        assertEquals("https://example.com/second", repository.getEntries().get(0).url);
        assertEquals("First page", repository.getEntries().get(1).title);
    }

    @Test
    public void addIgnoresInternalAndAboutUrls() {
        repository.add("about:blank", "Blank");
        repository.add("https://sifat.local/", "Home");

        assertTrue(repository.getEntries().isEmpty());
    }

    @Test
    public void getEntriesMergesLegacyUrlsWithoutDuplicates() {
        repository.add("https://example.com/current", "Current");
        Set<String> legacy = new HashSet<>();
        legacy.add("https://example.com/old");
        legacy.add("https://example.com/current");
        preferences.edit().putStringSet("history", legacy).commit();

        assertEquals(2, repository.getEntries().size());
        assertEquals("https://example.com/old", repository.getEntries().get(1).url);
    }

    @Test
    public void clearRemovesCurrentAndLegacyHistory() {
        repository.add("https://example.com/current", "Current");
        preferences.edit().putStringSet("history",
                new HashSet<>(java.util.Collections.singletonList("https://example.com/old"))).commit();

        repository.clear();

        assertTrue(repository.getEntries().isEmpty());
        assertTrue(preferences.getString("history_v2", "[]").equals("[]"));
        assertTrue(preferences.getStringSet("history", null) == null);
    }
}
