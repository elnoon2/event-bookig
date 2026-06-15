# Deep Full-System Audit Report — Badya University Event Booking System

**Audit date:** 2026-06-15
**Scope:** Entire repository — Spring Boot backend, vanilla JS frontend, MySQL/H2 schema, WhatsApp microservice, configuration & build.
**Method:** Static analysis with execution-path tracing across files; every finding below was verified against the actual source (file + line). A subset of Critical infrastructure issues were remediated in the same pass (marked **✅ REMEDIATED**); the rest are documented for follow-up.

> Note: this is the same repository to which "Sign in with Microsoft" was added. The OAuth feature was implemented securely (state + nonce + PKCE, server-side token exchange, ID-token signature/claims validation) and is described in `MICROSOFT_LOGIN_SETUP.md`. It does not introduce the issues below.

---

# Executive Summary

The system is a functional university event-booking app with a clean layered backend (Controller → Service → Repository → JPA) and a feature-rich vanilla-JS frontend. However, it is **not production-ready**. The dominant problem is that **there is no real server-side authentication or authorization**: protected actions are gated only by client-side `localStorage`, and the few server checks that exist are optional and unenforced. This is compounded by **concurrency bugs in the core booking flow** (overbooking / duplicate bookings) and **insecure defaults** (repo-root file exposure, default admin password, wildcard CORS with credentials).

| Metric | Value |
|---|---|
| **Overall Health Score** | **42 / 100** |
| Critical Issues | 7 (3 remediated in this pass) |
| High Issues | 7 |
| Medium Issues | 9 |
| Low Issues | 6 |

**Top risks:** broken access control on admin/event endpoints, no server-side auth (privilege escalation via localStorage), and booking race conditions enabling overbooking.

---

# Critical Issues

## C-1. Broken access control on admin & event-management endpoints
- **Severity:** Critical
- **Files:**
  - `backend/.../controller/AdminController.java:122-129` (`createEvent`), `:139-145` (`updateEvent`), `:154-165` (`deleteEvent`)
  - `backend/.../service/EventService.java:112-116` (`updateEvent` — comment: *"Relaxed: any admin can update any event."*)
- **Issue:** `createEvent` / `updateEvent` accept the caller's identity from an HTTP header `@RequestHeader(value = "X-Admin-Id", required = false) Long adminId`. It is **optional** and **never validated** against any admin record. `deleteEvent` takes no identity at all.
- **Why it's a problem:** Any anonymous client can create, modify, or delete any event with a plain HTTP request. There is no Spring Security, no `@PreAuthorize`, and the header (when present) is fully attacker-controlled.
- **Impact:** Complete compromise of event integrity — fraudulent events, defacement, deletion of real events (and their bookings via the cascade in `EventService.deleteEvent`).
- **Root cause:** Authorization was deferred to the frontend; the backend trusts unauthenticated headers.
- **Recommended fix:** Introduce real authentication (signed token/JWT or server session) and enforce `ADMIN` role on these endpoints server-side. Validate the acting admin from the verified principal, not a header.
```java
// Example: verify a bearer token and require ADMIN before mutating events
@PostMapping("/events")
public ResponseEntity<EventDTO> createEvent(@AuthenticationPrincipal AdminPrincipal admin,
                                            @Valid @RequestBody EventDTO dto) {
    // admin is null/!ADMIN -> 401/403 via a SecurityFilterChain
    return ResponseEntity.status(CREATED).body(eventService.createEvent(dto, admin.id()));
}
```

## C-2. No server-side authentication — authorization relies on client `localStorage`
- **Severity:** Critical
- **Files:** `backend/.../config/SecurityConfig.java:12-22` (only a `BCryptPasswordEncoder` bean — no filter chain); `script.js` (role decided from `localStorage`, e.g. `:1448` `updateAuthButtons`, admin checks at `:499-510`, `:1509`).
- **Issue:** The backend never establishes an authenticated principal. After login the frontend stores the user object (including `role`) in `localStorage` and makes trust decisions from it. The server does not re-verify identity or role on subsequent requests.
- **Why it's a problem:** `localStorage` is fully attacker-controlled. Setting `{"role":"admin"}` (or calling the API directly) grants admin-only UI/flows, and the backend does not stop the underlying API calls (see C-1).
- **Impact:** Privilege escalation and impersonation; any user can act as admin or as another student.
- **Root cause:** Stateless design without a server-trust boundary.
- **Recommended fix:** Issue a signed token (JWT) or server session at login; verify it in a filter on every protected request; derive role server-side. Keep email/password + the new Microsoft login as front-door methods feeding the same token issuance.

