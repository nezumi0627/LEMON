# Build

LEMON のビルドと導入手順です。

## 必要環境

- Windows PowerShell
- JDK 17 以上
- Android SDK
- Gradle Wrapper 同梱の `gradlew.bat`
- Rooted Android device
- LSPosed または Vector

## ビルド

```powershell
cd E:\projects\LEMON\lemon
.\gradlew.bat assembleDebug
```

APK:

```txt
E:\projects\LEMON\lemon\app\build\outputs\apk\debug\app-debug.apk
```

## インストール

```powershell
adb install -r E:\projects\LEMON\lemon\app\build\outputs\apk\debug\app-debug.apk
```

署名不一致で失敗する場合は、既存の LEMON をアンインストールしてから再インストールします。

```powershell
adb uninstall io.github.nezumi0627.lemon
adb install E:\projects\LEMON\lemon\app\build\outputs\apk\debug\app-debug.apk
```

## LSPosed/Vector 設定

1. LEMON モジュールを有効化する。
2. スコープに LINE (`jp.naver.line.android`) を追加する。
3. LINE を完全終了して再起動する。

## よくある確認

```powershell
adb logcat | Select-String LEMON
```

高頻度ログは抑制しているため、通常時は大量に出力されません。調査時は `Logger.d()` の追加場所を限定してください。
