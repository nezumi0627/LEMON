package io.github.nezumi0627.lemon.utils;

import android.content.Context;
import io.github.nezumi0627.lemon.constants.LemonConstants;

public final class ProfileResolver {
    private ProfileResolver() {}

    public static final class ProfileData {
        public String mid;
        public String name;
        public String iconUrl;
    }

    public static ProfileData resolve(Context context) {
        ProfileData data = new ProfileData();
        data.mid = getMyMid(context);
        data.name = "Nezumi";
        data.iconUrl = null;

        if (data.mid == null) {
            return data;
        }

        try {
            java.io.File dbFile = context.getDatabasePath("contact");
            if (dbFile.exists()) {
                android.database.sqlite.SQLiteDatabase db = android.database.sqlite.SQLiteDatabase.openDatabase(
                        dbFile.getAbsolutePath(), null, android.database.sqlite.SQLiteDatabase.OPEN_READONLY);
                android.database.Cursor cursor = db.rawQuery(
                        "SELECT coalesce(overridden_name, profile_name, address_book_name), picture_path FROM contacts WHERE mid = ?",
                        new String[] {data.mid});
                if (cursor.moveToFirst()) {
                    data.name = cursor.getString(0);
                    String path = cursor.getString(1);
                    if (path != null && !path.isEmpty()) {
                        data.iconUrl = LemonConstants.PROFILE_ICON_BASE_URL + path;
                    }
                }
                cursor.close();
                db.close();
            }
        } catch (Throwable ignored) {}

        return data;
    }

    private static String getMyMid(Context context) {
        try {
            ClassLoader cl = context.getClassLoader();
            Class<?> h13b = cl.loadClass("h13.b");
            java.lang.reflect.Field j3Field = h13b.getDeclaredField("J3");
            j3Field.setAccessible(true);
            Object h3 = j3Field.get(null);

            if (h3 != null) {
                Class<?> g50f = cl.loadClass("g50.f");
                java.lang.reflect.Method aMethod = g50f.getMethod("a", Context.class, cl.loadClass("g50.a"));
                Object manager = aMethod.invoke(null, context, h3);
                if (manager != null) {
                    Object profile = manager.getClass().getMethod("getProfile").invoke(manager);
                    if (profile != null) {
                        java.lang.reflect.Field midField = profile.getClass().getDeclaredField("b");
                        midField.setAccessible(true);
                        return (String) midField.get(profile);
                    }
                }
            }
        } catch (Exception e) {
            Logger.d("Failed to get MID via reflection: " + e.getMessage());
        }
        return null;
    }
}
