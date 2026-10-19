# identity (`hospital-identity`)

Who is using the system: user accounts and roles, registration, login with a short JWT and a refresh cookie (login sessions, [docs/SAFER_LOGIN_TOKENS_PLAN.md](../../docs/SAFER_LOGIN_TOKENS_PLAN.md)), phone OTP, forgot/reset password, login throttling, looking up the current user, and creating the first admin at startup.

It does **not** create patient or staff profiles: it publishes `UserRegisteredEvent` and the patients and staff modules create their own profiles.

**Depends on:** common, notifications · **Used by:** patients, staff, scheduling, appointments, clinical, administration, app (`SecurityConfig`)

## Public API — `com.itmonteur.hospitalerp.identity`

| Class | Purpose |
|---|---|
| `User`, `Role` | The account entity (`users` table) and the roles `ADMIN`, `DOCTOR`, `PATIENT`, `RECEPTIONIST`. `active` / `deactivatedAt` mark a deactivated account (see [Deactivated accounts](#deactivated-accounts)); `createdAt` is set by `AuthService.createUser` (empty for accounts older than 2026-10-06). `tokenVersion` is carried by every access token and moves on when every login of the account ends (see [Log out everywhere](#log-out-everywhere)). |
| `UserService` | Other modules use this instead of the repository: `findUser`, `getAllUsers`, `updateContactDetails(user, email, phone)` (keeps the login account in sync when a profile's contact data changes), `deactivate(user)` / `reactivate(user)` (set the flag, and a deactivation ends every login; the use case is in administration), `deleteUser`, `deleteUserById` (permanent delete); for the admin dashboard `countActiveAccountsPerRole`, `countNewAccountsPerDay(role, from, to)` (by `createdAt`) and `firstAccountCreationTime` (since when new accounts are counted). |
| `CurrentUserService` | The logged-in user: `getCurrentUser`, `getCurrentUserId`, `hasRole`, `hasAnyRole`, `isStaff`, `requireSelfOrRole`. |
| `AuthService` | `register` (self sign-up, always a patient; logged in at once), `createUser(request, role)` (used by administration), `login`, `refresh`, `logout`, `logoutEverywhere` (the current user), `isOtpRequired`, `parseRole`. A login is a `Session`: the access token for the response body and the refresh token for the cookie, with its lifetime. |
| `UserRegisteredEvent` | Published when an account is created. |
| `CustomUserDetailsService`, `JWTAuthenticationFilter` | Spring Security wiring, used by `SecurityConfig` in `app`. The account is an `AccountDetails` (package-private) with its token version. A deactivated account is a *disabled* user, and the filter ignores its tokens and tokens of an older version. |
| `ModuleSecurityRules` | Interface for a module's URL access rules (`endpointRules`, `areaRules`); every module with endpoints has one bean, `SecurityConfig` combines them. |
| `UserDTO`, `RegisterRequestDTO`, `LoginRequestDTO`, `AuthResponseDTO`, `ForgotPasswordRequestDTO`, `ResetPasswordRequestDTO` | Request/response objects. |

## Events

- **Publishes:** `UserRegisteredEvent(User user)`, inside the registration transaction.
- **Listens to:** nothing.

## Endpoints — `identity.web`

`AuthController`, under `/api/auth`, all public except `logout-all`: `register`, `login`, `refresh`, `logout`, `logout-all`, `send-otp`, `verify-otp`, `otp-required`, `forgot-password`, `reset-password`.

**Login sessions:**
- `login` and `register` answer `{token}`, the access token, and set the cookie `hospital_refresh`: `HttpOnly`, `SameSite=Strict`, `Path=/api/auth`, `Max-Age` the session's lifetime, and `Secure` with `AUTH_COOKIE_SECURE`.
- `POST /refresh` replaces the cookie's refresh token and answers a new access token. When the token is missing, unknown, ended or expired, it answers 401 with the cookie cleared.
- `POST /logout` ends the session and clears the cookie.
- Both need `X-Requested-With: XMLHttpRequest` (403 otherwise); with `SameSite=Strict` that blocks cross-site forms.
- `POST /logout-all` ("Log out of all devices") needs the access token (401 without one): every login of the account ends, its access tokens are refused at once, and this device's cookie is cleared.

Access rules (`identity.web.IdentitySecurityRules`, a `ModuleSecurityRules` bean): `/api/auth/**` is public; an endpoint rule makes `POST /api/auth/logout-all` need a login.

## Internal — `identity.internal`

`UserRepository`, `JWTService` (sign/verify the access tokens, 15 minutes by default), `RefreshTokenService` (login sessions: a 256-bit refresh token kept only as its SHA-256 hash in `refresh_tokens`, replaced on every use; an old one used again more than 30 seconds after it was replaced ends the session — within them it is two tabs; patients 7 days after the last use and at most 90 days, staff 12 hours after the login; a deactivated account can't refresh; `endEverywhere(user, reason)` moves the account's token version on and ends all its sessions; `deleteOld`), `RefreshTokenCleanup` (every night at 03:45, `app.auth.cleanup-cron`: deletes the tokens that expired, or whose session ended, more than 30 days ago), `RefreshToken`, `RefreshTokenRepository`, `SessionEndReason`, `OtpService` (6-digit codes), `LoginAttemptService` (locks a username for 15 minutes after 5 failed logins), `PasswordResetService`, `AdminBootstrap` (creates the first admin from `ADMIN_*` settings if it doesn't exist).

## Deactivated accounts

Accounts are deactivated, not deleted ([docs/ACCOUNT_DEACTIVATION_PLAN.md](../../docs/ACCOUNT_DEACTIVATION_PLAN.md)). The use case (cancelling bookings, reactivating, the guarded permanent delete) lives in administration. In identity, `users.active = false` means:

- **Login:** the account status is checked only after the password (`SecurityConfig` in `app`), so a wrong password gets the usual 401 and reveals nothing. The right password gets **403** "This account has been deactivated. Please contact the hospital." (`GlobalExceptionHandler` in common). It doesn't count towards the login lock.
- **Tokens:** `JWTAuthenticationFilter` ignores the token of a deactivated user, so a token issued before the deactivation gets 401 on the next request. The deactivation also ends every login, so a reactivation doesn't bring the old ones back.
- **Forgot password:** `PasswordResetService` treats the account like an unknown one: same answer, no code, no reset.

## Log out everywhere

Every access token carries the account's `tokenVersion` (`users.token_version`, the claim `ver`; tokens without it count as 0). `JWTAuthenticationFilter` refuses a token whose version isn't the account's current one, so a 15-minute token stops working on the next request, not when it runs out. `RefreshTokenService.endEverywhere` moves the version on and ends every session of the account. It runs for:

- **"Log out of all devices"**: `POST /api/auth/logout-all`.
- **A password reset**: `PasswordResetService.resetPassword`; whoever knew the old password is out.
- **A deactivation**: `UserService.deactivate`.

## Configuration

| Env variable | Default | Meaning |
|---|---|---|
| `JWT_SECRET` | random per start | Base64 key, 32+ bytes. Without it, access tokens stop working on restart (the app gets new ones through the refresh cookie). |
| `JWT_EXPIRATION_MS` | `900000` (15 minutes) | The access token's lifetime. |
| `AUTH_COOKIE_SECURE` | `false` | Mark the refresh cookie `Secure`; set it to `true` behind HTTPS. |
| `AUTH_PATIENT_IDLE_DAYS`, `AUTH_PATIENT_MAX_DAYS` | `7`, `90` | A patient's login: days after the last use, and at most after logging in. |
| `AUTH_STAFF_HOURS` | `12` | A staff login (admin, doctor, front desk), from logging in, used or not. |
| `OTP_REQUIRED` | `true` | Require phone verification on sign-up (needs Twilio). |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PHONE` | empty | First admin account, created once at startup. |


## Tests

`OtpServiceTest`, `PasswordResetServiceTest`, `RefreshTokenServiceTest` (a clock moved by hand: hashing and replacement, the patient and staff lifetimes, reuse within and after the grace, a deactivated account, logout, the other logins of a user, ending everywhere, the clean-up), `JWTServiceTest` (the version claim, tokens without it); `LoginSessionsH2Test` in `app` (the cookie's attributes, the 15-minute access token, refresh and reuse, the header, logout, a deactivated account, log out of all devices, a reactivated account, the nightly clean-up); login/registration flows, including a deactivated account, are also covered by the end-to-end tests in `app`.

Module diagram: [docs/modules/module-identity.puml](../../docs/modules/module-identity.puml).
