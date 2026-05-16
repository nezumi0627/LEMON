# Architecture

LEMON は LSPosed/Vector 上で LINE プロセスにロードされる Android モジュールです。エントリーポイント、フック登録、個別機能、UI、ユーティリティを分け、LINE 側の変更に追従しやすい構成にしています。

## 構成

```txt
lemon/app/src/main/java/io/github/nezumi0627/lemon/
├── LemonEntry.java              Xposed エントリーポイント
├── bridge/HookHelper.java       Xposed API 呼び出しの安全なラッパー
├── constants/LemonConstants.java 共通定数と設定キー
├── core/HookDispatcher.java     フック登録の集約
├── hooks/                       LINE への機能別フック
├── hooks/adblock/               広告非表示
├── hooks/ai/                    AI 導線置換
├── hooks/resource/              文字列置換管理
├── ui/                          動的 UI 生成
└── utils/                       設定、ログ、テーマ処理
```

## 起動フロー

1. `LemonEntry.handleLoadPackage()` が LINE パッケージだけを対象にする。
2. `HookDispatcher` が `BaseHook` 実装を順番に初期化する。
3. 各 Hook は内部で設定値や LINE の状態を見て処理する。
4. 設定 UI は LINE 内の既存 UI に動的に差し込まれる。
5. バージョン変更時は `ChangelogManager` が初回起動時に自動履歴を記録する。

## 設計方針

- フック失敗は機能単位で閉じ込め、他機能を巻き込まない。
- LINE の高頻度イベントではログを出しすぎない。
- 文字列置換とテーマ適用の実機検証済みロジックは安易に変更しない。
- 解析由来の難読化クラス名は docs に残し、コードコメントは必要最小限にする。

## 主要機能

| 機能 | 主な実装 |
|---|---|
| 文字列置換 | `StringReplacementManager` |
| 登録画面ボタン | `RegistrationHook`, `LemonButton` |
| AI 導線置換 | `ChatAiAssistantHook`, `LemonSettingsUI` |
| Info ライセンス表示 | `LemonSettingsUI`, `ModuleAssetReader` |
| Chat ID コピー | `ChatListDialogHook`, `LineDbHelper` |
| 広告ブロック | `AdBlockHook` |
| 既読回避 | `ReadReceiptHook`, `LineDbHelper` |
| 既読履歴管理 | `ReadHistoryHook`, `LineDbHelper` |
| テーマ適用 | `ThemeManager`, `ThemeDownloader`, `ThemeHook` |
| テーマストック管理 | `ThemeStockManager`, `LemonSettingsUI` |
| テーマストック行UI | `ThemeStockRowFactory` |
| サブ端末テーマ補助 | `SecondaryDeviceThemeHook` |
| 自動変更履歴 | `ChangelogManager`, `LemonEntry` |
| プロフィール解決 | `ProfileResolver`, `LineDbHelper` |

## DB アクセス層

複数のフックで必要になる LINE の SQLite 操作は `LineDbHelper` に集約しています。
直接 DB パスを叩くコードを各フックに書かず、このクラスを経由してください。

- `getMyMid()` ― 自分の MID を naver_line から取得
- `getDisplayName(mid)` ― 連絡先名の解決
- `getPicturePath(mid)` ― プロフィール画像パス
- `resolveChatIdByName(name)` ― 表示名から MID/GID を解決
- `getMessageContent(chatId, messageId)` ― メッセージ本文を取得

## データ保存

LEMON の設定は `LemonSettings` / `LemonJsonSettingsStore` 経由で JSON に保存します（既定: `context.getFilesDir()/LEMON/lemon_settings.json`）。`LemonConstants.PREF_BOOTSTRAP_NAME` の SharedPreferences は保存先パス上書きなど最小ブートストラップ用途のみです。LINE の `ThemeManager` SharedPreferences はテーマ検証ループを避けるため、デフォルトテーマ ID へ戻す用途に限定します。
