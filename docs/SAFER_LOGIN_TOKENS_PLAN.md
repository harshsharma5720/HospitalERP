# Safer login tokens — plan

Today a login gives one token that is valid for 10 hours. The browser keeps it in `localStorage`, and every request sends it as `Authorization: Bearer …`. Any script that runs on the page can read it there (a malicious package, a cross-site scripting bug), and a stolen token works for up to 10 hours. Nothing can revoke it except deactivating the account. The app now takes payments, so this matters more. This piece of work is idea #3 in [CODE_REVIEW_AND_IDEAS.md](CODE_REVIEW_AND_IDEAS.md).

## Decisions (2026-10-16)

| Question | Decision |
|---|---|
| Patients | Stay logged in **7 days, renewed while used**: coming back within 7 days of the last use keeps them in. At most 90 days, then they log in again. |
| Staff (admin, doctors, front desk) | **12 hours from login** at most (a working shift), used or not; a forgotten login on a shared PC doesn't last until the next day. |
| Log out everywhere | A **"Log out of all devices"** button, and the same **automatically** after a password reset and when an admin deactivates the account. It takes effect **at once**, not after the short token runs out. |
| Way of working | Each finished, tested step is committed without asking; the branch is pushed at the end with the PR link. |

## Design

**Two tokens per login:**
- The **access token** is a JWT, as today, but it lasts only **15 minutes** (`JWT_EXPIRATION_MS`, now 900000 by default). The app keeps it **in memory only**, never in `localStorage`, and sends it as `Authorization: Bearer …`. A stolen one is worth 15 minutes at most.
- The **refresh token** is a random 256-bit value in an **`httpOnly` cookie**:
  - page scripts can't read it;
  - `SameSite=Strict`;
  - `Path=/api/auth`, so it travels only to the login endpoints;
  - `Secure` when `AUTH_COOKIE_SECURE=true` (set it behind HTTPS).
  
  The server keeps only its **SHA-256 hash**, in a new table `refresh_tokens`. Each row belongs to a **session**, i.e. one login on one device.

**Refresh and rotation:**
- `POST /api/auth/refresh` takes the cookie and returns a new access token and a **new** refresh token; the old one is marked used.
- **Reuse is theft:** if an old refresh token is presented again, more than 30 seconds after it was replaced, the whole session is ended. Within those 30 seconds it counts as two tabs refreshing at once, and gets a new token.
- **Lifetimes:** patients get 7 days from the last refresh, capped at 90 days from the login. Staff get 12 hours from the login, however often they refresh. The role is the one at login.
- `POST /api/auth/refresh` and `POST /api/auth/logout` also need the header `X-Requested-With: XMLHttpRequest`. Together with `SameSite=Strict` that blocks cross-site forms (CSRF).

**Logging out:**
- `POST /api/auth/logout` ends this session and clears the cookie.
- `POST /api/auth/logout-all` (logged in) ends **all** of the user's sessions. It also raises the user's **token version** (new column `users.token_version`). Access tokens carry the version (`ver`), so the filter refuses older ones at once.
- A password reset and an admin's deactivation do the same. A deactivated account can't refresh at all.
- A daily job deletes sessions that ended more than 30 days ago.

**Frontend:**
- `useAuthStore` keeps the access token in memory. On start the app restores the login with `POST /api/auth/refresh`, and shows "Loading…" until it knows, so a reload doesn't bounce staff to the login page.
- About a minute before the access token runs out it is refreshed. A 401 triggers one refresh (shared by requests made at the same time), then the request is retried once. If the refresh fails, the user goes to the login page ("Your session ended. Please log in again.").
- Logging out calls the server. Other tabs are told through a `BroadcastChannel` and log out too; logging in in one tab restores the others.
- The old `jwtToken` in `localStorage` is removed. Three pages that read it directly use the store.
- **"Log out of all devices"** sits on the profile pages (My account, the doctor's Profile, the receptionist's My profile) and in the staff top bar's menu for admins.

## Steps

