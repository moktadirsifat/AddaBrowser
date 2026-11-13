package com.adda.browser.update;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class UpdateManagerE2ETest {
    private static final String METADATA_URL = "https://updates.example.test/update.json";
    private static final String APK_URL = "https://updates.example.test/sifat-browser.apk";

    @Test
    public void checksDownloadsVerifiesAndOpensAndroidInstaller() throws Exception {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context target = instrumentation.getTargetContext();
        byte[] candidateApk = readAsset(instrumentation.getContext(), "candidate-update.apk");
        String sha256 = hex(MessageDigest.getInstance("SHA-256").digest(candidateApk));
        byte[] metadata = ("{"
                + "\"versionCode\":166,"
                + "\"versionName\":\"1.16.6\","
                + "\"releaseNotes\":\"CI updater end-to-end test\","
                + "\"apkUrl\":\"" + APK_URL + "\","
                + "\"sha256\":\"" + sha256 + "\""
                + "}").getBytes("UTF-8");

        UpdateManager manager = new UpdateManager(target, METADATA_URL, url -> {
            if (url.toString().equals(METADATA_URL)) return new StubConnection(url, metadata);
            if (url.toString().equals(APK_URL)) return new StubConnection(url, candidateApk);
            throw new IOException("Unexpected test URL: " + url);
        });

        CountDownLatch checkFinished = new CountDownLatch(1);
        AtomicReference<UpdateManager.Release> releaseResult = new AtomicReference<>();
        AtomicReference<String> checkError = new AtomicReference<>();
        manager.check(new UpdateManager.Listener() {
            @Override public void noUpdate() { checkFinished.countDown(); }
            @Override public void available(UpdateManager.Release release) {
                releaseResult.set(release);
                checkFinished.countDown();
            }
            @Override public void progress(int percent) { }
            @Override public void downloaded(java.io.File apk) { }
            @Override public void error(String message) {
                checkError.set(message);
                checkFinished.countDown();
            }
        });

        assertTrue("Update check timed out", checkFinished.await(20, TimeUnit.SECONDS));
        assertNull("Update check failed: " + checkError.get(), checkError.get());
        UpdateManager.Release release = releaseResult.get();
        assertNotNull("Test metadata did not produce an available update", release);
        assertEquals(166, release.versionCode);

        CountDownLatch downloadFinished = new CountDownLatch(1);
        AtomicReference<java.io.File> downloadedApk = new AtomicReference<>();
        AtomicReference<String> downloadError = new AtomicReference<>();
        manager.download(release, new UpdateManager.Listener() {
            @Override public void noUpdate() { }
            @Override public void available(UpdateManager.Release ignored) { }
            @Override public void progress(int percent) { }
            @Override public void downloaded(java.io.File apk) {
                downloadedApk.set(apk);
                downloadFinished.countDown();
            }
            @Override public void error(String message) {
                downloadError.set(message);
                downloadFinished.countDown();
            }
            @Override public void cancelled() { downloadFinished.countDown(); }
        });

        assertTrue("APK download/verification timed out", downloadFinished.await(90, TimeUnit.SECONDS));
        assertNull("APK download or verification failed: " + downloadError.get(), downloadError.get());
        java.io.File verifiedApk = downloadedApk.get();
        assertNotNull("Verified APK was not returned", verifiedApk);
        assertTrue("Verified APK is missing or empty", verifiedApk.isFile() && verifiedApk.length() > 0);
        assertEquals(candidateApk.length, verifiedApk.length());

        Intent launchIntent = target.getPackageManager().getLaunchIntentForPackage(target.getPackageName());
        assertNotNull("App launch activity is missing", launchIntent);
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Activity activity = instrumentation.startActivitySync(launchIntent);
        assertNotNull("Could not launch SIFAT Browser for installer handoff", activity);
        try {
            AtomicReference<Exception> installError = new AtomicReference<>();
            instrumentation.runOnMainSync(() -> {
                try {
                    UpdateManager.install(activity, verifiedApk);
                } catch (Exception exception) {
                    installError.set(exception);
                }
            });
            assertNull("Android installer handoff failed", installError.get());

            UiDevice device = UiDevice.getInstance(instrumentation);
            boolean installerVisible = device.wait(
                    Until.hasObject(By.pkg("com.google.android.packageinstaller")), 15000);
            if (!installerVisible) {
                installerVisible = device.wait(
                        Until.hasObject(By.pkg("com.android.packageinstaller")), 5000);
            }
            if (!installerVisible) {
                java.io.File hierarchy = new java.io.File(target.getCacheDir(), "updater-installer-window.xml");
                device.dumpWindowHierarchy(hierarchy);
            }
            assertTrue("Android package installer did not become visible after UpdateManager.install()", installerVisible);
            device.pressBack();
        } finally {
            instrumentation.runOnMainSync(activity::finish);
            if (verifiedApk.exists()) verifiedApk.delete();
        }
    }

    private static byte[] readAsset(Context context, String name) throws IOException {
        try (InputStream input = context.getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[16384];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            return output.toByteArray();
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte value : bytes) result.append(String.format(java.util.Locale.US, "%02x", value & 255));
        return result.toString();
    }

    private static final class StubConnection extends HttpURLConnection {
        private final byte[] body;

        StubConnection(URL url, byte[] body) {
            super(url);
            this.body = body;
        }

        @Override public int getResponseCode() { return HTTP_OK; }
        @Override public int getContentLength() { return body.length; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(body); }
        @Override public void disconnect() { }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() { }
    }
}
