package io.github.nezumi0627.lemon.utils;

import android.content.Context;
import io.github.nezumi0627.lemon.constants.LemonConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ThemeStockManager {
    private static final String KEY_THEME_STOCKS_JSON = "theme_stocks_json";
    private static final String KEY_THEME_ACTIVE_STOCK_ID = "theme_active_stock_id";
    private static final Pattern UUID_PATTERN = Pattern.compile("([a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})");
    private static final Pattern VERSION_PATTERN = Pattern.compile("V\\s*([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);

    private ThemeStockManager() {}

    public static final class ThemeStock {
        public String themeId;
        public String name;
        public String description;
        public String thumbnailUrl;
        public String storeUrl;
        public String author;
        public int currentVersion;
        public int previousVersion;
        public long updatedAt;
    }

    public static String extractThemeId(String input) {
        if (input == null) return null;
        Matcher matcher = UUID_PATTERN.matcher(input);
        if (!matcher.find()) return null;
        return matcher.group(1).toLowerCase(Locale.US);
    }

    public static ThemeStock fetchStockFromInput(String input) throws Exception {
        String themeId = extractThemeId(input);
        if (themeId == null) {
            throw new Exception("テーマIDを検出できませんでした");
        }

        ThemeStock stock = new ThemeStock();
        stock.themeId = themeId;
        stock.storeUrl = normalizeStoreUrl(input, themeId);
        stock.updatedAt = System.currentTimeMillis();

        if (input != null && input.startsWith("http")) {
            String html = downloadText(input);
            stock.name = parseMetaContent(html, "property=\"og:title\"");
            if (stock.name != null) {
                stock.name = stock.name.replace(" - LINE 着せかえ | LINE STORE", "").trim();
            }
            stock.description = parseMetaContent(html, "name=\"description\"");
            stock.thumbnailUrl = parseMetaContent(html, "property=\"og:image\"");
            stock.author = parseByDataTest(html, "theme-author");
            stock.currentVersion = parseVersionFromHtml(html);
        }

        int latestVersion = ThemeDownloader.getLatestVersion(themeId);
        if (latestVersion > 0) {
            stock.currentVersion = latestVersion;
        } else if (stock.currentVersion <= 0) {
            stock.currentVersion = 1;
        }
        if (stock.thumbnailUrl == null || stock.thumbnailUrl.isEmpty()) {
            stock.thumbnailUrl = ThemeDownloader.buildStoreIconUrl(stock.themeId, stock.currentVersion);
        }
        if (stock.name == null || stock.name.isEmpty()) {
            stock.name = "Theme " + stock.themeId.substring(0, 8);
        }
        if (stock.description == null) {
            stock.description = "";
        }
        if (stock.author == null) {
            stock.author = "";
        }
        return stock;
    }

    public static synchronized List<ThemeStock> getStocks(Context context) {
        List<ThemeStock> result = new ArrayList<>();
        String json = LemonSettings.getString(context, KEY_THEME_STOCKS_JSON, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                ThemeStock stock = new ThemeStock();
                stock.themeId = obj.optString("themeId", "");
                stock.name = obj.optString("name", "");
                stock.description = obj.optString("description", "");
                stock.thumbnailUrl = obj.optString("thumbnailUrl", "");
                stock.storeUrl = obj.optString("storeUrl", "");
                stock.author = obj.optString("author", "");
                stock.currentVersion = obj.optInt("currentVersion", 0);
                stock.previousVersion = obj.optInt("previousVersion", 0);
                stock.updatedAt = obj.optLong("updatedAt", 0L);
                if (!stock.themeId.isEmpty()) {
                    result.add(stock);
                }
            }
        } catch (Throwable ignored) {}
        return result;
    }

    public static synchronized void upsertStock(Context context, ThemeStock incoming) {
        List<ThemeStock> stocks = getStocks(context);
        boolean replaced = false;
        for (ThemeStock stock : stocks) {
            if (stock.themeId.equalsIgnoreCase(incoming.themeId)) {
                incoming.previousVersion = stock.currentVersion;
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            incoming.previousVersion = 0;
        } else {
            removeStockInternal(stocks, incoming.themeId);
        }
        stocks.add(0, incoming);
        saveStocks(context, stocks);
    }

    public static synchronized void deleteStock(Context context, String themeId) {
        List<ThemeStock> stocks = getStocks(context);
        removeStockInternal(stocks, themeId);
        saveStocks(context, stocks);
        String active = getActiveStockId(context);
        if (active != null && active.equalsIgnoreCase(themeId)) {
            setActiveStockId(context, "");
        }
    }

    public static synchronized ThemeStock getStock(Context context, String themeId) {
        for (ThemeStock stock : getStocks(context)) {
            if (stock.themeId.equalsIgnoreCase(themeId)) return stock;
        }
        return null;
    }

    public static synchronized void setActiveStockId(Context context, String themeId) {
        LemonSettings.setString(context, KEY_THEME_ACTIVE_STOCK_ID, themeId == null ? "" : themeId);
    }

    public static synchronized String getActiveStockId(Context context) {
        return LemonSettings.getString(context, KEY_THEME_ACTIVE_STOCK_ID, "");
    }

    private static void removeStockInternal(List<ThemeStock> stocks, String themeId) {
        for (int i = stocks.size() - 1; i >= 0; i--) {
            if (stocks.get(i).themeId.equalsIgnoreCase(themeId)) {
                stocks.remove(i);
            }
        }
    }

    private static void saveStocks(Context context, List<ThemeStock> stocks) {
        JSONArray arr = new JSONArray();
        for (ThemeStock stock : stocks) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("themeId", stock.themeId);
                obj.put("name", stock.name);
                obj.put("description", stock.description);
                obj.put("thumbnailUrl", stock.thumbnailUrl);
                obj.put("storeUrl", stock.storeUrl);
                obj.put("author", stock.author);
                obj.put("currentVersion", stock.currentVersion);
                obj.put("previousVersion", stock.previousVersion);
                obj.put("updatedAt", stock.updatedAt);
                arr.put(obj);
            } catch (Throwable ignored) {}
        }
        LemonSettings.setString(context, KEY_THEME_STOCKS_JSON, arr.toString());
    }

    private static String normalizeStoreUrl(String input, String themeId) {
        if (input != null && input.startsWith("http")) {
            return input;
        }
        return "https://store.line.me/themeshop/product/" + themeId + "/ja";
    }

    private static int parseVersionFromHtml(String html) {
        Matcher matcher = VERSION_PATTERN.matcher(html);
        if (!matcher.find()) return 0;
        String raw = matcher.group(1);
        try {
            if (raw.contains(".")) {
                return Integer.parseInt(raw.substring(raw.indexOf('.') + 1));
            }
            return Integer.parseInt(raw);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static String parseMetaContent(String html, String marker) {
        String token = "<meta " + marker;
        int idx = html.indexOf(token);
        if (idx < 0) return null;
        int contentIdx = html.indexOf("content=\"", idx);
        if (contentIdx < 0) return null;
        int start = contentIdx + "content=\"".length();
        int end = html.indexOf("\"", start);
        if (end <= start) return null;
        return html.substring(start, end).trim();
    }

    private static String parseByDataTest(String html, String dataTest) {
        String marker = "data-test=\"" + dataTest + "\"";
        int idx = html.indexOf(marker);
        if (idx < 0) return null;
        int gt = html.indexOf(">", idx);
        if (gt < 0) return null;
        int end = html.indexOf("<", gt + 1);
        if (end < 0) return null;
        return html.substring(gt + 1, end).trim();
    }

    private static String downloadText(String urlStr) throws Exception {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlStr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(20000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");
            connection.connect();
            int code = connection.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) {
                throw new Exception("ページ取得に失敗しました: HTTP " + code);
            }

            try (BufferedInputStream in = new BufferedInputStream(connection.getInputStream());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = in.read(buf)) != -1) {
                    out.write(buf, 0, read);
                }
                return out.toString("UTF-8");
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
