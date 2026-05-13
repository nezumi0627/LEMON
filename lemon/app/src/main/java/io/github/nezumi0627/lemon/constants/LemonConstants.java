package io.github.nezumi0627.lemon.constants;

/**
 * LEMON🍋 モジュール全体で使用する定数を一元管理するクラス。
 * マジックストリングやハードコードされた値はすべてここに集約する。
 */
public final class LemonConstants {

    private LemonConstants() {} // インスタンス化禁止

    // --- ブランド ---
    public static final String MODULE_NAME = "LEMON";
    public static final String MODULE_VERSION = "0.2.0";
    public static final String MODULE_AUTHOR = "nezumi0627";
    public static final String LOG_TAG = "LEMON";

    // --- ターゲットアプリ ---
    public static final String TARGET_PACKAGE = "jp.naver.line.android";
    public static final String TARGET_LINE_VERSION = "26.6.1";

    // --- Xposed モジュール情報 ---
    public static String MODULE_PATH = "";

    // --- カラー ---
    public static final String LEMON_YELLOW = "#FFF352";

    // --- リソースID (LINE v26.6.1 で解析済み) ---
    /** startUpFlow_androidRestore_welcome_lbl_title ("LINEへようこそ") */
    public static final int ID_WELCOME_TITLE = 0x7f153c68;
    /** startUpFlow_introPage_lbl_desc ("無料のメールや音声・ビデオ通話を楽しもう！") */
    public static final int ID_WELCOME_DESC = 0x7f153c8b;

    // --- Activity 名 ---
    public static final String REGISTRATION_ACTIVITY = "com.linecorp.line.registration.ui.RegistrationActivity";

    // --- UI 定数 ---
    /** ウェルカム画面の toolbar_container の最大高さ (px) */
    public static final int WELCOME_TOOLBAR_MAX_HEIGHT = 80;

    // --- 設定 ---
    public static final String PREF_NAME = "lemon_settings";
    /** 保存先パス上書きなど、JSON 本体とは別の最小ブートストラップ用（モジュールの SharedPreferences）。 */
    public static final String PREF_BOOTSTRAP_NAME = "lemon_bootstrap";
    /** 空なら {@link #DEFAULT_SETTINGS_SUBDIR}/{@link #DEFAULT_SETTINGS_FILENAME} を使用。 */
    public static final String KEY_SETTINGS_JSON_PATH = "settings_json_path";
    /** 外部ストレージ直下のサブディレクトリ名（例: /sdcard/LEMON/）。 */
    public static final String DEFAULT_SETTINGS_SUBDIR = "LEMON";
    public static final String DEFAULT_SETTINGS_FILENAME = "lemon_settings.json";
    public static final String KEY_AD_BLOCK = "ad_block_enabled";
    public static final String KEY_CHAT_AI_ASSISTANT = "chat_ai_assistant_enabled";
    public static final String KEY_LOG_ENABLED = "log_enabled";
    public static final String KEY_STARTUP_TOAST_ENABLED = "startup_toast_enabled";
    public static final String KEY_THEME_FORCE_ENABLED = "theme_force_enabled";
    public static final String KEY_THEME_FORCE_ID = "theme_force_id";
    public static final String KEY_READ_RECEIPT_BLOCK_ENABLED = "read_receipt_block_enabled";
    /** ON のとき、送信直後に短時間だけ既読送信を許可する（送信時既読）。 */
    public static final String KEY_READ_RECEIPT_SEND_ON_SEND = "read_receipt_send_on_send";
    /**
     * Thrift の操作名を差し替えて既読系 RPC を無効化するときに使うダミー文字列。
     * 実機・対象 LINE バージョンで妥当であることを前提とする。
     */
    public static final String READ_RECEIPT_THRIFT_DUMMY_OPERATION = "noop";
    public static final String KEY_READ_RECEIPT_HEADER_BUTTON_ENABLED = "read_receipt_header_button_enabled";
    public static final String KEY_READ_RECEIPT_DISABLED_CHAT_IDS = "read_receipt_disabled_chat_ids";
    public static final String KEY_READ_HISTORY_ENABLED = "read_history_enabled";
    public static final String KEY_READ_HISTORY_DATA = "read_history_data";

    // --- テーマ ID (LINE 公式) ---
    public static final String THEME_ID_DEFAULT = "3e261192-3a69-4849-b35d-35aeddd5a368";
    public static final String THEME_ID_BROWN = "ec4a14ea-7437-407b-aee7-96b1cbbc1b4b";
    public static final String THEME_ID_CONY = "a0768339-c2d3-4189-9653-2909e9bb6f58";
    public static final String THEME_ID_WHITE = "3cc08ba6-5d04-4c52-ab76-651231ead8fd";
    public static final String THEME_ID_BLACK = "16753051-549f-4318-8777-703350325f4b";

    // --- 外部リンク ---
    public static final String GITHUB_URL = "https://github.com/nezumi0627";
    public static final String PROFILE_ICON_BASE_URL = "https://profile.line-scdn.net";

    // --- 文字列置換 ---
    public static final String ORIGINAL_WELCOME = "LINEへようこそ";
    public static final String REPLACED_WELCOME = "LEMON🍋へようこそ";
}
