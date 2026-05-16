package io.github.nezumi0627.lemon.analysis;

import android.content.Context;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import io.github.nezumi0627.lemon.utils.Logger;

/**
 * LINE プロセス内での動的解析エンジン。
 *
 * <p>APK をデコンパイルする代わりに、すでにロード済みの ClassLoader を利用して
 * 実行時にクラス・メソッド・フィールドを探索することで難読化名を特定する。
 * アップデート検知時に一度だけ実行され、結果は {@link ObfuscationCache} に保存される。
 *
 * <p>解析戦略:
 * <ol>
 *   <li>ProfileManager 系 — シグネチャ（引数型 Context, パラメータクラス）で特定</li>
 *   <li>ReadReceiptManager 系 — 引数 (long, String, boolean) のメソッドを持つクラスを特定</li>
 *   <li>TalkClient — 完全修飾名が既知のため直接ロード確認</li>
 *   <li>BadgeClear — 引数 (String) の void メソッドを持つ短名クラスを探索</li>
 * </ol>
 *
 * <p>各探索は独立して失敗しても他に影響しない。
 * 取得できなかったエントリは旧キャッシュ値または定数フォールバックで補完される。
 */
public final class DynamicAnalyzer {

    private DynamicAnalyzer() {}

    /**
     * 動的解析を実行し、解析結果 Map を返す。
     * 各エントリが取得できなかった場合はそのキーは含まれない。
     *
     * @param context     コンテキスト（LINE プロセス内）
     * @param classLoader LINE の ClassLoader
     * @return 解析結果 Map（{@link ObfuscationCache} のキー定数 → 難読化名）
     */
    public static Map<String, String> analyze(Context context, ClassLoader classLoader) {
        Map<String, String> result = new HashMap<>();

        Logger.i("DynamicAnalyzer: 動的解析開始");

        analyzeProfileManager(context, classLoader, result);
        analyzeReadReceiptManager(classLoader, result);
        analyzeTalkClient(classLoader, result);
        analyzeBadgeClear(classLoader, result);

        Logger.i("DynamicAnalyzer: 動的解析完了 entries=" + result.size());
        return result;
    }

    // -----------------------------------------------------------------------
    // ProfileManager 系
    // -----------------------------------------------------------------------

