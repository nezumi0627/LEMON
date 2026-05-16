package io.github.nezumi0627.lemon.utils;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * LINE の SQLite データベースへの共通アクセス層。
 * 同一クエリが複数フックで重複しないよう一元管理する。
 */
public final class LineDbHelper {

    private static final String DB_CONTACT    = "/data/user/0/jp.naver.line.android/databases/contact";
    private static final String DB_NAVER_LINE = "/data/user/0/jp.naver.line.android/databases/naver_line";

    private LineDbHelper() {}

    /**
     * 自分の MID を返す（取得不能なら null）。
     *
     * 取得戦略（優先順）:
     *   1. naver_line.db の my_profile テーブル (古いLINEバージョン向け、通常は存在しない)
     *
     * ※ setting テーブルの PROFILE_MID は LINE 独自の AES 暗号化のため直接参照不可。
     * ※ リフレクションによるMID取得は ProfileResolver.getMyMidViaReflection() で行う。
     */
    public static String getMyMid() {
        // 旧バージョン向け my_profile テーブル（通常は存在しない）
        try (SQLiteDatabase db = openReadonly(DB_NAVER_LINE)) {
            if (db == null) return null;
            try (Cursor c = db.rawQuery("SELECT my_mid FROM my_profile LIMIT 1", null)) {
                return c.moveToFirst() ? c.getString(0) : null;
            }
        } catch (Throwable ignored) { return null; }
    }

    /**
     * 指定した MID のプロフィール名を返す。
     * overridden_name → profile_name → address_book_name の優先順。
     */
    public static String getDisplayName(String mid) {
        if (mid == null || mid.isEmpty()) return null;
        try (SQLiteDatabase db = openReadonly(DB_CONTACT)) {
            if (db == null) return null;
            try (Cursor c = db.rawQuery(
                    "SELECT coalesce(overridden_name, profile_name, address_book_name) FROM contacts WHERE mid = ? LIMIT 1",
                    new String[]{mid})) {
                return c.moveToFirst() ? c.getString(0) : null;
            }
        } catch (Throwable ignored) { return null; }
    }

    /**
     * 指定した MID のプロフィール画像パス（CDN 相対パス）を返す。
     * 取得できなければ null。
     */
    public static String getPicturePath(String mid) {
        if (mid == null || mid.isEmpty()) return null;
        try (SQLiteDatabase db = openReadonly(DB_CONTACT)) {
            if (db == null) return null;
            try (Cursor c = db.rawQuery("SELECT picture_path FROM contacts WHERE mid = ? LIMIT 1", new String[]{mid})) {
                if (!c.moveToFirst()) return null;
                String path = c.getString(0);
                return (path != null && !path.isEmpty()) ? path : null;
            }
        } catch (Throwable ignored) { return null; }
    }

    /**
     * 表示名からチャット ID（MID / グループ ID）を解決して返す。
     * 個人 → contacts テーブル、グループ → groups テーブルの順。
     */
    public static String resolveChatIdByName(String name) {
        if (name == null || name.isEmpty()) return null;
        try (SQLiteDatabase db = openReadonly(DB_CONTACT)) {
            if (db != null) {
                try (Cursor c = db.rawQuery(
                        "SELECT mid FROM contacts WHERE profile_name = ? OR overridden_name = ? OR address_book_name = ? LIMIT 1",
                        new String[]{name, name, name})) {
                    if (c.moveToFirst()) return c.getString(0);
                }
            }
        } catch (Throwable ignored) {}
        try (SQLiteDatabase db = openReadonly(DB_NAVER_LINE)) {
            if (db != null) {
                try (Cursor c = db.rawQuery("SELECT id FROM groups WHERE name = ? LIMIT 1", new String[]{name})) {
                    if (c.moveToFirst()) return c.getString(0);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    /**
     * 複数の chatId に対して表示名を一括取得する。
     * contacts（個人 MID）と groups（グループ ID）を両方一度に検索する。
     * 行ごとに個別クエリを発行する代わりに IN 句でまとめて取得することで、
     * DB オープン回数を最小限に抑える。
     *
     * @param ids chatId の集合（MID / グループ ID 混在可）
     * @return Map&lt;chatId, displayName&gt;（取得できなかった ID は含まれない）
     */
    public static java.util.Map<String, String> bulkGetNamesForIds(java.util.Collection<String> ids) {
        java.util.Map<String, String> result = new java.util.HashMap<>();
        if (ids == null || ids.isEmpty()) return result;

        java.util.List<String> idList = new java.util.ArrayList<>(ids);
        String placeholders = makePlaceholders(idList.size());
        String[] args = idList.toArray(new String[0]);

        // contacts テーブル（個人 MID）
        try (SQLiteDatabase db = openReadonly(DB_CONTACT)) {
            if (db != null) {
                try (Cursor c = db.rawQuery(
                        "SELECT mid, coalesce(overridden_name, profile_name, address_book_name)"
                        + " FROM contacts WHERE mid IN (" + placeholders + ")",
                        args)) {
                    while (c.moveToNext()) {
                        String id   = c.getString(0);
                        String name = c.getString(1);
                        if (id != null && name != null) result.put(id, name);
                    }
                }
            }
        } catch (Throwable ignored) {}

        // groups テーブル（グループ ID）
        try (SQLiteDatabase db = openReadonly(DB_NAVER_LINE)) {
            if (db != null) {
                try (Cursor c = db.rawQuery(
                        "SELECT id, name FROM groups WHERE id IN (" + placeholders + ")",
                        args)) {
                    while (c.moveToNext()) {
                        String id   = c.getString(0);
                        String name = c.getString(1);
                        if (id != null && name != null) result.put(id, name);
                    }
                }
            }
        } catch (Throwable ignored) {}

        return result;
    }

    private static String makePlaceholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) sb.append(',');
            sb.append('?');
        }
        return sb.toString();
    }

    /**
     * 指定チャット・メッセージ ID のメッセージ本文を返す。
     * 取得できなければ null。
     */
    public static String getMessageContent(String chatId, String messageId) {
        if (chatId == null || messageId == null) return null;
        try (SQLiteDatabase db = openReadonly(DB_NAVER_LINE)) {
            if (db == null) return null;
            try (Cursor c = db.rawQuery(
                    "SELECT content FROM messages WHERE chat_id = ? AND _id = ? LIMIT 1",
                    new String[]{chatId, messageId})) {
                return c.moveToFirst() ? c.getString(0) : null;
            }
        } catch (Throwable ignored) { return null; }
    }

    private static SQLiteDatabase openReadonly(String path) {
        try {
            return SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        } catch (Throwable ignored) { return null; }
    }
}
