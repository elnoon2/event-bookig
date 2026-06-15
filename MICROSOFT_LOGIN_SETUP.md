# Sign in with Microsoft — Setup & Testing Guide

This document explains how to configure, run, and test **"Sign in with Microsoft"**, which is now the
**ONLY login method for the public user site** of the Badya University Event Booking System. It uses
the OAuth 2.0 **Authorization Code flow with PKCE**, OpenID Connect **ID-token validation**, and the
**Microsoft Graph API** to fetch the user's profile and photo.

- The app is configured **multi-tenant** (`common`): **personal Microsoft, Microsoft 365, university,
  and school** accounts can all sign in. There is no email-domain restriction by default (set
  `MICROSOFT_ALLOWED_EMAIL_DOMAIN` if you want one).
- Email/password **login and registration have been removed** from the public site; the legacy
  endpoints `POST /api/users/authenticate` and `POST /api/users` now return **410 Gone**.
- The separate **admin portal (`admin.html`) keeps its own email/password login** — it is unaffected.

---

## 1. How it works (architecture)

The flow is implemented manually (no Spring Security filter chain), so the existing authentication
keeps working untouched. The frontend ends up with the **same** `localStorage['currentUser']` object
it gets from password login, so the rest of the app behaves identically.

```
Browser                         Backend (Spring Boot)                Microsoft (Entra ID)
  |  click "Sign in with MS"        |                                      |
  |-- GET /api/auth/microsoft/login ->|                                    |
  |                                  | make state+nonce+PKCE, save in       |
  |                                  | HTTP session                         |
  |<--------- 302 to Microsoft ------|------------------ authorize -------->|
  |                                                     (user signs in / consents)
  |<------------------------ 302 back to /callback?code&state --------------|
  |-- GET /api/auth/microsoft/callback ->|                                  |
  |                                  | verify state; exchange code -------->| /token (server-to-server,
  |                                  |<--- id_token + access_token ---------|  client secret over TLS)
  |                                  | validate id_token (sig via JWKS,     |
  |                                  | aud, exp, iss, nonce); enforce domain|
  |                                  | find-or-create User; store result    |
  |                                  | in session                           |
  |<------- 302 to /index.html?msauth=1 ----|                               |
  |-- GET /api/auth/microsoft/session-user ->| (one-time read from session) |
  |<-------- user JSON (same shape as password login) ----|                 |
  | store in localStorage['currentUser'], update UI       |                 |
```

**Files involved**
- Backend: `controller/MicrosoftAuthController.java`, `service/MicrosoftAuthService.java`,
  `service/MicrosoftAuthException.java`, `config/MicrosoftOAuthProperties.java`,
  `service/UserService.java` (`provisionMicrosoftUser`), `model/User.java` (`oauthProvider`,`oauthId`),
  `repository/UserRepository.java` (`findByOauthProviderAndOauthId`).
- Frontend: `index.html` (buttons), `script.js` (`startMicrosoftLogin`, `handleMicrosoftRedirectResult`,
  `checkMicrosoftLoginStatus`), `styles.css` (`.ms-login-btn`).
- Config: `backend/src/main/resources/application.properties` (`app.microsoft.*`).

---

## 2. Register the application in Microsoft Entra ID

1. Go to the **Microsoft Entra admin center** → **Azure Active Directory** → **App registrations**
   → **New registration**.
2. **Name:** e.g. `Badya Event Booking`.
3. **Supported account types:** select
   **"Accounts in any organizational directory (Any Microsoft Entra ID tenant – Multitenant) and
   personal Microsoft accounts (e.g. Skype, Xbox)"**. This admits personal Microsoft, Microsoft 365,
   university, and school accounts. Keep `MICROSOFT_TENANT_ID=common` (the default).
4. **Redirect URI:** platform **Web**, value exactly:
   ```
   http://localhost:5000/api/auth/microsoft/callback
   ```
   (Add your production URL too, e.g. `https://events.badyauni.edu.eg/api/auth/microsoft/callback`.)
