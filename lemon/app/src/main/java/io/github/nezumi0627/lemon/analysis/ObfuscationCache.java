package io.github.nezumi0627.lemon.analysis;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import io.github.nezumi0627.lemon.utils.Logger;

/**
 * 動的解析で得た難読化クラス名・メソッド名・フィールド名を
 * バージョン単位でファイルキャッシュに保存/読み込みするクラス。
 *
 * <p>保存先: /sdcard/LEMON/obf_cache_&lt;lineVersion&gt;.json
 *
 * <p>JSON 構造:
 * <pre>
 * {
 *   "line_version": "14.3.2",
 *   "analyzed_at": 1700000000000,
 *   "entries": {
 *     "ProfileManager.class":         "g50.f",
 *     "ProfileManagerHolder.field":   "h13.b/J3",
 *     "Profile.mid.field":            "b",
 *     "ReadReceiptManager.class":     "at2.e",
 *     "ReadReceiptManager.send":      "d",
 *     "ReadReceiptManager.execAsync": "e",
 *     "ReadReceiptManager.readAll":   "c",
 *     "TalkClient.thriftDispatch":    "r1",
 *     "TalkClient.sendMessage":       "u0",
 *     "BadgeClear.class":             "dc8.b",
 *     "BadgeClear.method":            "e"
 *   }
 * }
 * </pre>
 */
public final class ObfuscationCache {

    // -----------------------------------------------------------------------
    // キー定数（entries オブジェクト内で使用）
    // -----------------------------------------------------------------------
    public static final String KEY_PROFILE_MANAGER_CLASS         = "ProfileManager.class";
    public static final String KEY_PROFILE_HOLDER_CLASS          = "ProfileManagerHolder.class";
    public static final String KEY_PROFILE_HOLDER_FIELD          = "ProfileManagerHolder.field";
    public static final String KEY_PROFILE_MID_FIELD             = "Profile.mid.field";
    public static final String KEY_RR_MANAGER_CLASS              = "ReadReceiptManager.class";
    public static final String KEY_RR_SEND_METHOD                = "ReadReceiptManager.send";
    public static final String KEY_RR_EXEC_ASYNC_METHOD          = "ReadReceiptManager.execAsync";
    public static final String KEY_RR_READ_ALL_METHOD            = "ReadReceiptManager.readAll";
    public static final String KEY_TALK_CLIENT_THRIFT_DISPATCH   = "TalkClient.thriftDispatch";
    public static final String KEY_TALK_CLIENT_SEND_MESSAGE      = "TalkClient.sendMessage";
    public static final String KEY_BADGE_CLEAR_CLASS             = "BadgeClear.class";
    public static final String KEY_BADGE_CLEAR_METHOD            = "BadgeClear.method";
    public static final String KEY_PROFILE_MANAGER_GET_METHOD    = "ProfileManager.get";
    public static final String KEY_PROFILE_MANAGER_PARAM_CLASS   = "ProfileManager.paramClass";

    private static final String CACHE_DIR  = "/sdcard/LEMON/";
    private static final String FILE_PREFIX = "obf_cache_";
    private static final String FILE_SUFFIX = ".json";

    private ObfuscationCache() {}

    // -----------------------------------------------------------------------
    // 保存
    // -----------------------------------------------------------------------

    /**
     * 解析結果マップをファイルに保存する。
     *
     * @param lineVersion LINE バージョン文字列
     * @param entries     解析結果 Map&lt;キー定数, 難読化名&gt;
     */
    public static void save(String lineVersion, Map<String, String> entries) {
        if (lineVersion == null || entries == null || entries.isEmpty()) return;
        try {
            File dir = new File(CACHE_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                Logger.e("ObfuscationCache: ディレクトリ作成失敗: " + CACHE_DIR);
                return;
            }
            JSONObject root = new JSONObject();
            root.put("line_version", lineVersion);
            root.put("analyzed_at", System.currentTimeMillis());

            JSONObject entriesJson = new JSONObject();
            for (Map.Entry<String, String> e : entries.entrySet()) {
                entriesJson.put(e.getKey(), e.getValue());
            }
            root.put("entries", entriesJson);

            File file = cacheFile(lineVersion);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(root.toString(2).getBytes(StandardCharsets.UTF_8));
            }
            Logger.i("ObfuscationCache: 保存完了 → " + file.getAbsolutePath());
        } catch (Throwable t) {
            Logger.e("ObfuscationCache: 保存失敗", t);
        }
    }

    // -----------------------------------------------------------------------
    // 読み込み
    // -----------------------------------------------------------------------

    /**
     * 指定バージョンのキャッシュを読み込んで Map で返す。
     * ファイルが存在しない・壊れている場合は空の Map を返す。
     */
    public static Map<String, String> load(String lineVersion) {
        Map<String, String> result = new HashMap<>();
        if (lineVersion == null) return result;
        File file = cacheFile(lineVersion);
        if (!file.exists()) {
            Logger.d("ObfuscationCache: キャッシュなし (" + lineVersion + ")");
            return result;
        }
        try {
            byte[] bytes = readBytes(file);
            JSONObject root = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            JSONObject entries = root.optJSONObject("entries");
            if (entries != null) {
                Iterator<String> keys = entries.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    String v = entries.optString(k, null);
                    if (v != null) result.put(k, v);
                }
            }
            Logger.i("ObfuscationCache: ロード完了 (" + lineVersion + ") entries=" + result.size());
        } catch (Throwable t) {
            Logger.e("ObfuscationCache: 読み込み失敗", t);
        }
        return result;
    }

    /**
     * 指定バージョンのキャッシュが存在し、かつ空でないなら {@code true}。
     */
    public static boolean exists(String lineVersion) {
        if (lineVersion == null) return false;
        File file = cacheFile(lineVersion);
        if (!file.exists() || file.length() == 0) return false;
        // 最低限 entries が存在するか確認
        Map<String, String> m = load(lineVersion);
        return !m.isEmpty();
    }

    /**
     * 指定バージョンのキャッシュを削除する（再解析を強制したいとき）。
     */
    public static void invalidate(String lineVersion) {
        if (lineVersion == null) return;
        File file = cacheFile(lineVersion);
        if (file.exists()) {
            boolean deleted = file.delete();
            Logger.i("ObfuscationCache: キャッシュ削除 (" + lineVersion + ") deleted=" + deleted);
        }
    }

    // -----------------------------------------------------------------------
    // 内部ヘルパー
    // -----------------------------------------------------------------------

    private static File cacheFile(String lineVersion) {
        // バージョン文字列をファイル名に安全な形に変換 ("." → "_")
        String safe = lineVersion.replace('.', '_').replaceAll("[^a-zA-Z0-9_\\-]", "");
        return new File(CACHE_DIR + FILE_PREFIX + safe + FILE_SUFFIX);
    }

    private static byte[] readBytes(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[(int) file.length()];
            int read = fis.read(buf);
            if (read != buf.length) throw new IOException("読み込みサイズ不一致");
            return buf;
        }
    }
}
