# UI Style Guide

LEMON の設定 UI は LINE のネイティブ設定画面に寄せた、軽いリスト UI を基本にします。

## 色

| 用途 | 色 |
|---|---|
| 背景 | `#FFFFFF` |
| 画面背景 | `#F4F4F4` |
| 主テキスト | `#333333` |
| 補助テキスト | `#888888` |
| セクション見出し | `#608bc5` |
| 区切り線 | `#EFEFEF` |
| LEMON ブランド | `#FFF352` |

## コンポーネント

| コンポーネント | 実装 | 用途 |
|---|---|---|
| Header | `createNativeHeader` | フルスクリーン設定画面の上部 |
| Section Header | `createSectionHeader` | 設定カテゴリ |
| Switch Item | `createSwitchItem` | boolean 設定 |
| Navigation Item | `createNavigationItem` | 詳細画面/操作画面への遷移 |
| Profile Section | `createProfileSection` | MID、名前、アイコン表示 |
| Lemon AI Button | `createLemonAiButton` | LINE AI 導線の置換 |

## 実装方針

- LINE 内に動的 View として差し込むため、過度な依存ライブラリは使わない。
- AI 導線置換ボタンは `🍋` 絵文字表示を基本とし、外部画像差し替えに依存しない。
- 画面遷移は右スライドまたは下スライドに統一する。
- ダミー UI は置かない。
- 文字が長い項目は説明テキストに逃がし、ボタン内に詰め込まない。
- 高頻度に再生成される View ではログを出さない。