5. Click **Register**. Copy the **Application (client) ID** and the **Directory (tenant) ID**.
6. **Certificates & secrets** → **New client secret** → copy the secret **Value** (not the Secret ID).
7. **API permissions** → **Microsoft Graph → Delegated** → add **`User.Read`** (and ensure `openid`,
   `profile`, `email`). `User.Read` is what lets the app read the profile and photo from Graph
   (`/me` and `/me/photo`). Grant admin consent if your tenant requires it.

---

## 3. Required environment variables

| Variable | Required | Example / Default | Purpose |
|---|---|---|---|
| `MICROSOFT_CLIENT_ID` | **Yes** | `11111111-2222-3333-4444-555555555555` | Application (client) ID |
| `MICROSOFT_CLIENT_SECRET` | **Yes** | `abc8Q~secretvalue...` | Client secret **Value** |
| `MICROSOFT_TENANT_ID` | No | `common` (default) — multi-tenant + personal; or a tenant GUID / `organizations` | Which accounts to trust |
| `MICROSOFT_REDIRECT_URI` | No | `http://localhost:5000/api/auth/microsoft/callback` (default) | Must match the Entra redirect URI exactly |
| `MICROSOFT_ALLOWED_EMAIL_DOMAIN` | No | _blank_ (default; allow any verified email) | Optional email-domain allow-list |

The feature stays **disabled** until both `MICROSOFT_CLIENT_ID` and `MICROSOFT_CLIENT_SECRET` are set
(`GET /api/auth/microsoft/status` returns `{"enabled":false}` and the buttons hide themselves).

### Example (Linux/macOS)
```bash
export MICROSOFT_CLIENT_ID="11111111-2222-3333-4444-555555555555"
export MICROSOFT_CLIENT_SECRET="your-secret-value"
export MICROSOFT_TENANT_ID="common"   # personal + work + school accounts
export MICROSOFT_REDIRECT_URI="http://localhost:5000/api/auth/microsoft/callback"
```
### Example (Windows PowerShell)
```powershell
$env:MICROSOFT_CLIENT_ID="11111111-2222-3333-4444-555555555555"
$env:MICROSOFT_CLIENT_SECRET="your-secret-value"
$env:MICROSOFT_TENANT_ID="common"
$env:MICROSOFT_REDIRECT_URI="http://localhost:5000/api/auth/microsoft/callback"
```

> **Security:** the client secret is only ever used server-side in the token exchange; it is never sent
> to the browser. Do not commit it. Keep it in environment variables or a secrets manager.

### User data stored (via Microsoft Graph)
On each sign-in the app stores, in the `users` table: the **Microsoft user id** (`oauth_id` = the
Entra `oid`), **full name**, **email**, and the **profile photo** (`profile_photo_base64`, a data URL
fetched from `GET /me/photo/$value`; null when the account has no photo). The photo is also returned
to the browser as `photoUrl` and shown as the header avatar. Accounts are created automatically on
first login and reused on subsequent logins (matched by `oauth_id`).

---

## 4. Run the application

From the `backend/` directory (so the app serves the frontend from the repo root):
```bash
# Dev (H2 in-memory) — default profile
mvn spring-boot:run
# or build and run the jar
mvn -DskipTests package
java -jar target/event-booking-backend-1.0.0.jar
```
The new `oauth_provider` / `oauth_id` columns are added automatically by Hibernate
(`ddl-auto=update`). For a manual MySQL setup, the columns are also in `database_schema.sql`.

Open `http://localhost:5000/index.html`.

---

## 5. Testing

### 5.1 Without Entra credentials (wiring smoke test)
With the variables unset:
```bash
curl http://localhost:5000/api/auth/microsoft/status
# -> {"enabled":false}

curl -i http://localhost:5000/api/auth/microsoft/login
# -> 302 Location: /index.html?msauth_error=not_configured
```
In the browser the **"Sign in with Microsoft"** buttons hide automatically (status = disabled).

