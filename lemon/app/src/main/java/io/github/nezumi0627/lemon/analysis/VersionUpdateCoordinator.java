package io.github.nezumi0627.lemon.analysis;

import android.content.Context;

import java.util.Map;

import io.github.nezumi0627.lemon.utils.Logger;

/**
 * バージョン検知 → 動的解析 → キャッシュ保存 → レジストリ初期化の
 * 全フローを調整するコーディネータークラス。
 *
 * <p>呼び出しポイント:
 * <pre>
 *   // handleLoadPackage タイミング（LINE バージョンのみ確認）
 *   VersionUpdateCoordinator.onLoadPackage(lpparam.appInfo.packageName);
 *
 *   // Application.onCreate() 後（フル解析・レジストリ初期化）
 *   VersionUpdateCoordinator.onApplicationCreate(context, classLoader);
 * </pre>
 */
public final class VersionUpdateCoordinator {

    /** キャッシュに保存する "前回解析済みバージョン" キー */
    private static final String PREF_KEY_ANALYZED_VERSION = "analyzed_line_version";

    /** 前回解析したバージョンをメモリに保持（Application 起動前に先読み） */
    private static volatile String cachedLineVersion = null;

    private VersionUpdateCoordinator() {}

    // -----------------------------------------------------------------------
    // handleLoadPackage タイミング
    // -----------------------------------------------------------------------

    /**
     * handleLoadPackage 完了直後に呼ぶ。
     * キャッシュの存在確認のみ行い（高速）、必要なら後続の解析フラグを立てる。
     * この時点では Context が取れないため、バージョン文字列の取得は行わない。
     */
    public static void onLoadPackage() {
        // handleLoadPackage では PackageManager が使えないため、
        // バージョン確認は onApplicationCreate に委ねる。
        Logger.d("VersionUpdateCoordinator: onLoadPackage 完了");
    }

    // -----------------------------------------------------------------------
    // Application.onCreate() タイミング
    // -----------------------------------------------------------------------

    /**
     * Application.onCreate() 後に呼ぶ。
     * バージョン変化を検知したら動的解析を実行し、キャッシュと ObfuscationRegistry を更新する。
     * 変化がなければキャッシュから高速ロードする。
     *
     * @param context     Application コンテキスト（LINE プロセス内）
     * @param classLoader LINE の ClassLoader
     */
    public static void onApplicationCreate(Context context, ClassLoader classLoader) {
        String currentVersion = LineVersionDetector.getInstalledVersion(context);
        if (currentVersion == null) {
            Logger.w("VersionUpdateCoordinator: LINE バージョン取得失敗 → フォールバック使用");
            ObfuscationRegistry.init(null);
            return;
        }

        String previousVersion = loadAnalyzedVersion(context);
        boolean needsAnalysis = LineVersionDetector.isUpdateDetected(context, previousVersion);

        Map<String, String> resolvedEntries;

        if (!needsAnalysis && ObfuscationCache.exists(currentVersion)) {
            // ---- キャッシュヒット: 解析スキップ ----
            Logger.i("VersionUpdateCoordinator: キャッシュ使用 (LINE " + currentVersion + ")");
            resolvedEntries = ObfuscationCache.load(currentVersion);
        } else {
            // ---- アップデート検知 or 初回: 動的解析実行 ----
            Logger.i("VersionUpdateCoordinator: 動的解析を開始 (LINE "
                    + previousVersion + " → " + currentVersion + ")");

            // 旧バージョンのキャッシュは削除
            if (previousVersion != null && !previousVersion.equals(currentVersion)) {
                ObfuscationCache.invalidate(previousVersion);
            }

            resolvedEntries = DynamicAnalyzer.analyze(context, classLoader);

            if (!resolvedEntries.isEmpty()) {
                ObfuscationCache.save(currentVersion, resolvedEntries);
                saveAnalyzedVersion(context, currentVersion);
                Logger.i("VersionUpdateCoordinator: 解析完了・キャッシュ保存 (LINE "
                        + currentVersion + ")");
            } else {
                Logger.w("VersionUpdateCoordinator: 解析結果が空 → フォールバック使用");
            }
        }

        cachedLineVersion = currentVersion;
        ObfuscationRegistry.init(resolvedEntries);
    }

    // -----------------------------------------------------------------------
    // 解析済みバージョンの永続化
    // -----------------------------------------------------------------------

    /**
     * キャッシュファイルから前回解析したバージョン文字列を読み込む。
     * ファイルがなければ null を返す。
     */
    private static String loadAnalyzedVersion(Context context) {
        // ObfuscationCache の JSON から取得するため、
        // 現在インストール済みバージョンのキャッシュが存在するかチェック
        String current = LineVersionDetector.getInstalledVersion(context);
        if (current == null) return null;

        // キャッシュが存在するなら、前回解析 = 現在のバージョン（変化なし）
        if (ObfuscationCache.exists(current)) return current;

        // 存在しないなら別バージョンのキャッシュを探す（旧キャッシュファイル名から逆引き）
        // 簡易実装: /sdcard/LEMON/ に obf_cache_*.json があれば、
        // その中の line_version を読む
        try {
            java.io.File dir = new java.io.File("/sdcard/LEMON/");
            if (!dir.exists()) return null;
            java.io.File[] files = dir.listFiles(
                    f -> f.getName().startsWith("obf_cache_") && f.getName().endsWith(".json"));
            if (files == null || files.length == 0) return null;

            // 最新の更新日時のファイルを選ぶ
            java.io.File latest = null;
            for (java.io.File f : files) {
                if (latest == null || f.lastModified() > latest.lastModified()) {
                    latest = f;
                }
            }
            if (latest == null) return null;

            // JSON を読んで line_version を取得
            byte[] bytes = new byte[(int) latest.length()];
            try (java.io.FileInputStream fis = new java.io.FileInputStream(latest)) {
                int read = fis.read(bytes);
                if (read != bytes.length) return null;
            }
            org.json.JSONObject json = new org.json.JSONObject(
                    new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
            return json.optString("line_version", null);
        } catch (Throwable t) {
            Logger.d("VersionUpdateCoordinator: 前回バージョン読み込み失敗: " + t.getMessage());
            return null;
        }
    }

    /**
     * 解析済みバージョンをキャッシュ JSON に含める形で保存する。
     * （ObfuscationCache.save が line_version を JSON に含めるため、別途保存は不要）
     */
    private static void saveAnalyzedVersion(Context context, String version) {
        // ObfuscationCache.save() が既に line_version を保存しているため何もしない
    }

    /** 現在のキャッシュ済み LINE バージョンを返す。 */
    public static String getCachedLineVersion() {
        return cachedLineVersion;
    }
}
