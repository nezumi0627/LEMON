# AI Handoff Protocol

別AIへ即時交代するための運用ファイルです。  
このファイルと末尾テンプレを読ませれば、現状と次アクションを短時間で引き継げます。

## 使い方

1. 作業開始時にこのファイルを最新化する。  
2. 交代時は `Current Snapshot` と `Next Actions` を必ず更新する。  
3. 新しいAIには `AGENTS.md` とこのファイルを最初に読ませる。  

## Current Snapshot

- Project: `LEMON` (`E:\projects\LEMON`)
- Target App: `jp.naver.line.android` (`LINE v26.6.1`)
- Current Module Version: `v0.2.0`
- Branch: `main`
- Remote: `origin = https://github.com/nezumi0627/LEMON.git`
- Repo Visibility: `PRIVATE`

## Non-Negotiables

- `StringReplacementManager` / `ResourceHook` の置換ロジックは実機検証済みとして扱う。
- Theme は `theme/load/theme.json` + `images|timeline|groupboard` 展開方式を維持する。
- `ThemeManager.ThemePackageName` はデフォルトID維持（実テーマIDを書かない）。
- 適用後に LINE の自動 kill/restart はしない（手動再起動案内）。
- 一時ファイルをリポジトリルートへ散らかさない。

## Build / Deploy Routine

```powershell
cd E:\projects\LEMON\lemon
.\gradlew.bat assembleDebug
adb shell am force-stop jp.naver.line.android
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Quick Map

- Entry: `lemon/app/src/main/java/io/github/nezumi0627/lemon/LemonEntry.java`
- Hooks registry: `.../core/HookDispatcher.java`
- Settings UI: `.../ui/LemonSettingsUI.java`
- Theme core: `.../utils/ThemeManager.java`, `ThemeDownloader.java`, `ThemeStockManager.java`
- Docs index: `docs/README.md`

## Next Actions

- （ここに直近タスクを3-7行で書く）
- 例: Themeタブの詳細画面を分離
- 例: 既読系フックの回帰確認

## Open Risks

- LINE更新に伴う難読化名変更でフック無効化の可能性あり。
- 反映確認は実機再起動前提（再起動忘れで未反映に見える）。

## Handoff Template (Copy/Paste)

```md
## Handoff
- Date:
- Owner:
- Branch / Commit:
- Done:
- Not Done:
- Files Touched:
- Test:
- Deploy:
- Blockers:
- Next 3 Steps:
```

