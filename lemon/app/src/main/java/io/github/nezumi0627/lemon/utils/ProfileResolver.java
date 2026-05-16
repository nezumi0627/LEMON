package io.github.nezumi0627.lemon.utils;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import io.github.nezumi0627.lemon.analysis.ObfuscationCache;
import io.github.nezumi0627.lemon.analysis.ObfuscationRegistry;
import io.github.nezumi0627.lemon.constants.LemonConstants;

/**
 * LINE プロセス内で自分のプロフィール情報（MID・名前・アイコン URL）を解決するクラス。
 *
 * <p><b>アップデート対応</b><br>
 * 難読化クラス名・フィールド名は {@link ObfuscationRegistry} から動的に取得する。
 * キャッシュが存在する場合はキャッシュ値を使用し、存在しない場合は
 * {@link ObfuscationRegistry#getFallback} で LemonConstants の定数値にフォールバックする。
 *
 * <p><b>取得戦略（優先順）</b>
 * <ol>
 *   <li>{@link #getMyMidViaReflection(Context)} — ObfuscationRegistry の解析済み名でリフレクション</li>
 *   <li>{@link LineDbHelper#getMyMid()} — naver_line DB の my_profile（旧バージョン向け）</li>
 *   <li>MID 取得後: contact DB から名前・アイコン取得</li>
 *   <li>取れなければ my_profile テーブルから補完</li>
 * </ol>
 */
public final class ProfileResolver {

    private static final String DB_NAVER_LINE =
            "/data/user/0/jp.naver.line.android/databases/naver_line";

    private ProfileResolver() {}

    /** プロフィールデータの保持クラス。 */
    public static final class ProfileData {
        public String mid;
        public String name;
        public String iconUrl;
    }

    /**
     * プロフィール情報を解決して返す。
     * 取得できなかったフィールドは null のまま返す（name だけは "Unknown" をフォールバック）。
     */
    public static ProfileData resolve(Context context) {
        ProfileData data = new ProfileData();
        data.name    = "Unknown";
        data.iconUrl = null;

        // ---- Step 1: MID 取得 ----
        data.mid = getMyMidViaReflection(context);
        if (data.mid == null || data.mid.isEmpty()) {
            Logger.d("ProfileResolver: リフレクションで MID 取得失敗 → my_profile テーブルを試みます");
            data.mid = LineDbHelper.getMyMid();
        }

        if (data.mid == null || data.mid.isEmpty()) {
            Logger.d("ProfileResolver: MID を取得できませんでした");
            fillFromMyProfile(data);
            return data;
        }
        Logger.d("ProfileResolver: MID = " + data.mid);

        // ---- Step 2: contact DB から名前・アイコン取得 ----
        String name = LineDbHelper.getDisplayName(data.mid);
        if (name != null && !name.isEmpty()) {
            data.name = name;
        }

        String picturePath = LineDbHelper.getPicturePath(data.mid);
        if (picturePath != null && !picturePath.isEmpty()) {
            data.iconUrl = LemonConstants.PROFILE_ICON_BASE_URL + picturePath;
        }

        // ---- Step 3: contact で取れなかったフィールドを my_profile で補完 ----
        if ("Unknown".equals(data.name) || data.iconUrl == null) {
            fillFromMyProfile(data);
        }

        Logger.d("ProfileResolver: name=" + data.name + "  iconUrl=" + data.iconUrl);
        return data;
    }

    // -----------------------------------------------------------------------
    // 内部ヘルパー
    // -----------------------------------------------------------------------

    /**
     * naver_line DB の my_profile テーブルから名前・アイコンを補完する。
     */
    private static void fillFromMyProfile(ProfileData data) {
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(
                DB_NAVER_LINE, null,
                SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {

            String nameExpr    = buildNameExpr(db);
            String pictureExpr = buildPictureExpr(db);

            String query = "SELECT " + nameExpr + ", " + pictureExpr + " FROM my_profile LIMIT 1";
            try (Cursor c = db.rawQuery(query, null)) {
                if (c.moveToFirst()) {
                    String name = c.getString(0);
                    if ((name != null && !name.isEmpty()) && "Unknown".equals(data.name)) {
                        data.name = name;
                    }
                    String path = c.getString(1);
                    if (path != null && !path.isEmpty() && data.iconUrl == null) {
                        data.iconUrl = LemonConstants.PROFILE_ICON_BASE_URL + path;
                    }
                }
            }
        } catch (Throwable t) {
            Logger.d("ProfileResolver: my_profile からの補完失敗: " + t.getMessage());
        }
    }

    private static String buildNameExpr(SQLiteDatabase db) {
        java.util.List<String> candidates = new java.util.ArrayList<>();
        candidates.add("display_name");
        candidates.add("profile_name");
        candidates.add("name");
        return buildCoalesce(db, "my_profile", candidates, "'Unknown'");
    }

    private static String buildPictureExpr(SQLiteDatabase db) {
        java.util.List<String> candidates = new java.util.ArrayList<>();
        candidates.add("picture_path");
        candidates.add("icon_path");
        return buildCoalesce(db, "my_profile", candidates, "NULL");
    }

    private static String buildCoalesce(SQLiteDatabase db, String table,
                                        java.util.List<String> candidates, String fallback) {
        java.util.Set<String> actual = getColumns(db, table);
        java.util.List<String> found = new java.util.ArrayList<>();
        for (String col : candidates) {
            if (actual.contains(col)) found.add(col);
        }
        if (found.isEmpty()) return fallback;
        if (found.size() == 1) return found.get(0);
        return "coalesce(" + android.text.TextUtils.join(", ", found) + ")";
    }

    private static java.util.Set<String> getColumns(SQLiteDatabase db, String table) {
        java.util.Set<String> cols = new java.util.HashSet<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) {
            int nameIdx = c.getColumnIndex("name");
            if (nameIdx < 0) nameIdx = 1;
            while (c.moveToNext()) {
                String col = c.getString(nameIdx);
                if (col != null) cols.add(col);
            }
        } catch (Throwable ignored) {}
        return cols;
    }

