# LINE Analysis

このフォルダは LINE APK 解析から得た、LEMON 実装に関係する情報だけを残す場所です。外部ライブラリや LEMON に使っていない LINE 内部処理は追いません。

## 対象

- パッケージ: `jp.naver.line.android`
- 解析基準: LINE v26.6.1 系
- 主な用途: フックポイント、リソース ID、DB 名、テーマ読込パスの確認

## ファイル

- [details.md](details.md): 現在使っている解析結果
- [legal.md](legal.md): 免責事項
- [history.md](history.md): 古い環境修復メモ

## 実装へ反映済みの重要事項

- テーマは `theme/load/theme.json` を読む。
- `ThemePackageName` に実テーマ ID を書くと公式検証に入りやすい。
- AI 導線は `ai_chip_bar` と `ai_input_button` を手がかりに置換できる。
- トーク ID 解決は `contact` と `naver_line` DB を参照する。
