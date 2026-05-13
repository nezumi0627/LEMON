# AGENTS.md

このリポジトリで作業する AI/開発者向けのプロジェクト固有ルールです。汎用テンプレートではなく、LEMON の現在の実装を守るための短い運用メモとして扱ってください。

## 基本方針

- 回答と作業説明は日本語で行う。
- 変更前に既存コードを読み、LINE 解析結果と現在の動作実績を優先する。
- 動作確認済みの文字列置換ロジック、テーマ展開パス、テーマ検証回避ロジックは安易に変更しない。
- 生成物、UI dump、外部 APK、解析サンプルはルートに散らかさず、既存の管理フォルダへ置く。
- 既存のユーザー変更を勝手に戻さない。

## 重要な保持事項

### 文字列置換

`StringReplacementManager` と `ResourceHook` の置換内容は、実機で見つけた最適解として扱う。文言や対象 ID の変更は、明示的な依頼または実機検証がある場合だけ行う。

### Theme Customizer

テーマ適用は次の方式を守る。

- LINE 側の `SharedPreferences("ThemeManager").ThemePackageName` はデフォルトテーマ ID のままにする。
- 実際に入力されたテーマ ID は LEMON の設定に保存する。
- テーマ ZIP は `theme/load/theme.json`、`theme/load/images/*`、`theme/load/timeline/*`、`theme/load/groupboard/*` に展開する。
- `fo5.k#n()` は LEMON テーマ有効時もデフォルトテーマ ID を返す。
- `fo5.k#z()` は LEMON テーマが有効で `theme/load/theme.json` が存在する場合だけ `false` を返す。
- 自動で LINE を kill/restart しない。適用後はユーザーに手動再起動を促す。

この方式は「適用中」ポップアップや LINE の多重起動ループを避けるための中核仕様。

## 作業場所

```txt
docs/                 ドキュメント
lemon/                Android モジュール本体
line_decompiled/      LINE 解析ソース（git 管理外）
references/           参考データ・サンプル（git 管理外）
tools/                補助スクリプト
xmls/                 UI dump XML（git 管理外）
```

## ビルド確認

通常の確認コマンド:

```powershell
cd E:\projects\LEMON\lemon
.\gradlew.bat assembleDebug
```

大きな変更、Java import の整理、フック追加、UI 変更、テーマ処理変更の後は必ずビルドする。

## ログ方針

- 通常フローの成功ログや高頻度レイアウト監視ログは増やさない。
- `Logger.d()` は必要な調査時だけ使う。
- XposedBridge へ出す `Logger.i/w/e()` は、起動、失敗、復旧不能な状態などに絞る。

## ドキュメント方針

- 実装を変えたら関連する `docs/*.md` も更新する。
- 古い調査メモは「現在の仕様」と混ぜない。
- LINE の難読化クラス名は、確認できたバージョンと役割を併記する。

## 禁止事項

- `git reset --hard` や `git checkout --` によるユーザー変更の破棄。
- ルート直下への一時 XML、DB、展開済みテーマ、検証ログの放置。
- テーマ適用で実テーマ ID を `ThemeManager.ThemePackageName` に書き込むこと。
- 自動再起動で LINE をループさせること。
