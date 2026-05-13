package io.github.nezumi0627.lemon.utils;

import android.content.Context;
import io.github.nezumi0627.lemon.constants.LemonConstants;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class ChangelogManager {
    private static final String KEY_LAST_TRACKED_VERSION = "last_tracked_version";
    private static final String KEY_CHANGELOG_HISTORY = "changelog_history_json";

    private ChangelogManager() {}

    public static synchronized void trackCurrentVersion(Context context) {
        String current = LemonConstants.MODULE_VERSION;
        String previous = LemonSettings.getString(context, KEY_LAST_TRACKED_VERSION, "");
        if (current.equals(previous)) {
            return;
        }
        JSONArray history = getHistoryArray(context);
        JSONObject entry = new JSONObject();
        try {
            entry.put("version", current);
            entry.put("date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
            entry.put("note", "自動記録: このバージョンが端末で有効化されました。");
            if (previous != null && !previous.isEmpty()) {
                entry.put("from", previous);
            }
            history.put(0, entry);
        } catch (Exception ignored) {}
        LemonSettings.setString(context, KEY_CHANGELOG_HISTORY, history.toString());
        LemonSettings.setString(context, KEY_LAST_TRACKED_VERSION, current);
    }

    public static synchronized String buildHistoryText(Context context) {
        JSONArray arr = getHistoryArray(context);
        if (arr.length() == 0) {
            return "変更履歴はまだありません。";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject item = arr.optJSONObject(i);
            if (item == null) continue;
            String version = item.optString("version", "unknown");
            String date = item.optString("date", "");
            String note = item.optString("note", "");
            String from = item.optString("from", "");
            sb.append("v").append(version).append(" (").append(date).append(")\n");
            if (!from.isEmpty()) {
                sb.append("from: v").append(from).append("\n");
            }
            sb.append(note).append("\n\n");
        }
        return sb.toString().trim();
    }

    private static JSONArray getHistoryArray(Context context) {
        String raw = LemonSettings.getString(context, KEY_CHANGELOG_HISTORY, "[]");
        try {
            return new JSONArray(raw);
        } catch (Exception ignored) {
            return new JSONArray();
        }
    }
}
