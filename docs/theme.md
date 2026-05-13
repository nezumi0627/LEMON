# Theme Customizer

Theme Customizer は LEMON の公式機能です。LINE 公式テーマの検証フローに入らず、ローカルに展開したテーマファイルを LINE に読ませる方式で動作します。

## 結論

LINE v26.6.1 は `theme/<themeId>/themefile` ではなく、次のパスを読みます。

```txt
/data/user/0/jp.naver.line.android/theme/load/theme.json
/data/user/0/jp.naver.line.android/theme/load/images/*
/data/user/0/jp.naver.line.android/theme/load/timeline/*
/data/user/0/jp.naver.line.android/theme/load/groupboard/*
```

LEMON は `ThemeManager.ThemePackageName` に実テーマ ID を書きません。実テーマ ID を書くと、LINE の購入/利用権/更新確認が走り、サブ端末で「適用中」ポップアップや再起動ループに入るためです。

## 適用フロー

1. ユーザーがテーマ ID を入力する。
2. `ThemeDownloader` が配信 URL から最新バージョンの `theme.zip` を取得する。
3. `ThemeManager` が既存の `theme/load` を `theme/load.lemon_backup` に保存する。
4. ZIP 内の JSON と画像を LINE 互換の `theme/load` へ展開する。
5. LINE 側の `ThemePackageName` をデフォルトテーマ ID に戻す。
6. LEMON 設定に実テーマ ID と有効フラグを保存する。
7. ユーザーが LINE を完全終了して再起動すると反映される。

## 役割分担

| ファイル | 役割 |
|---|---|
| `ThemeManager.java` | バックアップ、ZIP 展開、LINE 設定のサニタイズ、LEMON 設定保存 |
| `ThemeDownloader.java` | テーマ配信 URL の構築、最新バージョン検出、ZIP ダウンロード |
| `ThemeHook.java` | LINE ThemeManager の戻り値を調整し、ローカルテーマを読ませる |
| `SecondaryDeviceThemeHook.java` | サブ端末で隠されるテーマ導線や可用性判定を補助する |
| `LemonSettingsUI.java` | テーマ ID 入力、適用/復元 UI、手動再起動案内 |
| `ThemeStockManager.java` | Theme ストック管理、LINE STORE URL パース、メタ情報保存 |

## Theme タブ (vNext)

- `Info / Settings / Theme` の 3 タブ構成。
- Theme タブで複数テーマをストック管理できる。
- URL または Theme ID を入力してストック追加できる。
- ストックごとに「適用 / 更新して適用 / 情報更新 / ストアで開く / 削除」ができる。
- 適用後は自動再起動せず、手動で LINE を開き直して反映する。

### ストック情報

- 名前
- 概要
- サムネイル URL
- 作者名
- 現在バージョン（追加時にサーバーから最新を再取得して保存）
- 前回バージョン（更新時に保持）

### バックアップ

- 従来の `theme/load.lemon_backup` は互換用として維持。
- 追加で `theme/backups/load_yyyyMMdd_HHmmss_<suffix>/` 形式の世代バックアップを作成可能。
- Theme タブからバックアップ一覧を選んで復元可能。

## フック仕様

| クラス | メソッド | LEMON の処理 |
|---|---|---|
| `fo5.k` | `C(Context)` | 残留した実テーマ ID をデフォルト ID に戻す |
| `fo5.k` | `n()` | LEMON テーマ有効時もデフォルトテーマ ID を返す |
| `fo5.k` | `z()` | LEMON テーマ JSON がある場合だけ `false` を返す |
| `t88.k` | `b(Context)` | テーマ機能の可用性を `true` にする |
| `pp4.q2$z0` | `invokeSuspend(Object)` | テーマ設定項目の表示判定を `true` にする |
| `r88.a` | `e()` | 登録完了状態を `true` にする |

## デフォルトテーマ ID

```txt
3e261192-3a69-4849-b35d-35aeddd5a368
```

この ID は LINE に「公式デフォルトテーマを使っている」と見せるために使います。LEMON が適用した実テーマ ID は LEMON 側の SharedPreferences に保存します。

## ZIP 展開ルール

- ZIP 内の `*.json` は `theme/load/theme.json` として保存する。
- `images/*` は `theme/load/images/` に保存する。
- `timeline/*` は `theme/load/timeline/` に保存する。
- `groupboard/*` は `theme/load/groupboard/` に保存する。
- `../` や絶対パスを含む ZIP エントリは拒否する。
- 未対応のエントリは読み飛ばす。

## 失敗しやすい実装

- 実テーマ ID を `ThemeManager.ThemePackageName` に書く。
- `theme/<id>/themefile` にだけ配置する。
- `fo5.k#z()` を常時 `true` または常時 `false` に固定する。
- 適用完了時に自動で `killProcess()` する。
- ZIP エントリを検証せずに展開する。

## 検証

```powershell
cd E:\projects\LEMON\lemon
.\gradlew.bat assembleDebug
```

実機確認では、適用完了後に LINE を手動で完全終了し、再起動してテーマ反映を見る。
