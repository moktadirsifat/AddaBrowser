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
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UpdateManager {
    public static final int INSTALL_PERMISSION_REQUEST = 6105;
    private static final String METADATA_URL = "https://raw.githubusercontent.com/moktadirsifat/AddaBrowser/main/update.json";
    private final Context context;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public UpdateManager(Context context) { this.context = context.getApplicationContext(); }

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
    }

    public void check(Listener listener) {
        worker.execute(() -> {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) new URL(METADATA_URL).openConnection();
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(20000);
                conn.setRequestProperty("Accept", "application/json");
                if (conn.getResponseCode() != HttpURLConnection.HTTP_OK || !"https".equalsIgnoreCase(conn.getURL().getProtocol())) throw new IOException("Metadata request failed");
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
                validateHttpsUrl(apk);
                if (name.isEmpty() || name.length() > 64 || notes.length() > 4000
                    || hash.length() != 64 || !hash.matches("[0-9a-f]{64}")) {
                    throw new IOException("Invalid release metadata");
                }
                Release release = new Release((int) parsedCode, name, notes, apk, hash);
                main.post(() -> listener.available(release));
            } catch (Exception ex) {
                main.post(() -> listener.error("আপডেট খোঁজা যায়নি। ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।"));
            } finally { if (conn != null) conn.disconnect(); }
        });
    }

    public void download(Release release, Listener listener) {
        worker.execute(() -> {
            HttpURLConnection conn = null;
            File apk = null;
            try {
                validateHttpsUrl(release.apkUrl);
                URL url = new URL(release.apkUrl);
                conn = (HttpURLConnection) url.openConnection();
                conn.setInstanceFollowRedirects(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(30000);
                conn.setRequestProperty("Accept", "application/vnd.android.package-archive, application/octet-stream");
                int responseCode = conn.getResponseCode();
                URL finalUrl = conn.getURL();
                validateHttpsUrl(finalUrl.toString());
                if (responseCode < 200 || responseCode >= 300) throw new IOException("Download failed");
                long declaredLength = conn.getContentLength();
                if (declaredLength == 0 || declaredLength > 150L * 1024L * 1024L) {
                    throw new IOException("Invalid update package size");
                }
                File dir = new File(context.getCacheDir(),"updates");
                if (!dir.exists() && !dir.mkdirs()) throw new IOException("Storage unavailable");
                apk = new File(dir,"sifat-browser-update.apk");
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                long length = declaredLength, received=0;
                try (InputStream in = new BufferedInputStream(conn.getInputStream());
                     FileOutputStream out = new FileOutputStream(apk)) {
                    byte[] buf = new byte[16384]; int n;
                    while ((n=in.read(buf))!=-1) {
                        received+=n;
                        if (received > 150L*1024L*1024L) throw new IOException("Update package is too large");
                        out.write(buf,0,n); digest.update(buf,0,n);
                        if (length > 0) {
                            final int p=(int)Math.min(99,received*100/length);
                            main.post(() -> listener.progress(p));
                        }
                    }
                    out.getFD().sync();
                }
                if (!hex(digest.digest()).equalsIgnoreCase(release.sha256))
                    throw new SecurityException("APK integrity check failed.");
                verifySameSignature(apk);
                File ready=apk;
                main.post(() -> { listener.progress(100); listener.downloaded(ready); });
            } catch (Exception ex) {
                if (apk != null) apk.delete();
                String message = ex instanceof SecurityException
                    ? ex.getMessage()
                    : "ডাউনলোড সম্পন্ন হয়নি। পরে আবার চেষ্টা করুন।";
                main.post(() -> listener.error(message));
            } finally { if (conn != null) conn.disconnect(); }
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
        if (value == null || value.trim().isEmpty() || value.length() > 2048) {
            throw new IOException("Invalid update URL");
        }
        java.net.URI uri = new java.net.URI(value).normalize();
        if (!"https".equalsIgnoreCase(uri.getScheme())
            || uri.getHost() == null || uri.getHost().isEmpty()
            || uri.getUserInfo() != null || uri.getFragment() != null) {
            throw new IOException("Update URL must be a valid HTTPS URL");
        }
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
        Uri uri=FileProvider.getUriForFile(a,a.getPackageName()+".fileprovider",apk);
        Intent i=new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(uri,"application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        a.startActivity(i);
    }
}
