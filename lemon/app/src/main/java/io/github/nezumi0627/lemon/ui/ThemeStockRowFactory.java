package io.github.nezumi0627.lemon.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import io.github.nezumi0627.lemon.utils.ThemeStockManager;

public final class ThemeStockRowFactory {
    private ThemeStockRowFactory() {}

    public static View create(Context context, ThemeStockManager.ThemeStock stock, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(Color.WHITE);
        row.setPadding(dpToPx(context, 12), dpToPx(context, 10), dpToPx(context, 12), dpToPx(context, 10));
        row.setGravity(Gravity.CENTER_VERTICAL);

        ImageView thumb = new ImageView(context);
        LinearLayout.LayoutParams thumbLp = new LinearLayout.LayoutParams(dpToPx(context, 48), dpToPx(context, 68));
        thumbLp.rightMargin = dpToPx(context, 10);
        thumb.setLayoutParams(thumbLp);
        thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
        thumb.setImageResource(android.R.drawable.ic_menu_gallery);
        row.addView(thumb);

        if (stock.thumbnailUrl != null && !stock.thumbnailUrl.isEmpty()) {
            loadThumbnailAsync(thumb, stock.thumbnailUrl);
        }

        LinearLayout textWrap = new LinearLayout(context);
        textWrap.setOrientation(LinearLayout.VERTICAL);
        textWrap.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView title = new TextView(context);
        title.setText(stock.name);
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.parseColor("#333333"));
        textWrap.addView(title);

        TextView version = new TextView(context);
        String ver = "V" + stock.currentVersion + (stock.previousVersion > 0 ? " / prev V" + stock.previousVersion : "");
        version.setText(ver);
        version.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        version.setTextColor(Color.parseColor("#608bc5"));
        textWrap.addView(version);

        TextView desc = new TextView(context);
        desc.setText(stock.description == null ? "" : stock.description);
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        desc.setTextColor(Color.parseColor("#888888"));
        desc.setMaxLines(2);
        textWrap.addView(desc);

        row.addView(textWrap);
        if (onClick != null) row.setOnClickListener(onClick);
        return row;
    }

    private static void loadThumbnailAsync(ImageView target, String urlStr) {
        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(urlStr);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(6000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                conn.connect();
                try (java.io.InputStream in = conn.getInputStream()) {
                    Bitmap bmp = BitmapFactory.decodeStream(in);
                    if (bmp != null) {
                        target.post(() -> target.setImageBitmap(bmp));
                    }
                }
                conn.disconnect();
            } catch (Throwable ignored) {}
        }, "LemonThemeThumb").start();
    }

    private static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }
}