### 5.2 Full end-to-end (with Entra credentials)
1. Set the four environment variables (Section 3) and restart the backend.
2. `curl http://localhost:5000/api/auth/microsoft/status` → `{"enabled":true}`.
3. Open `index.html`, click **Sign In** → **Sign in with Microsoft**. The button shows
   *"Redirecting to Microsoft…"* and the page redirects to Microsoft.
4. Sign in with a **`@badyauni.edu.eg`** account and consent.
5. You are redirected back to `index.html?msauth=1`; the app finishes login, shows
   *"Signed in with Microsoft!"*, and the header switches to the logged-in state.
6. **Verify provisioning** (dev/H2 console at `http://localhost:5000/h2-console`,
   JDBC URL `jdbc:h2:mem:badyaunievents`, user `sa`):
   ```sql
   SELECT id, name, email, role, oauth_provider, oauth_id FROM users WHERE oauth_provider = 'microsoft';
   ```
   You should see one row with `role = USER` and a populated `oauth_id`.
7. **Sign out and sign in again** with the same account → no new row is created (matched by `oauth_id`).
8. **Account linking:** if a password account already existed with that email, signing in with Microsoft
   links it (sets `oauth_provider`/`oauth_id`) instead of creating a duplicate.

### 5.3 Negative tests
- Sign in with a **non-`@badyauni.edu.eg`** account → redirect `…?msauth_error=domain_not_allowed`,
  and the UI shows *"Only badyauni.edu.eg Microsoft accounts are allowed."*
- Cancel at the Microsoft consent screen → `…?msauth_error=access_denied` → *"Microsoft sign-in was cancelled."*
- Let the login session expire (or tamper with `state`) → `…?msauth_error=state_mismatch` → friendly retry message.

---

## 6. Security controls implemented

- **CSRF for OAuth:** random `state`, stored in the session and compared with a constant-time check.
- **Replay protection:** OIDC `nonce`, stored in the session and matched against the ID-token claim.
- **PKCE (S256):** code verifier in the session, code challenge sent to Microsoft.
- **Confidential token exchange:** the authorization code is exchanged server-to-server with the client
  secret over TLS; the secret never reaches the browser.
- **ID-token validation:** RS256 signature verified against the tenant **JWKS**, plus `aud` (= client id),
  `exp`, issuer (`https://login.microsoftonline.com/{tid}/v2.0`), and `nonce`. If a specific tenant GUID
  is configured, the token's `tid` must match it.
- **Domain allow-list:** only `@badyauni.edu.eg` (configurable) accounts are provisioned.
- **No PII in URLs:** the login result is handed off via a one-time HTTP-session read, not query strings.
- **Graceful states:** loading state on the button; friendly error messages for every failure code;
  the feature self-disables when unconfigured.

---

## 7. Troubleshooting

| Symptom | Likely cause / fix |
|---|---|
| `AADSTS50011: redirect URI mismatch` | `MICROSOFT_REDIRECT_URI` must match the Entra redirect URI **exactly** (scheme, host, port, path). |
| `…?msauth_error=token_exchange_failed` | Wrong/expired client secret, or the secret **ID** was used instead of its **Value**. |
| `…?msauth_error=domain_not_allowed` | The account's email isn't in `MICROSOFT_ALLOWED_EMAIL_DOMAIN`. |
| Buttons don't appear | `status` is `false` — client id/secret not set, or the backend isn't reachable. |
| `…?msauth_error=invalid_token` | Tenant/issuer mismatch — check `MICROSOFT_TENANT_ID` vs. the "Supported account types" you chose. |
| Cookie/session lost on return | Use the same host/port throughout; the JSESSIONID cookie (SameSite=Lax) must survive the top-level redirect back from Microsoft. |
