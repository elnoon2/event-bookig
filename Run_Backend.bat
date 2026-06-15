@echo off
setlocal
title Badya_Backend
cd /d "%~dp0backend"

echo ============================================================
echo   Badya University - Backend (Spring Boot)
echo ============================================================
echo.

:: Find Maven: prefer the known full path (most reliable), else fall back to PATH.
set "MVN=C:\Users\LAP TREND\Downloads\apache-maven-3.9.14-bin\apache-maven-3.9.14\bin\mvn.cmd"
if not exist "%MVN%" (
    for /f "delims=" %%i in ('where mvn 2^>nul') do set "MVN=%%i"
)
if not exist "%MVN%" (
    echo [ERROR] Maven not found. Install Maven or fix the path in this file.
    pause
    exit /b 1
)
echo [OK] Using Maven: %MVN%
echo [+] Starting backend on http://localhost:5000  (first run may take a minute)
echo.

call "%MVN%" spring-boot:run

echo.
echo ============================================================
echo  Backend stopped. Read any error message above.
echo ============================================================
pause
