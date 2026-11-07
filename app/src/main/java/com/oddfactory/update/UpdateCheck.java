package com.oddfactory.update;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.view.View;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import javax.net.ssl.HttpsURLConnection;

public final class UpdateCheck {
    private static final String FEED =
            "https://drive.google.com/uc?export=download&id=1-HgFR-PJqhYTYb7BEZlYJ3vfdrjWmGAi";
    private static final int MAX_BYTES = 16 * 1024;
    private static final int TIMEOUT_MS = 6000;
    private static final long AUTO_INTERVAL_MS = 2 * 60_000L;

    private final Activity activity;
    private final Consumer<View> applyFont;
    private long lastRequest = -AUTO_INTERVAL_MS;
    private boolean foreground;
    private boolean running;
    private boolean manual;
    private AlertDialog dialog;

    public UpdateCheck(Activity activity, Consumer<View> applyFont) {
        this.activity = activity;
        this.applyFont = applyFont;
    }

    public void resume() {
        foreground = true;
        check(false);
    }

    public void pause() {
        foreground = false;
        manual = false;
        if (dialog != null) dialog.dismiss();
    }

    public void checkNow() {
        check(true);
    }

    private void check(boolean requested) {
        if (!foreground || dialog != null) return;
        long now = SystemClock.elapsedRealtime();
        if (!requested && now - lastRequest < AUTO_INTERVAL_MS) return;
        if (requested && !manual) {
            manual = true;
            toast("새 버전을 확인하고 있어요.");
        }
        if (running) return;
        running = true;
        lastRequest = now;
        String packageName = activity.getPackageName();
        new Thread(() -> {
            Release release = null;
            boolean failed = false;
            try {
                android.content.pm.PackageInfo info = activity.getPackageManager()
                        .getPackageInfo(packageName, 0);
                long installed = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
                release = select(readFeed(), packageName, installed);
            } catch (Exception e) {
                failed = true;
            }
            Release result = release;
            boolean error = failed;
            activity.runOnUiThread(() -> deliver(result, error));
        }, "update-check").start();
    }

    private void deliver(Release release, boolean failed) {
        running = false;
        boolean requested = manual;
        manual = false;
        if (!foreground || activity.isFinishing() || activity.isDestroyed()) return;
        if (failed) {
            if (requested) toast("업데이트를 확인할 수 없어요. 잠시 후 다시 시도해 주세요.");
            return;
        }
        if (release == null) {
            if (requested) toast("최신 버전을 사용 중이에요.");
            return;
        }
        if (Build.VERSION.SDK_INT < release.minSdk) {
            if (requested) toast("새 버전은 더 최신 Android가 필요해요.");
            return;
        }
        if (!requested && !activity.hasWindowFocus()) {
            lastRequest = -AUTO_INTERVAL_MS;
            return;
        }
        dialog = new AlertDialog.Builder(activity)
                .setTitle("새 버전이 있어요")
                .setMessage(release.versionName + " 버전을 Google 드라이브에서 받을 수 있어요.")
                .setPositiveButton("다운로드", (d, w) -> openDrive(release))
                .setNegativeButton("나중에", null)
                .create();
        dialog.setOnDismissListener(d -> dialog = null);
        dialog.show();
        applyFont.accept(dialog.getWindow().getDecorView());
    }

    private void openDrive(Release release) {
        Uri.Builder uri = new Uri.Builder().scheme("https").authority("drive.google.com")
                .appendPath("uc").appendQueryParameter("export", "download")
                .appendQueryParameter("id", release.fileId);
        if (!release.resourceKey.isEmpty()) uri.appendQueryParameter("resourcekey", release.resourceKey);
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, uri.build()));
        } catch (ActivityNotFoundException e) {
            toast("다운로드 링크를 열 앱이 없어요.");
        }
    }

    private void toast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    static Release select(String json, String packageName, long installed) throws JSONException {
        JSONObject feed = new JSONObject(json);
        if (feed.getInt("schema") != 1) throw new JSONException("Unsupported update list");
        JSONObject apps = feed.getJSONObject("apps");
        if (!apps.has(packageName)) throw new JSONException("Missing app");
        JSONObject app = apps.getJSONObject(packageName);
        int versionCode = app.getInt("versionCode");
        if (versionCode <= 0) throw new JSONException("Invalid version code");
        if (versionCode <= installed) return null;
        String version = app.getString("versionName");
        String fileId = app.getString("fileId");
        String resourceKey = app.optString("resourceKey", "");
        int minSdk = app.getInt("minSdk");
        if (!version.matches("[0-9A-Za-z][0-9A-Za-z.+-]{0,39}")
                || !fileId.matches("[A-Za-z0-9_-]{12,150}")
                || !resourceKey.matches("[A-Za-z0-9_-]{0,150}")
                || minSdk < 26 || minSdk > 100) {
            throw new JSONException("Invalid update listing");
        }
        return new Release(version, fileId, resourceKey, minSdk);
    }

    private static String readFeed() throws IOException {
        URL url = new URL(FEED);
        long deadline = SystemClock.elapsedRealtime() + TIMEOUT_MS;
        for (int hop = 0; hop < 4; hop++) {
            String host = url.getHost();
            if (!"https".equals(url.getProtocol())
                    || !("drive.google.com".equals(host) || "drive.usercontent.google.com".equals(host))
                    || url.getUserInfo() != null || (url.getPort() != -1 && url.getPort() != 443)) {
                throw new IOException("Unexpected Drive redirect");
            }
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            try {
                connection.setInstanceFollowRedirects(false);
                connection.setConnectTimeout(remaining(deadline));
                connection.setReadTimeout(remaining(deadline));
                connection.setUseCaches(false);
                connection.setRequestProperty("Cache-Control", "no-cache");
                int status = connection.getResponseCode();
                if (status >= 300 && status < 400) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Missing Drive redirect");
                    url = new URL(url, location);
                    continue;
                }
                if (status != 200 || connection.getContentLength() > MAX_BYTES) {
                    throw new IOException("Invalid update list response");
                }
                try (InputStream stream = connection.getInputStream()) {
                    ByteArrayOutputStream data = new ByteArrayOutputStream();
                    byte[] block = new byte[2048];
                    int length;
                    while ((length = stream.read(block)) != -1) {
                        remaining(deadline);
                        if (data.size() + length > MAX_BYTES) throw new IOException("Update list too large");
                        data.write(block, 0, length);
                    }
                    String value = new String(data.toByteArray(), StandardCharsets.UTF_8);
                    return value.startsWith("\uFEFF") ? value.substring(1) : value;
                }
            } finally {
                connection.disconnect();
            }
        }
        throw new IOException("Too many Drive redirects");
    }

    private static int remaining(long deadline) throws IOException {
        long left = deadline - SystemClock.elapsedRealtime();
        if (left <= 0) throw new IOException("Update check timed out");
        return (int) left;
    }

    static final class Release {
        final String versionName;
        final String fileId;
        final String resourceKey;
        final int minSdk;

        Release(String versionName, String fileId, String resourceKey, int minSdk) {
            this.versionName = versionName;
            this.fileId = fileId;
            this.resourceKey = resourceKey;
            this.minSdk = minSdk;
        }
    }
}
