# AdBlock

広告ブロックは `AdBlockHook` が担当します。LINE の広告 View を描画または追加されるタイミングで非表示にします。

## 対象

| 対象 | 処理 |
|---|---|
| `SmartChannelViewLayout#dispatchDraw(Canvas)` | 親コンテナを `View.GONE` |
| `LadAdView#onAttachedToWindow()` | 外側コンテナの高さを 0 にして `View.GONE` |
| `ViewGroup#addView(View, LayoutParams)` | クラス名から広告系 View を検知して `View.GONE` |

## 設定

設定キー:

```txt
ad_block
```

デフォルトは有効です。設定 UI の `GENERAL > 広告ブロック` から切り替えます。

## ログ方針

広告 View は高頻度で描画・追加されるため、通常の非表示成功ログは出しません。クラス未検出やフック登録失敗など、調査に必要な失敗のみ記録します。
