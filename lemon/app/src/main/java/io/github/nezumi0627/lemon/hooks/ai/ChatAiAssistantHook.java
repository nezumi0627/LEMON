package io.github.nezumi0627.lemon.hooks.ai;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.hooks.BaseHook;
import io.github.nezumi0627.lemon.ui.LemonSettingsUI;
import io.github.nezumi0627.lemon.ui.LemonUIBuilder;

/**
 * LINEのネイティブAIボタンを非表示にし、LEMONの設定ボタンに置き換えるフック。
 */
public class ChatAiAssistantHook extends BaseHook {

    private int idAiChipBar = 0;
    private int idAiInputButton = 0;
    private int idMainTabAiIconContainer = 0;

    private static final int TAG_MONITORING = 0xDEADC0DE;

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!lpparam.processName.equals("jp.naver.line.android")) return;

        io.github.nezumi0627.lemon.bridge.HookHelper.findAndHookMethod(
            Activity.class, "onResume",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    String className = activity.getClass().getName();
                    
                    resolveIds(activity);

                    if (className.contains("ChatHistoryActivity") || className.contains("ChatActivity")) {
                        monitorChatAiButtons(activity);
                    } else if (className.contains("MainActivity")) {
                        monitorMainTabAiIcon(activity.getWindow().getDecorView(), activity);
                    }
                }
            }
        );

        // ChatListFragment の onViewCreated をフック
        io.github.nezumi0627.lemon.bridge.HookHelper.findAndHookMethod(
            "com.linecorp.line.chatlist.view.fragment.ChatListFragment", lpparam.classLoader,
            "onViewCreated", View.class, android.os.Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    View view = (View) param.args[0];
                    Activity activity = (Activity) XposedHelpers.callMethod(param.thisObject, "getActivity");
                    if (activity != null) {
                        resolveIds(activity);
                        if (idMainTabAiIconContainer != 0) {
                            View aiIcon = view.findViewById(idMainTabAiIconContainer);
                            if (aiIcon instanceof ViewGroup) {
                                replaceMainTabAiIcon((ViewGroup) aiIcon, activity);
                            }
                        }
                    }
                }
            }
        );
    }

    private void resolveIds(Context context) {
        if (idMainTabAiIconContainer == 0) {
            idMainTabAiIconContainer = context.getResources().getIdentifier("main_tab_ai_entry_icon_container", "id", "jp.naver.line.android");
        }
        if (idAiChipBar == 0) {
            idAiChipBar = context.getResources().getIdentifier("ai_chip_bar", "id", "jp.naver.line.android");
            if (idAiChipBar == 0) idAiChipBar = 0x7f0b068b;
        }
        if (idAiInputButton == 0) {
            idAiInputButton = context.getResources().getIdentifier("ai_input_button", "id", "jp.naver.line.android");
            if (idAiInputButton == 0) idAiInputButton = 0x7f0b0772;
        }
    }

    private void monitorMainTabAiIcon(View decorView, Activity activity) {
        if (decorView.getTag(TAG_MONITORING) != null) return;
        decorView.setTag(TAG_MONITORING, true);

        decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // ユーザーの要望により、常に有効化
                boolean enabled = true;
                
                if (idMainTabAiIconContainer == 0) return;
                
                View aiIconContainer = decorView.findViewById(idMainTabAiIconContainer);
                if (aiIconContainer instanceof ViewGroup) {
                    ViewGroup container = (ViewGroup) aiIconContainer;
                    if (enabled) {
                        replaceMainTabAiIcon(container, activity);
                    } else {
                        restoreMainTabAiIcon(container);
                    }
                }
            }
        });
    }

    private void restoreMainTabAiIcon(ViewGroup container) {
        View lemonView = container.findViewWithTag("lemon_main_ai_icon");
        if (lemonView != null) {
            container.removeView(lemonView);
            for (int i = 0; i < container.getChildCount(); i++) {
                container.getChildAt(i).setVisibility(View.VISIBLE);
            }
            container.setOnClickListener(null);
        }
    }

    private void replaceMainTabAiIcon(ViewGroup container, Activity activity) {
        if (container.findViewWithTag("lemon_main_ai_icon") != null) {
            return;
        }

        // 既存のアイコン画像を非表示にする
        for (int i = 0; i < container.getChildCount(); i++) {
            container.getChildAt(i).setVisibility(View.GONE);
        }

        // 🍋アイコンを表示
        final String chatId = activity.getIntent().getStringExtra("chatMid");
        View lemonBtn = LemonUIBuilder.createLemonAiButton(activity, false, v -> {
            LemonSettingsUI.show(v.getContext(), false, chatId);
        });
        lemonBtn.setTag("lemon_main_ai_icon");
        
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        container.addView(lemonBtn, lp);
        container.setVisibility(View.VISIBLE);
        
        // コンテナ全体のクリックイベントもLEMON設定に
        container.setOnClickListener(v -> {
            LemonSettingsUI.show(v.getContext(), false, chatId);
        });
        
    }

    private void monitorChatAiButtons(Activity activity) {
        final View decorView = activity.getWindow().getDecorView();
        if (decorView.getTag(TAG_MONITORING) != null) return;
        decorView.setTag(TAG_MONITORING, true);
        
        decorView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // ユーザーの要望により、常に有効化
                boolean enabled = true;

                if (idAiChipBar != 0) {
                    View aiChipBar = decorView.findViewById(idAiChipBar);
                    if (aiChipBar != null) {
                        if (enabled && aiChipBar.getVisibility() == View.VISIBLE) {
                            replaceComposeView(aiChipBar, activity);
                        } else if (!enabled) {
                            restoreComposeView(aiChipBar);
                        }
                    }
                }

                if (idAiInputButton != 0) {
                    View aiInputBtn = decorView.findViewById(idAiInputButton);
                    if (aiInputBtn != null) {
                        if (enabled && aiInputBtn.getVisibility() == View.VISIBLE) {
                            replaceAiInputButton(aiInputBtn, activity);
                        } else if (!enabled) {
                            restoreAiInputButton(aiInputBtn);
                        }
                    }
                }
            }
        });
    }

    private void restoreAiInputButton(View aiInputBtn) {
        aiInputBtn.setVisibility(View.VISIBLE);
        aiInputBtn.setTag(null);
        ViewGroup parent = (ViewGroup) aiInputBtn.getParent();
        if (parent != null) {
            View lemonBtn = parent.findViewWithTag("lemon_chat_settings_btn");
            if (lemonBtn != null) {
                parent.removeView(lemonBtn);
            }
        }
    }

    private void restoreComposeView(View composeView) {
        ViewGroup parent = (ViewGroup) composeView.getParent();
        if (parent != null) {
            View customPanel = parent.findViewWithTag("lemon_custom_ai_bar");
            if (customPanel != null) {
                parent.removeView(customPanel);
                composeView.setVisibility(View.VISIBLE);
            }
        }
    }

    private void replaceAiInputButton(View aiInputBtn, Activity activity) {
        if (aiInputBtn.getTag() != null && aiInputBtn.getTag().equals("lemon_replaced")) return;

        aiInputBtn.setVisibility(View.GONE);
        aiInputBtn.setTag("lemon_replaced");

        ViewGroup parent = (ViewGroup) aiInputBtn.getParent();
        if (parent == null) return;
        if (parent.findViewWithTag("lemon_chat_settings_btn") != null) return;

        final String chatId = activity.getIntent().getStringExtra("chatMid");
        View lemonBtn = LemonUIBuilder.createLemonAiButton(activity, true, v -> {
            LemonSettingsUI.show(v.getContext(), true, chatId);
        });
        lemonBtn.setTag("lemon_chat_settings_btn");
        
        int size = (int) android.util.TypedValue.applyDimension(
            android.util.TypedValue.COMPLEX_UNIT_DIP, 32, activity.getResources().getDisplayMetrics());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
        lp.gravity = android.view.Gravity.BOTTOM;
        lp.setMargins(0, 0, 10, 5);
        lemonBtn.setLayoutParams(lp);

        int index = parent.indexOfChild(aiInputBtn);
        parent.addView(lemonBtn, index, lp);
    }

    private void replaceComposeView(View composeView, Activity activity) {
        composeView.setVisibility(View.GONE);
        ViewGroup parent = (ViewGroup) composeView.getParent();
        if (parent == null) return;
        if (parent.findViewWithTag("lemon_custom_ai_bar") != null) return;

        final String chatId = activity.getIntent().getStringExtra("chatMid");
        View customPanel = LemonUIBuilder.createLemonAiPanel(activity, v -> {
            LemonSettingsUI.show(v.getContext(), true, chatId);
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        int index = parent.indexOfChild(composeView);
        parent.addView(customPanel, index + 1, lp);
    }

    @Override
    public String getName() {
        return "ChatAiAssistantHook";
    }
}
