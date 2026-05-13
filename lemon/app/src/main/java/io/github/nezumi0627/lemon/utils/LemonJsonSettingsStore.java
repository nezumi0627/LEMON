package io.github.nezumi0627.lemon.utils;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import de.robv.android.xposed.XSharedPreferences;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import io.github.nezumi0627.lemon.constants.LemonConstants;

/**
 * LEMON 設定の共通ストレージ。デフォルトは内部ストレージ上の JSON ファイル。
 * 保存先はモジュールの {@link LemonConstants#PREF_BOOTSTRAP_NAME} でパス上書き可能。
 */
public final class LemonJsonSettingsStore {
    private LemonJsonSettingsStore() {}

    private static final Object LOCK = new Object();
    private static final String MODULE_PACKAGE = "io.github.nezumi0627.lemon";

    private static volatile JSONObject sMemory;
    private static volatile long sLoadedMtime = Long.MIN_VALUE;
    private static volatile String sResolvedPath;

    public static File resolveSettingsFile(Context context) {
        String override = readBootstrapPathOverride(context);
        if (override != null && !override.trim().isEmpty()) {
            return new File(override.trim());
        }
        // 内部ストレージを使用
        File root = context != null ? context.getFilesDir() : new File("/data/data/io.github.nezumi0627.lemon/files");
        return new File(root, LemonConstants.DEFAULT_SETTINGS_SUBDIR + "/" + LemonConstants.DEFAULT_SETTINGS_FILENAME);
    }

    /** モジュール UI から保存先の絶対パスを設定（空でデフォルトに戻す）。 */
    public static void setSettingsJsonPathOverride(Context moduleContext, String absolutePathOrEmpty) {
        if (moduleContext == null) return;
        String pkg = moduleContext.getPackageName();
        if (!MODULE_PACKAGE.equals(pkg)) return;
        SharedPreferences sp = moduleContext.getApplicationContext()
                .getSharedPreferences(LemonConstants.PREF_BOOTSTRAP_NAME, Context.MODE_PRIVATE);
        if (absolutePathOrEmpty == null || absolutePathOrEmpty.trim().isEmpty()) {
            sp.edit().remove(LemonConstants.KEY_SETTINGS_JSON_PATH).apply();
        } else {
            sp.edit().putString(LemonConstants.KEY_SETTINGS_JSON_PATH, absolutePathOrEmpty.trim()).apply();
        }
        synchronized (LOCK) {
            sMemory = null;
            sLoadedMtime = Long.MIN_VALUE;
            sResolvedPath = null;
        }
    }

    public static String getSettingsJsonPathOverride(Context context) {
        return readBootstrapPathOverride(context);
    }

    public static String getEffectiveSettingsPath(Context context) {
        return resolveSettingsFile(context).getAbsolutePath();
    }

