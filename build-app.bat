@echo off
REM REON Music App - Build Script
REM Quick build and deployment script for Windows

echo ========================================
echo REON Music App - Build Script
echo ========================================
echo.

REM Check if gradlew exists
if not exist "gradlew.bat" (
    echo ERROR: gradlew.bat not found!
    echo Please run this script from the project root directory.
    pause
    exit /b 1
)

echo Select build option:
echo 1. Clean build
echo 2. Build debug APK (Full flavor)
echo 3. Build debug APK (FOSS flavor)
echo 4. Build and install on device
echo 5. Run app on device
echo 6. Check for errors
echo 7. Generate build report
echo.

set /p choice="Enter your choice (1-7): "

if "%choice%"=="1" goto clean
if "%choice%"=="2" goto build_full
if "%choice%"=="3" goto build_foss
if "%choice%"=="4" goto install
if "%choice%"=="5" goto run
if "%choice%"=="6" goto check
if "%choice%"=="7" goto report
goto invalid

:clean
echo.
echo Cleaning project...
call gradlew.bat clean
echo.
echo Clean complete!
goto end

:build_full
echo.
echo Building Full Debug APK...
call gradlew.bat assembleFullDebug
echo.
echo Build complete!
echo APK location: app\build\outputs\apk\full\debug\app-full-debug.apk
goto end

:build_foss
echo.
echo Building FOSS Debug APK...
call gradlew.bat assembleFossDebug
echo.
echo Build complete!
echo APK location: app\build\outputs\apk\foss\debug\app-foss-debug.apk
goto end

:install
echo.
echo Building and installing on device...
call gradlew.bat installFullDebug
echo.
echo Installation complete!
goto end

:run
echo.
echo Launching app on device...
adb shell am start -n com.reon.music.debug/com.reon.music.MainActivity
echo.
echo App launched!
goto end

:check
echo.
echo Checking for compilation errors...
call gradlew.bat compileFullDebugKotlin
echo.
echo Check complete!
goto end

:report
echo.
echo Generating build report...
call gradlew.bat assembleFullDebug --scan
echo.
echo Report generated! Check the console output for the URL.
goto end

:invalid
echo.
echo Invalid choice! Please select 1-7.
goto end

:end
echo.
echo ========================================
echo Press any key to exit...
pause >nul
