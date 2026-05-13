# LEMON🍋

**LEMON🍋** は、Android版 LINE (`jp.naver.line.android`) の UI・挙動・テーマシステムを拡張する、LSPosed / Vector 向け Xposed モジュールです。
LINE v26.6.1 系の解析結果をベースに開発されています。

> 制作者: **nezumi0627**

---

# ✨ 主な機能

## 🍋 UI / UX カスタマイズ

* **文字列置換**

  * 「LINEへようこそ」→「LEMON🍋へようこそ」など、LINE 内のリソース文字列をカスタム可能。

* **LEMON ボタン追加**

  * `RegistrationActivity` に純正デザインへ馴染む専用ボタンを追加。

* **AI アシスタント置換**

  * LINE 標準 AI 導線を、LEMON 独自の設定 UI / インターフェースへ置換。

---

## 🧩 情報・ユーティリティ

* **Chat ID コピー**

  * トーク一覧やダイアログから MID / GID を簡単にコピー可能。

* **プロフィール表示**

  * MID・プロフィール名・アイコン URL を取得し、設定画面に表示。

* **プロフィール画像保存**

  * 自分のプロフィール画像を高画質で保存可能。

* **既読履歴管理**

  * 既読状態の履歴を記録・管理します。

---

## 🚫 広告ブロック

以下の広告 View や導線を非表示化:

* SmartChannel
* LadAdView
* その他 LINE 内広告コンポーネント

---

## 🎨 Theme Customizer

LEMON 最大の特徴の一つです。

### 機能

* テーマ ID から `theme.zip` を取得
* LINE 互換の `theme/load` に自動展開
* ローカルテーマとして適用

### 重要仕様

LEMON は `ThemeManager.ThemePackageName` を直接変更しません。

これは LINE 側の:

* テーマ検証
* 整合性チェック
* 公式テーマ確認ループ

を回避するためです。

代わりに:

1. LINE にはデフォルトテーマ ID を返す
2. 実際のテーマ ID は LEMON 側へ保存
3. `theme/load/theme.json` を読ませる

という独自構造を採用しています。

詳細:

* `docs/theme.md`

---

# 🏗️ プロジェクト構成

```txt
LEMON/
├── docs/                 開発・解析ドキュメント
├── lemon/                Android / Gradle プロジェクト
├── line_decompiled/      LINE 解析ソース（git管理外）
├── references/           参考データ・サンプル（git管理外）
├── tools/                補助ツール / スクリプト
├── xmls/                 UI Dump XML（git管理外）
│
├── app/
├── core/
├── hooks/
├── ui/
├── utils/
├── constants/
```

---

# 🧠 アーキテクチャ設計

LEMON は保守性・拡張性を重視して設計されています。

## 構成方針

### `hooks/`

LINE への Hook 本体

* Activity Hook
* Method Hook
* Resource Hook
* View Hook

### `core/`

コア機能

* HookDispatcher
* BaseHook

### `ui/`

LEMON 独自 UI

* Settings
* Dialog
* Custom View

### `utils/`

共通ユーティリティ

* Reflection
* Logger
* File
* Network
* Theme Loader

### `constants/`

固定値・キー・パス管理

---

# 🚀 ビルド

```powershell
cd E:\projects\LEMON\lemon
.\gradlew.bat assembleDebug
```

## 出力 APK

```txt
E:\projects\LEMON\lemon\app\build\outputs\apk\debug\app-debug.apk
```

---

# 📦 導入方法

1. [LSPosed](https://github.com/LSPosed/LSPosed?utm_source=chatgpt.com) または [Vector (JingMatrix)](https://github.com/JingMatrix/Vector?utm_source=chatgpt.com) を導入
2. LEMON APK をインストール
3. モジュールを有効化
4. LINE を再起動

---

# 📚 ドキュメント

* `AGENTS.md`

  * AI / 開発者向けルール

* `docs/README.md`

  * ドキュメント索引

* `docs/architecture.md`

  * システム構成

* `docs/hooks.md`

  * Hook ポイント一覧

* `docs/code_index.md`

  * Java コード索引（ファイルと役割）

* `docs/theme.md`

  * Theme Customizer 詳細

* `docs/line_analysis/README.md`

  * LINE 解析メモ

* `docs/build.md`

  * ビルド・導入手順

---

# 💻 開発状況

## 既読回避バッジ
現在、パフォーマンス最適化を模索中です。リアルタイムなバッジ更新とMainActivity再起動のバランス調整を継続的に改善しています。

---

# 📌 対応バージョン

| 対象           | 状態   |
| ------------ | ---- |
| LINE v26.6.1 | ✅ 対応 |
| Android 10+  | ✅ 推奨 |
| LSPosed      | ✅    |
| Vector       | ✅    |

> LINE 側の難読化・内部構造変更により、Hook 更新が必要になる場合があります。

---

# 🛡️ 注意事項

* 本プロジェクトは非公式です
* LY Corporation および LINE とは一切関係ありません
* 学習・研究目的で開発されています
* 利用は自己責任で行ってください
* 規約・法令・権利の回避を目的とした利用は行わないでください

---

# 📄 ライセンス
[LICENSE](LICENSE)
