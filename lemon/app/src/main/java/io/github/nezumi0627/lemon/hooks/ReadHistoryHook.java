package io.github.nezumi0627.lemon.hooks;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONObject;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.bridge.HookHelper;
import io.github.nezumi0627.lemon.utils.Logger;
import io.github.nezumi0627.lemon.utils.ReadHistorySettings;

public class ReadHistoryHook extends BaseHook {

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookReadReceiptQueue(lpparam.classLoader);
        hookThriftMethods(lpparam.classLoader);
    }

    private void hookReadReceiptQueue(ClassLoader classLoader) {
        try {
            // Hook he8.b#c method for read receipt queue (LINE 26.6+)
            Class<?> queueClass = XposedHelpers.findClassIfExists("he8.b", classLoader);
            if (queueClass != null) {
                XposedBridge.hookAllMethods(queueClass, "c", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            if (!ReadHistorySettings.isReadHistoryEnabled()) return;

                            // Check signature: (long, String, String, long, Enum)
                            java.lang.reflect.Method m = (java.lang.reflect.Method) param.method;
                            Class<?>[] types = m.getParameterTypes();
                            if (types.length != 5 || types[0] != long.class || types[1] != String.class) return;

                            long createdTime = (long) param.args[0];
                            String chatId = (String) param.args[1];
                            String senderMid = (String) param.args[2];
                            long lastMsgId = (long) param.args[3];

                            if (chatId != null && senderMid != null) {
                                recordReadEvent(chatId, senderMid, String.valueOf(lastMsgId), createdTime);
                            }
                        } catch (Throwable t) {
                            Logger.e("ReadHistory queue hook error", t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            Logger.e("Failed to hook read receipt queue", t);
        }
    }

    private void hookThriftMethods(ClassLoader classLoader) {
        try {
            // Hook LegacyTalkServiceClientImpl#r1 for thrift operations
            Class<?> talkClient = XposedHelpers.findClassIfExists("jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl", classLoader);
            if (talkClient != null) {
                XposedBridge.hookAllMethods(talkClient, "r1", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        try {
                            if (!ReadHistorySettings.isReadHistoryEnabled()) return;

                            Object operation = param.args[0];
                            if (operation == null) return;

                            String opName = XposedHelpers.getObjectField(operation, "operationType").toString();
                            if ("NOTIFIED_READ_MESSAGE".equals(opName)) {
                                long createdTime = XposedHelpers.getLongField(operation, "createdTime");
                                String chatId = XposedHelpers.getObjectField(operation, "chatId").toString();
                                String senderMid = XposedHelpers.getObjectField(operation, "senderMid").toString();
                                String lastMsgId = XposedHelpers.getObjectField(operation, "lastMessageId").toString();

                                if (chatId != null && senderMid != null) {
                                    recordReadEvent(chatId, senderMid, lastMsgId, createdTime);
                                }
                            }
                        } catch (Throwable t) {
                            Logger.e("ReadHistory thrift hook error", t);
                        }
                    }
                });
            }
        } catch (Throwable t) {
            Logger.e("Failed to hook thrift methods", t);
        }
    }

    private void recordReadEvent(String chatId, String senderMid, String lastMsgId, long createdTime) {
        try {
            String myMid = getMyMid();
            if (myMid == null || !myMid.equals(senderMid)) return; // Only record my reads

            String content = getLastMessageContent(chatId, lastMsgId);
            String readerName = getMyDisplayName();

            ReadHistorySettings.addReadEvent(chatId, lastMsgId, content, readerName, createdTime);
        } catch (Throwable t) {
            Logger.e("Failed to record read event", t);
        }
    }

    private String getMyMid() {
        try {
            Context context = AndroidAppHelper.currentApplication();
            if (context == null) return null;

            String dbPath = "/data/user/0/jp.naver.line.android/databases/naver_line";
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
                try (Cursor cursor = db.rawQuery("SELECT my_mid FROM my_profile LIMIT 1", null)) {
                    if (cursor.moveToFirst()) {
                        return cursor.getString(0);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private String getMyDisplayName() {
        try {
            Context context = AndroidAppHelper.currentApplication();
            if (context == null) return null;

            String dbPath = "/data/user/0/jp.naver.line.android/databases/contact";
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
                String myMid = getMyMid();
                if (myMid == null) return null;

                try (Cursor cursor = db.rawQuery("SELECT display_name FROM contacts WHERE mid = ? LIMIT 1", new String[]{myMid})) {
                    if (cursor.moveToFirst()) {
                        return cursor.getString(0);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return "自分";
    }

    private String getLastMessageContent(String chatId, String messageId) {
        try {
            String dbPath = "/data/user/0/jp.naver.line.android/databases/naver_line";
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
                try (Cursor cursor = db.rawQuery("SELECT content FROM messages WHERE chat_id = ? AND _id = ? LIMIT 1", new String[]{chatId, messageId})) {
                    if (cursor.moveToFirst()) {
                        return cursor.getString(0);
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return "メッセージ";
    }

    @Override
    public String getName() {
        return "ReadHistoryHook";
    }
}
