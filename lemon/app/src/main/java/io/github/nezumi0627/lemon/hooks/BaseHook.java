package io.github.nezumi0627.lemon.hooks;

import android.content.Context;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * すべてのフッククラスの基底インターフェース。
 */
public abstract class BaseHook {

    /**
     * フックを初期化する。
     * handleLoadPackage のタイミング（Application生成前）で呼ばれる。
     * ClassLoader が完全に初期化される前なので、難読化クラスへの findAndHookMethod は
     * onApplicationCreate() に移すこと。
     * @param lpparam LoadPackageParam
     */
    public abstract void init(XC_LoadPackage.LoadPackageParam lpparam);

    /**
     * Application.onCreate() 完了後に呼ばれる遅延フックポイント。
     * t88.k や r88.a など、マルチdex の後段に配置されたクラスのフックはここで行う。
     * デフォルト実装は何もしない。必要なサブクラスだけオーバーライドすること。
     *
     * @param context Application の Context
     * @param classLoader LINE の ClassLoader
     */
    public void onApplicationCreate(Context context, ClassLoader classLoader) {
        // デフォルトは何もしない
    }

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
