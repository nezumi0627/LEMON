package io.github.nezumi0627.lemon.hooks.resource;

import java.util.HashMap;
import java.util.Map;

import io.github.nezumi0627.lemon.constants.LemonConstants;

/**
 * 文字列置換を一括管理するマネージャークラス。
 */
public final class StringReplacementManager {

    private static final Map<Integer, String> idReplacements = new HashMap<>();
    private static final Map<String, String> literalReplacements = new HashMap<>();

    static {
        // デフォルトの置換ルール (Welcome画面など)
        addReplacement(LemonConstants.ID_WELCOME_TITLE, LemonConstants.REPLACED_WELCOME);
        addReplacement(LemonConstants.ORIGINAL_WELCOME, LemonConstants.REPLACED_WELCOME);
        
        // 汎用的なブランド置換
        // addReplacement("LINE", "LEMON🍋");
        addReplacement("LINEについて", "LEMON🍋について");

        // AI機能のネイティブボタン置換
        addReplacement("返信を提案", "Dummy 1");
        addReplacement("話題を提案", "Dummy 2");
        addReplacement("ムードを分析", "Dummy 3");
    }

    /** ID指定での置換ルールを追加 */
    public static void addReplacement(int resId, String replacement) {
        idReplacements.put(resId, replacement);
    }

    /** 文字列リテラル指定での置換ルールを追加 */
    public static void addReplacement(String original, String replacement) {
        literalReplacements.put(original, replacement);
    }

    /**
     * 与えられたリソースIDと現在の文字列を元に、置換後の文字列を返す。
     * 置換対象でない場合は元の文字列をそのまま返す。
     */
    public static String getReplacedText(int resId, String currentText) {
        // IDによる一致を優先
        if (idReplacements.containsKey(resId)) {
            return idReplacements.get(resId);
        }
        
        // 文字列リテラルによる一致
        if (literalReplacements.containsKey(currentText)) {
            return literalReplacements.get(currentText);
        }

        return currentText;
    }
}
