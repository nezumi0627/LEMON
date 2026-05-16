package io.github.nezumi0627.lemon.hooks;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.bridge.HookHelper;
import io.github.nezumi0627.lemon.constants.LemonConstants;
import io.github.nezumi0627.lemon.utils.LineDbHelper;
import io.github.nezumi0627.lemon.utils.Logger;

/**
 * トーク一覧の長押しダイアログに MID / GID コピーボタンを注入するフック。
 */
public class ChatListDialogHook extends BaseHook {

    private static final String TAG_COPY_BTN = "lemon_copy_id_btn";

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookHelper.findAndHookMethod(Dialog.class, "show", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Dialog dialog = (Dialog) param.thisObject;
                Context context = dialog.getContext();
                if (dialog.getWindow() == null) return;

                View decorView = dialog.getWindow().getDecorView();
                decorView.postDelayed(() -> injectCopyButton(dialog, context, decorView), 100);
            }
        });
    }

    private void injectCopyButton(Dialog dialog, Context context, View decorView) {
        try {
            int listViewId = res(context, "common_dialog_vertical_buttons_listview", "id");
            int titleId    = res(context, "common_dialog_title_text", "id");
            int bgId       = res(context, "common_dialog_background", "id");
            if (listViewId == 0 || titleId == 0 || bgId == 0) return;

            ListView listView      = decorView.findViewById(listViewId);
            TextView titleView     = decorView.findViewById(titleId);
            LinearLayout bgView    = decorView.findViewById(bgId);
            if (listView == null || titleView == null || bgView == null) return;

            // 重複注入防止
            if (bgView.findViewWithTag(TAG_COPY_BTN) != null) return;

            String chatName = titleView.getText().toString();
            String chatId   = LineDbHelper.resolveChatIdByName(chatName);
            if (chatId == null) return;

            boolean isGroup = chatId.startsWith("c") || chatId.startsWith("g");
            String  label   = isGroup ? "GIDをコピー" : "MIDをコピー";

            Button copyBtn = new Button(context);
            copyBtn.setText(label);
            copyBtn.setTag(TAG_COPY_BTN);
            copyBtn.setTextColor(Color.parseColor("#333333"));
            copyBtn.setBackgroundResource(android.R.drawable.list_selector_background);
            copyBtn.setGravity(Gravity.CENTER);
            copyBtn.setAllCaps(false);
            copyBtn.setTextSize(15);

            int heightPx = dp(context, 48);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx);

            copyBtn.setOnClickListener(v -> {
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText(isGroup ? "GID" : "MID", chatId));
                Toast.makeText(context, (isGroup ? "GID" : "MID") + " copied!", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });

            bgView.addView(copyBtn, lp);
        } catch (Throwable t) {
            Logger.e("ChatListDialogHook: ボタン注入に失敗しました", t);
        }
    }

    private static int res(Context context, String name, String type) {
        return context.getResources().getIdentifier(name, type, LemonConstants.TARGET_PACKAGE);
    }

    private static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density);
    }

    @Override
    public String getName() { return "ChatListDialogHook"; }
}
