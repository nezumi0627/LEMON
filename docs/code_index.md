# Code Index

`lemon/app/src/main/java/io/github/nezumi0627/lemon/` 配下の現行コード索引です。既存仕様を崩さず追記する際の入口として使います。

## Entry / Core

| ファイル | 役割 |
|---|---|
| `LemonEntry.java` | Xposed エントリーポイント |
| `MainActivity.java` | モジュールアプリ側の Activity |
| `core/HookDispatcher.java` | Hook 初期化の集約 |
| `hooks/BaseHook.java` | 各 Hook の共通基底 |
| `bridge/HookHelper.java` | Xposed API 呼び出しラッパー |
| `constants/LemonConstants.java` | 定数・設定キー管理 |

## Hooks

| ファイル | 役割 |
|---|---|
| `hooks/ResourceHook.java` | 文字列リソース置換の実行 |
| `hooks/resource/StringReplacementManager.java` | 置換対象文字列の管理 |
| `hooks/RegistrationHook.java` | Registration 画面への LEMON ボタン注入 |
| `hooks/ChatListDialogHook.java` | トーク長押しダイアログ拡張（Chat ID 系） |
| `hooks/ReadHistoryHook.java` | 既読履歴記録と UI 連携 |
| `hooks/ReadReceiptHook.java` | 既読回避（チャット単位トグル、送信経路抑止） |
| `hooks/ThemeHook.java` | Theme Customizer 本体フック |
| `hooks/SecondaryDeviceThemeHook.java` | サブ端末向けテーマ補助 |
| `hooks/ai/ChatAiAssistantHook.java` | AI 導線の置換 |
| `hooks/adblock/AdBlockHook.java` | 広告ブロック |

## UI

| ファイル | 役割 |
|---|---|
| `ui/LemonSettingsUI.java` | 設定ダイアログ全体 |
| `ui/LemonUIBuilder.java` | 設定 UI の部品生成 |
| `ui/LemonButton.java` | Registration 向けボタンコンポーネント |
| `ui/ThemeStockRowFactory.java` | テーマストック一覧行の生成 |

## Utils

| ファイル | 役割 |
|---|---|
| `utils/LemonSettings.java` | 設定アクセス API |
| `utils/LemonJsonSettingsStore.java` | JSON 設定ストア本体 |
| `utils/LineDbHelper.java` | LINE SQLite DB への共通アクセス（MID / チャット名解決 / メッセージ取得） |
| `utils/Logger.java` | ログ出力制御 |
| `utils/IntentUtils.java` | 外部 Intent 補助 |
| `utils/ModuleAssetReader.java` | モジュール assets 読み出し |
| `utils/ProfileResolver.java` | プロフィール情報取得 |
| `utils/ChangelogManager.java` | 自動変更履歴管理 |
| `utils/ReadHistorySettings.java` | 既読履歴データ保存 |
| `utils/ReadReceiptSettings.java` | 既読回避設定保存 |
| `utils/ThemeDownloader.java` | テーマ取得・更新確認 |
| `utils/ThemeManager.java` | テーマ適用・復元・展開 |
| `utils/ThemeStockManager.java` | テーマストック管理 |

## 補足

- `hooks/ReadReceiptHook.java.bak` は退避用ファイルで、通常ビルド対象ではありません。
