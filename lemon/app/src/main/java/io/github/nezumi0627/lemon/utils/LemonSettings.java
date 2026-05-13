package io.github.nezumi0627.lemon.utils;

import android.content.Context;

import io.github.nezumi0627.lemon.constants.LemonConstants;

/**
 * 設定の読み書き。実体は {@link LemonJsonSettingsStore}（デフォルト: 外部ストレージの JSON）。
 */
public class LemonSettings {

    public static boolean getBoolean(Context context, String key, boolean defaultValue) {
        return LemonJsonSettingsStore.getBoolean(context, key, defaultValue);
    }

    public static void setBoolean(Context context, String key, boolean value) {
        LemonJsonSettingsStore.putBoolean(context, key, value);
    }

    public static String getString(Context context, String key, String defaultValue) {
        return LemonJsonSettingsStore.getString(context, key, defaultValue);
    }

    public static void setString(Context context, String key, String value) {
        LemonJsonSettingsStore.putString(context, key, value);
    }

    /** 現在の設定 JSON の絶対パス（表示用）。 */
    public static String getEffectiveSettingsPath(Context context) {
        return LemonJsonSettingsStore.getEffectiveSettingsPath(context);
    }

    /** モジュールからのみ。空文字でデフォルトパスに戻す。 */
    public static void setSettingsJsonPathOverride(Context moduleContext, String absolutePathOrEmpty) {
        LemonJsonSettingsStore.setSettingsJsonPathOverride(moduleContext, absolutePathOrEmpty);
    }

    /**
     * LINE プロセスなどから保存先を書くとき用。モジュールの {@link LemonConstants#PREF_BOOTSTRAP_NAME} に保存する。
     */
    public static void setSettingsJsonPathOverrideFromHostApp(Context hostContext, String absolutePathOrEmpty) {
        if (hostContext == null) return;
        try {
            Context mc = hostContext.createPackageContext(
                    "io.github.nezumi0627.lemon", Context.CONTEXT_IGNORE_SECURITY);
            setSettingsJsonPathOverride(mc, absolutePathOrEmpty);
        } catch (Exception e) {
            setSettingsJsonPathOverride(hostContext, absolutePathOrEmpty);
        }
    }

    public static String getSettingsJsonPathOverride(Context context) {
        return LemonJsonSettingsStore.getSettingsJsonPathOverride(context);
    }
}
