# History

古い環境修復・調査メモです。現在仕様は上位 docs を参照してください。

## 解決済み

- Gradle の文字化け対策として UTF-8 コンソールで確認。
- Android SDK パスを Gradle が参照できるように整理。
- JDK/Gradle の組み合わせを現在の `lemon` プロジェクトでビルド可能な状態に修正。
- 旧 `LineAdMod.java` 由来の構文崩れを廃止し、現在の `io.github.nezumi0627.lemon` パッケージへ整理。

## 現在のビルドコマンド

```powershell
cd E:\projects\LEMON\lemon
.\gradlew.bat assembleDebug
```
