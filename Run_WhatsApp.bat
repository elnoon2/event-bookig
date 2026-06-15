@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

echo ============================================================
echo   Badya WhatsApp Notification Service
echo ============================================================
echo.

:: 1. Locate the service folder
set "WHATSAPP_DIR=%~dp0whatsapp-service"
if not exist "%WHATSAPP_DIR%" set "WHATSAPP_DIR=%~dp0PROJECT2\whatsapp-service"
if not exist "%WHATSAPP_DIR%\index.js" (
    echo [ERROR] Could not find whatsapp-service\index.js
    pause
    exit /b 1
)

:: 2. Find Node.js / npm (PATH first, then the default install folder)
set "NODE_BIN="
set "NPM_BIN="
where node >nul 2>&1 && ( set "NODE_BIN=node" & set "NPM_BIN=npm" )
if not defined NODE_BIN if exist "C:\Program Files\nodejs\node.exe" (
    set "NODE_BIN=C:\Program Files\nodejs\node.exe"
    set "NPM_BIN=C:\Program Files\nodejs\npm.cmd"
)
if not defined NODE_BIN (
    echo [ERROR] Node.js is not installed.
    echo         Install it from https://nodejs.org and run this again.
    pause
    exit /b 1
)
echo [OK] Node.js: !NODE_BIN!

:: 3. Install dependencies on first run
cd /d "%WHATSAPP_DIR%"
if not exist "node_modules" (
    echo [+] First run - installing dependencies ^(this can take a few minutes^)...
    call "!NPM_BIN!" install
    if errorlevel 1 (
        echo [ERROR] npm install failed. Check your internet connection and try again.
        pause
        exit /b 1
    )
)

:: 4. Start the service
echo.
echo ============================================================
echo  WhatsApp service starting...
echo  On first use, a QR code appears below - scan it with
echo  WhatsApp on your phone:  Settings ^> Linked devices.
echo  Keep this window open while the system is running.
echo ============================================================
echo.
"!NODE_BIN!" index.js

echo.
echo [i] WhatsApp service stopped.
pause
