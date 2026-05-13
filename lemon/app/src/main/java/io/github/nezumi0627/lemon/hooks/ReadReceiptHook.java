package io.github.nezumi0627.lemon.hooks;

import android.app.Activity;
import android.app.AndroidAppHelper;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.bridge.HookHelper;
import io.github.nezumi0627.lemon.constants.LemonConstants;
import io.github.nezumi0627.lemon.utils.Logger;
import io.github.nezumi0627.lemon.utils.ReadReceiptSettings;

public class ReadReceiptHook extends BaseHook {

    // -----------------------------------------------------------------------
    // タグ定数
    // -----------------------------------------------------------------------
    private static final String TAG_HEADER_BTN = "lemon_read_receipt_header_btn";
    private static final String TAG_LIST_BADGE = "lemon_read_receipt_badge";
    private static final int TAG_MONITORING = 0x13EAD001;

    // -----------------------------------------------------------------------
    // 既読回避関連定数
    // -----------------------------------------------------------------------
    /** 送信時既読などで使う短いバイパス時間（ミリ秒） */
    private static final long READ_RECEIPT_BYPASS_MS = 100L;

    // -----------------------------------------------------------------------
    // LINEクラス・メソッド名（難読化済み）
    // -----------------------------------------------------------------------
    /** {@link LemonConstants#TARGET_LINE_VERSION} 向けに解析した難読化名 */
    private static final String RR_MANAGER_CLASS = "at2.e";
    private static final String RR_METHOD_SEND_READ_RECEIPT = "d";
    private static final String RR_METHOD_EXECUTE_READ_RECEIPT_ASYNC = "e";
    private static final String RR_METHOD_READ_ALL = "c";
    private static final String RR_TALK_CLIENT_CLASS =
            "jp.naver.line.android.thrift.client.impl.LegacyTalkServiceClientImpl";
    private static final String RR_THRIFT_DISPATCH = "r1";
    private static final String RR_SEND_MESSAGE = "u0";
    private static final String RR_BADGE_CLEAR_CLASS = "dc8.b";
    private static final String RR_BADGE_CLEAR_METHOD = "e";

    // -----------------------------------------------------------------------
    // 実行時状態
    // -----------------------------------------------------------------------
    private static volatile String currentResumedChatId;
    private static volatile long bypassExpiry = 0L;
    private static volatile Object cachedManagerInstance = null;

