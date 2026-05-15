@echo off
setlocal

set "PROJECT_ROOT=%~dp0.."
set "LEMON_DIR=%PROJECT_ROOT%\lemon"
set "APK_PATH=%LEMON_DIR%\app\build\outputs\apk\debug\app-debug.apk"
set "LINE_PACKAGE=jp.naver.line.android"

set "COMMAND=%~1"

if "%COMMAND%"=="" goto usage
if /I "%COMMAND%"=="all" goto all_process
if /I "%COMMAND%"=="build_install" goto build_install
if /I "%COMMAND%"=="build" goto build_only
if /I "%COMMAND%"=="install" goto install_only
if /I "%COMMAND%"=="start" goto start_line
if /I "%COMMAND%"=="stop" goto stop_line
if /I "%COMMAND%"=="logs" goto view_logs
if /I "%COMMAND%"=="restart" goto clear_restart
if /I "%COMMAND%"=="dump" goto dump_ui

:usage
echo ========================================
echo LEMON Tools
echo ========================================
echo.
echo Usage: tool.bat [command]
echo.
echo Commands:
echo   all             - Build, install, and restart LINE
echo   build_install   - Build and install module
echo   build           - Build only
echo   install         - Install only
echo   start           - Start LINE
echo   stop            - Stop LINE
echo   logs            - View recent LEMON logs
echo   restart         - Clear logs and restart LINE
echo   dump            - Dump UI hierarchy
echo.
goto exit

:all_process
call :build_only
if errorlevel 1 goto exit
call :install_only
if errorlevel 1 goto exit
goto clear_restart

:build_install
call :build_only
if errorlevel 1 goto exit
call :install_only
goto exit

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
goto exit

:stop_line
echo.
echo Stopping LINE...
adb shell am force-stop %LINE_PACKAGE%
goto exit

:view_logs
echo.
echo Recent LEMON logs:
adb logcat -d | powershell -NoProfile -Command "$input | Select-String 'LEMON' | Select-Object -Last 40"
goto exit

:clear_restart
echo.
echo Clearing logs and restarting LINE...
adb logcat -c
adb shell am force-stop %LINE_PACKAGE%
adb shell monkey -p %LINE_PACKAGE% -c android.intent.category.LAUNCHER 1
timeout /t 3 /nobreak >nul
adb logcat -d | powershell -NoProfile -Command "$input | Select-String 'LEMON' | Select-Object -Last 40"
goto exit

:dump_ui
echo.
echo Dumping UI hierarchy...
adb shell uiautomator dump /sdcard/lemon_dump.xml
adb pull /sdcard/lemon_dump.xml "%PROJECT_ROOT%\xmls\lemon_dump.xml"
powershell -NoProfile -Command "Select-String -Path '%PROJECT_ROOT%\xmls\lemon_dump.xml' -Pattern 'LEMON' | ForEach-Object { $_.Line }"
goto exit

:exit
endlocal
exit /b 0
