@echo off
REM REON Music App - Quick Deployment Script
echo ========================================
echo REON Music App - Quick Deployment
echo ========================================
echo.

echo Cleaning previous builds...
call gradlew.bat clean
if %ERRORLEVEL% NEQ 0 (
    echo Clean failed!
    pause
    exit /b 1
)

echo.
echo Building Full Release APK...
call gradlew.bat assembleFullRelease
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ Build successful!
    echo.
    echo APK Location: app\build\outputs\apk\full\release\app-full-release.apk
) else (
    echo.
    echo ✗ Build failed!
    pause
    exit /b 1
)

echo.
echo Building Release AAB (for Play Store)...
call gradlew.bat bundleFullRelease
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ AAB Build successful!
    echo.
    echo AAB Location: app\build\outputs\bundle\fullRelease\app-full-release.aab
) else (
    echo.
    echo ✗ AAB Build failed!
)

echo.
echo ========================================
echo Deployment Build Complete!
echo ========================================
echo.
pause