    /**
     * LINE 内部クラスのリフレクションで MID を取得する。
     *
     * <p>クラス名・フィールド名・メソッド名はすべて {@link ObfuscationRegistry} から取得する。
     * アップデート後でも DynamicAnalyzer が解析した最新の名前が使われる。
     */
    private static String getMyMidViaReflection(Context context) {
        if (context == null) return null;
        try {
            ClassLoader cl = context.getClassLoader();

            // ObfuscationRegistry から解析済みの名前を取得
            String holderClassName = ObfuscationRegistry.get(ObfuscationCache.KEY_PROFILE_HOLDER_CLASS);
            String holderFieldName = ObfuscationRegistry.get(ObfuscationCache.KEY_PROFILE_HOLDER_FIELD);
            String managerClassName = ObfuscationRegistry.get(ObfuscationCache.KEY_PROFILE_MANAGER_CLASS);
            String managerMethodName = ObfuscationRegistry.get(ObfuscationCache.KEY_PROFILE_MANAGER_GET_METHOD);
            String paramClassName = ObfuscationRegistry.get(ObfuscationCache.KEY_PROFILE_MANAGER_PARAM_CLASS);
            String midFieldName = ObfuscationRegistry.get(ObfuscationCache.KEY_PROFILE_MID_FIELD);

            if (holderClassName.isEmpty() || holderFieldName.isEmpty()
                    || managerClassName.isEmpty() || midFieldName.isEmpty()) {
                Logger.d("ProfileResolver: レジストリに ProfileManager 情報なし → フォールバック試行");
                return getMyMidViaReflectionLegacy(context);
            }

            Logger.d("ProfileResolver: リフレクション開始 holder=" + holderClassName
                    + " field=" + holderFieldName + " manager=" + managerClassName);

            // ProfileManagerHolder から保持インスタンスを取得
            Class<?> holderClass = cl.loadClass(holderClassName);
            java.lang.reflect.Field holderField = holderClass.getDeclaredField(holderFieldName);
            holderField.setAccessible(true);
            Object holder = holderField.get(null);
            if (holder == null) {
                Logger.d("ProfileResolver: holder フィールドが null");
                return null;
            }

            // ProfileManager インスタンスを取得
            Class<?> managerClass = cl.loadClass(managerClassName);
            Class<?> paramClass = paramClassName.isEmpty()
                    ? holder.getClass()
                    : cl.loadClass(paramClassName);

            java.lang.reflect.Method getMethod = managerClass.getMethod(
                    managerMethodName.isEmpty() ? "a" : managerMethodName,
                    Context.class, paramClass);
            Object manager = getMethod.invoke(null, context, holder);
            if (manager == null) {
                Logger.d("ProfileResolver: manager が null");
                return null;
            }

            // Profile オブジェクトを取得
            Object profile = manager.getClass().getMethod("getProfile").invoke(manager);
            if (profile == null) {
                Logger.d("ProfileResolver: profile が null");
                return null;
            }

            // MID フィールドを取得
            java.lang.reflect.Field midField = profile.getClass().getDeclaredField(midFieldName);
            midField.setAccessible(true);
            String mid = (String) midField.get(profile);
            if (mid != null && !mid.isEmpty()) {
                Logger.d("ProfileResolver: リフレクションで MID 取得成功: " + mid);
                return mid;
            }
        } catch (Exception e) {
            Logger.d("ProfileResolver: リフレクションで MID を取得できませんでした: " + e.getMessage());
            // レジストリ情報が古い可能性があるためレガシー手順も試みる
            return getMyMidViaReflectionLegacy(context);
        }
        return null;
    }

    /**
     * フォールバック用レガシーリフレクション（v26.6.1 ハードコード値）。
     * ObfuscationRegistry が未初期化または失敗した場合に使用する。
     */
    private static String getMyMidViaReflectionLegacy(Context context) {
        if (context == null) return null;
        try {
            ClassLoader cl = context.getClassLoader();
            Class<?> h13b = cl.loadClass("h13.b");
            java.lang.reflect.Field j3Field = h13b.getDeclaredField("J3");
            j3Field.setAccessible(true);
            Object h3 = j3Field.get(null);
            if (h3 == null) return null;

            Class<?> g50f = cl.loadClass("g50.f");
            java.lang.reflect.Method aMethod =
                    g50f.getMethod("a", Context.class, cl.loadClass("g50.a"));
            Object manager = aMethod.invoke(null, context, h3);
            if (manager == null) return null;

            Object profile = manager.getClass().getMethod("getProfile").invoke(manager);
            if (profile == null) return null;

            java.lang.reflect.Field midField = profile.getClass().getDeclaredField("b");
            midField.setAccessible(true);
            String mid = (String) midField.get(profile);
            if (mid != null && !mid.isEmpty()) {
                Logger.d("ProfileResolver: レガシーリフレクションで MID 取得成功: " + mid);
                return mid;
            }
        } catch (Exception e) {
            Logger.d("ProfileResolver: レガシーリフレクションも失敗: " + e.getMessage());
        }
        return null;
    }
}
