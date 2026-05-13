package io.github.nezumi0627.lemon.bridge;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import io.github.nezumi0627.lemon.utils.Logger;

/**
 * Xposed API を安全に呼び出すためのブリッジ層。
 * リフレクションやフック登録時の例外をキャッチし、モジュール全体のクラッシュを防ぐ。
 */
public final class HookHelper {

    private HookHelper() {}

    /**
     * メソッドを安全にフックする。
     */
    public static void findAndHookMethod(String className, ClassLoader classLoader, String methodName, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(className, classLoader, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            Logger.e("Failed to hook: " + className + "#" + methodName, t);
        }
    }

    /**
     * クラス内の特定のメソッドを安全にフックする。
     */
    public static void findAndHookMethod(Class<?> clazz, String methodName, Object... parameterTypesAndCallback) {
        try {
            XposedHelpers.findAndHookMethod(clazz, methodName, parameterTypesAndCallback);
        } catch (Throwable t) {
            Logger.e("Failed to hook: " + clazz.getName() + "#" + methodName, t);
        }
    }
}
