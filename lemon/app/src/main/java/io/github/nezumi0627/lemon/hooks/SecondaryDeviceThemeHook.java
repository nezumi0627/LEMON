package io.github.nezumi0627.lemon.hooks;

import android.content.Context;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.nezumi0627.lemon.utils.Logger;
import java.util.Set;

/**
 * サブ端末でのテーマ機能制限を回避するフック。
 *
 * LINE のアップデートで難読化クラス名が変化するため、
 * 各フックは独立した try-catch で囲み、一部失敗しても他に影響しない構造にする。
 *
 * ■ ThemeHook との役割分担
 *   - ThemeHook  : fo5.k（テーマ適用エンジン）のフック、t88.k#b(Context) の表示フック
 *   - SecondaryDeviceThemeHook : pp4.q2$z0（設定メニュー可視性）のみ担当
 *
 * ■ 削除した理由
 *   - t88.k#b(Context)  : ThemeHook 側で既にフック済みのため重複排除
 *   - t88$k コンストラクタ : LINE 現行バージョンで内部クラス名が変更されており
 *                            ClassNotFoundException が発生するため無効化
 *   - r88.a#e()         : LINE 現行バージョンでメソッドが存在せず
 *                            NoSuchMethodError が発生するため無効化
 */
public class SecondaryDeviceThemeHook extends BaseHook {

    @Override
    public void init(XC_LoadPackage.LoadPackageParam lpparam) {
        hookThemeSettingsMenuVisibility(lpparam);
    }

    /**
     * テーマ設定メニューの可視性制御をフック。
     * pp4.q2$z0#invokeSuspend → 常に true を返してメニューを表示させる。
     */
    private void hookThemeSettingsMenuVisibility(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            XposedHelpers.findAndHookMethod(
                    "pp4.q2$z0",
                    lpparam.classLoader,
                    "invokeSuspend",
                    Object.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(true);
                        }
                    });
        } catch (Throwable t) {
            // クラス名変更時も致命的にならないよう warn レベルで記録
            Logger.d("SecondaryDeviceThemeHook: pp4.q2$z0#invokeSuspend のフック失敗（LINEアップデートでクラス名変更の可能性）: " + t.getMessage());
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
