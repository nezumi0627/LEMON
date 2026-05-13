package io.github.nezumi0627.lemon.core;

import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.hooks.BaseHook;
import io.github.nezumi0627.lemon.hooks.ReadHistoryHook;
import io.github.nezumi0627.lemon.hooks.ResourceHook;
import io.github.nezumi0627.lemon.hooks.RegistrationHook;
import io.github.nezumi0627.lemon.hooks.ai.ChatAiAssistantHook;
import io.github.nezumi0627.lemon.hooks.ChatListDialogHook;
import io.github.nezumi0627.lemon.hooks.ReadReceiptHook;
import io.github.nezumi0627.lemon.hooks.SecondaryDeviceThemeHook;
import io.github.nezumi0627.lemon.hooks.ThemeHook;
import io.github.nezumi0627.lemon.hooks.adblock.AdBlockHook;
import io.github.nezumi0627.lemon.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * すべてのフックの登録と管理を担当するクラス。
 */
public final class HookDispatcher {

    private final List<BaseHook> hooks = new ArrayList<>();

    public HookDispatcher() {
        // 現在実装されているフックを登録
        hooks.add(new ResourceHook());
        hooks.add(new RegistrationHook());
        hooks.add(new ChatAiAssistantHook());
        hooks.add(new AdBlockHook());
        hooks.add(new ChatListDialogHook());
        hooks.add(new ReadReceiptHook());
        hooks.add(new ReadHistoryHook());
        hooks.add(new ThemeHook());
        hooks.add(new SecondaryDeviceThemeHook());
    }

    /**
     * すべての有効なフックを初期化する。
     */
    public void dispatch(XC_LoadPackage.LoadPackageParam lpparam) {
        for (BaseHook hook : hooks) {
            if (hook.isEnabled()) {
                try {
                    hook.init(lpparam);
                } catch (Throwable t) {
                    Logger.e("Critical error in hook: " + hook.getName(), t);
                }
            }
        }
    }
}
