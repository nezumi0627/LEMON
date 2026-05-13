@echo off
setlocal

set "PROJECT_ROOT=%~dp0.."
set "LEMON_DIR=%PROJECT_ROOT%\lemon"
set "APK_PATH=%LEMON_DIR%\app\build\outputs\apk\debug\app-debug.apk"
set "LINE_PACKAGE=jp.naver.line.android"

:menu
cls
echo ========================================
echo LEMON Tools
echo ========================================
echo.
echo 1. Build and install module
echo 2. Build only
echo 3. Install only
echo 4. Start LINE
echo 5. Stop LINE
echo 6. View recent LEMON logs
echo 7. Clear logs and restart LINE
echo 8. Dump UI hierarchy
echo 0. Exit
echo.
set /p choice="Choose an option (0-8): "

if "%choice%"=="1" goto build_install
if "%choice%"=="2" goto build_only
if "%choice%"=="3" goto install_only
if "%choice%"=="4" goto start_line
if "%choice%"=="5" goto stop_line
if "%choice%"=="6" goto view_logs
if "%choice%"=="7" goto clear_restart
if "%choice%"=="8" goto dump_ui
if "%choice%"=="0" goto exit
goto invalid

:build_install
call :build_only
if errorlevel 1 goto pause_menu
call :install_only
goto pause_menu

:build_only
echo.
echo Building LEMON module...
pushd "%LEMON_DIR%"
call gradlew.bat assembleDebug
set "RESULT=%ERRORLEVEL%"
popd
exit /b %RESULT%

:install_only
echo.
echo Installing LEMON module...
if not exist "%APK_PATH%" (
    echo APK not found. Build first: %APK_PATH%
    exit /b 1
)
adb install -r "%APK_PATH%"
exit /b %ERRORLEVEL%

:start_line
echo.
echo Starting LINE...
adb shell monkey -p %LINE_PACKAGE% -c android.intent.category.LAUNCHER 1
goto pause_menu

:stop_line
echo.
echo Stopping LINE...
adb shell am force-stop %LINE_PACKAGE%
goto pause_menu

:view_logs
echo.
echo Recent LEMON logs:
adb logcat -d | powershell -NoProfile -Command "$input | Select-String 'LEMON' | Select-Object -Last 40"
goto pause_menu

:clear_restart
echo.
echo Clearing logs and restarting LINE...
adb logcat -c
adb shell am force-stop %LINE_PACKAGE%
adb shell monkey -p %LINE_PACKAGE% -c android.intent.category.LAUNCHER 1
timeout /t 3 /nobreak >nul
adb logcat -d | powershell -NoProfile -Command "$input | Select-String 'LEMON' | Select-Object -Last 40"
goto pause_menu

:dump_ui
echo.
echo Dumping UI hierarchy...
adb shell uiautomator dump /sdcard/lemon_dump.xml
adb pull /sdcard/lemon_dump.xml "%PROJECT_ROOT%\xmls\lemon_dump.xml"
powershell -NoProfile -Command "Select-String -Path '%PROJECT_ROOT%\xmls\lemon_dump.xml' -Pattern 'LEMON' | ForEach-Object { $_.Line }"
goto pause_menu

:invalid
echo.
echo Invalid option. Choose 0-8.
goto pause_menu

:pause_menu
echo.
pause
goto menu

:exit
endlocal
exit /b 0
