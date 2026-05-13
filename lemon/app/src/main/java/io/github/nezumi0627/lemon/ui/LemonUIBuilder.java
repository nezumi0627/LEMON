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
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.app.AlertDialog;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import io.github.nezumi0627.lemon.constants.LemonConstants;

/**
 * LINEのネイティブUIに似たコンポーネントを動的に生成するクラス。
 */
public class LemonUIBuilder {

    private static int dpToPx(Context context, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    /**
     * LINEネイティブに近いヘッダーバーを作成
     */
    public static View createNativeHeader(Context context, String title, View.OnClickListener onBackListener) {
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setBackgroundColor(Color.WHITE);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);
        topRow.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 56)));

        // 戻るボタン
        TextView backBtn = new TextView(context);
        backBtn.setText("＜");
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        backBtn.setTextColor(Color.parseColor("#333333"));
        backBtn.setGravity(Gravity.CENTER);
        backBtn.setPadding(dpToPx(context, 16), 0, dpToPx(context, 16), 0);
        if (onBackListener != null) backBtn.setOnClickListener(onBackListener);
        topRow.addView(backBtn);

        // タイトル
        TextView titleTv = new TextView(context);
        titleTv.setText(title);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        titleTv.setTypeface(null, Typeface.BOLD);
        titleTv.setTextColor(Color.parseColor("#333333"));
        topRow.addView(titleTv);

        header.addView(topRow);
        header.addView(createDivider(context, 0)); // ヘッダー下の区切り線はマージンなし

        return header;
    }

    /**
     * セクションの見出し（例：「メッセージ」）を作成
     */
    public static View createSectionHeader(Context context, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#608bc5"));
        tv.setPadding(dpToPx(context, 16), dpToPx(context, 27), dpToPx(context, 16), dpToPx(context, 10));
        return tv;
    }

    /**
     * プロフィール領域を作成（アイコン、名前、MID）
     */
    public static View createProfileSection(Context context, String name, String iconPath, String mid, String gid) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dpToPx(context, 16), dpToPx(context, 20), dpToPx(context, 16), dpToPx(context, 20));
        container.setBackgroundColor(Color.WHITE);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        // アイコン
        ImageView iconView = new ImageView(context);
        int iconSize = dpToPx(context, 80);
        LinearLayout.LayoutParams iconLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconLp.bottomMargin = dpToPx(context, 12);
        iconView.setLayoutParams(iconLp);

        boolean iconLoaded = false;
        if (iconPath != null && !iconPath.isEmpty()) {
            if (iconPath.startsWith("http")) {
                loadBitmapFromUrl(iconView, iconPath);
                iconLoaded = true; // 読み込み開始したので一旦true扱い
            } else {
                try {
                    Bitmap bitmap = BitmapFactory.decodeFile(iconPath);
                    if (bitmap != null) {
                        iconView.setImageBitmap(bitmap);
                        iconLoaded = true;
                    }
                } catch (Throwable ignored) {}
            }
        }

        if (!iconLoaded) {
            iconView.setImageResource(android.R.drawable.ic_menu_gallery); // デフォルト
        }

        // 長押しで保存ダイアログを表示
        iconView.setOnLongClickListener(v -> {
            Drawable drawable = iconView.getDrawable();
            if (drawable instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                new AlertDialog.Builder(context)
                    .setTitle("画像を保存")
                    .setMessage("このプロフィール画像を保存しますか？")
                    .setPositiveButton("保存", (dialog, which) -> {
                        saveBitmapToGallery(context, bitmap, "lemon_profile_" + System.currentTimeMillis());
                    })
                    .setNegativeButton("キャンセル", null)
                    .show();
            }
            return true;
        });

        container.addView(iconView);

        // 名前
        TextView nameTv = new TextView(context);
        nameTv.setText(name != null ? name : "Unknown User");
        nameTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        nameTv.setTypeface(null, Typeface.BOLD);
        nameTv.setTextColor(Color.parseColor("#333333"));
        container.addView(nameTv);

        // MID
        LinearLayout midLayout = new LinearLayout(context);
        midLayout.setOrientation(LinearLayout.HORIZONTAL);
        midLayout.setGravity(Gravity.CENTER_VERTICAL);
        midLayout.setPadding(0, dpToPx(context, 4), 0, 0);

        TextView midTv = new TextView(context);
        midTv.setText("MID: " + (mid != null ? mid : "Unknown"));
        midTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        midTv.setTextColor(Color.parseColor("#888888"));
        midLayout.addView(midTv);

        if (mid != null) {
            midLayout.addView(createCopyButton(context, "MID", mid, "MID copied!"));
        }

        container.addView(midLayout);

        // GID (if present)
        if (gid != null) {
            LinearLayout gidLayout = new LinearLayout(context);
            gidLayout.setOrientation(LinearLayout.HORIZONTAL);
            gidLayout.setGravity(Gravity.CENTER_VERTICAL);
            gidLayout.setPadding(0, dpToPx(context, 4), 0, 0);

            TextView gidTv = new TextView(context);
            gidTv.setText("GID: " + gid);
            gidTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            gidTv.setTextColor(Color.parseColor("#888888"));
            gidLayout.addView(gidTv);

            gidLayout.addView(createCopyButton(context, "GID", gid, "GID copied!"));
            container.addView(gidLayout);
        }

        return container;
    }

    /**
     * GitHubリンクを作成
     */
    public static View createGitHubLink(Context context, String username) {
        TextView tv = new TextView(context);
        tv.setText("@" + username + " (GitHub)");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        tv.setTextColor(Color.parseColor("#608bc5"));
        tv.setPadding(dpToPx(context, 16), dpToPx(context, 10), dpToPx(context, 16), dpToPx(context, 10));
        tv.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/" + username));
            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
        return tv;
    }

    /**
     * トグルスイッチ付きの設定項目を作成（永続化対応）
     */
    public static View createSwitchItem(Context context, String title, String description, String prefKey, boolean defaultValue) {
        boolean isChecked = io.github.nezumi0627.lemon.utils.LemonSettings.getBoolean(context, prefKey, defaultValue);

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setPadding(dpToPx(context, 16), dpToPx(context, 14), dpToPx(context, 16), dpToPx(context, 14));
        container.setBackgroundColor(Color.WHITE);
        container.setMinimumHeight(dpToPx(context, 55));

        // テキスト部分
        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textLayout.setLayoutParams(textLp);

        TextView titleTv = new TextView(context);
        titleTv.setText(title);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleTv.setTextColor(Color.parseColor("#333333"));
        textLayout.addView(titleTv);

        if (description != null && !description.isEmpty()) {
            TextView descTv = new TextView(context);
            descTv.setText(description);
            descTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            descTv.setTextColor(Color.parseColor("#888888"));
            textLayout.addView(descTv);
        }

        CheckBox cb = new CheckBox(context);
        cb.setChecked(isChecked);
        cb.setOnCheckedChangeListener((buttonView, isCheckedNow) -> {
            io.github.nezumi0627.lemon.utils.LemonSettings.setBoolean(context, prefKey, isCheckedNow);
        });

        container.addView(textLayout);
        container.addView(cb);

        container.setOnClickListener(v -> cb.toggle());

        return container;
    }

    /**
     * 遷移用の設定項目を作成
     */
    public static View createNavigationItem(Context context, String title, String value, View.OnClickListener listener) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setPadding(dpToPx(context, 16), dpToPx(context, 14), dpToPx(context, 16), dpToPx(context, 14));
        container.setBackgroundColor(Color.WHITE);
        container.setMinimumHeight(dpToPx(context, 55));

        TextView titleTv = new TextView(context);
        titleTv.setText(title);
        titleTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        titleTv.setTextColor(Color.parseColor("#333333"));
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleTv.setLayoutParams(titleLp);

        TextView valueTv = new TextView(context);
        valueTv.setText(value);
        valueTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        valueTv.setTextColor(Color.parseColor("#888888"));
        valueTv.setPadding(0, 0, dpToPx(context, 8), 0);

        TextView arrow = new TextView(context);
        arrow.setText(">");
        arrow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        arrow.setTextColor(Color.parseColor("#CCCCCC"));

        container.addView(titleTv);
        if (value != null) container.addView(valueTv);
        container.addView(arrow);

        if (listener != null) container.setOnClickListener(listener);

        return container;
    }

    /**
     * 区切り線を作成
     */
    public static View createDivider(Context context) {
        return createDivider(context, 15); // デフォルトのマージン
    }

    public static View createDivider(Context context, int marginLeftDp) {
        View divider = new View(context);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (context.getResources().getDisplayMetrics().density * 0.7f));
        lp.leftMargin = dpToPx(context, marginLeftDp);
        divider.setLayoutParams(lp);
        divider.setBackgroundColor(Color.parseColor("#EFEFEF"));
        return divider;
    }

    /**
     * 🍋アイコンボタンを作成
     */
    public static View createLemonAiButton(Context context, boolean isSmall, View.OnClickListener listener) {
        FrameLayout container = new FrameLayout(context);
        // LayoutParamsは呼び出し側で設定されるため、ここでは最小限に
        TextView emojiBtn = new TextView(context);
        emojiBtn.setText("🍋");
        // サイズを大きくして視認性を向上
        emojiBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, isSmall ? 18 : 26);
        emojiBtn.setGravity(Gravity.CENTER);
        emojiBtn.setPadding(0, 0, 0, 0); // パディングをゼロにして最大限大きく

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.CENTER;
        container.addView(emojiBtn, lp);

        if (listener != null) container.setOnClickListener(listener);

        return container;
    }

    /**
     * カスタムAIパネルを作成
     */
    public static View createLemonAiPanel(Context context, View.OnClickListener lemonListener) {
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        scrollView.setTag("lemon_custom_ai_bar");
        scrollView.setHorizontalScrollBarEnabled(false);

        LinearLayout customPanel = new LinearLayout(context);
        customPanel.setOrientation(LinearLayout.HORIZONTAL);
        customPanel.setGravity(Gravity.CENTER_VERTICAL);
        customPanel.setPadding(dpToPx(context, 10), dpToPx(context, 5), dpToPx(context, 10), dpToPx(context, 5));
        customPanel.setBackgroundColor(Color.parseColor("#F5F5F5"));

        customPanel.addView(createLemonAiButton(context, false, lemonListener));

        scrollView.addView(customPanel);
        return scrollView;
    }

    public interface OnToggleListener {
        void onToggle(boolean isChecked);
    }

    private static TextView createCopyButton(Context context, String label, String value, String doneMessage) {
        TextView copyBtn = new TextView(context);
        copyBtn.setText(" [Copy]");
        copyBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        copyBtn.setTextColor(Color.parseColor("#608bc5"));
        copyBtn.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText(label, value);
            clipboard.setPrimaryClip(clip);
            android.widget.Toast.makeText(context, doneMessage, android.widget.Toast.LENGTH_SHORT).show();
        });
        return copyBtn;
    }

    /**
     * URLから画像を非同期で読み込んでImageViewにセットする
     */
    private static void loadBitmapFromUrl(final ImageView imageView, final String url) {
        new Thread(() -> {
            java.net.HttpURLConnection conn = null;
            try {
                java.net.URL imageUrl = new java.net.URL(url);
                conn = (java.net.HttpURLConnection) imageUrl.openConnection();
                conn.setDoInput(true);
                conn.connect();
                final Bitmap bitmap;
                try (java.io.InputStream input = conn.getInputStream()) {
                    bitmap = BitmapFactory.decodeStream(input);
                }

                imageView.post(() -> {
                    if (bitmap != null) {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            } catch (Exception e) {
                io.github.nezumi0627.lemon.utils.Logger.e("Failed to load image from URL: " + url, e);
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }, "LemonProfileImageLoader").start();
    }

    /**
     * Bitmapをギャラリーに保存する
     */
    private static void saveBitmapToGallery(Context context, Bitmap bitmap, String fileName) {
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, fileName + ".jpg");
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES + "/LEMON");
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 1);
            }

            android.net.Uri uri = context.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                java.io.OutputStream out = context.getContentResolver().openOutputStream(uri);
                if (out != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.close();
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0);
                    context.getContentResolver().update(uri, values, null, null);
                }
                android.widget.Toast.makeText(context, "画像を保存しました", android.widget.Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            io.github.nezumi0627.lemon.utils.Logger.e("Failed to save image to gallery", e);
            android.widget.Toast.makeText(context, "画像の保存に失敗しました", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
