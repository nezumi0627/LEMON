package io.github.nezumi0627.lemon.hooks;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.utils.Logger;
import java.util.Set;

/**
 * サブ端末でのテーマ機能制限を回避するフック
 * プライマリデバイス以外でもテーマ機能を有効化する
 */
public class SecondaryDeviceThemeHook extends BaseHook {

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookThemeMenuVisibility(lpparam);
        hookThemeApplicationRestrictions(lpparam);
    }

    /**
     * テーマ設定メニューの表示制限を回避
     */
    private void hookThemeMenuVisibility(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("pp4.q2$z0", lpparam.classLoader, "invokeSuspend", Object.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Throwable t) {
            Logger.e("Failed to hook theme settings item visibility", t);
        }

        try {
            XposedHelpers.findAndHookConstructor("t88.k", lpparam.classLoader, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    @SuppressWarnings("unchecked")
                    Set<String> themes = (Set<String>) XposedHelpers.getObjectField(param.thisObject, "a");
                    if (themes != null && themes.isEmpty()) {
                        themes.add("3e261192-3a69-4849-b35d-35aeddd5a368");
                    }
                }
            });
        } catch (Throwable t) {
            Logger.e("Failed to hook theme list empty state", t);
        }
    }

    /**
     * テーマ適用機能の制限を回避
     */
    private void hookThemeApplicationRestrictions(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("r88.a", lpparam.classLoader, "e", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Throwable t) {
            Logger.e("Failed to hook registration completion", t);
        }

        try {
            XposedHelpers.findAndHookMethod("t88.k", lpparam.classLoader, "b", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Throwable t) {
            Logger.e("Failed to hook theme service availability", t);
        }

        try {
            XposedHelpers.findAndHookMethod("jp.naver.line.android.initialization.poststartup.deviceattestation.a", 
                lpparam.classLoader, "f", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(null);
                }
            });
        } catch (Throwable t) {
            Logger.e("Failed to hook device attestation", t);
        }
    }

    @Override
    public String getName() {
        return "SecondaryDeviceThemeHook";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
