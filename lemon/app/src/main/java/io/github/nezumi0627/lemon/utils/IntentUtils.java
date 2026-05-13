package io.github.nezumi0627.lemon.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public final class IntentUtils {
    private IntentUtils() {}

    public static boolean openUrl(Context context, String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
