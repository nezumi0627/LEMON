package io.github.nezumi0627.lemon;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.constants.LemonConstants;
import io.github.nezumi0627.lemon.utils.ChangelogManager;
import io.github.nezumi0627.lemon.core.HookDispatcher;
import io.github.nezumi0627.lemon.utils.LemonSettings;
import io.github.nezumi0627.lemon.utils.Logger;

/**
 * LEMON🍋 Xposed モジュールのメインエントリーポイント。
 */
public class LemonEntry implements IXposedHookLoadPackage {

    private final HookDispatcher dispatcher = new HookDispatcher();

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(LemonConstants.TARGET_PACKAGE)) return;

        LemonConstants.MODULE_PATH = lpparam.appInfo.sourceDir;
        Logger.i("=== " + LemonConstants.MODULE_NAME + " v" + LemonConstants.MODULE_VERSION + " by " + LemonConstants.MODULE_AUTHOR + " ===");

        // フックの実行
        dispatcher.dispatch(lpparam);

        try {
            XposedHelpers.findAndHookMethod(Application.class, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context ctx = (Context) param.thisObject;
                    ChangelogManager.trackCurrentVersion(ctx);
                    if (!LemonSettings.getBoolean(ctx, LemonConstants.KEY_STARTUP_TOAST_ENABLED, false)) {
                        return;
                    }
                    new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(ctx, "🍋 " + LemonConstants.MODULE_NAME + " Loaded", Toast.LENGTH_SHORT).show()
                    );
                }
            });
        } catch (Throwable t) {
            Logger.e("Failed to hook Application.onCreate for Toast notification", t);
        }
    }
}
