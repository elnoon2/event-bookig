#!/usr/bin/env bash
#
# Badya University Event Booking — Linux/macOS launcher
# (Shell equivalent of Run_Project.bat)
#
# Usage:
#   ./run_project.sh                 # run backend with the dev profile (H2, no MySQL needed)
#   ./run_project.sh --prod          # run backend with the prod profile (MySQL on localhost:3306)
#   ./run_project.sh --whatsapp      # also start the WhatsApp microservice (needs Node.js)
#   ./run_project.sh --prod --whatsapp
#   ./run_project.sh -h | --help
#
# The backend serves the frontend, so once it is up open:  http://localhost:5000/index.html
#
# Microsoft login: set these in your environment before launching to enable it
#   export MICROSOFT_CLIENT_ID=...      export MICROSOFT_CLIENT_SECRET=...
#   export MICROSOFT_TENANT_ID=...      export MICROSOFT_REDIRECT_URI=...
# (see MICROSOFT_LOGIN_SETUP.md). Admin password: export APP_ADMIN_PASSWORD=... for production.

set -euo pipefail

# --- Resolve the project root (directory of this script) ---
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PROFILE="dev"
START_WHATSAPP=0

for arg in "$@"; do
    case "$arg" in
        --prod) PROFILE="prod" ;;
        --dev) PROFILE="dev" ;;
        --whatsapp) START_WHATSAPP=1 ;;
        -h|--help)
            # Print only the contiguous header comment block (after the shebang).
            awk 'NR==1 && /^#!/ {next} /^#/ {sub(/^# ?/, ""); print; next} {exit}' "$0"
            exit 0
            ;;
        *) echo "[WARN] Unknown option: $arg (ignored)" ;;
    esac
done

echo "============================================================"
echo "   Badya University Event Booking (Final Version)"
echo "   Profile: $PROFILE"
echo "============================================================"
echo

# Auto-load Microsoft credentials if the (gitignored) env file exists, so MS login works
# regardless of how the project is launched.
if [[ -f "$SCRIPT_DIR/microsoft-env.sh" ]]; then
    # shellcheck disable=SC1091
    source "$SCRIPT_DIR/microsoft-env.sh"
fi

# Free port 5000 if a previous instance is still holding it (prevents "Port 5000 already in use").
PORT_PID="$(ss -ltnp 2>/dev/null | grep ':5000 ' | grep -oP 'pid=\K[0-9]+' | head -1)"
if [[ -n "$PORT_PID" ]]; then
    echo "[i] Port 5000 is in use (pid $PORT_PID) — stopping that instance first..."
    kill "$PORT_PID" 2>/dev/null || true
    sleep 3
fi

# --- 1. Check Java (17+) ---
if ! command -v java >/dev/null 2>&1; then
    echo "[ERROR] Java not found. Please install JDK 17+ and ensure 'java' is on PATH."
    exit 1
fi
JAVA_MAJOR="$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+).*/\1/')"
if [[ "${JAVA_MAJOR:-0}" -lt 17 ]]; then
    echo "[ERROR] Java 17+ is required (found Java ${JAVA_MAJOR})."
    exit 1
fi
echo "[OK] Java ${JAVA_MAJOR} detected."

# --- 2. Detect Maven ---
# Order: $MVN override -> mvn on PATH -> ./backend/mvnw wrapper -> standalone Maven in /tmp.
MVN=""
if [[ -n "${MVN:-}" ]] && command -v "${MVN}" >/dev/null 2>&1; then
    :
elif command -v mvn >/dev/null 2>&1; then
    MVN="mvn"
elif [[ -x "backend/mvnw" ]]; then
    MVN="./mvnw"
else
    STANDALONE="$(ls -d /tmp/apache-maven-*/bin/mvn 2>/dev/null | head -1 || true)"
    if [[ -n "$STANDALONE" && -x "$STANDALONE" ]]; then
        MVN="$STANDALONE"
    fi
fi

if [[ -z "$MVN" ]]; then
    echo "[ERROR] Maven not found. Install it (e.g. 'sudo apt-get install maven')"
    echo "        or set MVN=/path/to/mvn and re-run."
    exit 1
fi
echo "[OK] Using Maven: $MVN"

# --- 3. (prod only) hint about MySQL ---
if [[ "$PROFILE" == "prod" ]]; then
    echo "[i] prod profile selected: expecting MySQL at localhost:3306 (db 'badyaunievents')."
    echo "    Import database_schema.sql first if this is a fresh database."
fi

# --- 4. Microsoft login status hint ---
if [[ -n "${MICROSOFT_CLIENT_ID:-}" && -n "${MICROSOFT_CLIENT_SECRET:-}" ]]; then
    echo "[OK] Microsoft login is configured (MICROSOFT_CLIENT_ID set)."
else
    echo "[i] Microsoft login is disabled (MICROSOFT_CLIENT_ID/SECRET not set). Email/password still works."
fi
echo

# --- 5. Optionally start the WhatsApp microservice (background, non-fatal) ---
WHATSAPP_PID=""
if [[ "$START_WHATSAPP" -eq 1 ]]; then
    if command -v node >/dev/null 2>&1 && [[ -d "whatsapp-service" ]]; then
        echo "[+] Starting WhatsApp microservice..."
        (
            cd whatsapp-service
            if [[ ! -d node_modules ]]; then
                echo "[+] Installing WhatsApp dependencies (first run)..."
                npm install --no-audit --no-fund >/tmp/ebs-whatsapp-install.log 2>&1 || \
                    echo "[WARN] npm install failed; see /tmp/ebs-whatsapp-install.log"
            fi
            node index.js
        ) >/tmp/ebs-whatsapp.log 2>&1 &
        WHATSAPP_PID=$!
        echo "[OK] WhatsApp service started (PID $WHATSAPP_PID, logs: /tmp/ebs-whatsapp.log)."
        echo "     Watch that log for the QR code you need to scan."
    else
        echo "[WARN] Skipping WhatsApp service (Node.js missing or whatsapp-service/ not found)."
    fi
    echo
fi

# Stop the background WhatsApp service when the backend exits.
cleanup() {
    if [[ -n "$WHATSAPP_PID" ]] && kill -0 "$WHATSAPP_PID" 2>/dev/null; then
        echo
        echo "[+] Stopping WhatsApp service (PID $WHATSAPP_PID)..."
        kill "$WHATSAPP_PID" 2>/dev/null || true
    fi
}
trap cleanup EXIT INT TERM

# --- 6. Start the backend (foreground; serves the frontend at :5000) ---
echo "[+] Starting backend (this may take a minute on first run)..."
echo "[+] When it is up, open:  http://localhost:5000/index.html"
echo "    Press Ctrl+C to stop."
echo

cd backend
exec "$MVN" spring-boot:run -Dspring-boot.run.profiles="$PROFILE"