## C-3. Booking overbooking — time-of-check/time-of-use race condition
- **Severity:** Critical
- **File:** `backend/.../service/BookingService.java:80-95` (capacity check) then `:125` / `:134` (save)
- **Issue:** Capacity is computed with a `SUM` query (`BookingRepository.countTotalTicketsByEventId`, `BookingRepository.java:45`) and compared to `event.getCapacity()`, then a booking is saved later — with no row lock, no `@Version` optimistic lock, and no DB constraint. `@Transactional` at default isolation does not serialize these reads/writes.
- **Why it's a problem:** Concurrent requests all read the same "available" count before any commits, so they all pass the check.
- **Impact:** Events can be overbooked beyond `capacity` under concurrency (e.g., capacity 50 → 100 bookings).
- **Root cause:** Check-then-act without locking or an atomic guard.
- **Recommended fix:** Pessimistically lock the event row (`SELECT ... FOR UPDATE`) for the duration of the check+insert, or add a `@Version` column and retry, or enforce capacity with a DB-level guard. Example:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select e from Event e where e.id = :id")
Optional<Event> findByIdForUpdate(@Param("id") Long id);
// load event with this inside the @Transactional booking method, then re-check the SUM.
```

## C-4. Duplicate bookings — race condition + missing DB uniqueness
- **Severity:** Critical
- **Files:** `backend/.../service/BookingService.java:74-77`; `backend/.../model/Booking.java:11-13` (no composite unique key); `database_schema.sql:90-111` (only non-unique indexes on `event_id`, `student_email`).
- **Issue:** Duplicate prevention is a check-then-act (`existsByEvent_IdAndStudentEmail` → later `save`) with **no unique constraint** at the database level.
- **Impact:** The same user can book the same event multiple times under concurrency, corrupting capacity accounting and attendance.
- **Root cause:** Business rule enforced only in application code.
- **Recommended fix:** Add `UNIQUE KEY uq_bookings_event_email (event_id, student_email)` (and the matching `@Table(uniqueConstraints=...)`), then handle the resulting `DataIntegrityViolationException` as "already booked".

## C-5. Repository-root file exposure via the static handler — ✅ REMEDIATED
- **Severity:** Critical
- **File:** `backend/.../config/WebConfig.java:35-45` (maps `/**` to `Paths.get("..")` = the repo root).
- **Issue:** The whole repository root was web-served, making downloadable: `database_schema.sql` (which contains `INSERT INTO admins ... '0000'` at `database_schema.sql:171`), `test_user.json` (`password123`), `admin-config.js`, the entire `backend/` source tree (including `application.properties` and its `tokenPepper`), `pom.xml`, `*.bat`, and docs.
- **Impact:** Source, schema, and credential disclosure to any anonymous visitor.
- **Fix applied:** Added `backend/.../config/StaticContentSecurityFilter.java`, a `OncePerRequestFilter` (`@Order(0)`) that returns 404 for sensitive prefixes/extensions (`/backend`, `/whatsapp-service`, `/.git`, `*.sql`, `*.properties`, `*.json`, `*.xml`, `*.bat`, `server.js`, …) while leaving `/api`, `/uploads`, `/h2-console` and real assets (`index.html`, `script.js`, `styles.css`, `/images`) untouched. **Verified at runtime:** `/database_schema.sql`, `/test_user.json`, `/backend/pom.xml`, `/server.js` → 404; `/index.html`, `/script.js`, `/styles.css` → 200.
- **Residual:** Best long-term fix is to serve the frontend from a dedicated directory rather than the repo root.

## C-6. Default / hard-coded admin credentials — ✅ REMEDIATED (partial)
- **Severity:** Critical
- **Files:** `backend/src/main/resources/application.properties` (`app.admin.password=0000`), `backend/.../config/DataInitializer.java:42-44,78,85`, `database_schema.sql:170-172`, `admin-config.js` (comment leaked `password: 0000`).
- **Issue:** A well-known weak admin password was hard-coded and documented, and a plaintext admin row was seeded in the SQL.
- **Impact:** Trivial full admin takeover.
- **Fix applied:** Externalized to `${APP_ADMIN_PASSWORD:0000}` / `${APP_ADMIN_EMAIL:…}` in `application.properties`; added a loud startup **SECURITY WARNING** in `DataInitializer` when the `0000` default is in use (verified printed at boot); removed the credential comment from `admin-config.js`; the static filter now blocks downloading `database_schema.sql`.
- **Residual:** The `0000` dev fallback still exists for local use; set `APP_ADMIN_PASSWORD` in every real environment. Consider forcing a password change on first login.

## C-7. CORS wildcard origin combined with credentials — ✅ REMEDIATED
- **Severity:** Critical
- **File:** `backend/.../config/CorsConfig.java:20-23` (was `setAllowedOriginPatterns("*")` + `setAllowCredentials(true)`).
- **Issue:** Allowing any origin together with credentials is unsafe (and spec-violating for real cookies); it invites cross-site authenticated calls.
- **Fix applied:** Replaced with an explicit, configurable allow-list (`app.cors.allowed-origins` ← `CORS_ALLOWED_ORIGINS`), restricted headers, and `setAllowCredentials(false)`. **Verified:** a disallowed `Origin` no longer receives an `Access-Control-Allow-Origin` header.
- **Residual:** See H-7 — per-controller `@CrossOrigin(origins = "*")` annotations remain and should be removed so the central policy is authoritative.

---

# High Severity Issues

## H-1. Payments are simulated client-side; no real gateway
- **Severity:** High
- **Files:** `script.js:~1002-1017` (`validateVisaForm` is client-only; payment "processed" via `await new Promise(r => setTimeout(r, 1600))`); `backend/.../service/BookingService.java:100-110` (server only checks the *string* `VISA`/`CASH`).
- **Impact:** Paid-event bookings can be completed without any real payment; card validation is bypassable via devtools or a direct API call.
- **Fix:** Integrate a real payment provider (Stripe/PayPal) and only confirm bookings after a verified payment webhook; never trust client-side payment state.

## H-2. Insecure file upload (no type/size validation; web-served location)
- **Severity:** High
- **File:** `backend/.../controller/FileUploadController.java:26-51`
- **Issue:** Accepts any `MultipartFile` with no content-type/extension allow-list and no size cap; stores it under `uploads/`, which `WebConfig` serves at `/uploads/**`.
- **Impact:** Arbitrary file hosting (malware/HTML for stored-XSS/phishing) and DoS via large uploads. (Files are served, not executed, so not direct RCE — but still high risk.)
- **Fix:** Whitelist image MIME types, validate magic bytes, cap size (`spring.servlet.multipart.max-file-size`), and serve uploads with `Content-Disposition: attachment` / a non-executable static context. Require authentication.

## H-3. No authentication/rate-limiting on login → brute force
- **Severity:** High
- **Files:** `backend/.../controller/UserController.java:60-79`, `backend/.../controller/AdminController.java:81-101`
- **Impact:** Unlimited password guessing against user/admin accounts (worsened by the weak password policy, H-4).
- **Fix:** Add per-IP/per-account throttling (e.g., Bucket4j/Resilience4j) and lockout/backoff on repeated failures.

## H-4. Weak password policy (min length 4)
- **Severity:** High
- **Files:** `backend/.../model/User.java:36`, `backend/.../model/Admin.java` (`@Size(min = 4, ...)`)
- **Impact:** Easily brute-forced credentials.
- **Fix:** Require ≥ 8–12 chars with complexity/breach checks; align frontend validation.

## H-5. Sensitive exception details leaked to clients
- **Severity:** High
- **File:** `backend/.../exception/GlobalExceptionHandler.java:29-37` (and `:72-81`) return `ex.getMessage()` directly.
- **Impact:** Internal/DB error messages (table/column names, constraint text) reach the client, aiding attackers.
- **Fix:** Return generic messages with a correlation id; log details server-side only.

## H-6. Stored/DOM XSS via `innerHTML` with un-escaped data + no CSP
- **Severity:** High
- **File:** `script.js` — `innerHTML` assignments interpolating server/user data at `:457` (event cards), `:840-864` (event details incl. `contactEmail`), `:1469/1487/1499`, `:1568` (bookings list).
- **Issue:** Event/booking fields are injected into the DOM without escaping. An admin (or anyone who can write event data — see C-1) can store `<img src=x onerror=…>` and run script in every visitor's session. No `Content-Security-Policy` is set.
- **Impact:** Account/session compromise, data theft.
- **Fix:** Escape/encode all interpolated values (there is a helper around `script.js:266-268` that should be applied consistently), prefer `textContent`, and add a strict CSP header.

## H-7. Per-controller `@CrossOrigin(origins = "*")` overrides the hardened CORS policy
- **Severity:** High (partially mitigated)
- **Files:** `UserController.java:28`, `AdminController.java:35`, `EventController.java:17`, `FileUploadController.java:20`
- **Issue:** These annotations re-open all origins for their endpoints, partially defeating the central `CorsConfig` hardening (C-7). They don't set credentials, so they are not the same Critical combo, but they should not remain wildcard.
- **Fix:** Remove the `@CrossOrigin(origins="*")` annotations and rely on the central `CorsConfig` allow-list.

---

# Medium Severity Issues

## M-1. Missing composite unique constraint on bookings
`database_schema.sql:90-111` and `Booking.java` lack `UNIQUE (event_id, student_email)`. Root cause of C-4’s race window. **Fix:** add the constraint (DB + entity).

## M-2. N+1 query when listing events
`EventService.getAllEvents()` (`EventService.java:48-53`) maps each event through `convertToDTO`, which calls `countTotalTicketsByEventId` per event (`:200`) — 1 + N queries. **Fix:** compute sold counts in a single `GROUP BY` query and join in memory.

## M-3. Redundant event re-query when building a booking response
`BookingService.convertToResponseDTO` (`:275-282`) calls `eventService.getEventById(...)` even though `Booking.event` is `FetchType.EAGER` (`Booking.java:20`). **Fix:** map from the already-loaded `booking.getEvent()`.

## M-4. Non-atomic manual cascade delete that swallows exceptions
`EventService.deleteEvent` (`:141-163`) manually deletes bookings/attendance/notifications in a `try/catch` that only prints a warning, although the schema already declares `ON DELETE CASCADE` (`database_schema.sql:107-110,124-127,161-162`). **Impact:** partial deletes / orphans, hidden failures, N queries. **Fix:** rely on DB cascade (or `@OnDelete`) and let failures roll back the transaction.

## M-5. Authentication endpoints accept raw `Map`, not validated DTOs
`UserController.authenticateUser` (`:60-61`) and `AdminController.authenticateAdmin` (`:82`) bind `Map<String,String>` with no `@Valid`. **Fix:** use typed, validated request DTOs.

## M-6. Booking identity is spoofable (trusts `studentEmail` from the body)
`BookingService.createBooking` (`:64-67`) looks up the student purely by the request-supplied `studentEmail`. With no server identity (C-2), one user can book "as" another. **Fix:** derive the booking owner from the authenticated principal.

## M-7. Production DB configuration weaknesses
`application-prod.properties`: `spring.datasource.password=` (blank), `useSSL=false`, `allowPublicKeyRetrieval=true`, and `spring.jpa.hibernate.ddl-auto=update` (schema drift in prod). **Fix:** require a real secret, enable TLS, and manage schema via migrations (Flyway/Liquibase) instead of `ddl-auto=update`.

## M-8. No HTTPS enforcement
`script.js:2-4` accepts both `http` and `https`. **Fix:** enforce HTTPS/HSTS in production and avoid mixed content.

## M-9. `System.out`/`System.err` used instead of a logger
e.g. `BookingService.java:141`, `EventService.java:158`, `DataInitializer.java:87`. **Impact:** unstructured logs, potential info leakage. **Fix:** use SLF4J with appropriate levels and redaction.

---

# Low Severity Issues

- **L-1. Dead/unused files:** `models` (0 bytes), `server.js` (deprecated Node stub), `event booking.html.json`, `test_cf.bat`/`test_*.bat`, and `backend/src/test/java/HashGen.java` (a hash generator, not a test). **Fix:** remove.
- **L-2. Inconsistent role casing:** new users get `"USER"` while admin login returns `"admin"`; the frontend lowercases defensively. **Fix:** standardize an enum.
- **L-3. Misleading error text:** `EventService.java:114` throws *"Event found with id"* on not-found. **Fix:** correct the message.
- **L-4. Duplicated base-URL logic:** `UserController.getBaseUrl` and `FileUploadController.getBaseUrl` are identical. **Fix:** extract a shared helper.
- **L-5. Lombok declared but unused:** the entities hand-write getters/setters; `pom.xml` still includes Lombok. **Fix:** drop the dependency or adopt it consistently.
- **L-6. `tokenPepper` committed in properties:** `app.attendance.tokenPepper=badya-secret-pepper-2026` (`application.properties:32`). **Fix:** move to an environment variable.

---

# Security Findings (consolidated)

| Class | Status | Evidence |
|---|---|---|
| SQL Injection | Not found | All data access via Spring Data/JPQL with bound params; no string-concatenated SQL. |
| XSS | **Present** (H-6) | `innerHTML` interpolation in `script.js`; no CSP. |
| CSRF | **Present (latent)** | No CSRF tokens; risk was amplified by wildcard-CORS+credentials (now C-7 fixed); state-changing GET-free but unauthenticated. |
| SSRF | Low | `X-Forwarded-Host`/`Proto` trusted for base-URL building (`UserController:132-139`); used only to build QR URLs, but should be validated. |
| RCE | Not directly | File upload (H-2) stores but does not execute; no `eval`/reflection sinks found. |
| Broken Auth | **Critical** (C-2) | No server-side session/JWT. |
| Broken Access Control | **Critical** (C-1) | Unprotected admin/event endpoints. |
| Secrets exposure | **Critical** (C-5/C-6, fixed) | Repo-root serving + default creds + committed pepper (L-6). |
| Token vulnerabilities | Medium | Attendance tokens are hashed with a static pepper; QR ticket token is a random UUID (OK). |
| Weak encryption | Low | Passwords use BCrypt (good); no other crypto misuse found. |
| Insecure storage | High | Identity/role in `localStorage` (C-2). |

---

# Performance Findings

- **N+1 on event listing** (M-2) — scales linearly with event count on the busiest endpoint (`GET /api/events`).
- **Redundant per-booking event query** (M-3).
- **Monolithic frontend** — `script.js` (~63 KB / 1.7k+ lines) and `styles.css` (~48 KB) are unminified, unbundled, and parsed on every page; no caching headers configured for app assets.
- **`qrImageBase64` (LONGTEXT) returned in user/auth payloads** (`UserController.authenticateUser:76`) — inflates every auth/login response with a base64 PNG.
- **No pagination** on `GET /api/admin/bookings` / `GET /api/events` — unbounded result sets.

---

# Architecture Findings

- **Trust boundary inverted** — authorization lives in the browser; the server is effectively open (C-1/C-2). This is the central architectural flaw.
- **Layering is otherwise sound** — Controller→Service→Repository with DTOs and a global exception handler; low coupling between modules; no circular dependencies observed.
- **Frontend has no framework/state model** — global mutable variables and `localStorage` used as a data store (`script.js:17`), causing client/server divergence and making the XSS surface larger.
- **Two parallel identity tables** (`users`, `admins`) with separate auth paths increase complexity; consider a unified principal with roles.
- **WhatsApp integration via an unofficial library** (`whatsapp-web.js`) couples a core notification path to a fragile, ToS-risky dependency.

---

# Business Logic Findings

- **Overbooking** (C-3) and **duplicate booking** (C-4) under concurrency.
- **"Max 1 ticket per user" is enforced only client-side** (`script.js:~939-945`); the server only checks capacity, so a direct API call can book more than one (`BookingService.java:90-95`).
- **Payment can be skipped** (H-1).
- **Free vs paid logic** is correct (`BookingService.java:97-110`) but the total is computed server-side only after the bypassable client flow.
- **Booking owner trust** (M-6) — bookings are attributed by request-body email.
- **No booking cancellation / refund / capacity-release path** — once booked, capacity is never returned (no cancel endpoint), so accidental bookings permanently consume seats.

---

# Technical Debt Findings

- **Near-zero automated tests** (see *Missing Tests*).
- **`ddl-auto=update` in prod** instead of migrations (M-7).
- **`System.out`/`System.err` logging** throughout (M-9).
- **Monolithic, unbundled frontend**; duplicated helpers (L-4).
- **Default `dev`/H2 profile** in `application.properties:6` — easy to ship a non-persistent build by accident.
- **Lombok declared but unused** (L-5).

---

# Dead Code Findings

| Item | Path | Note |
|---|---|---|
| Empty file | `models` | 0 bytes, no purpose. |
| Deprecated server | `server.js` | Old Node/Express stub replaced by Spring Boot. |
| Stray data file | `event booking.html.json` | Unreferenced. |
| Test batch scripts | `test_cf.bat`, `test_*.bat` | Ad-hoc, not part of any pipeline. |
| Not-a-test | `backend/src/test/java/HashGen.java` | A `main` that prints a BCrypt hash; lives in the test source root but is not a test. |
| `findByUsernameAndPasswordAndRole` | `UserRepository.java:38` | Plaintext-style lookup method, unused by current flows. |

---

# Dependency Findings

**Backend (`backend/pom.xml`):**
- **Spring Boot 3.1.5** — past OSS end-of-life (3.1.x is unsupported; current is 3.3.x/3.4.x). **Upgrade** to receive security patches.
- **H2 on the runtime classpath** alongside MySQL — fine for dev, but ensure H2 is not active in prod (it is `runtime` scope and enabled by the `dev` profile/console).
- **`spring-boot-devtools` (runtime, optional)** — excluded from the repackaged jar by the plugin, so OK, but should never be enabled in prod.
- **Lombok** — declared but unused (L-5).
- **ZXing 3.5.2** — current enough; no known critical CVEs for this use.
- **nimbus-jose-jwt 9.37.3** — newly added for Microsoft login; actively maintained.

**WhatsApp service (`whatsapp-service/package.json`):**
- **`whatsapp-web.js ^1.23.0`** — unofficial (drives WhatsApp Web via Puppeteer); brittle, heavy, and against WhatsApp ToS. Consider the official Cloud API.
- **`express ^4.18.2`**, **`qrcode-terminal ^0.12.0`** — fine; keep patched.

**License/general:** no copyleft conflicts observed; pin versions and add a lockfile for the Node service. Add automated dependency scanning (OWASP Dependency-Check / `npm audit` / Dependabot).

---

# Missing Tests

- **There are effectively no tests.** The only file under `backend/src/test` is `HashGen.java` (a utility `main`, not a test). The frontend and WhatsApp service have none.
- **Untested critical paths that most need coverage:**
  - Booking concurrency — overbooking (C-3) and duplicate prevention (C-4).
  - Authorization on admin/event endpoints (C-1) once auth is added.
  - Authentication (password + the new Microsoft OAuth provisioning/linking in `UserService.provisionMicrosoftUser`).
  - Capacity math and free-vs-paid pricing in `BookingService`.
  - QR ticket issue/scan lifecycle (`BookingService.scanTicket`).
  - The new `StaticContentSecurityFilter` allow/deny matrix.
- **Recommendation:** add JUnit + Spring Boot test slices and at least one concurrency test (parallel bookings against capacity), plus a small E2E for the login flows.

---

# Recommended Action Plan (prioritized)

**P0 — before any production use**
1. Implement real server-side auth (token/JWT or session) and enforce roles on all mutating endpoints (fixes C-1, C-2, M-5, M-6).
2. Fix booking concurrency: pessimistic lock or `@Version` **and** the `UNIQUE (event_id, student_email)` constraint (fixes C-3, C-4, M-1).
3. Keep the applied infra fixes (C-5 static filter, C-6 admin password, C-7 CORS) and set `APP_ADMIN_PASSWORD` + `CORS_ALLOWED_ORIGINS` in every environment.

**P1 — first hardening sprint**
4. Real payment gateway (H-1); secure file upload (H-2); login rate-limiting (H-3); strong password policy (H-4).
5. Stop leaking exception detail (H-5); escape output + add CSP (H-6); remove wildcard `@CrossOrigin` (H-7).

**P2 — quality & ops**
6. Migrations (Flyway), TLS + real DB secret (M-7), HTTPS/HSTS (M-8), structured logging (M-9).
7. Fix N+1 and redundant queries (M-2, M-3); remove dead code (L-1…L-6).
8. Establish a test suite (concurrency, auth, booking) and CI with dependency scanning; upgrade Spring Boot.

---

# Final Verdict

| Dimension | Assessment |
|---|---|
| **Production readiness** | **Not ready.** Blocking auth/authorization and booking-concurrency defects must be fixed first. |
| **Reliability** | **Low–Medium.** Core booking flow is racy; manual cascade delete can leave inconsistent state. |
| **Scalability** | **Low–Medium.** N+1 queries, unbounded lists, base64 in payloads, and unbundled assets limit throughput. |
| **Security** | **Poor (improving).** Three Critical infra issues were remediated this pass; the two most serious (broken access control, no server-side auth) and the booking races remain open. |
| **Maintainability** | **Medium.** Backend layering is clean and readable; the monolithic, test-less frontend and duplicated helpers drag it down. |

**Bottom line:** solid bones and good developer ergonomics, but the security trust model and the booking concurrency logic need to be rebuilt before this can safely go live. The infrastructure fixes applied here remove the easiest attack paths; the P0 items above remove the rest.
