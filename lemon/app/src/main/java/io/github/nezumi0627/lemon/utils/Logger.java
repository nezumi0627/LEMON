package io.github.nezumi0627.lemon.utils;

import android.util.Log;
import de.robv.android.xposed.XposedBridge;
import io.github.nezumi0627.lemon.constants.LemonConstants;

/**
 * 統一ログユーティリティ。
 * すべてのログ出力はこのクラスを経由する。
 */
public final class Logger {

    private Logger() {}

    public static void i(String msg) {
        Log.i(LemonConstants.LOG_TAG, msg);
        XposedBridge.log(LemonConstants.LOG_TAG + ": " + msg);
    }

    public static void e(String msg) {
        Log.e(LemonConstants.LOG_TAG, msg);
        XposedBridge.log(LemonConstants.LOG_TAG + " [ERROR]: " + msg);
    }

    public static void w(String msg) {
        Log.w(LemonConstants.LOG_TAG, msg);
        XposedBridge.log(LemonConstants.LOG_TAG + " [WARN]: " + msg);
    }

    public static void e(String msg, Throwable t) {
        Log.e(LemonConstants.LOG_TAG, msg, t);
        XposedBridge.log(LemonConstants.LOG_TAG + " [ERROR]: " + msg + " → " + t.toString());
    }

    public static void d(String msg) {
        Log.d(LemonConstants.LOG_TAG, msg);
    }

    /** デバッグログが有効かどうか（Log.isLoggable で判定）。 */
    public static boolean isDebugEnabled() {
        return Log.isLoggable(LemonConstants.LOG_TAG, Log.DEBUG);
    }
}
