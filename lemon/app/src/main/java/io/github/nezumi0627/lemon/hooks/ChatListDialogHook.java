package io.github.nezumi0627.lemon.hooks;

import android.app.Dialog;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import io.github.nezumi0627.lemon.utils.Logger;

public class ChatListDialogHook extends BaseHook {

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        HookHelper.findAndHookMethod(Dialog.class, "show", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Dialog dialog = (Dialog) param.thisObject;
                Context context = dialog.getContext();
                
                if (dialog.getWindow() == null) return;
                View decorView = dialog.getWindow().getDecorView();
                
                decorView.postDelayed(() -> {
                    try {
                        int listViewId = context.getResources().getIdentifier("common_dialog_vertical_buttons_listview", "id", "jp.naver.line.android");
                        int titleId = context.getResources().getIdentifier("common_dialog_title_text", "id", "jp.naver.line.android");
                        int bgId = context.getResources().getIdentifier("common_dialog_background", "id", "jp.naver.line.android");
                        int itemLayoutId = context.getResources().getIdentifier("common_dialog_item", "id", "jp.naver.line.android");
                        
                        if (listViewId == 0 || titleId == 0 || bgId == 0) return;
                        
                        ListView listView = decorView.findViewById(listViewId);
                        TextView titleView = decorView.findViewById(titleId);
                        LinearLayout bgView = decorView.findViewById(bgId);
                        
                        if (listView != null && titleView != null && bgView != null) {
                            String chatName = titleView.getText().toString();
                            
                            // 既にボタンが追加されているかチェック
                            if (bgView.findViewWithTag("lemon_copy_id_btn") != null) return;
                            
                            String chatId = resolveChatIdByName(context, chatName);
                            if (chatId != null) {
                                Button copyBtn = new Button(context);
                                boolean isGroup = chatId.startsWith("c") || chatId.startsWith("g");
                                copyBtn.setText(isGroup ? "GIDをコピー" : "MIDをコピー");
                                copyBtn.setTag("lemon_copy_id_btn");
                                
                                // 元のボタンとスタイルを揃えるための設定
                                copyBtn.setTextColor(Color.parseColor("#333333"));
                                copyBtn.setBackgroundResource(android.R.drawable.list_selector_background);
                                copyBtn.setGravity(Gravity.CENTER);
                                copyBtn.setAllCaps(false);
                                copyBtn.setTextSize(15);
                                
                                int heightPx = (int) (48 * context.getResources().getDisplayMetrics().density);
                                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, heightPx);
                                
                                copyBtn.setOnClickListener(v -> {
                                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                                    android.content.ClipData clip = android.content.ClipData.newPlainText(isGroup ? "GID" : "MID", chatId);
                                    clipboard.setPrimaryClip(clip);
                                    Toast.makeText(context, (isGroup ? "GID" : "MID") + " copied!", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();
                                });
                                
                                // ListViewの中にViewを追加できないため、親のLinearLayoutに追加
                                bgView.addView(copyBtn, lp);
                            }
                        }
                    } catch (Throwable t) {
                        Logger.e("Error injecting into dialog", t);
                    }
                }, 100);
            }
        });
    }
    
    private String resolveChatIdByName(Context context, String name) {
        String dbPath = "/data/user/0/jp.naver.line.android/databases/contact";
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
            try (Cursor cursor = db.rawQuery("SELECT mid FROM contacts WHERE profile_name = ? OR overridden_name = ? OR address_book_name = ? LIMIT 1", new String[]{name, name, name})) {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            }
        } catch (Throwable t) {
            // Error
        }
        
        // Contactsにない場合はgroupsテーブルを検索
        String chatDbPath = "/data/user/0/jp.naver.line.android/databases/naver_line";
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(chatDbPath, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
            try (Cursor cursor = db.rawQuery("SELECT id FROM groups WHERE name = ? LIMIT 1", new String[]{name})) {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            }
        } catch (Throwable t) {
            Logger.d("Failed to resolve chat ID from groups: " + t.getMessage());
        }
        
        return null;
    }

    @Override
    public String getName() {
        return "ChatListDialogHook";
    }
}
