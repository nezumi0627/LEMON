package io.github.nezumi0627.lemon.hooks;

import android.content.res.Resources;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.bridge.HookHelper;
import io.github.nezumi0627.lemon.constants.LemonConstants;
import io.github.nezumi0627.lemon.hooks.resource.StringReplacementManager;

/**
 * リソース（文字列）の置換を担当するフック。
 * StringReplacementManager を使用して一括置換を行う。
 */
public class ResourceHook extends BaseHook {

    public static volatile boolean welcomeDescLoaded = false;

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        XC_MethodHook hook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                int resId = (int) param.args[0];
                Object result = param.getResult();
                if (result == null) return;
                
                String text = result.toString();

                // 画面判定用のフラグ更新
                if (resId == LemonConstants.ID_WELCOME_DESC) {
                    welcomeDescLoaded = true;
                }

                // StringReplacementManager を呼び出して置換
                String replaced = StringReplacementManager.getReplacedText(resId, text);
                if (!replaced.equals(text)) {
                    param.setResult(replaced);
                }
            }
        };

        HookHelper.findAndHookMethod(Resources.class, "getText", int.class, hook);
        HookHelper.findAndHookMethod(Resources.class, "getString", int.class, hook);
    }

    @Override
    public String getName() {
        return "ResourceHook";
    }
}
