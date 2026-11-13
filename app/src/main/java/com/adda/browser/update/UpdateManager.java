package com.adda.browser.update;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import androidx.core.content.FileProvider;
import com.adda.browser.BuildConfig;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;
import android.util.Log;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdateManager {
    public static final int INSTALL_PERMISSION_REQUEST = 6105;
    private static final String METADATA_URL = "https://raw.githubusercontent.com/moktadirsifat/AddaBrowser/main/update.json";
    private final Context context;
    private final String metadataUrl;
    private final ConnectionFactory connectionFactory;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile DownloadSession activeDownload;

    interface ConnectionFactory {
        HttpURLConnection open(URL url) throws IOException;
    }

    private static final class DownloadSession {
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        volatile HttpURLConnection connection;
        volatile Future<?> future;
    }

    public void cancelDownload() {
        DownloadSession session = activeDownload;
        if (session == null) return;
        session.cancelled.set(true);
        HttpURLConnection connection = session.connection;
        if (connection != null) connection.disconnect();
        Future<?> future = session.future;
        if (future != null) future.cancel(true);
        if (activeDownload == session) activeDownload = null;
    }

    private static void checkDownloadCancelled(DownloadSession session) throws IOException {
        if (session.cancelled.get() || Thread.currentThread().isInterrupted()) {
            throw new IOException("Update download cancelled");
        }
    }

    private void clearDownloadSession(DownloadSession session) {
        if (activeDownload == session) activeDownload = null;
    }

    private HttpURLConnection openDownloadConnection(DownloadSession session, URL start) throws Exception {
        URL current = start;
        for (int redirects = 0; ; redirects++) {
            checkDownloadCancelled(session);
            validateHttpsUrl(current.toString());
            HttpURLConnection connection = connectionFactory.open(current);
            session.connection = connection;
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);
            connection.setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream");
            int status = connection.getResponseCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                session.connection = null;
                if (redirects >= 5 || location == null || location.trim().isEmpty()) {
                    throw new IOException("Invalid or excessive update redirects");
                }
                current = new URL(current, location.trim());
                validateHttpsUrl(current.toString());
                continue;
            }
            if (status < 200 || status >= 300) throw new IOException("Download failed");
            return connection;
        }
    }

    public UpdateManager(Context context) {
        this(context, METADATA_URL, url -> (HttpURLConnection) url.openConnection());
    }

    UpdateManager(Context context, String metadataUrl, ConnectionFactory connectionFactory) {
        this.context = context.getApplicationContext();
        this.metadataUrl = metadataUrl;
        this.connectionFactory = connectionFactory;
    }

    public static final class Release {
        public final int versionCode;
        public final String versionName, notes, apkUrl, sha256;
        Release(int c, String n, String r, String u, String h) {
            versionCode=c; versionName=n; notes=r; apkUrl=u; sha256=h;
        }
    }

    public interface Listener {
        void noUpdate();
        void available(Release release);
        void progress(int percent);
        void downloaded(File apk);
        void error(String message);
        default void cancelled() { }
    }

    public void check(Listener listener) {
        worker.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = connectionFactory.open(new URL(metadataUrl));
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(20000);
                conn.setRequestProperty("Accept", "application/json");
                int responseCode = conn.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Metadata HTTP " + responseCode);
                }
                if (!"https".equalsIgnoreCase(conn.getURL().getProtocol())) {
                    throw new IOException("Metadata redirect was not HTTPS");
                }
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (InputStream in = conn.getInputStream()) {
                    byte[] buf = new byte[4096]; int n, total=0;
                    while ((n=in.read(buf))!=-1) {
                        total += n;
                        if (total > 65536) throw new IOException("Metadata too large");
                        out.write(buf,0,n);
                    }
                }
                JSONObject j = new JSONObject(out.toString("UTF-8"));
                Object codeValue = j.opt("versionCode");
                if (!(codeValue instanceof Integer) && !(codeValue instanceof Long)) {
                    throw new IOException("Invalid version code");
                }
                long parsedCode = ((Number) codeValue).longValue();
                if (parsedCode <= BuildConfig.VERSION_CODE || parsedCode > Integer.MAX_VALUE) {
                    main.post(listener::noUpdate);
                    return;
                }
                Object nameValue = j.opt("versionName");
                Object notesValue = j.opt("releaseNotes");
                Object apkValue = j.opt("apkUrl");
                Object hashValue = j.opt("sha256");
                if (!(nameValue instanceof String) || !(apkValue instanceof String)
                    || !(hashValue instanceof String)
                    || (notesValue != null && !(notesValue instanceof String))) {
                    throw new IOException("Invalid release metadata fields");
                }
                String name = ((String) nameValue).trim();
                String notes = notesValue == null ? "Improvements and fixes" : ((String) notesValue).trim();
                String apk = ((String) apkValue).trim();
                String hash = ((String) hashValue).trim().toLowerCase(Locale.US);
                UpdateValidation.validateReleaseMetadata(
                    parsedCode, BuildConfig.VERSION_CODE, name, notes, apk, hash);
                Release release = new Release((int) parsedCode, name, notes, apk, hash);
                main.post(() -> listener.available(release));
            } catch (Exception ex) {
                Log.e("UpdateManager", "Metadata check failed", ex);
                String reason;
                if (ex instanceof UnknownHostException) {
                    reason = "সার্ভার পাওয়া যায়নি (DNS/ইন্টারনেট)";
                } else if (ex instanceof SocketTimeoutException) {
                    reason = "সার্ভারের উত্তর পেতে সময় শেষ হয়েছে";
                } else if (ex instanceof SSLException) {
                    reason = "নিরাপদ HTTPS সংযোগ ব্যর্থ হয়েছে";
                } else if (ex instanceof org.json.JSONException) {
                    reason = "সার্ভারের update.json সঠিক JSON নয়";
                } else if (ex.getMessage() != null && !ex.getMessage().trim().isEmpty()) {
                    reason = ex.getMessage().trim();
                } else {
                    reason = ex.getClass().getSimpleName();
                }
                final String errorMessage = "আপডেট চেক ব্যর্থ: " + reason;
                main.post(() -> listener.error(errorMessage));
            } finally { if (conn != null) conn.disconnect(); }
        });
    }

    public void download(Release release, Listener listener) {
        cancelDownload();
        DownloadSession session = new DownloadSession();
        activeDownload = session;
        session.future = worker.submit(() -> {
            HttpURLConnection conn = null;
            File apk = null;
            try {
                validateHttpsUrl(release.apkUrl);
                conn = openDownloadConnection(session, new URL(release.apkUrl));
                checkDownloadCancelled(session);
                long declaredLength = conn.getContentLength();
                if (declaredLength == 0 || declaredLength > 150L * 1024L * 1024L) {
                    throw new IOException("Invalid update package size");
                }
                File dir = new File(context.getCacheDir(), "updates");
                if (!dir.exists() && !dir.mkdirs()) throw new IOException("Storage unavailable");
                apk = new File(dir, "sifat-browser-update.apk");
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long length = declaredLength, received = 0;
                try (InputStream in = new BufferedInputStream(conn.getInputStream());
                     FileOutputStream out = new FileOutputStream(apk)) {
                    byte[] buf = new byte[16384];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        checkDownloadCancelled(session);
                        received += n;
                        if (received > 150L * 1024L * 1024L) {
                            throw new IOException("Update package is too large");
                        }
                        out.write(buf, 0, n);
                        digest.update(buf, 0, n);
                        if (length > 0) {
                            final int percent = (int) Math.min(99, received * 100 / length);
                            main.post(() -> {
                                if (!session.cancelled.get()) listener.progress(percent);
                            });
                        }
                    }
                    out.getFD().sync();
                }
                checkDownloadCancelled(session);
                if (!hex(digest.digest()).equalsIgnoreCase(release.sha256)) {
                    throw new SecurityException("APK integrity check failed.");
                }
                verifySameSignature(apk);
                checkDownloadCancelled(session);
                File ready = apk;
                main.post(() -> {
                    try {
                        if (session.cancelled.get()) {
                            if (ready.exists()) ready.delete();
                            listener.cancelled();
                        } else {
                            listener.progress(100);
                            listener.downloaded(ready);
                        }
                    } finally {
                        clearDownloadSession(session);
                    }
                });
            } catch (Exception ex) {
                if (apk != null && apk.exists()) apk.delete();
                if (session.cancelled.get() || Thread.currentThread().isInterrupted()) {
                    main.post(() -> {
                        try { listener.cancelled(); }
                        finally { clearDownloadSession(session); }
                    });
                } else {
                    String message = ex instanceof SecurityException
                        ? ex.getMessage()
                        : "ডাউনলোড সম্পন্ন হয়নি। পরে আবার চেষ্টা করুন।";
                    main.post(() -> {
                        try { listener.error(message); }
                        finally { clearDownloadSession(session); }
                    });
                }
            } finally {
                if (conn != null) conn.disconnect();
                HttpURLConnection active = session.connection;
                if (active != null) active.disconnect();
            }
        });
    }

    private void verifySameSignature(File apk) throws Exception {
        PackageManager pm = context.getPackageManager();
        final int signatureFlags = Build.VERSION.SDK_INT >= 28
            ? PackageManager.GET_SIGNING_CERTIFICATES
            : PackageManager.GET_SIGNATURES;
        PackageInfo current = pm.getPackageInfo(context.getPackageName(), signatureFlags);
        PackageInfo candidate = pm.getPackageArchiveInfo(apk.getAbsolutePath(), signatureFlags);
        long candidateVersion = candidate == null ? -1L
            : (Build.VERSION.SDK_INT >= 28 ? candidate.getLongVersionCode() : candidate.versionCode);
        if (candidate == null
            || !context.getPackageName().equals(candidate.packageName)
            || candidateVersion <= BuildConfig.VERSION_CODE) {
            throw new SecurityException("এটি বৈধ SIFAT Browser APK নয়।");
        }

        if (Build.VERSION.SDK_INT >= 28) {
            SigningInfo installedSigning = current.signingInfo;
            SigningInfo candidateSigning = candidate.signingInfo;
            if (installedSigning == null || candidateSigning == null) {
                throw new SecurityException("APK signing information পাওয়া যায়নি।");
            }
            Signature[] installedCurrent = installedSigning.getApkContentsSigners();
            Signature[] candidateCurrent = candidateSigning.getApkContentsSigners();
            if (installedCurrent == null || installedCurrent.length == 0
                || candidateCurrent == null || candidateCurrent.length == 0) {
                throw new SecurityException("APK signing certificate পাওয়া যায়নি।");
            }

            if (installedSigning.hasMultipleSigners() || candidateSigning.hasMultipleSigners()) {
                if (!sameCertificates(installedCurrent, candidateCurrent)) {
                    throw signingMismatch();
                }
            } else {
                Signature installedSigner = installedCurrent[0];
                Signature[] candidateHistory = candidateSigning.getSigningCertificateHistory();
                if (candidateHistory == null || candidateHistory.length == 0
                    || !containsCertificate(candidateHistory, installedSigner)) {
                    throw signingMismatch();
                }
            }
        } else {
            if (current.signatures == null || current.signatures.length == 0
                || candidate.signatures == null || candidate.signatures.length == 0
                || !sameCertificates(current.signatures, candidate.signatures)) {
                throw signingMismatch();
            }
        }
    }

    private static boolean containsCertificate(Signature[] certificates, Signature expected) throws Exception {
        String expectedDigest = certificateDigest(expected);
        for (Signature certificate : certificates) {
            if (certificate != null && expectedDigest.equals(certificateDigest(certificate))) return true;
        }
        return false;
    }

    private static boolean sameCertificates(Signature[] first, Signature[] second) throws Exception {
        if (first.length != second.length) return false;
        String[] a = new String[first.length];
        String[] b = new String[second.length];
        for (int i = 0; i < first.length; i++) {
            if (first[i] == null || second[i] == null) return false;
            a[i] = certificateDigest(first[i]);
            b[i] = certificateDigest(second[i]);
        }
        java.util.Arrays.sort(a);
        java.util.Arrays.sort(b);
        return java.util.Arrays.equals(a, b);
    }

    private static String certificateDigest(Signature signature) throws Exception {
        return hex(MessageDigest.getInstance("SHA-256").digest(signature.toByteArray()));
    }

    private static SecurityException signingMismatch() {
        return new SecurityException("এই APK-টির signing key বর্তমান অ্যাপের সঙ্গে মেলে না। বর্তমান অ্যাপ নিরাপদ আছে; আপডেট ইনস্টল করা হয়নি।");
    }

    private static void validateHttpsUrl(String value) throws Exception {
        UpdateValidation.validateHttpsUrl(value);
    }

    private static String hex(byte[] bytes) {
        StringBuilder s=new StringBuilder();
        for (byte b:bytes) s.append(String.format(Locale.US,"%02x",b&255));
        return s.toString();
    }

    public static boolean canInstall(Context c) {
        return Build.VERSION.SDK_INT < 26 || c.getPackageManager().canRequestPackageInstalls();
    }

    public static void requestInstallPermission(Activity a) {
        if (Build.VERSION.SDK_INT >= 26) {
            Intent i=new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:"+a.getPackageName()));
            a.startActivityForResult(i,INSTALL_PERMISSION_REQUEST);
        }
    }

    public static void install(Activity a, File apk) throws Exception {
        if (apk == null || !apk.isFile() || apk.length() <= 0) {
            throw new java.io.IOException("Update APK is missing or empty");
        }
        Uri uri = FileProvider.getUriForFile(a, a.getPackageName() + ".fileprovider", apk);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(android.content.ClipData.newUri(
            a.getContentResolver(), "SIFAT Browser update", uri));
        a.startActivity(intent);
    }
}