    /**
     * ProfileManager と ProfileManagerHolder を動的に特定する。
     *
     * <p>探索手順:
     * <ol>
     *   <li>既知候補クラス名リストを順に試してロードし、
     *       静的フィールドを持つクラスが ProfileManagerHolder であると判断。</li>
     *   <li>その静的フィールドの値（ProfileManager インスタンスホルダー）を取得。</li>
     *   <li>ProfileManager クラスは (Context, holderType) を引数に取る静的メソッドを持つ
     *       クラスとして探索。</li>
     *   <li>Profile クラスの MID フィールドは getProfile() 戻り値の String フィールドを探索。</li>
     * </ol>
     */
    private static void analyzeProfileManager(Context context, ClassLoader cl,
                                               Map<String, String> result) {
        // --- Step 1: ProfileManagerHolder を探す ---
        // 候補: 短い難読化名を持つクラスに Context 型の静的フィールドや
        //       getInstance() 相当を持つものを探す。
        // 旧バージョンで実績のある候補から始め、シグネチャで確認。
        String[] holderCandidates = {
            "h13.b", "h14.b", "i13.b", "i14.b", "j13.b",
            "h12.b", "g13.b", "g14.b", "f13.b", "k13.b"
        };
        String[] holderFieldCandidates = {"J3", "I3", "K3", "H3", "L3", "M3", "N3"};

        String holderClass = null;
        String holderField = null;
        Object holderInstance = null;

        outer:
        for (String candidateClass : holderCandidates) {
            Class<?> cls = loadSafe(cl, candidateClass);
            if (cls == null) continue;
            for (String candidateField : holderFieldCandidates) {
                try {
                    Field f = cls.getDeclaredField(candidateField);
                    f.setAccessible(true);
                    if (!Modifier.isStatic(f.getModifiers())) continue;
                    Object val = f.get(null);
                    if (val != null) {
                        holderClass = candidateClass;
                        holderField = candidateField;
                        holderInstance = val;
                        Logger.d("DynamicAnalyzer: ProfileManagerHolder=" + holderClass
                                + " field=" + holderField);
                        break outer;
                    }
                } catch (Throwable ignored) {}
            }
        }

        if (holderClass != null) {
            result.put(ObfuscationCache.KEY_PROFILE_HOLDER_CLASS, holderClass);
            result.put(ObfuscationCache.KEY_PROFILE_HOLDER_FIELD, holderField);
        }

        // --- Step 2: ProfileManager クラスを探す ---
        // 特徴: static method(Context, holderType) → ProfileManager インスタンスを返す
        if (holderInstance != null) {
            Class<?> holderType = holderInstance.getClass();
            String[] pmCandidates = {
                "g50.f", "g51.f", "h50.f", "h51.f", "f50.f", "f51.f",
                "g49.f", "g52.f", "h49.f", "h52.f"
            };
            for (String candidate : pmCandidates) {
                Class<?> cls = loadSafe(cl, candidate);
                if (cls == null) continue;
                // 静的メソッドで (Context, holderType) を引数に取るものを探す
                for (Method m : cls.getDeclaredMethods()) {
                    if (!Modifier.isStatic(m.getModifiers())) continue;
                    Class<?>[] params = m.getParameterTypes();
                    if (params.length == 2
                            && params[0] == Context.class
                            && params[1].isAssignableFrom(holderType)) {
                        result.put(ObfuscationCache.KEY_PROFILE_MANAGER_CLASS, candidate);
                        result.put(ObfuscationCache.KEY_PROFILE_MANAGER_GET_METHOD, m.getName());
                        result.put(ObfuscationCache.KEY_PROFILE_MANAGER_PARAM_CLASS,
                                holderType.getName());
                        Logger.d("DynamicAnalyzer: ProfileManager=" + candidate
                                + " method=" + m.getName());

                        // --- Step 3: Profile.mid フィールドを探す ---
                        try {
                            Object manager = m.invoke(null, context, holderInstance);
                            if (manager != null) {
                                Method getProfile = manager.getClass().getMethod("getProfile");
                                Object profile = getProfile.invoke(manager);
                                if (profile != null) {
                                    for (Field f : profile.getClass().getDeclaredFields()) {
                                        f.setAccessible(true);
                                        if (f.getType() == String.class) {
                                            Object val = f.get(profile);
                                            if (val instanceof String) {
                                                String s = (String) val;
                                                // MID は "u" で始まる 33 文字
                                                if (s.startsWith("u") && s.length() == 33) {
                                                    result.put(ObfuscationCache.KEY_PROFILE_MID_FIELD,
                                                            f.getName());
                                                    Logger.d("DynamicAnalyzer: Profile.mid="
                                                            + f.getName());
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Throwable t) {
                            Logger.d("DynamicAnalyzer: Profile.mid 解析失敗: " + t.getMessage());
                        }
                        break;
                    }
                }
                if (result.containsKey(ObfuscationCache.KEY_PROFILE_MANAGER_CLASS)) break;
            }
        }
    }

    // -----------------------------------------------------------------------
    // ReadReceiptManager 系
    // -----------------------------------------------------------------------

    /**
     * ReadReceiptManager を動的に特定する。
     *
     * <p>特徴: {@code void send(long, String, boolean)} シグネチャのメソッドを持つ短名クラス。
     */
    private static void analyzeReadReceiptManager(ClassLoader cl, Map<String, String> result) {
        // 旧バージョン実績候補から始める
        String[] candidates = {
            "at2.e", "at3.e", "au2.e", "au3.e", "bt2.e", "bt3.e",
            "as2.e", "as3.e", "at2.f", "at2.d", "at2.g"
        };

        for (String candidate : candidates) {
            Class<?> cls = loadSafe(cl, candidate);
            if (cls == null) continue;

            String sendMethod    = null;
            String execAsyncMethod = null;
            String readAllMethod  = null;

            for (Method m : cls.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                // send(long, String, boolean)
                if (params.length == 3
                        && params[0] == long.class
                        && params[1] == String.class
                        && params[2] == boolean.class) {
                    sendMethod = m.getName();
                }
                // executeReadReceiptAsync(String)
                if (params.length == 1 && params[0] == String.class
                        && m.getReturnType() == void.class) {
                    execAsyncMethod = m.getName();
                }
                // readAll()
                if (params.length == 0 && m.getReturnType() == void.class) {
                    readAllMethod = m.getName();
                }
            }

            if (sendMethod != null) {
                result.put(ObfuscationCache.KEY_RR_MANAGER_CLASS, candidate);
                result.put(ObfuscationCache.KEY_RR_SEND_METHOD, sendMethod);
                if (execAsyncMethod != null)
                    result.put(ObfuscationCache.KEY_RR_EXEC_ASYNC_METHOD, execAsyncMethod);
                if (readAllMethod != null)
                    result.put(ObfuscationCache.KEY_RR_READ_ALL_METHOD, readAllMethod);
                Logger.d("DynamicAnalyzer: ReadReceiptManager=" + candidate
                        + " send=" + sendMethod
                        + " execAsync=" + execAsyncMethod
                        + " readAll=" + readAllMethod);
                break;
            }
        }
    }

    // -----------------------------------------------------------------------
    // TalkClient 系
    // -----------------------------------------------------------------------

    /**
     * LegacyTalkServiceClientImpl はパッケージ名が固定のため直接確認する。
     * メソッド名（thriftDispatch, sendMessage）は難読化されるため
     * シグネチャで探索する。
     */
    private static void analyzeTalkClient(ClassLoader cl, Map<String, String> result) {
        final String TALK_CLIENT =
                "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
        Class<?> cls = loadSafe(cl, TALK_CLIENT);
        if (cls == null) {
            Logger.d("DynamicAnalyzer: TalkClient が見つかりません");
            return;
        }

        String thriftDispatch = null;
        String sendMessage    = null;

        for (Method m : cls.getDeclaredMethods()) {
            Class<?>[] params = m.getParameterTypes();
            // thriftDispatch: 第1引数が String (operation name)
            if (params.length >= 1 && params[0] == String.class && thriftDispatch == null) {
                thriftDispatch = m.getName();
            }
            // sendMessage: 引数にメッセージオブジェクト（Object）を含む
            // メッセージオブジェクトは通常 2 番目の引数で非プリミティブ
            if (params.length >= 2
                    && !params[1].isPrimitive()
                    && params[1].getName().length() <= 6  // 短い難読化名
                    && sendMessage == null) {
                sendMessage = m.getName();
            }
            if (thriftDispatch != null && sendMessage != null) break;
        }

        if (thriftDispatch != null) {
            result.put(ObfuscationCache.KEY_TALK_CLIENT_THRIFT_DISPATCH, thriftDispatch);
            Logger.d("DynamicAnalyzer: TalkClient thriftDispatch=" + thriftDispatch);
        }
        if (sendMessage != null) {
            result.put(ObfuscationCache.KEY_TALK_CLIENT_SEND_MESSAGE, sendMessage);
            Logger.d("DynamicAnalyzer: TalkClient sendMessage=" + sendMessage);
        }
    }

    // -----------------------------------------------------------------------
    // BadgeClear 系
    // -----------------------------------------------------------------------

    /**
     * BadgeClear クラスを動的に特定する。
     *
     * <p>特徴: {@code void e(String)} のような単純なシグネチャ。
     * 候補クラスはデコード結果から絞り込んだリスト。
     */
    private static void analyzeBadgeClear(ClassLoader cl, Map<String, String> result) {
        String[] candidates = {
            "dc8.b", "dc9.b", "dd8.b", "dd9.b", "db8.b", "db9.b",
            "dc8.c", "dc8.d", "dc7.b", "dc7.c"
        };

        for (String candidate : candidates) {
            Class<?> cls = loadSafe(cl, candidate);
            if (cls == null) continue;

            for (Method m : cls.getDeclaredMethods()) {
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1
                        && params[0] == String.class
                        && m.getReturnType() == void.class) {
                    result.put(ObfuscationCache.KEY_BADGE_CLEAR_CLASS, candidate);
                    result.put(ObfuscationCache.KEY_BADGE_CLEAR_METHOD, m.getName());
                    Logger.d("DynamicAnalyzer: BadgeClear=" + candidate
                            + " method=" + m.getName());
                    break;
                }
            }
            if (result.containsKey(ObfuscationCache.KEY_BADGE_CLEAR_CLASS)) break;
        }
    }

    // -----------------------------------------------------------------------
    // ユーティリティ
    // -----------------------------------------------------------------------

    private static Class<?> loadSafe(ClassLoader cl, String name) {
        try {
            return cl.loadClass(name);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
