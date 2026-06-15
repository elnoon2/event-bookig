@echo off
setlocal enabledelayedexpansion
set "CF_LOG=test_cf.log"
echo 2026-05-19T23:54:52Z INF ^|  https://warming-angels-see-sing.trycloudflare.com                                         ^| > !CF_LOG!
set "CF_URL="
for /f "usebackq tokens=*" %%a in (`powershell -Command "if (Test-Path '!CF_LOG!') { $txt = Get-Content '!CF_LOG!' -Raw; if ($txt -match '(https://[a-zA-Z0-9-]+\.trycloudflare\.com)') { Write-Host $matches[1] } }"`) do (
    set "CF_URL=%%a"
)
echo Found URL: !CF_URL!

