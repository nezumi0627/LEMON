package io.github.nezumi0627.lemon.hooks;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.bridge.HookHelper;
import io.github.nezumi0627.lemon.constants.LemonConstants;
import io.github.nezumi0627.lemon.ui.LemonButton;

/**
 * 登録・ログイン画面（RegistrationActivity）でのボタン注入を担当するフック。
 */
public class RegistrationHook extends BaseHook {

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        // onResume: ボタン追加と監視開始
        HookHelper.findAndHookMethod(
            "android.app.Activity", lpparam.classLoader, "onResume",
            new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    if (!activity.getClass().getName().equals(LemonConstants.REGISTRATION_ACTIVITY)) return;

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        addButton(activity);
                        setupToolbarWatcher(activity);
                    }, 300);
                }
            }
        );

        // onPause: ボタン削除
        HookHelper.findAndHookMethod(
            "android.app.Activity", lpparam.classLoader, "onPause",
            new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    Activity activity = (Activity) param.thisObject;
                    if (activity.getClass().getName().equals(LemonConstants.REGISTRATION_ACTIVITY)) {
                        removeButton(activity);
                    }
                }
            }
        );
    }

    private void addButton(Activity activity) {
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        if (decorView == null) return;

        if (decorView.findViewWithTag(LemonButton.TAG) != null) {
            decorView.findViewWithTag(LemonButton.TAG).setVisibility(View.VISIBLE);
            return;
        }

        Button btn = LemonButton.create(activity);
        decorView.addView(btn, LemonButton.createDefaultLayoutParams(activity));
        btn.bringToFront();
    }

    private void setupToolbarWatcher(Activity activity) {
        int toolbarId = activity.getResources().getIdentifier("toolbar_container", "id", LemonConstants.TARGET_PACKAGE);
        if (toolbarId == 0) return;

        View toolbar = activity.findViewById(toolbarId);
        if (toolbar == null) return;

        toolbar.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int height = bottom - top;
            if (height == (oldBottom - oldTop)) return;

            ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
            if (decorView == null) return;
            
            View btn = decorView.findViewWithTag(LemonButton.TAG);
            if (btn == null) return;

            // ウェルカム画面かどうかで表示を切り替え
            if (height <= LemonConstants.WELCOME_TOOLBAR_MAX_HEIGHT) {
                btn.setVisibility(View.VISIBLE);
                btn.bringToFront();
            } else {
                btn.setVisibility(View.GONE);
            }
        });
    }

    private void removeButton(Activity activity) {
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        if (decorView == null) return;
        View btn = decorView.findViewWithTag(LemonButton.TAG);
        if (btn != null) {
            decorView.removeView(btn);
        }
    }

    @Override
    public String getName() {
        return "RegistrationHook";
    }
}
