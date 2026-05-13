package io.github.nezumi0627.lemon.utils;

import android.content.Context;

import org.json.JSONArray;

import java.util.LinkedHashSet;
import java.util.Set;

import io.github.nezumi0627.lemon.constants.LemonConstants;

public final class ReadReceiptSettings {
    private ReadReceiptSettings() {}

    // -----------------------------------------------------------------------
    // グローバル設定（フック内・Context不要）
    // -----------------------------------------------------------------------

    /** 既読回避機能がグローバルで有効かどうか */
    public static boolean isEnabled() {
        return LemonJsonSettingsStore.getBooleanNoContext(LemonConstants.KEY_READ_RECEIPT_BLOCK_ENABLED, false);
    }

    /** 「送信時に既読を付ける」設定（送信直後の短いバイパス） */
    public static boolean isSendOnSendEnabled() {
        return LemonJsonSettingsStore.getBooleanNoContext(LemonConstants.KEY_READ_RECEIPT_SEND_ON_SEND, false);
    }

    // -----------------------------------------------------------------------
    // チャット単位設定（フック内・Context不要）
    // -----------------------------------------------------------------------

    /** 指定チャットの既読回避が有効かどうか（無効化リストに含まれていない場合にtrue） */
    public static boolean isReadReceiptBlockedForChat(String chatId) {
        if (chatId == null || chatId.isEmpty()) return false;
        return !loadChatSet(null).contains(chatId);
    }

    /** 指定チャットの既読回避が有効かどうか（Contextあり、UI用） */
    public static boolean isReadReceiptBlockedForChat(Context context, String chatId) {
        if (chatId == null || chatId.isEmpty()) return false;
        return !loadChatSet(context).contains(chatId);
    }

    /** 指定チャットで既読をブロックすべきか */
    public static boolean shouldBlockForChat(String chatId) {
        if (!isEnabled()) return false;
        return isReadReceiptBlockedForChat(chatId);
    }

    // -----------------------------------------------------------------------
    // グローバル設定（UI/Activity用）
    // -----------------------------------------------------------------------

    public static boolean isEnabled(Context context) {
        return LemonSettings.getBoolean(context, LemonConstants.KEY_READ_RECEIPT_BLOCK_ENABLED, false);
    }

    public static boolean isSendOnSendEnabled(Context context) {
        return LemonSettings.getBoolean(context, LemonConstants.KEY_READ_RECEIPT_SEND_ON_SEND, false);
    }

    public static boolean isHeaderButtonEnabled(Context context) {
        return LemonSettings.getBoolean(context, LemonConstants.KEY_READ_RECEIPT_HEADER_BUTTON_ENABLED, true);
    }

    // -----------------------------------------------------------------------
    // チャット単位設定（UI/Activity用）
    // -----------------------------------------------------------------------

    

    /** 指定チャットで既読をブロックすべきか */
    public static boolean shouldBlockForChat(Context context, String chatId) {
        if (!isEnabled(context)) return false;
        return isReadReceiptBlockedForChat(context, chatId);
    }

    /**
     * 指定チャットの既読回避設定をトグル
     * @return true: 既読回避を有効化（既読をブロック）、false: 既読回避を無効化（既読を送信）
     */
    public static boolean toggleChat(Context context, String chatId) {
        if (chatId == null || chatId.isEmpty()) return false;
        Set<String> disabledSet = loadChatSet(context);
        boolean willBlock;
        if (disabledSet.contains(chatId)) {
            disabledSet.remove(chatId);
            willBlock = true;
        } else {
            disabledSet.add(chatId);
            willBlock = false;
        }
        saveChatSet(context, disabledSet);
        return willBlock;
    }

    // -----------------------------------------------------------------------
    // 内部ヘルパー
    // -----------------------------------------------------------------------

    /** 既読回避を無効化するチャットIDのセットをロード */
    private static Set<String> loadChatSet(Context context) {
        Set<String> out = new LinkedHashSet<>();
        String raw = LemonJsonSettingsStore.getString(context, LemonConstants.KEY_READ_RECEIPT_DISABLED_CHAT_IDS, "[]");
        if (raw == null) raw = "[]";
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                String id = arr.optString(i, "");
                if (!id.isEmpty()) out.add(id);
            }
        } catch (Exception ignored) {}
        return out;
    }

    /** 既読回避を無効化するチャットIDのセットを保存 */
    private static void saveChatSet(Context context, Set<String> chatIds) {
        JSONArray arr = new JSONArray();
        for (String id : chatIds) arr.put(id);
        LemonJsonSettingsStore.putString(context, LemonConstants.KEY_READ_RECEIPT_DISABLED_CHAT_IDS, arr.toString());
    }
}
