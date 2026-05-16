# LINE データベース解析

## 概要

LINEアプリのデータベース構造と各テーブルの詳細について記述。

## データベース一覧

### naver_line.db

**端末側の場所**: `/data/user/0/jp.naver.line.android/databases/naver_line`

**サイズ**: 約 6.2 MB

**概要**: LINEの主要なデータを保持するメインデータベース。チャット、メッセージ、設定、スタンプ等の情報が含まれる。

#### テーブル一覧

| テーブル名 | 説明 |
|-----------|------|
| android_metadata | Androidメタデータ |
| my_theme | ユーザーのテーマ情報 |
| chat | チャットルーム情報 |
| reactions | リアクション情報 |
| chat_history | メッセージ履歴 |
| setting | アプリ設定 |
| chat_member | チャットメンバー |
| sticker | スタンプ詳細 |
| chat_notification | 通知設定 |
| sticker_auto_suggestion_tag_map | スタンプ自動提案タグ |
| group_home | グループホーム |
| groups | グループ情報 |
| sticker_package | スタンプパッケージ |
| membership | メンバーシップ |
| sticker_packages | (スキーマ未確認) |
| message_request_box | メッセージリクエスト |
| stickers | (スキーマ未確認) |
| multiple_image_message_mapping | 複数画像メッセージ |
| version | バージョン情報 |

#### 主要テーブル詳細

##### chat

チャットルームの基本情報を保持。

| カラム名 | 型 | 説明 |
|---------|------|------|
| chat_id | TEXT | チャットID (主キー) |
| chat_name | TEXT | チャット名 |
| owner_mid | TEXT | オーナーのMID |
| last_from_mid | TEXT | 最後のメッセージ送信者のMID |
| last_message | TEXT | 最後のメッセージ内容 |
| last_created_time | TEXT | 最後のメッセージ作成時間 |
| message_count | INTEGER | メッセージ数 |
| read_message_count | INTEGER | 既読メッセージ数 |
| type | INTEGER | チャットタイプ |
| is_notification | INTEGER | 通知有無 |
| skin_key | TEXT | スキンキー |
| input_text | TEXT | 入力テキスト |
| is_archived | INTEGER | アーカイブ有無 |

##### chat_history

メッセージ履歴を保持。

| カラム名 | 型 | 説明 |
|---------|------|------|
| id | INTEGER | メッセージID (主キー) |
| server_id | TEXT | サーバーID |
| type | INTEGER | メッセージタイプ |
| chat_id | TEXT | チャットID |
| from_mid | TEXT | 送信者MID |
| content | TEXT | メッセージ内容 |
| created_time | TEXT | 作成時間 |
| delivered_time | TEXT | 配信時間 |
| status | INTEGER | ステータス |
| sent_count | INTEGER | 送信数 |
| read_count | INTEGER | 既読数 |
| attachement_image | INTEGER | 添付画像 |
| attachement_local_uri | TEXT | 添付ファイルのローカルURI |
| chunks | BLOB | チャンクデータ |

##### setting

アプリ設定を保持。多くの値は暗号化されている。

| カラム名 | 型 | 説明 |
|---------|------|------|
| key | TEXT | 設定キー (主キー) |
| value | TEXT | 設定値 |

**主要設定キー**:
- `PROFILE_NAME` - ユーザー名 (暗号化)
- `PROFILE_MID` - ユーザーMID (暗号化)
- `PROFILE_ID` - ユーザーID (暗号化)
- `PROFILE_REGION` - リージョン (暗号化)
- `APP_CURRENT_VERSION` - アプリバージョン
- `CHANNEL_ENCRYPTED` - チャンネル暗号化フラグ
- `OBS_ENCRYPTED_ACCESS_TOKEN` - アクセストークン (暗号化)

##### contacts

連絡先情報を保持。

| カラム名 | 型 | 説明 |
|---------|------|------|
| mid | TEXT | MID (主キー) |
| contact_type | INTEGER | コンタクトタイプ |
| profile_updated_time_millis | INTEGER | プロフィール更新時間 |
| profile_name | TEXT | プロフィール名 |
| picture_status | TEXT | 画像ステータス |
| picture_path | TEXT | 画像パス |
| status_message | TEXT | ステータスメッセージ |
| status_message_metadata | TEXT | ステータスメッセージメタデータ |
| music_profile_json | TEXT | 音楽プロフィールJSON |
| video_profile_json | TEXT | 動画プロフィールJSON |
| friend_type | INTEGER | フレンドタイプ |
| friend_updated_time_millis | INTEGER | フレンド更新時間 |
| favorite_time_millis | INTEGER | お気に入り時間 |
| overridden_name | TEXT | 上書き名 |
| bot_category | INTEGER | ボットカテゴリ |

