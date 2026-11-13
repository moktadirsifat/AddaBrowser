package com.adda.browser.update;

import java.io.IOException;
import java.net.URI;

/** Pure validation rules shared by the updater and local JVM regression tests. */
final class UpdateValidation {
    private UpdateValidation() { }

    static void validateReleaseMetadata(long versionCode, int currentVersionCode,
            String versionName, String releaseNotes, String apkUrl, String sha256)
            throws IOException {
        if (versionCode <= currentVersionCode || versionCode > Integer.MAX_VALUE) {
            throw new IOException("Invalid release version");
        }
        if (versionName == null || releaseNotes == null || sha256 == null) {
            throw new IOException("Invalid release metadata fields");
        }
        validateHttpsUrl(apkUrl);
        if (versionName.trim().isEmpty() || versionName.length() > 64
                || releaseNotes.length() > 4000 || sha256.length() != 64
                || !sha256.matches("[0-9a-f]{64}")) {
            throw new IOException("Invalid release metadata");
        }
    }

    static void validateHttpsUrl(String value) throws IOException {
        if (value == null || value.trim().isEmpty() || value.length() > 2048) {
            throw new IOException("Invalid update URL");
        }
        try {
            URI uri = new URI(value).normalize();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || uri.getHost() == null || uri.getHost().isEmpty()
                    || uri.getUserInfo() != null || uri.getFragment() != null) {
                throw new IOException("Update URL must be a valid HTTPS URL");
            }
        } catch (java.net.URISyntaxException ex) {
            throw new IOException("Update URL must be a valid HTTPS URL", ex);
        }
    }
}
