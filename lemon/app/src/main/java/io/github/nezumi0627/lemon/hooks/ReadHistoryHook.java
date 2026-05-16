package io.github.nezumi0627.lemon.hooks;

import android.app.AndroidAppHelper;
import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.utils.LineDbHelper;
import io.github.nezumi0627.lemon.utils.Logger;
import io.github.nezumi0627.lemon.utils.ReadHistorySettings;

/**
 * 既読イベントを記録するフック。
 * 対象: he8.b#c（既読キュー）、LegacyTalkServiceClientImpl#r1（Thrift 層）。
 */
public class ReadHistoryHook extends BaseHook {

    // LINE v26.6.1 の難読化クラス名
    private static final String QUEUE_CLASS  = "he8.b";
    private static final String QUEUE_METHOD = "c";
    private static final String TALK_CLIENT_CLASS  =
            "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
    private static final String TALK_CLIENT_METHOD = "r1";

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookReadReceiptQueue(lpparam.classLoader);
        hookThriftMethods(lpparam.classLoader);
    }

    // -----------------------------------------------------------------------
    // フック
    // -----------------------------------------------------------------------

    private void hookReadReceiptQueue(ClassLoader classLoader) {
        Class<?> queueClass = XposedHelpers.findClassIfExists(QUEUE_CLASS, classLoader);
        if (queueClass == null) return;
        try {
            XposedBridge.hookAllMethods(queueClass, QUEUE_METHOD, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!ReadHistorySettings.isReadHistoryEnabled()) return;

                    java.lang.reflect.Method m = (java.lang.reflect.Method) param.method;
                    Class<?>[] types = m.getParameterTypes();
                    // シグネチャ確認: (long, String, String, long, Enum)
                    if (types.length != 5 || types[0] != long.class || types[1] != String.class) return;

                    long   createdTime = (long)   param.args[0];
                    String chatId      = (String) param.args[1];
                    String senderMid   = (String) param.args[2];
                    String lastMsgId   = String.valueOf(param.args[3]);

                    if (chatId != null && senderMid != null) {
                        recordReadEvent(chatId, senderMid, lastMsgId, createdTime);
                    }
                }
            });
        } catch (Throwable t) {
            Logger.e("ReadHistoryHook: キューのフックに失敗しました", t);
        }
    }

    private void hookThriftMethods(ClassLoader classLoader) {
        Class<?> talkClient = XposedHelpers.findClassIfExists(TALK_CLIENT_CLASS, classLoader);
        if (talkClient == null) return;
        try {
            XposedBridge.hookAllMethods(talkClient, TALK_CLIENT_METHOD, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    if (!ReadHistorySettings.isReadHistoryEnabled()) return;

                    Object operation = (param.args != null && param.args.length > 0) ? param.args[0] : null;
                    if (operation == null) return;

                    String opName = XposedHelpers.getObjectField(operation, "operationType").toString();
                    if (!"NOTIFIED_READ_MESSAGE".equals(opName)) return;

                    long   createdTime = XposedHelpers.getLongField(operation, "createdTime");
                    String chatId      = XposedHelpers.getObjectField(operation, "chatId").toString();
                    String senderMid   = XposedHelpers.getObjectField(operation, "senderMid").toString();
                    String lastMsgId   = XposedHelpers.getObjectField(operation, "lastMessageId").toString();

                    if (chatId != null && senderMid != null) {
                        recordReadEvent(chatId, senderMid, lastMsgId, createdTime);
                    }
                }
            });
        } catch (Throwable t) {
            Logger.e("ReadHistoryHook: Thrift フックに失敗しました", t);
        }
    }

    // -----------------------------------------------------------------------
    // 記録処理
    // -----------------------------------------------------------------------

    private void recordReadEvent(String chatId, String senderMid, String lastMsgId, long createdTime) {
        try {
            String myMid = LineDbHelper.getMyMid();
            // 自分が送信した既読イベントのみ記録
            if (myMid == null || !myMid.equals(senderMid)) return;

            String content     = LineDbHelper.getMessageContent(chatId, lastMsgId);
            String readerName  = LineDbHelper.getDisplayName(myMid);

            ReadHistorySettings.addReadEvent(
                    chatId, lastMsgId,
                    content    != null ? content    : "メッセージ",
                    readerName != null ? readerName : "自分",
                    createdTime);
        } catch (Throwable t) {
            Logger.e("ReadHistoryHook: イベント記録に失敗しました", t);
        }
    }

    @Override
    public String getName() { return "ReadHistoryHook"; }
}
