package io.github.nezumi0627.lemon.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ThemeDownloader {

    private static final String BASE_URI = "http://dl.shop.line.naver.jp/themeshop/v1/products";
    private static final int CONNECT_TIMEOUT_MS = 10000;
    private static final int READ_TIMEOUT_MS = 30000;
    private static final int MAX_VERSION = 100;
    private static final int BUFFER_SIZE = 8192;

    public interface DownloadCallback {
        void onProgress(int progress);
        void onSuccess(File file);
        void onError(Exception e);
    }

    public static void downloadTheme(String themeId, int version, File targetFile, DownloadCallback callback) {
        new Thread(() -> {
            try {
                File file;
                if (version > 0) {
                    String urlStr = buildUrl(themeId, version, "ANDROID/theme.zip");
                    file = downloadFile(urlStr, targetFile, callback);
                } else {
                    file = downloadThemeBlocking(themeId, targetFile, callback);
                }

                if (callback != null) {
                    callback.onSuccess(file);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        }, "LemonThemeDownloader").start();
    }

    public static File downloadThemeBlocking(String themeId, File targetFile, DownloadCallback callback) throws Exception {
        int latestVersion = findLatestVersion(themeId);
        String downloadUrl = buildUrl(themeId, latestVersion, "ANDROID/theme.zip");
        Logger.i("Theme download URL: " + downloadUrl);
        return downloadFile(downloadUrl, targetFile, callback);
    }

    public static int getLatestVersion(String themeId) throws Exception {
        return findLatestVersion(themeId);
    }

    public static String buildStoreIconUrl(String themeId, int version) {
        return buildUrl(themeId, version, "WEBSTORE/icon_198x278.png");
    }

    private static int findLatestVersion(String themeId) throws Exception {
        int latestVersion = -1;

        for (int version = 1; version <= MAX_VERSION; version++) {
            String checkUrl = buildUrl(themeId, version, "ANDROID/icon_86x123.png");
            if (urlExists(checkUrl)) {
                latestVersion = version;
                continue;
            }

            if (latestVersion > 0) {
                break;
            }
        }

        if (latestVersion < 0) {
            throw new Exception("テーマが見つかりません。テーマIDまたは配信状態を確認してください: " + themeId);
        }

        return latestVersion;
    }

    private static String buildUrl(String themeId, int version, String filePath) {
        String compactThemeId = themeId.replace("-", "");
        String subdir1 = compactThemeId.substring(0, 2);
        String subdir2 = compactThemeId.substring(2, 4);
        String subdir3 = compactThemeId.substring(4, 6);
        return BASE_URI + "/" + subdir1 + "/" + subdir2 + "/" + subdir3 + "/"
                + themeId + "/" + version + "/" + filePath;
    }

    private static boolean urlExists(String urlStr) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(CONNECT_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "Line/26.6.1 Android");
            int responseCode = connection.getResponseCode();
            return responseCode == HttpURLConnection.HTTP_OK;
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static File downloadFile(String urlStr, File targetFile, DownloadCallback callback) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", "Line/26.6.1 Android");
            connection.connect();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new Exception("サーバー応答が不正です: HTTP " + connection.getResponseCode());
            }

            File parent = targetFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new Exception("保存先ディレクトリを作成できません: " + parent.getAbsolutePath());
            }

            int fileLength = connection.getContentLength();
            try (InputStream input = new BufferedInputStream(connection.getInputStream(), BUFFER_SIZE);
                 FileOutputStream output = new FileOutputStream(targetFile)) {
                byte[] data = new byte[BUFFER_SIZE];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    output.write(data, 0, count);
                    if (callback != null && fileLength > 0) {
                        callback.onProgress((int) Math.min(100, total * 100 / fileLength));
                    }
                }
            }

            return targetFile;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
