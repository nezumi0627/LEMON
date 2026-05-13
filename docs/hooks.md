# Hooks

LINE v26.6.1 系で使用しているフックポイントの一覧です。難読化名は LINE 更新で変わる可能性があります。

## Resource

| 対象 | 用途 |
|---|---|
| `android.content.res.Resources#getText(int)` | 文字列リソース置換 |
| `android.content.res.Resources#getString(int)` | 文字列リソース置換 |

置換内容は `StringReplacementManager` が管理します。ここは実機で確認済みのため、文言や ID は不用意に変えません。

## Registration

| 対象 | 用途 |
|---|---|
| `android.app.Activity#onResume()` | RegistrationActivity 表示時に LEMON ボタンを注入 |
| `android.app.Activity#onPause()` | 画面離脱時にボタンを削除 |
| `toolbar_container` layout change | 同一 Activity 内の画面状態を判定 |

## Read History

| 対象 | 用途 |
|---|---|
| `Activity#onResume()` | Chat 画面で既読履歴を記録 |
| `Dialog#show()` | トーク長押しダイアログに履歴表示項目を追加 |

## AI

| 対象 | 用途 |
|---|---|
| `Activity#onResume()` | Chat/Main 画面で AI 導線を監視 |
| `ChatListFragment#onViewCreated(View, Bundle)` | メインタブの AI アイコンを LEMON 設定ボタンへ置換 |
| `ai_chip_bar` | チャット画面の AI パネルを LEMON パネルへ置換 |
| `ai_input_button` | 入力欄付近の AI ボタンを LEMON ボタンへ置換 |

## Theme

| 対象 | 用途 |
|---|---|
| `fo5.k#C(Context)` | LINE のテーマ ID 設定をデフォルトへ戻す |
| `fo5.k#n()` | LEMON テーマ有効時もデフォルトテーマ ID を返す |
| `fo5.k#z()` | `theme/load/theme.json` がある場合だけ非デフォルト扱いにする |
| `t88.k#b(Context)` | テーマ機能の可用性判定を許可 |
| `pp4.q2$z0#invokeSuspend(Object)` | テーマ設定項目の表示を許可 |
| `t88.k` constructor | 空テーマリストにデフォルト ID を補う |
| `r88.a#e()` | 登録完了状態を許可 |
| `deviceattestation.a#f()` | サブ端末周辺の追加検証を抑制 |

テーマ詳細は [theme.md](theme.md) を参照してください。

## AdBlock

| 対象 | 用途 |
|---|---|
| `SmartChannelViewLayout#dispatchDraw(Canvas)` | プロモーション枠の親 View を非表示 |
| `LadAdView#onAttachedToWindow()` | インフィード広告の高さを 0 にして非表示 |
| `ViewGroup#addView(View, LayoutParams)` | 動的に追加される広告 View を検知して非表示 |

## Chat ID Copy

| 対象 | 用途 |
|---|---|
| `Dialog#show()` | トーク一覧の共通ダイアログに MID/GID コピー項目を追加 |
| `contact` DB | 個人 MID の解決 |
| `naver_line` DB | グループ ID の解決 |

## Read Receipt

| 対象 | 用途 |
|---|---|
| `Activity#onResume()` | Chat 画面ヘッダーへ既読無効化トグル（目/スラッシュ）を注入 |
| `Activity#onPause()` | Chat 画面離脱時に現在チャットIDをクリアして判定残留を防止 |
| `Dialog#show()` | トーク長押しダイアログにチャット個別の既読無効化 ON/OFF 項目を追加 |
| `MainActivity` のトーク一覧レイアウト監視 | 個別ONチャット名の横へ赤い既読無効化シンボルを追加 |
| `LegacyTalkServiceClientImpl#r1`, `org.apache.thrift.l#b` | 引数から解決した chatId を優先し、個別OFFチャットにはブロックを適用しない |
| 既知候補メソッド (`sendChatChecked` 等) | チャット個別ON時の既読送信呼び出しを抑止（難読化変化に注意） |
