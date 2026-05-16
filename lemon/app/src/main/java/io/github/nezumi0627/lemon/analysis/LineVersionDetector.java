package io.github.nezumi0627.lemon.analysis;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import io.github.nezumi0627.lemon.constants.LemonConstants;
import io.github.nezumi0627.lemon.utils.Logger;

/**
 * LINE アプリのバージョン検知と APK パス解決を担当するクラス。
 *
 * <p>アップデート検知フロー:
 * <pre>
 *   LEMON起動
 *     ↓
 *   getInstalledVersion()  →  前回キャッシュと比較
 *     ↓ 変化あり
 *   isUpdateDetected() == true
 *     ↓
 *   DynamicAnalyzer を起動して難読化名を再解析
 *     ↓
 *   ObfuscationCache に保存
 *     ↓
 *   次回以降はキャッシュを使用
 * </pre>
 */
public final class LineVersionDetector {

    private LineVersionDetector() {}

    /**
     * インストール済み LINE のバージョン文字列を返す。
     * 取得できなければ {@code null}。
     */
    public static String getInstalledVersion(Context context) {
        if (context == null) return null;
        try {
            PackageInfo pi = context.getPackageManager()
                    .getPackageInfo(LemonConstants.TARGET_PACKAGE, 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.w("LineVersionDetector: LINE がインストールされていません");
            return null;
        } catch (Throwable t) {
            Logger.e("LineVersionDetector: バージョン取得失敗", t);
            return null;
        }
    }

    /**
     * インストール済み LINE の APK パスを返す。
     * 取得できなければ {@code null}。
     */
    public static String getApkPath(Context context) {
        if (context == null) return null;
        try {
            PackageInfo pi = context.getPackageManager()
                    .getPackageInfo(LemonConstants.TARGET_PACKAGE, 0);
            return pi.applicationInfo.sourceDir;
        } catch (Throwable t) {
            Logger.e("LineVersionDetector: APK パス取得失敗", t);
            return null;
        }
    }

    /**
     * 前回解析時のバージョンと現在のバージョンを比較し、
     * アップデートが検知された場合は {@code true} を返す。
     *
     * @param context    コンテキスト
     * @param cachedVersion キャッシュに保存されている前回解析バージョン（null なら初回扱い）
     * @return アップデート検知 or 初回なら true
     */
    public static boolean isUpdateDetected(Context context, String cachedVersion) {
        String current = getInstalledVersion(context);
        if (current == null) return false;
        if (cachedVersion == null || cachedVersion.isEmpty()) {
            Logger.i("LineVersionDetector: 初回解析 (LINE " + current + ")");
            return true;
        }
        boolean updated = !current.equals(cachedVersion);
        if (updated) {
            Logger.i("LineVersionDetector: バージョン変化を検知 "
                    + cachedVersion + " → " + current);
        }
        return updated;
    }

    /**
     * LemonConstants に定義されているターゲットバージョンと
     * 現在インストール済みバージョンが一致するかを返す。
     */
    public static boolean isKnownVersion(Context context) {
        String current = getInstalledVersion(context);
        return LemonConstants.TARGET_LINE_VERSION.equals(current);
    }
}
