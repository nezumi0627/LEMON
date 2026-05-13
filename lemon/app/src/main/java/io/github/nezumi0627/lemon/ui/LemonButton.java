package io.github.nezumi0627.lemon.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import io.github.nezumi0627.lemon.constants.LemonConstants;

/**
 * LEMON🍋 ボタンの生成とスタイル管理を担当するクラス。
 */
public final class LemonButton {

    public static final String TAG = "LEMON_BTN";

    private LemonButton() {}

    /**
     * LEMONボタンを新規作成して返す。
     */
    public static Button create(Context context) {
        Button btn = new Button(context);
        btn.setTag(TAG);
        btn.setText("🍋 " + LemonConstants.MODULE_NAME + " 🍋");
        btn.setTextSize(16);
        btn.setTextColor(Color.BLACK);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setAllCaps(false);

        float dp = context.getResources().getDisplayMetrics().density;
        
        // 背景設定 (角丸レモンイエロー)
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(24 * dp);
        bg.setColor(Color.parseColor(LemonConstants.LEMON_YELLOW));
        btn.setBackground(bg);
        
        // 影
        btn.setElevation(8 * dp);

        // クリックイベント
        btn.setOnClickListener(v -> 
            Toast.makeText(context, "🍋 " + LemonConstants.MODULE_NAME + " v" + LemonConstants.MODULE_VERSION + " Active 🍋", Toast.LENGTH_SHORT).show()
        );

        return btn;
    }

    /**
     * 標準的なレイアウトパラメータを生成する。
     */
    public static FrameLayout.LayoutParams createDefaultLayoutParams(Context context) {
        float dp = context.getResources().getDisplayMetrics().density;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
            (int)(328 * dp), // 幅
            (int)(48 * dp)   // 高さ
        );
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.topMargin = 1100; // 現行端末に合わせた位置
        return params;
    }
}