##### groups

グループ情報を保持。

| カラム名 | 型 | 説明 |
|---------|------|------|
| id | TEXT | グループID (主キー) |
| name | TEXT | グループ名 |
| picture_status | TEXT | 画像ステータス |
| creator | TEXT | 作成者 |
| status | INTEGER | ステータス |
| is_first | INTEGER | 最初のメンバーか |
| display_type | INTEGER | 表示タイプ |
| created_time | INTEGER | 作成時間 |
| updated_time | INTEGER | 更新時間 |
| invitation_ticket | TEXT | 招待チケット |
| invitation_enabled | INTEGER | 招待有効か |

##### reactions

リアクション情報を保持。

| カラム名 | 型 | 説明 |
|---------|------|------|
| server_message_id | INTEGER | サーバーメッセージID (主キー) |
| member_id | TEXT | メンバーID (主キー) |
| chat_id | TEXT | チャットID |
| reaction_time_millis | INTEGER | リアクション時間 |
| reaction_type | TEXT | リアクションタイプ |
| custom_reaction | TEXT | カスタムリアクション |

##### sticker_package

スタンプパッケージ情報を保持。

| カラム名 | 型 | 説明 |
|---------|------|------|
| package_id | INTEGER | パッケージID (主キー) |
| name | TEXT | パッケージ名 |
| version | INTEGER | バージョン |
| sticker_type | INTEGER | スタンプタイプ |
| sticker_size | INTEGER | スタンプサイズ |
| author_id | INTEGER | 作者ID |
| is_default | INTEGER | デフォルトか |
| is_subscription | INTEGER | サブスクリプションか |
| is_sendable | INTEGER | 送信可能か |
| expiration_time_millis | INTEGER | 有効期限 |
| valid_days | INTEGER | 有効日数 |
| download_status | INTEGER | ダウンロードステータス |

### contact.db

**端末側の場所**: `/data/user/0/jp.naver.line.android/databases/contact`

**サイズ**: 約 163 KB

**概要**: 連絡先とルーム情報を保持するデータベース。

#### テーブル一覧

| テーブル名 | 説明 |
|-----------|------|
| android_metadata | Androidメタデータ |
| contacts | 連絡先情報 |
| room_master_table | ルームマスター |
| unregistered_contacts | 未登録連絡先 |

#### 主要テーブル詳細

##### contacts

連絡先情報を保持。naver_line.dbのcontactsテーブルと同様の構造。

| カラム名 | 型 | 説明 |
|---------|------|------|
| mid | TEXT | MID (主キー) |
| contact_type | INTEGER | コンタクトタイプ |
| profile_updated_time_millis | INTEGER | プロフィール更新時間 |
| profile_name | TEXT | プロフィール名 |
| picture_status | TEXT | 画像ステータス |
| picture_path | TEXT | 画像パス |
| status_message | TEXT | ステータスメッセージ |
| friend_type | INTEGER | フレンドタイプ |

##### room_master_table

ルームマスター情報を保持。

| カラム名 | 型 | 説明 |
|---------|------|------|
| id | INTEGER | ID (主キー) |
| identity_hash | TEXT | IDハッシュ |

##### unregistered_contacts

未登録連絡先情報を保持。

| カラム名 | 型 | 説明 |
|---------|------|------|
| mid | TEXT | MID (主キー) |

## データ取得方法

```powershell
# データベースを端末から取得
adb shell "su -c 'cp /data/user/0/jp.naver.line.android/databases/naver_line /sdcard/tmp_nl && cp /data/user/0/jp.naver.line.android/databases/contact /sdcard/tmp_ct && chmod 644 /sdcard/tmp_nl /sdcard/tmp_ct'"
adb pull /sdcard/tmp_nl dbs\naver_line.db
adb pull /sdcard/tmp_ct dbs\contact.db
```

## 注意事項

- 多くの設定値は暗号化されている
- MIDはLINEのユーザー識別子
- タイムスタンプはミリ秒単位のUnixタイムスタンプ
- データベースの構造はLINEのバージョンによって変更される可能性がある
