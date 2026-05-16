package io.github.nezumi0627.lemon.analysis;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.github.nezumi0627.lemon.constants.LemonConstants;
import io.github.nezumi0627.lemon.utils.Logger;

/**
 * 動的解析・キャッシュから得た難読化名をランタイムで保持するシングルトンレジストリ。
 *
 * <p>起動フロー:
 * <pre>
 *   LemonEntry.handleLoadPackage()
 *     → VersionUpdateCoordinator.onLoadPackage()   ← キャッシュ読み込みのみ（高速）
 *   LemonEntry → Application.onCreate()
 *     → VersionUpdateCoordinator.onApplicationCreate()
 *         → アップデート検知時: DynamicAnalyzer.analyze() → ObfuscationCache.save()
 *     → ObfuscationRegistry.init(entries)          ← レジストリ初期化
 *   各フックは ObfuscationRegistry.get(KEY) でクラス名を取得
 * </pre>
 *
 * <p>キャッシュにないキーは {@link #getFallback(String)} で
 * LemonConstants の定数値（旧バージョン向けハードコード値）にフォールバックする。
 */
public final class ObfuscationRegistry {

    private static volatile Map<String, String> entries = Collections.emptyMap();

    private ObfuscationRegistry() {}

    /**
     * レジストリを初期化する。Application.onCreate() 後に呼ぶこと。
     *
     * @param resolvedEntries {@link ObfuscationCache#load} または
     *                        {@link DynamicAnalyzer#analyze} の結果
     */
    public static synchronized void init(Map<String, String> resolvedEntries) {
        if (resolvedEntries == null) {
            entries = Collections.emptyMap();
        } else {
            entries = new HashMap<>(resolvedEntries);
        }
        Logger.i("ObfuscationRegistry: 初期化完了 entries=" + entries.size());
        if (Logger.isDebugEnabled()) {
            for (Map.Entry<String, String> e : entries.entrySet()) {
                Logger.d("  " + e.getKey() + " = " + e.getValue());
            }
        }
    }

    /**
     * 指定キーの難読化名を返す。
     * キャッシュに存在しない場合は {@link #getFallback(String)} を返す。
     */
    public static String get(String key) {
        String val = entries.get(key);
        if (val != null && !val.isEmpty()) return val;
        return getFallback(key);
    }

    /**
     * 現在のエントリセットに指定キーが存在するか（空文字でないか）を返す。
     */
    public static boolean has(String key) {
        String val = entries.get(key);
        return val != null && !val.isEmpty();
    }

    /**
     * レジストリが空（未初期化またはすべてフォールバック依存）かを返す。
     */
    public static boolean isEmpty() {
        return entries.isEmpty();
    }

    // -----------------------------------------------------------------------
    // フォールバック（LemonConstants のハードコード値）
    // -----------------------------------------------------------------------

    /**
     * キャッシュにないキーに対して LemonConstants の定数値を返す。
     * 対応するフォールバックがなければ空文字を返す。
     */
    public static String getFallback(String key) {
        switch (key) {
            // ProfileManager 系（v26.6.1 実績値）
            case ObfuscationCache.KEY_PROFILE_HOLDER_CLASS:
                return "h13.b";
            case ObfuscationCache.KEY_PROFILE_HOLDER_FIELD:
                return "J3";
            case ObfuscationCache.KEY_PROFILE_MANAGER_CLASS:
                return "g50.f";
            case ObfuscationCache.KEY_PROFILE_MANAGER_GET_METHOD:
                return "a";
            case ObfuscationCache.KEY_PROFILE_MANAGER_PARAM_CLASS:
                return "g50.a";
            case ObfuscationCache.KEY_PROFILE_MID_FIELD:
                return "b";

            // ReadReceiptManager 系
            case ObfuscationCache.KEY_RR_MANAGER_CLASS:
                return LemonConstants.RR_MANAGER_CLASS_DEFAULT;
            case ObfuscationCache.KEY_RR_SEND_METHOD:
                return LemonConstants.RR_SEND_METHOD_DEFAULT;
            case ObfuscationCache.KEY_RR_EXEC_ASYNC_METHOD:
                return LemonConstants.RR_EXEC_ASYNC_METHOD_DEFAULT;
            case ObfuscationCache.KEY_RR_READ_ALL_METHOD:
                return LemonConstants.RR_READ_ALL_METHOD_DEFAULT;

            // TalkClient 系
            case ObfuscationCache.KEY_TALK_CLIENT_THRIFT_DISPATCH:
                return LemonConstants.RR_THRIFT_DISPATCH_DEFAULT;
            case ObfuscationCache.KEY_TALK_CLIENT_SEND_MESSAGE:
                return LemonConstants.RR_SEND_MESSAGE_DEFAULT;

            // BadgeClear 系
            case ObfuscationCache.KEY_BADGE_CLEAR_CLASS:
                return LemonConstants.RR_BADGE_CLEAR_CLASS_DEFAULT;
            case ObfuscationCache.KEY_BADGE_CLEAR_METHOD:
                return LemonConstants.RR_BADGE_CLEAR_METHOD_DEFAULT;

            default:
                return "";
        }
    }
}
