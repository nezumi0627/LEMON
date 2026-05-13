package io.github.nezumi0627.lemon.utils;

import android.content.Context;

import org.json.JSONObject;

import io.github.nezumi0627.lemon.constants.LemonConstants;

public final class ReadHistorySettings {
    private ReadHistorySettings() {}

    public static boolean isReadHistoryEnabled() {
        return LemonJsonSettingsStore.getBooleanNoContext(LemonConstants.KEY_READ_HISTORY_ENABLED, false);
    }

    public static boolean isReadHistoryEnabled(Context context) {
        return LemonSettings.getBoolean(context, LemonConstants.KEY_READ_HISTORY_ENABLED, false);
    }

    public static void setReadHistoryEnabled(Context context, boolean enabled) {
        LemonSettings.setBoolean(context, LemonConstants.KEY_READ_HISTORY_ENABLED, enabled);
    }

    public static JSONObject loadReadHistory() {
        try {
            String raw = LemonJsonSettingsStore.getStringNoContext(LemonConstants.KEY_READ_HISTORY_DATA, "{}");
            if (raw == null || raw.isEmpty()) raw = "{}";
            return new JSONObject(raw);
        } catch (Exception e) {
            return new JSONObject();
        }
    }

    public static void saveReadHistory(JSONObject history) {
        if (history == null) return;
        LemonJsonSettingsStore.putStringNoContext(LemonConstants.KEY_READ_HISTORY_DATA, history.toString());
    }

    public static void saveReadHistory(Context context, JSONObject history) {
        if (history == null) return;
        LemonSettings.setString(context, LemonConstants.KEY_READ_HISTORY_DATA, history.toString());
    }

    public static void addReadEvent(String chatId, String messageId, String content, String readerName, long readTime) {
        try {
            JSONObject history = loadReadHistory();
            JSONObject chats = history.optJSONObject("chats");
            if (chats == null) {
                chats = new JSONObject();
                history.put("chats", chats);
            }

            JSONObject chat = chats.optJSONObject(chatId);
            if (chat == null) {
                chat = new JSONObject();
                chats.put(chatId, chat);
            }

            JSONObject messages = chat.optJSONObject("messages");
            if (messages == null) {
                messages = new JSONObject();
                chat.put("messages", messages);
            }

            JSONObject message = messages.optJSONObject(messageId);
            if (message == null) {
                message = new JSONObject();
                message.put("content", content);
                message.put("timestamp", readTime);
                messages.put(messageId, message);
            }

            JSONObject readers = message.optJSONObject("readers");
            if (readers == null) {
                readers = new JSONObject();
                message.put("readers", readers);
            }

            JSONObject reader = new JSONObject();
            reader.put("name", readerName);
            reader.put("time", readTime);
            readers.put(readerName, reader);

            saveReadHistory(history);
        } catch (Exception ignored) {}
    }

    public static void clearChatHistory(String chatId) {
        try {
            JSONObject history = loadReadHistory();
            JSONObject chats = history.optJSONObject("chats");
            if (chats != null && chats.has(chatId)) {
                chats.remove(chatId);
                saveReadHistory(history);
            }
        } catch (Exception ignored) {}
    }

    public static void clearAllHistory() {
        try {
            saveReadHistory(new JSONObject());
        } catch (Exception ignored) {}
    }
}
