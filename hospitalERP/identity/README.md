# identity (`hospital-identity`)

Who is using the system: user accounts and roles, registration, login with JWT, phone OTP, forgot/reset password, login throttling, looking up the current user, and creating the first admin at startup.

It does **not** create patient or staff profiles: it publishes `UserRegisteredEvent` and the patients and staff modules create their own profiles.

**Depends on:** common, notifications · **Used by:** patients, staff, scheduling, appointments, clinical, administration, app (`SecurityConfig`)

## Public API — `com.itmonteur.hospitalerp.identity`

| Class | Purpose |
|---|---|
| `User`, `Role` | The account entity (`users` table) and the roles `ADMIN`, `DOCTOR`, `PATIENT`, `RECEPTIONIST`. `active` / `deactivatedAt` mark a deactivated account (see [Deactivated accounts](#deactivated-accounts)). |
| `UserService` | Other modules use this instead of the repository: `findUser`, `getAllUsers`, `updateContactDetails(user, email, phone)` (keeps the login account in sync when a profile's contact data changes), `deactivate(user)` / `reactivate(user)` (only set the flag; the use case is in administration), `deleteUser`, `deleteUserById` (permanent delete). |
| `CurrentUserService` | The logged-in user: `getCurrentUser`, `getCurrentUserId`, `hasRole`, `hasAnyRole`, `isStaff`, `requireSelfOrRole`. |
| `AuthService` | `register` (self sign-up, always a patient), `createUser(request, role)` (used by administration), `login`, `isOtpRequired`, `parseRole`. |
| `UserRegisteredEvent` | Published when an account is created. |
| `CustomUserDetailsService`, `JWTAuthenticationFilter` | Spring Security wiring, used by `SecurityConfig` in `app`. A deactivated account is a *disabled* user, and the filter ignores its tokens. |
| `ModuleSecurityRules` | Interface for a module's URL access rules (`endpointRules`, `areaRules`); every module with endpoints has one bean, `SecurityConfig` combines them. |
| `UserDTO`, `RegisterRequestDTO`, `LoginRequestDTO`, `AuthResponseDTO`, `ForgotPasswordRequestDTO`, `ResetPasswordRequestDTO` | Request/response objects. |

## Events

- **Publishes:** `UserRegisteredEvent(User user)`, inside the registration transaction.
- **Listens to:** nothing.

## Endpoints — `identity.web`

`AuthController`, all public, under `/api/auth`: `register`, `login`, `send-otp`, `verify-otp`, `otp-required`, `forgot-password`, `reset-password`.

Access rules (`identity.web.IdentitySecurityRules`, a `ModuleSecurityRules` bean): `/api/auth/**` is public.

## Internal — `identity.internal`

`UserRepository`, `JWTService` (sign/verify tokens), `OtpService` (6-digit codes), `LoginAttemptService` (locks a username for 15 minutes after 5 failed logins), `PasswordResetService`, `AdminBootstrap` (creates the first admin from `ADMIN_*` settings if it doesn't exist).

## Deactivated accounts

Accounts are deactivated, not deleted ([docs/ACCOUNT_DEACTIVATION_PLAN.md](../../docs/ACCOUNT_DEACTIVATION_PLAN.md)). The use case (cancelling bookings, reactivating, the guarded permanent delete) lives in administration. In identity, `users.active = false` means:

- **Login:** the account status is checked only after the password (`SecurityConfig` in `app`), so a wrong password gets the usual 401 and reveals nothing. The right password gets **403** "This account has been deactivated. Please contact the hospital." (`GlobalExceptionHandler` in common). It doesn't count towards the login lock.
- **Tokens:** `JWTAuthenticationFilter` ignores the token of a deactivated user, so a token issued before the deactivation gets 401 on the next request.
- **Forgot password:** `PasswordResetService` treats the account like an unknown one: same answer, no code, no reset.

## Configuration

| Env variable | Default | Meaning |
|---|---|---|
| `JWT_SECRET` | random per start | Base64 key, 32+ bytes. Without it everyone is logged out on restart. |
| `OTP_REQUIRED` | `true` | Require phone verification on sign-up (needs Twilio). |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `ADMIN_EMAIL`, `ADMIN_PHONE` | empty | First admin account, created once at startup. |

`jwt.expiration-ms` (default 10 hours) sets the token lifetime.

## Tests

`OtpServiceTest`, `PasswordResetServiceTest`; login/registration flows, including a deactivated account, are also covered by the end-to-end tests in `app`.

Module diagram: [docs/modules/module-identity.puml](../../docs/modules/module-identity.puml).
