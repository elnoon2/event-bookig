#!/usr/bin/env bash
#
# Microsoft "Sign in with Microsoft" credentials.
#
# 1) Copy this file:           cp microsoft-env.example.sh microsoft-env.sh
# 2) Paste your 2 values from the Entra app registration (CLIENT_ID + CLIENT_SECRET).
# 3) Load them and start the app:
#        source ./microsoft-env.sh && ./run_project.sh
#
# microsoft-env.sh is gitignored so your secret is never committed.

# Application (client) ID — Entra > App registrations > your app > Overview
export MICROSOFT_CLIENT_ID="PASTE_APPLICATION_CLIENT_ID_HERE"

# Client secret VALUE (not the Secret ID) — Entra > Certificates & secrets > New client secret
export MICROSOFT_CLIENT_SECRET="PASTE_CLIENT_SECRET_VALUE_HERE"

# "common" = personal + work + school + university accounts (multi-tenant). Leave as-is.
export MICROSOFT_TENANT_ID="common"

# Must match the Redirect URI you register in Entra EXACTLY.
export MICROSOFT_REDIRECT_URI="http://localhost:5000/api/auth/microsoft/callback"

# Optional: restrict to one email domain (leave empty to allow any Microsoft account).
export MICROSOFT_ALLOWED_EMAIL_DOMAIN=""

echo "[ok] Microsoft env vars loaded (client id: ${MICROSOFT_CLIENT_ID:0:8}...)."
