# Versioning

LEMON は `MAJOR.MINOR.PATCH` 形式で管理します。

- `MAJOR`: LINE 内部構造の大きな変更対応、互換性のない設計変更
- `MINOR`: 新機能追加
- `PATCH`: バグ修正、ドキュメント更新、内部整理

## 履歴

### v0.2.0

- Theme タブを新設し、`Info / Settings / Theme` の 3 タブ構成へ整理。
- テーマストック管理を追加（URL/ID 追加、適用、更新、削除）。
- ストック追加時のバージョンを最新再取得で保存するように変更。
- 世代バックアップを `load_yyyyMMdd_HHmmss_<suffix>` 命名で作成可能に変更。
- Info から License 表示、Contributor の GitHub 遷移を追加。
- バージョン詳細で自動記録の変更履歴を参照可能に変更。
- `LemonSettingsUI` からプロフィール解決処理を `ProfileResolver` へ分離。
- テーマストック行生成を `ThemeStockRowFactory` へ分離し、UI責務を整理。
- URL起動・アセット読み込み処理を共通ユーティリティに統一。

### v0.1.2

- Theme Customizer を公式機能として追加。
- `ThemePackageName` をデフォルト ID に保ち、ローカル `theme/load` を読ませる方式へ確定。
- テーマ適用後の自動再起動を廃止。
- 過剰な XposedBridge ログを削減。
- ルート直下の XML/検証ファイルを整理。
- README、AGENTS、docs を現在仕様に合わせて整理。

### v0.1.1

- AI 導線を LEMON 設定 UI へ置換。
- プロフィール情報表示と画像保存を追加。
- メインタブ AI アイコンの置換を追加。
- 設定画面を整理。

### v0.1.0

- モジュール構成を分割。
- パッケージ名を `io.github.nezumi0627.lemon` に統一。
- 登録画面ボタンと文字列置換を実装。
