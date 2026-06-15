@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

cls
echo ============================================================
echo      Badya University Event Booking (Final Version)
echo ============================================================
echo.

:: ------------------------------------------------------------
:: 1. Detect Maven
:: ------------------------------------------------------------
set "MVN_EXE=mvn"
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    set "INTEL_PATH=c:\Program Files\JetBrains\IntelliJ IDEA 2025.2.5\plugins\maven\lib\maven3\bin\mvn.cmd"
    set "DL_PATH=C:\Users\LAP TREND\Downloads\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd"
    if exist "!INTEL_PATH!" (
        set "MVN_EXE=!INTEL_PATH!"
    ) else if exist "!DL_PATH!" (
        set "MVN_EXE=!DL_PATH!"
    ) else (
        echo [ERROR] Maven not found! Please install Maven and add it to PATH.
        pause
        exit /b 1
    )
)
echo [OK] Maven: !MVN_EXE!

:: ------------------------------------------------------------
:: 2. Load credentials (Microsoft login + QR secret + admin password)
::    microsoft-env.bat is gitignored. Copy microsoft-env.bat.example to
::    microsoft-env.bat and fill it in to enable Microsoft sign-in.
:: ------------------------------------------------------------
if exist "microsoft-env.bat" (
    call "microsoft-env.bat"
    echo [OK] Loaded credentials from microsoft-env.bat
) else (
    echo [i] microsoft-env.bat not found - Microsoft login will be disabled.
    echo     ^(copy microsoft-env.bat.example to microsoft-env.bat and fill it in^)
)
if defined MICROSOFT_CLIENT_ID (
    echo [OK] Microsoft login is configured.
) else (
    echo [i] Microsoft login NOT configured ^(admin email/password still works^).
)
echo.

:: ------------------------------------------------------------
:: 3. Try to start MySQL (ignored if not installed; dev profile uses H2)
:: ------------------------------------------------------------
echo [+] Attempting to start MySQL ^(optional^)...
net start MySQL80 >nul 2>&1 || net start MySQL >nul 2>&1

:: ------------------------------------------------------------
:: 4. Start the backend (serves the frontend at http://localhost:5000)
:: ------------------------------------------------------------
set "BACKEND_DIR=%~dp0backend"
if not exist "%BACKEND_DIR%" set "BACKEND_DIR=%~dp0PROJECT2\backend"

echo [+] Starting backend server... ^(first run may take a minute^)
start "Badya_Backend" "%~dp0Run_Backend.bat"

:: ------------------------------------------------------------
:: 5. Start the WhatsApp microservice (auto-installs deps on first run)
:: ------------------------------------------------------------
if exist "%~dp0Run_WhatsApp.bat" (
    echo [+] Starting WhatsApp microservice in a separate window...
    start "Badya_WhatsApp_Service" "%~dp0Run_WhatsApp.bat"
) else (
    echo [i] Run_WhatsApp.bat not found - skipping WhatsApp service.
)

:: ------------------------------------------------------------
:: 6. Open the app in the browser once the backend is up
:: ------------------------------------------------------------
echo.
echo [+] Waiting 25 seconds for the backend to start...
timeout /t 25 /nobreak >nul
start "" "http://localhost:5000/index.html"

echo.
echo ============================================================
echo   The system is running!
echo   - App:        http://localhost:5000/index.html
echo   - Admin:      http://localhost:5000/admin.html  (admin@gmail.com / 0000)
echo   - QR scanner: http://localhost:5000/scan.html
echo   Keep the backend window open.
echo ============================================================
pause
