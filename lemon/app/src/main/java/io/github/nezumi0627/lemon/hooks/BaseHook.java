package io.github.nezumi0627.lemon.hooks;

import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * すべてのフッククラスの基底インターフェース。
 */
public abstract class BaseHook {

    /**
     * フックを初期化する。
     * @param lpparam LoadPackageParam
     */
    public abstract void init(XC_LoadPackage.LoadPackageParam lpparam);

    /**
     * このフックが有効かどうかを返す。
     * 将来的に設定画面から個別にON/OFFできるようにするための拡張ポイント。
     */
    public boolean isEnabled() {
        return true;
    }

    /**
     * フックの名前を返す。ログ出力用。
     */
    public abstract String getName();
}
