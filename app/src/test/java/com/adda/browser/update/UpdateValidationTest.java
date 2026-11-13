package com.adda.browser.update;

import org.junit.Test;
import java.io.IOException;
import static org.junit.Assert.*;

public class UpdateValidationTest {
    private static final String VALID_HASH =
            "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    public void acceptsValidReleaseMetadata() throws Exception {
        UpdateValidation.validateReleaseMetadata(166, 165, "1.16.6",
                "Security and stability fixes", "https://example.com/update.apk", VALID_HASH);
    }

    @Test
    public void rejectsNonIncreasingOrOutOfRangeVersion() {
        assertInvalid(165, 165, "1.16.6", "notes",
                "https://example.com/update.apk", VALID_HASH);
        assertInvalid(2147483648L, 165, "1.16.6", "notes",
                "https://example.com/update.apk", VALID_HASH);
    }

    @Test
    public void rejectsInvalidHttpsUrls() {
        assertUrlInvalid("http://example.com/update.apk");
        assertUrlInvalid("https:///missing-host.apk");
        assertUrlInvalid("https://user@example.com/update.apk");
        assertUrlInvalid("https://example.com/update.apk#fragment");
        assertUrlInvalid("not a url");
        assertUrlInvalid(null);
    }

    @Test
    public void acceptsHttpsUrlWithPortAndPath() throws Exception {
        UpdateValidation.validateHttpsUrl("https://updates.example.com:8443/releases/app.apk");
    }

    @Test
    public void rejectsInvalidHashAndOversizedFields() {
        assertInvalid(166, 165, "1.16.6", "notes",
                "https://example.com/update.apk", "abc");
        assertInvalid(166, 165, repeat('v', 65), "notes",
                "https://example.com/update.apk", VALID_HASH);
        assertInvalid(166, 165, "1.16.6", repeat('n', 4001),
                "https://example.com/update.apk", VALID_HASH);
    }

    @Test
    public void rejectsMissingMetadataFields() {
        assertInvalid(166, 165, null, "notes",
                "https://example.com/update.apk", VALID_HASH);
        assertInvalid(166, 165, "1.16.6", "notes",
                "https://example.com/update.apk", null);
    }

    private static void assertUrlInvalid(String value) {
        try {
            UpdateValidation.validateHttpsUrl(value);
            fail("Expected invalid URL: " + value);
        } catch (IOException expected) { }
    }

    private static void assertInvalid(long code, int current, String name,
            String notes, String url, String hash) {
        try {
            UpdateValidation.validateReleaseMetadata(code, current, name, notes, url, hash);
            fail("Expected invalid release metadata");
        } catch (IOException expected) { }
    }

    private static String repeat(char value, int count) {
        StringBuilder result = new StringBuilder(count);
        for (int i = 0; i < count; i++) result.append(value);
        return result.toString();
    }
}