    // -----------------------------------------------------------------------
    // リソースIDキャッシュ
    // -----------------------------------------------------------------------
    private int idHeaderMainGroup = 0;
    private int idName = 0;
    private int idNotificationOff = 0;

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        // UIフック
        hookChatHeaderButton();
        hookChatListLongPressDialog();
        hookChatListIndicator();
        // 既読送信フック
        hookKnownReadReceiptMethods(lpparam.classLoader);
    }

    // -----------------------------------------------------------------------
    // UIフック：チャット画面ヘッダーボタン
    // -----------------------------------------------------------------------

    private void hookChatHeaderButton() {
        HookHelper.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                String className = activity.getClass().getName();
                if (!className.contains("ChatHistoryActivity") && !className.contains("ChatActivity")) return;
                if (!ReadReceiptSettings.isHeaderButtonEnabled(activity)) return;

                resolveIds(activity.getResources());
                String chatId = activity.getIntent().getStringExtra("chatMid");
                if (chatId == null || chatId.isEmpty()) return;
                currentResumedChatId = chatId;

                View decor = activity.getWindow().getDecorView();
                ViewGroup headerGroup = idHeaderMainGroup == 0 ? null : decor.findViewById(idHeaderMainGroup);
                if (headerGroup == null) return;
                if (headerGroup.findViewWithTag(TAG_HEADER_BTN) != null) return;

                FrameLayout button = createHeaderButton(activity, chatId);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(activity, 40), dp(activity, 40));
                lp.gravity = Gravity.CENTER_VERTICAL;
                lp.leftMargin = dp(activity, 4);
                headerGroup.addView(button, lp);
            }
        });
        HookHelper.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                String className = activity.getClass().getName();
                if (!className.contains("ChatHistoryActivity") && !className.contains("ChatActivity")) return;
                currentResumedChatId = null;
            }
        });
    }

    private FrameLayout createHeaderButton(Context context, String chatId) {
        FrameLayout root = new FrameLayout(context);
        root.setTag(TAG_HEADER_BTN);
        root.setClickable(true);
        root.setFocusable(true);

        ImageView icon = new ImageView(context);
        icon.setImageResource(android.R.drawable.ic_menu_view);
        icon.setScaleType(ImageView.ScaleType.CENTER);
        FrameLayout.LayoutParams iconLp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(icon, iconLp);

        updateIconState(context, chatId, icon);
        root.setOnClickListener(v -> {
            Context lineContext = getAnyContext(null);
            if (lineContext == null) {
                Toast.makeText(context, "エラー: 設定を保存できません", Toast.LENGTH_SHORT).show();
                return;
            }
            boolean isBlocked = ReadReceiptSettings.toggleChat(lineContext, chatId);
            updateIconState(context, chatId, icon);
            animateToggle(root);
            Toast.makeText(context, isBlocked ? "このトークの既読回避: ON" : "このトークの既読回避: OFF", Toast.LENGTH_SHORT).show();
            // 設定変更後に即座にバッジを更新
            refreshChatListBadges();
        });
        return root;
    }

    private void updateIconState(Context context, String chatId, ImageView icon) {
        boolean isBlocked = ReadReceiptSettings.shouldBlockForChat(context, chatId);
        icon.setColorFilter(isBlocked ? android.graphics.Color.parseColor("#FF3B30") : android.graphics.Color.parseColor("#333333"));
        icon.setImageResource(isBlocked ? android.R.drawable.ic_menu_close_clear_cancel : android.R.drawable.ic_menu_view);
    }

    private void animateToggle(View view) {
        view.setScaleX(0.86f);
        view.setScaleY(0.86f);
        view.animate().scaleX(1f).scaleY(1f).setDuration(160).start();
        view.animate().alpha(0.65f).setDuration(80).withEndAction(() -> view.animate().alpha(1f).setDuration(120).start()).start();
    }

    

    private void refreshChatListBadges() {
        try {
            Context context = getAnyContext(null);
            if (context == null) {
                Logger.w("Context is null, cannot refresh badges");
                return;
            }
            // ブロードキャストを送信してチャットリスト画面にバッジ更新を通知
            Intent intent = new Intent("io.github.nezumi0627.lemon.REFRESH_BADGES");
            context.sendBroadcast(intent);
            
        } catch (Throwable t) {
            Logger.e("Failed to send broadcast for badge refresh", t);
        }
    }

    // -----------------------------------------------------------------------
    // UIフック：チャットリスト長押しダイアログ
    // -----------------------------------------------------------------------

    private void hookChatListLongPressDialog() {
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
                        if (listViewId == 0 || titleId == 0 || bgId == 0) return;
                        ListView listView = decorView.findViewById(listViewId);
                        TextView titleView = decorView.findViewById(titleId);
                        LinearLayout bgView = decorView.findViewById(bgId);
                        if (listView == null || titleView == null || bgView == null) return;
                        if (bgView.findViewWithTag("lemon_read_receipt_dialog_btn") != null) return;

                        String chatId = resolveChatIdByName(context, titleView.getText().toString());
                        if (chatId == null) return;

                        TextView toggleBtn = createDialogActionButton(context, "lemon_read_receipt_dialog_btn");
                        boolean isBlocked = ReadReceiptSettings.shouldBlockForChat(context, chatId);
                        toggleBtn.setText(isBlocked ? "既読回避: ON" : "既読回避: OFF");
                        toggleBtn.setOnClickListener(v -> {
                            Context lineContext = getAnyContext(null);
                            if (lineContext == null) {
                                Toast.makeText(context, "エラー: 設定を保存できません", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            boolean nowBlocked = ReadReceiptSettings.toggleChat(lineContext, chatId);
                            Toast.makeText(context, nowBlocked ? "既読回避を有効化しました" : "既読回避を無効化しました", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            // 設定変更後に即座にバッジを更新
                            refreshChatListBadges();
                        });
                        bgView.addView(toggleBtn, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 48)));
                    } catch (Throwable t) {
                        Logger.e("ReadReceipt dialog injection failed", t);
                    }
                }, 100);
            }
        });
    }

    private TextView createDialogActionButton(Context context, String tag) {
        TextView button = new TextView(context);
        button.setTag(tag);
        button.setGravity(Gravity.CENTER);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        button.setTextColor(Color.parseColor("#333333"));
        button.setBackgroundResource(android.R.drawable.list_selector_background);
        return button;
    }

    // -----------------------------------------------------------------------
    // UIフック：チャットリストインジケーター（バツマーク）
    // -----------------------------------------------------------------------

    private void hookChatListIndicator() {
		// onCreate時にもバッジ追加処理を実行（最初の画面読込み対応）
		HookHelper.findAndHookMethod(Activity.class, "onCreate", Bundle.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Activity activity = (Activity) param.thisObject;
				if (!activity.getClass().getName().contains("MainActivity")) return;
				
				resolveIds(activity.getResources());
				View decor = activity.getWindow().getDecorView();
				// onCreate時に少し遅延してバッジ追加処理を実行
				decor.postDelayed(() -> {
					try {
						injectChatListBadges(decor, activity);
					} catch (Throwable t) {
						Logger.e("Failed to inject badges (onCreate)", t);
					}
				}, 500);
			}
		});

		// onStart時にもバッジ追加処理を実行
		HookHelper.findAndHookMethod(Activity.class, "onStart", new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				Activity activity = (Activity) param.thisObject;
				if (!activity.getClass().getName().contains("MainActivity")) return;
				
				resolveIds(activity.getResources());
				View decor = activity.getWindow().getDecorView();
				// onStart時にバッジ追加処理を実行
				decor.postDelayed(() -> {
					try {
						injectChatListBadges(decor, activity);
					} catch (Throwable t) {
						Logger.e("Failed to inject badges (onStart)", t);
					}
				}, 200);
			}
		});

        HookHelper.findAndHookMethod(Activity.class, "onResume", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                if (!activity.getClass().getName().contains("MainActivity")) return;
                
                resolveIds(activity.getResources());
                View decor = activity.getWindow().getDecorView();
                // onResume時に即時再描画をトリガー（毎回実行）
                decor.postDelayed(() -> {
                    try {
                        injectChatListBadges(decor, activity);
                    } catch (Throwable t) {
                        Logger.e("Failed to inject badges (300ms)", t);
                    }
                }, 300);
                // ブロードキャストレシーバーを登録
                registerBadgeRefreshReceiver(activity, decor);
            }
        });

        HookHelper.findAndHookMethod(Activity.class, "onPause", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                Activity activity = (Activity) param.thisObject;
                if (!activity.getClass().getName().contains("MainActivity")) return;
                // ブロードキャストレシーバーを解除
                unregisterBadgeRefreshReceiver(activity);
            }
        });
    }

    private void registerBadgeRefreshReceiver(Activity activity, View decor) {
        try {
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    decor.post(() -> {
                        try {
                            injectChatListBadges(decor, activity);
                        } catch (Throwable t) {
                            Logger.e("Failed to inject badges from broadcast", t);
                        }
                    });
                }
            };
            IntentFilter filter = new IntentFilter("io.github.nezumi0627.lemon.REFRESH_BADGES");
            activity.registerReceiver(receiver, filter);
            decor.setTag(0x13EAD002, receiver);
        } catch (Throwable t) {
            Logger.e("Failed to register badge refresh receiver", t);
        }
    }

    private void unregisterBadgeRefreshReceiver(Activity activity) {
        try {
            View decor = activity.getWindow().getDecorView();
            BroadcastReceiver receiver = (BroadcastReceiver) decor.getTag(0x13EAD002);
            if (receiver != null) {
                activity.unregisterReceiver(receiver);
                decor.setTag(0x13EAD002, null);
            }
        } catch (Throwable t) {
            Logger.e("Failed to unregister badge refresh receiver", t);
        }
    }

    private void injectChatListBadges(View root, Context context) {
        if (idName == 0) {
            Logger.w("idName is 0, skipping badge injection");
            return;
        }
        if (!ReadReceiptSettings.isEnabled(context)) {
            
            return;
        }
        
        // 既存のバッジをすべて削除してから再描画
        clearExistingBadges(root);
        traverseAndInject(root, context);
        
    }

    private void clearExistingBadges(View root) {
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = group.getChildCount() - 1; i >= 0; i--) {
                View child = group.getChildAt(i);
                if (TAG_LIST_BADGE.equals(child.getTag())) {
                    group.removeView(child);
                } else if (child instanceof ViewGroup) {
                    clearExistingBadges(child);
                }
            }
        }
    }

    private void traverseAndInject(View view, Context context) {
        if (!(view instanceof ViewGroup)) return;
        ViewGroup group = (ViewGroup) view;
        String entryName = safeEntryName(context.getResources(), group.getId());
        if (group.getId() != 0 && "root".equals(entryName)) {
            tryInjectBadgeToRow(group, context);
        }
        for (int i = 0; i < group.getChildCount(); i++) {
            traverseAndInject(group.getChildAt(i), context);
        }
    }

    private void tryInjectBadgeToRow(ViewGroup row, Context context) {
        TextView nameTv = row.findViewById(idName);
        if (nameTv == null) return;
        
        String chatName = nameTv.getText() == null ? "" : nameTv.getText().toString();
        if (chatName.isEmpty()) return;
        
        String chatId = resolveChatIdByName(context, chatName);
        if (chatId == null) return;
        
        boolean isBlocked = ReadReceiptSettings.shouldBlockForChat(context, chatId);
        View old = row.findViewWithTag(TAG_LIST_BADGE);
        if (!isBlocked) {
            if (old != null) row.removeView(old);
            return;
        }
        if (old != null) return;
        ImageView badge = new ImageView(context);
        badge.setTag(TAG_LIST_BADGE);
        badge.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        badge.setColorFilter(Color.parseColor("#FF3B30"));
        badge.setScaleType(ImageView.ScaleType.CENTER);

        ViewGroup.MarginLayoutParams flp = new ViewGroup.MarginLayoutParams(dp(context, 24), dp(context, 24));
        int x = dp(context, 0);
        if (idNotificationOff != 0) {
            View mute = row.findViewById(idNotificationOff);
            if (mute != null && mute.getLeft() > 0) {
                x = mute.getLeft() + mute.getWidth() + dp(context, 3);
            }
        }
        flp.leftMargin = x == 0 ? dp(context, 330) : x;
        flp.topMargin = dp(context, 18);
        row.addView(badge, flp);
        
    }

    // -----------------------------------------------------------------------
    // 既読送信フック
    // -----------------------------------------------------------------------

    private void hookKnownReadReceiptMethods(ClassLoader cl) {
        // Talk クライアント → 既読マネージャ → 送信後に既読 → バッジ表示抑止 → 補助 Thrift 層
        hookTalkClientThriftDispatch(cl);
        hookReadReceiptManagerSendBlock(cl);
        hookReadReceiptManagerBypassWindow(cl);
        hookAfterSendMessageTriggerReadReceipt(cl);
        hookBadgeClearLocalRead(cl);
        hookAuxiliaryThriftLayer(cl);
        hookFallbackNamedMethods(cl);
    }

    /** LegacyTalkServiceClientImpl の Thrift 送出（解析上のメソッド名 {@link #RR_THRIFT_DISPATCH}）。 */
    private void hookTalkClientThriftDispatch(ClassLoader cl) {
        Class<?> talkClient = XposedHelpers.findClassIfExists(RR_TALK_CLIENT_CLASS, cl);
        if (talkClient == null) return;
        XposedBridge.hookAllMethods(talkClient, RR_THRIFT_DISPATCH, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args == null || param.args.length == 0 || param.args[0] == null) return;
                String chatId = resolveTargetChatId(param.args);
                if (!ReadReceiptSettings.shouldBlockForChat(chatId)) return;
                if (ReadReceiptSettings.isSendOnSendEnabled()
                        && bypassExpiry > System.currentTimeMillis()) return;
                if (param.args[0] instanceof String) {
                    param.args[0] = LemonConstants.READ_RECEIPT_THRIFT_DUMMY_OPERATION;
                } else param.setResult(null);
            }
        });
    }

    /** 低層 Thrift プロトコル経由の経路（補助）。 */
    private void hookAuxiliaryThriftLayer(ClassLoader cl) {
        Class<?> thriftProto = XposedHelpers.findClassIfExists("org.apache.thrift.l", cl);
        if (thriftProto == null) return;
        XposedBridge.hookAllMethods(thriftProto, "b", new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (param.args == null || param.args.length == 0 || param.args[0] == null) return;
                String chatId = resolveTargetChatId(param.args);
                if (!ReadReceiptSettings.shouldBlockForChat(chatId)) return;
                if (ReadReceiptSettings.isSendOnSendEnabled()
                        && bypassExpiry > System.currentTimeMillis()) return;
                if (param.args[0] instanceof String) {
                    param.args[0] = LemonConstants.READ_RECEIPT_THRIFT_DUMMY_OPERATION;
                } else param.setResult(null);
            }
        });
    }

    private void hookReadReceiptManagerSendBlock(ClassLoader cl) {
        Class<?> managerClass = XposedHelpers.findClassIfExists(RR_MANAGER_CLASS, cl);
        if (managerClass == null) return;
        XposedBridge.hookAllMethods(managerClass, RR_METHOD_SEND_READ_RECEIPT, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                java.lang.reflect.Method m = (java.lang.reflect.Method) param.method;
                Class<?>[] types = m.getParameterTypes();
                if (types.length != 3 || types[0] != long.class
                        || types[1] != String.class || types[2] != boolean.class) return;
                if (cachedManagerInstance == null) cachedManagerInstance = param.thisObject;
                String chatId = (String) param.args[1];
                if (!ReadReceiptSettings.shouldBlockForChat(chatId)) return;
                if (ReadReceiptSettings.isSendOnSendEnabled()
                        && bypassExpiry > System.currentTimeMillis()) return;
                param.setResult(null);
            }
        });
    }

    /** 送信時既読用の短いバイパス窓（executeReadReceiptAsync / readAll）。 */
    private void hookReadReceiptManagerBypassWindow(ClassLoader cl) {
        Class<?> managerClass = XposedHelpers.findClassIfExists(RR_MANAGER_CLASS, cl);
        if (managerClass == null) return;
        XposedBridge.hookAllMethods(managerClass, RR_METHOD_EXECUTE_READ_RECEIPT_ASYNC, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!ReadReceiptSettings.isEnabled() || !ReadReceiptSettings.isSendOnSendEnabled()) return;
                java.lang.reflect.Method m = (java.lang.reflect.Method) param.method;
                Class<?>[] params = m.getParameterTypes();
                if (params.length == 1 && params[0] == String.class) {
                    bypassExpiry = System.currentTimeMillis() + READ_RECEIPT_BYPASS_MS;
                }
            }
        });
        XposedBridge.hookAllMethods(managerClass, RR_METHOD_READ_ALL, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!ReadReceiptSettings.isEnabled() || !ReadReceiptSettings.isSendOnSendEnabled()) return;
                java.lang.reflect.Method m = (java.lang.reflect.Method) param.method;
                if (m.getParameterTypes().length == 0) {
                    bypassExpiry = System.currentTimeMillis() + READ_RECEIPT_BYPASS_MS;
                }
            }
        });
    }

    /** メッセージ送信後に既読送信を明示的に呼ぶ（設定 ON 時）。 */
    private void hookAfterSendMessageTriggerReadReceipt(ClassLoader cl) {
        Class<?> talkClient = XposedHelpers.findClassIfExists(RR_TALK_CLIENT_CLASS, cl);
        if (talkClient == null) return;
        XposedBridge.hookAllMethods(talkClient, RR_SEND_MESSAGE, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {}

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (!ReadReceiptSettings.isEnabled() || !ReadReceiptSettings.isSendOnSendEnabled()) return;
                try {
                    if (param.args == null || param.args.length < 2) return;
                    Object md = param.args[1];
                    if (md == null) return;
                    String chatId = (String) XposedHelpers.getObjectField(md, "b");
                    if (chatId == null || chatId.isEmpty()) return;
                    if (!ReadReceiptSettings.shouldBlockForChat(chatId)) return;
                    Object inst = cachedManagerInstance;
                    if (inst == null) return;
                    bypassExpiry = System.currentTimeMillis() + READ_RECEIPT_BYPASS_MS;
                    XposedHelpers.callMethod(inst, RR_METHOD_SEND_READ_RECEIPT, 0L, chatId, true);
                } catch (Throwable t) {
                    Logger.e("ReadReceipt send-on-send hook error", t);
                }
            }
        });
    }

    /**
     * バッジ／未読クリア系。スタックがトーク画面・一覧由来のときだけ抑止する。
     * 第1引数を chatId とみなし、個別ブロック設定と突き合わせる。
     */
    private void hookBadgeClearLocalRead(ClassLoader cl) {
        Class<?> dcCls = XposedHelpers.findClassIfExists(RR_BADGE_CLEAR_CLASS, cl);
        if (dcCls == null) return;
        XposedBridge.hookAllMethods(dcCls, RR_BADGE_CLEAR_METHOD, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                if (!ReadReceiptSettings.isEnabled()) return;
                Class<?>[] params = ((java.lang.reflect.Method) param.method).getParameterTypes();
                if (params.length != 1 || params[0] != String.class) return;
                if (param.args == null || param.args.length < 1) return;
                if (ReadReceiptSettings.isSendOnSendEnabled()
                        && bypassExpiry > System.currentTimeMillis()) return;
                try {
                    Object a0 = param.args[0];
                    String chatId = a0 instanceof String ? (String) a0 : null;
                    if (chatId == null || !ReadReceiptSettings.shouldBlockForChat(chatId)) return;

                    StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                    boolean isLocalRead = false;
                    for (StackTraceElement element : stack) {
                        String className = element.getClassName();
                        if (className.contains("ChatHistoryActivity")
                                || className.contains("MessageList")
                                || className.contains("ChatList")) {
                            isLocalRead = true;
                            break;
                        }
                    }
                    if (!isLocalRead) return;
                } catch (Throwable ignored) {
                    return;
                }
                param.setResult(null);
            }
        });
    }

    private void hookFallbackNamedMethods(ClassLoader cl) {
        String[] classes = new String[] {"he9.c0", "he9.s", "me1.g", "fo5.k"};
        String[] methods = new String[] {"sendChatChecked", "markAsRead", "sendReadReceipt", "setRead"};
        for (String className : classes) {
            Class<?> clazz = XposedHelpers.findClassIfExists(className, cl);
            if (clazz == null) continue;
            for (String method : methods) {
                try {
                    XposedBridge.hookAllMethods(clazz, method, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            String chatId = tryResolveChatIdFromArgs(param.args);
                            if (chatId == null || chatId.isEmpty()) chatId = currentResumedChatId;
                            if (!ReadReceiptSettings.shouldBlockForChat(chatId)) return;
                            if (ReadReceiptSettings.isSendOnSendEnabled()
                                    && bypassExpiry > System.currentTimeMillis()) return;
                            param.setResult(null);
                        }
                    });
                } catch (Throwable ignored) {
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // ヘルパーメソッド
    // -----------------------------------------------------------------------

    private Context tryResolveContext(Object holder) {
        if (holder instanceof Context) return (Context) holder;
        try {
            Object field = XposedHelpers.getObjectField(holder, "a");
            if (field instanceof Context) return (Context) field;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Context getAnyContext(Object holder) {
        Context context = tryResolveContext(holder);
        if (context != null) return context;
        try {
            return AndroidAppHelper.currentApplication();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private String tryResolveChatIdFromArgs(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof String) {
                String s = (String) arg;
                if (s.length() > 20 && (s.startsWith("u") || s.startsWith("c") || s.startsWith("r"))) return s;
            }
        }
        return null;
    }

    private String resolveTargetChatId(Object[] args) {
        String chatId = tryResolveChatIdFromArgs(args);
        if (chatId != null && !chatId.isEmpty()) return chatId;
        return currentResumedChatId;
    }

    private void resolveIds(Resources resources) {
        if (idHeaderMainGroup == 0) {
            idHeaderMainGroup = resources.getIdentifier("main_view_group", "id", "jp.naver.line.android");
            
        }
        if (idName == 0) {
            idName = resources.getIdentifier("name", "id", "jp.naver.line.android");
            
        }
        if (idNotificationOff == 0) {
            idNotificationOff = resources.getIdentifier("notification_off", "id", "jp.naver.line.android");
            
        }
    }

    private String safeEntryName(Resources res, int id) {
        try {
            return res.getResourceEntryName(id);
        } catch (Throwable ignored) {
            return "";
        }
    }

    private String resolveChatIdByName(Context context, String name) {
        if (name == null || name.isEmpty()) return null;
        String dbPath = "/data/user/0/jp.naver.line.android/databases/contact";
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
            try (Cursor cursor = db.rawQuery("SELECT mid FROM contacts WHERE profile_name = ? OR overridden_name = ? OR address_book_name = ? LIMIT 1", new String[]{name, name, name})) {
                if (cursor.moveToFirst()) return cursor.getString(0);
            }
        } catch (Throwable ignored) {
        }
        String chatDbPath = "/data/user/0/jp.naver.line.android/databases/naver_line";
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(chatDbPath, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
            try (Cursor cursor = db.rawQuery("SELECT id FROM groups WHERE name = ? LIMIT 1", new String[]{name})) {
                if (cursor.moveToFirst()) return cursor.getString(0);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private int dp(Context context, int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics());
    }

    @Override
    public String getName() {
        return "ReadReceiptHook";
    }
}