| Step | Work | Checked by |
|---|---|---|
| ST.1 | **Sessions and refresh (backend):** `refresh_tokens` (Flyway), `RefreshTokenService` (hashing, rotation, reuse detection with the grace window, patient / staff lifetimes), the cookie on login and register, `refresh`, `logout`, the 15-minute access token, configuration. | Unit tests with a frozen clock; end-to-end tests (cookie attributes, rotation, reuse, lifetimes, the header, a deactivated user); mutation checks |
| ST.2 | **Log out everywhere (backend):** `users.token_version`, the `ver` claim checked by the filter, `logout-all`, automatic after a password reset and a deactivation, the clean-up job. | End-to-end tests (old access tokens refused at once, all sessions ended); mutation checks |
| ST.3 | **The app (frontend):** the token in memory, restoring on start, refresh before expiry and on 401, logout through the server, tabs kept in step, no more `localStorage`. | Tests; mutation checks |
| ST.4 | **"Log out of all devices"** buttons; the login page's "session ended" message. | Tests; mutation checks; screenshots |
| ST.5 | **Docs** (README security notes and configuration, identity README, the end-to-end test guide, the ideas doc, the MySQL runbook), final check, push and PR link. | Full backend and frontend runs; build |

## Progress log

| Step | Status | Date | Notes |
|---|---|---|---|
| ST.0 Plan + decisions | ✅ done | 2026-10-16 | This file. |
| ST.1 Sessions and refresh (backend) | ✅ done | 2026-10-19 | `refresh_tokens` (Flyway `V2026_10_16_1`), `RefreshTokenService` (the token kept only as its SHA-256 hash, replaced on every use, reuse after the 30-second grace ends the session; patients 7 days after the last use and at most 90, staff 12 hours), the `hospital_refresh` cookie on login and register (`HttpOnly`, `SameSite=Strict`, `Path=/api/auth`), `POST /api/auth/refresh` and `/logout` (both need `X-Requested-With`), access tokens of 15 minutes, configuration `AUTH_*`. Tests: `RefreshTokenServiceTest` (5, a clock moved by hand), `LoginSessionsH2Test` (3); snapshots and the identity README updated. Mutation checks: 10 mutations, 9 killed by the harness; "expired tokens accepted" showed BROKEN there, and a run by hand had it caught by 2 named tests (`RefreshTokenServiceTest`). Full backend run green without `DatabaseMigrationMySqlTest`: Docker Desktop doesn't answer (`docker info` hangs), so that test (its list now has `2026.10.16.1` and `refresh_tokens`) still has to run. |
| ST.2 Log out everywhere (backend) | ✅ done | 2026-10-19 | `users.token_version` (Flyway `V2026_10_16_2`, existing accounts 0), the access token's `ver` claim (tokens without it count as 0), checked by `JWTAuthenticationFilter` through `AccountDetails`; `RefreshTokenService.endEverywhere` moves the version on and ends every session; `POST /api/auth/logout-all` (needs a login, clears the cookie); automatic after a password reset and a deactivation (a reactivation doesn't bring the old logins back); `RefreshTokenCleanup` every night at 03:45 deletes tokens expired or ended over 30 days ago. Tests: `JWTServiceTest` (2, new), `RefreshTokenServiceTest` +2, `PasswordResetServiceTest` (the reset ends every login), `LoginSessionsH2Test` +3 (log out of all devices, a reactivated account, the clean-up); snapshots, the identity README, the MySQL test's list (`2026.10.16.2`, `token_version = 0`). Mutation checks: 10 of 10 killed by named tests. |
| ST.3 The app (frontend) | ✅ done | 2026-10-19 | `useAuthStore`: the access token in memory only (the old `jwtToken` in localStorage is dropped), `ready` once the login is known (`restoreSession` in `index.js`, "Loading..." in `App` until then), a refresh a minute before the token runs out (one request shared by everyone who needs it; the timer capped at setTimeout's limit), logout through the server, other tabs kept in step with a `BroadcastChannel`. `setupAxios`: a 401 refreshes once and tries the request again; an ended login goes to `/login?expired=1`. The pages that read localStorage (leave management, the doctor's profile, the popup form) read the store. Tests: `useAuthStore.test.js` (9), `setupAxios.test.js` (5), `App.test.js` +1; 272 of 272 pass; the build is clean. Mutation checks: 15 of 15 killed. "Retried again and again" first made jest run out of memory (the fake server answered 401 forever); it now answers from the fourth request, so the test fails by name. |
