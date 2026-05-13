package io.github.nezumi0627.lemon.hooks;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.constants.LemonConstants;
import io.github.nezumi0627.lemon.utils.LemonSettings;
import io.github.nezumi0627.lemon.utils.Logger;
import java.io.File;

/**
 * テーマの強制適用と LINE 側テーマ判定の整合性を制御するフック。
 */
public class ThemeHook extends BaseHook {

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod("fo5.k", lpparam.classLoader, "C", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];
                    if (context == null) {
                        return;
                    }

                    if (LemonSettings.getBoolean(context, LemonConstants.KEY_THEME_FORCE_ENABLED, false)) {
                        sanitizeLineThemePreference(context);
                    }
                }
            });
        } catch (Throwable t) {
            Logger.e("Failed to hook theme manager context setup", t);
        }

        // LINE側には公式デフォルトテーマを使っているように見せる。
        // 実テーマIDを返すと購入/利用権/更新チェックが走り、サブ端末では再適用ループに入りやすい。
        try {
            XposedHelpers.findAndHookMethod("fo5.k", lpparam.classLoader, "n", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = getContext(param.thisObject);
                    if (context == null) {
                        return;
                    }

                    if (!LemonSettings.getBoolean(context, LemonConstants.KEY_THEME_FORCE_ENABLED, false)) {
                        return;
                    }

                    param.setResult(LemonConstants.THEME_ID_DEFAULT);
                }
            });
        } catch (Throwable t) {
            Logger.e("Failed to hook theme manager implementation", t);
        }

        // fo5.k.z() は「現在テーマがデフォルトか」を返す。
        // LINEの設定上はデフォルトIDのままにしつつ、LEMONテーマJSONがある場合だけ false を返して
        // fo5.k.H() に theme/load/theme.json を読ませる。
        try {
            XposedHelpers.findAndHookMethod("fo5.k", lpparam.classLoader, "z", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = getContext(param.thisObject);
                    if (context == null) {
                        return;
                    }

                    if (LemonSettings.getBoolean(context, LemonConstants.KEY_THEME_FORCE_ENABLED, false)
                            && hasLocalThemeJson(context)) {
                        param.setResult(false);
                    }
                }
            });
        } catch (Throwable t) {
            Logger.e("Failed to hook default theme check (fo5.k->z)", t);
        }

        // テーマ設定メニューを強制表示する
        try {
            XposedHelpers.findAndHookMethod("t88.k", lpparam.classLoader, "b", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    param.setResult(true);
                }
            });
        } catch (Throwable t) {
            Logger.e("Failed to hook theme menu visibility (t88.k)", t);
        }

        // t88.k もテーマ管理に関与するが、公式テーマIDを書き換えない方式にしたため、
        // 権利検証や更新検証に入る導線を増やさない。
    }

    private static Context getContext(Object themeManager) {
        try {
            Object context = XposedHelpers.getObjectField(themeManager, "i");
            if (context instanceof Context) {
                return (Context) context;
            }
        } catch (Throwable t) {
            Logger.d("ThemeHook: context field is not ready: " + t.getMessage());
        }
        return null;
    }

    private static boolean hasLocalThemeJson(Context context) {
        File themeJson = new File(context.getApplicationInfo().dataDir, "theme/load/theme.json");
        return themeJson.exists() && themeJson.length() > 0;
    }

    private static void sanitizeLineThemePreference(Context context) {
        try {
            context.getSharedPreferences("ThemeManager", Context.MODE_PRIVATE)
                    .edit()
                    .putString("ThemePackageName", LemonConstants.THEME_ID_DEFAULT)
                    .apply();
            Logger.d("ThemeHook: sanitized LINE ThemePackageName");
        } catch (Throwable t) {
            Logger.e("ThemeHook: failed to sanitize LINE ThemePackageName", t);
        }
    }

    @Override
    public String getName() {
        return "ThemeHook";
    }

    @Override
    public boolean isEnabled() {
        // 設定画面でON/OFFできるようにするため、ここでは常にtrue(dispatcher側で個別判断)
        // または、初期化自体は常に行い、内部で設定値をチェックする
        return true;
    }
}
