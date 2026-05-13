# LINE Analysis Details

LEMON が現在参照している LINE 解析結果です。外部ライブラリや未使用機能の解析は省略しています。

## RegistrationActivity

UI dump から、登録/ログイン導線は単一 Activity 内で Compose 画面が切り替わる構造です。

| 要素 | 内容 |
|---|---|
| Activity | `jp.naver.line.android.activity.registration.RegistrationActivity` |
| Compose root | `androidx.compose.ui.platform.ComposeView` |
| 判定補助 | `toolbar_container` の高さ変化 |
| LEMON ボタン | DecorView に追加し、画面状態で表示/非表示 |

## AI 導線

| リソース | ID/名前 | 用途 |
|---|---|---|
| AI Chip Bar | `ai_chip_bar` / fallback `0x7f0b068b` | チャット画面の AI パネル |
| AI Input Button | `ai_input_button` / fallback `0x7f0b0772` | 入力欄付近の AI ボタン |
| Main Tab AI | `main_tab_ai_entry_icon_container` | メインタブの AI アイコン |

これらを検出し、LEMON 設定 UI への導線に置き換えます。

## プロフィール

| 項目 | 値 |
|---|---|
| DB | `/data/user/0/jp.naver.line.android/databases/contact` |
| Table | `contacts` |
| 主な列 | `mid`, `profile_name`, `overridden_name`, `address_book_name`, `picture_path` |
| CDN | `https://profile.line-scdn.net` |

`picture_path` は CDN ベース URL と結合して画像を読み込みます。

## Chat ID

| 種別 | 参照先 |
|---|---|
| MID | `contact.contacts` |
| GID | `naver_line.groups` |

トーク一覧の共通ダイアログから表示名を取り、DB で MID/GID を解決します。

## Read Receipt UI

| 項目 | 解析結果 |
|---|---|
| チャットヘッダー中央グループ | `main_view_group` |
| トーク一覧の名前 | `name` |
| ミュート記号 | `notification_off` |

既読無効化の UI は上記 ID を基準に注入します。LINE 更新で ID が変わる場合は再解析が必要です。

## Theme

| 項目 | 解析結果 |
|---|---|
| Theme JSON | `theme/load/theme.json` |
| 画像 | `theme/load/images/*` |
| タイムライン | `theme/load/timeline/*` |
| グループボード | `theme/load/groupboard/*` |
| デフォルトテーマ ID | `3e261192-3a69-4849-b35d-35aeddd5a368` |

`fo5.k#z()` は「現在テーマがデフォルトか」を返す判定として扱います。LEMON テーマが有効で `theme/load/theme.json` が存在する場合だけ `false` にします。
