package io.github.nezumi0627.lemon.hooks.adblock;

import android.content.Context;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.hooks.BaseHook;
import io.github.nezumi0627.lemon.utils.Logger;
import io.github.nezumi0627.lemon.utils.LemonSettings;
import io.github.nezumi0627.lemon.constants.LemonConstants;

/**
 * 画面上のプロモーション枠やスポンサー枠を検知して非表示にする機能。
 */
public class AdBlockHook extends BaseHook {

    private static final String TARGET_APP = "jp.naver.line.android";

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_APP.equals(lpparam.processName)) {
            return;
        }

        disablePromotionBanners(lpparam.classLoader);
        disableSponsoredFeeds(lpparam.classLoader);
        filterInjectedComponents();
    }

    private void disablePromotionBanners(ClassLoader classLoader) {
        try {
            Class<?> bannerClass = XposedHelpers.findClass("com.linecorp.line.admolin.smartch.v2.view.SmartChannelViewLayout", classLoader);
            XposedHelpers.findAndHookMethod(bannerClass, "dispatchDraw", Canvas.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        View component = (View) param.thisObject;
                        if (!LemonSettings.getBoolean(component.getContext(), LemonConstants.KEY_AD_BLOCK, true)) {
                            return;
                        }
                        ViewGroup layoutRoot = (ViewGroup) component.getParent();
                        if (layoutRoot != null && layoutRoot.getVisibility() != View.GONE) {
                            layoutRoot.setVisibility(View.GONE);
                        }
                    } catch (Exception ignored) {
                        // 握りつぶす
                    }
                }
            });
        } catch (Throwable t) {
            Logger.e("Promotion banner class missing", t);
        }
    }

    private void disableSponsoredFeeds(ClassLoader classLoader) {
        try {
            Class<?> feedClass = XposedHelpers.findClass("com.linecorp.line.ladsdk.ui.common.view.lifecycle.LadAdView", classLoader);
            XposedHelpers.findAndHookMethod(feedClass, "onAttachedToWindow", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        View component = (View) param.thisObject;
                        if (!LemonSettings.getBoolean(component.getContext(), LemonConstants.KEY_AD_BLOCK, true)) {
                            return;
                        }
                        View wrapper = (View) component.getParent();
                        if (wrapper == null) return;
                        View outerContainer = (View) wrapper.getParent();
                        if (outerContainer != null) {
                            ViewGroup.LayoutParams params = outerContainer.getLayoutParams();
                            if (params != null) {
                                params.height = 0;
                                outerContainer.setLayoutParams(params);
                            }
                            outerContainer.setVisibility(View.GONE);
                        }
                    } catch (Exception ignored) {
                        // 握りつぶす
                    }
                }
            });
        } catch (Throwable t) {
            Logger.e("Sponsored feed class missing", t);
        }
    }

    private void filterInjectedComponents() {
        try {
            XposedHelpers.findAndHookMethod(ViewGroup.class, "addView", View.class, ViewGroup.LayoutParams.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    View newElement = (View) param.args[0];
                    if (newElement == null) return;
                    if (!isBlockedElement(newElement.getClass().getName())) return;
                    try {
                        Context ctx = newElement.getContext();
                        if (ctx == null) return;
                        if (LemonSettings.getBoolean(ctx, LemonConstants.KEY_AD_BLOCK, true)) {
                            newElement.setVisibility(View.GONE);
                        }
                    } catch (Exception ignored) {}
                }
            });
        } catch (Throwable t) {
            Logger.e("Failed to hook component injection", t);
        }
    }

    private boolean isBlockedElement(String className) {
        if (className == null) return false;
        String normalized = className.toLowerCase();
        
        return normalized.contains("adview") || 
               normalized.contains("sponsored") || 
               normalized.contains("promoted") || 
               className.contains("NativeAd") || 
               className.contains("AdBanner");
    }

    @Override
    public String getName() {
        return "AdBlockHook";
    }
}