    private static String readBootstrapPathOverride(Context context) {
        try {
            if (context != null && MODULE_PACKAGE.equals(context.getPackageName())) {
                return context.getApplicationContext()
                        .getSharedPreferences(LemonConstants.PREF_BOOTSTRAP_NAME, Context.MODE_PRIVATE)
                        .getString(LemonConstants.KEY_SETTINGS_JSON_PATH, "");
            }
        } catch (Throwable ignored) {}
        try {
            XSharedPreferences x = new XSharedPreferences(MODULE_PACKAGE, LemonConstants.PREF_BOOTSTRAP_NAME);
            x.reload();
            return x.getString(LemonConstants.KEY_SETTINGS_JSON_PATH, "");
        } catch (Throwable ignored) {
            return "";
        }
    }

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        synchronized (LOCK) {
            JSONObject root = loadJson(context);
            if (!root.has(key)) return defaultValue;
            try {
                Object v = root.get(key);
                if (v instanceof Boolean) return (Boolean) v;
                if (v instanceof String) return Boolean.parseBoolean((String) v);
                if (v instanceof Number) return ((Number) v).intValue() != 0;
            } catch (Exception ignored) {}
            return defaultValue;
        }
    }

    public static void putBoolean(Context context, String key, boolean value) {
        synchronized (LOCK) {
            JSONObject root = loadJson(context);
            try {
                root.put(key, value);
            } catch (Exception ignored) {}
            saveJson(context, root);
        }
    }

    public static String getString(Context context, String key, String defaultValue) {
        synchronized (LOCK) {
            JSONObject root = loadJson(context);
            if (!root.has(key)) return defaultValue;
            try {
                Object v = root.get(key);
                if (v == null || JSONObject.NULL.equals(v)) return defaultValue;
                return String.valueOf(v);
            } catch (Exception e) {
                return defaultValue;
            }
        }
    }

    public static void putString(Context context, String key, String value) {
        synchronized (LOCK) {
            JSONObject root = loadJson(context);
            try {
                root.put(key, value == null ? "" : value);
            } catch (Exception ignored) {}
            saveJson(context, root);
        }
    }

    /** フックプロセスなど Context が無い場合。既定の内部保存先パスで解決する。 */
    public static boolean getBooleanNoContext(String key, boolean defaultValue) {
        return getBoolean(null, key, defaultValue);
    }

    public static String getStringNoContext(String key, String defaultValue) {
        return getString(null, key, defaultValue);
    }

    public static void putStringNoContext(String key, String value) {
        putString(null, key, value);
    }

    private static JSONObject loadJson(Context context) {
        File f = resolveSettingsFile(context);
        String path = f.getAbsolutePath();
        if (sMemory != null && path.equals(sResolvedPath)) {
            long m = f.lastModified();
            if (m == sLoadedMtime && f.exists()) return sMemory;
        }
        JSONObject root = new JSONObject();
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) {
                byte[] buf = readAll(in);
                if (buf.length > 0) {
                    root = new JSONObject(new String(buf, StandardCharsets.UTF_8));
                }
            } catch (Exception ignored) {}
        } else {
            tryMigrateFromLegacy(context, root);
            if (root.length() > 0) {
                saveJson(context, root);
            }
        }
        sMemory = root;
        sResolvedPath = path;
        sLoadedMtime = f.exists() ? f.lastModified() : 0L;
        return root;
    }

    private static void saveJson(Context context, JSONObject root) {
        File f = resolveSettingsFile(context);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) {
            // noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
        byte[] data = root.toString().getBytes(StandardCharsets.UTF_8);
        try (FileOutputStream out = new FileOutputStream(tmp)) {
            out.write(data);
            out.flush();
        } catch (Exception ignored) {
            return;
        }
        // noinspection ResultOfMethodCallIgnored
        if (f.exists()) {
            // noinspection ResultOfMethodCallIgnored
            f.delete();
        }
        // noinspection ResultOfMethodCallIgnored
        tmp.renameTo(f);
        sMemory = root;
        sResolvedPath = f.getAbsolutePath();
        sLoadedMtime = f.exists() ? f.lastModified() : 0L;
    }

    private static void tryMigrateFromLegacy(Context context, JSONObject target) {
        try {
            XSharedPreferences x = new XSharedPreferences(MODULE_PACKAGE, LemonConstants.PREF_NAME);
            x.reload();
            mergePrefsMapIntoJson(x.getAll(), target);
        } catch (Throwable ignored) {
            try {
                XSharedPreferences x = new XSharedPreferences(MODULE_PACKAGE, LemonConstants.PREF_NAME);
                x.reload();
                mergeFromXSharedPreferencesByGetters(x, target);
            } catch (Throwable ignored2) {}
        }
        try {
            if (context != null) {
                SharedPreferences sp = context.getApplicationContext()
                        .getSharedPreferences(LemonConstants.PREF_NAME, Context.MODE_PRIVATE);
                mergePrefsMapIntoJson(sp.getAll(), target);
            }
        } catch (Throwable ignored) {}
        try {
            Application app = android.app.AndroidAppHelper.currentApplication();
            if (app != null) {
                SharedPreferences sp = app.getSharedPreferences(LemonConstants.PREF_NAME, Context.MODE_PRIVATE);
                mergePrefsMapIntoJson(sp.getAll(), target);
            }
        } catch (Throwable ignored) {}
    }

    /** getAll() が使えない環境向け。 */
    private static void mergeFromXSharedPreferencesByGetters(XSharedPreferences x, JSONObject target) {
        putIfAbsentBoolean(x, target, LemonConstants.KEY_AD_BLOCK, true);
        putIfAbsentBoolean(x, target, LemonConstants.KEY_READ_RECEIPT_BLOCK_ENABLED, false);
        putIfAbsentBoolean(x, target, LemonConstants.KEY_READ_RECEIPT_SEND_ON_SEND, false);
        putIfAbsentBoolean(x, target, LemonConstants.KEY_READ_RECEIPT_HEADER_BUTTON_ENABLED, true);
        putIfAbsentString(x, target, LemonConstants.KEY_READ_RECEIPT_DISABLED_CHAT_IDS, "[]");
        putIfAbsentBoolean(x, target, LemonConstants.KEY_READ_HISTORY_ENABLED, false);
        putIfAbsentString(x, target, LemonConstants.KEY_READ_HISTORY_DATA, "{}");
        putIfAbsentBoolean(x, target, LemonConstants.KEY_THEME_FORCE_ENABLED, false);
        putIfAbsentString(x, target, LemonConstants.KEY_THEME_FORCE_ID, "");
        putIfAbsentBoolean(x, target, LemonConstants.KEY_LOG_ENABLED, false);
        putIfAbsentBoolean(x, target, LemonConstants.KEY_STARTUP_TOAST_ENABLED, false);
        putIfAbsentBoolean(x, target, LemonConstants.KEY_CHAT_AI_ASSISTANT, false);
    }

    private static void putIfAbsentBoolean(XSharedPreferences x, JSONObject target, String key, boolean def) {
        try {
            if (target.has(key)) return;
            target.put(key, x.getBoolean(key, def));
        } catch (Throwable ignored) {}
    }

    private static void putIfAbsentString(XSharedPreferences x, JSONObject target, String key, String def) {
        try {
            if (target.has(key)) return;
            String v = x.getString(key, def);
            target.put(key, v != null ? v : def);
        } catch (Throwable ignored) {}
    }

    private static void mergePrefsMapIntoJson(Map<String, ?> map, JSONObject target) {
        if (map == null) return;
        for (Map.Entry<String, ?> e : map.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            try {
                if (!target.has(e.getKey())) {
                    putValue(target, e.getKey(), e.getValue());
                }
            } catch (Exception ignored) {}
        }
    }

    private static void putValue(JSONObject target, String key, Object value) throws org.json.JSONException {
        if (value instanceof Boolean) target.put(key, value);
        else if (value instanceof Integer) target.put(key, value);
        else if (value instanceof Long) target.put(key, value);
        else if (value instanceof Float) target.put(key, value);
        else if (value instanceof String) target.put(key, value);
        else target.put(key, String.valueOf(value));
    }

    private static byte[] readAll(FileInputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) >= 0) {
            bos.write(buf, 0, n);
        }
        return bos.toByteArray();
    }
}
