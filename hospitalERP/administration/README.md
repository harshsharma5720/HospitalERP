# administration (`hospital-administration`)

Admin use cases that span several modules: creating users of any role, the admin dashboard lists, leave decisions, and **account deletion**. It sits at the top of the module graph, so it may call every other module — and no module calls it.

**Depends on:** common, notifications, identity, patients, staff, scheduling, appointments, clinical (uses all but notifications today) · **Used by:** nothing (only `app` assembles it)

## Public API — `com.itmonteur.hospitalerp.administration`

None. Nothing else may depend on administration.

## Events

- **Publishes:** `AppointmentNotificationEvent` (appointments) with kind `CANCELLED` for each upcoming booking removed when a **doctor** account is deleted, so the patients are told after the deletion commits. (Deleting a patient frees their booked slots again; nobody else needs a message.)
- **Listens to:** nothing.

## Endpoints — `administration.web`

| Controller | URLs |
|---|---|
| `AdminController` | `/api/admin/**` — create users, user and staff lists, leave approval/rejection, appointment overviews |
| `AccountController` | `DELETE /api/patient/deleteAccount/{ptId}`, `DELETE /api/doctor/delete/{id}` and `DELETE /api/receptionist/delete/{receptionistId}` (admin only) |

Access rules (`administration.web.AdministrationSecurityRules`, a `ModuleSecurityRules` bean): `/api/admin/**` admins only.

## Internal — `administration.internal`

| Class | Purpose |
|---|---|
| `AdminService` | The admin use cases, calling the other modules' services. |
| `UserAccountService` | Account deletion in one transaction and a fixed order: leave requests; then the role's data — consultations → appointments → (doctor: slots and weekly schedule) → profile; then the login account. Before that, upcoming bookings are handled (doctor: patients notified; patient: slots released). Uses only other modules' public services, never their repositories. |

## Configuration

None.

## Tests

No unit tests of its own; account deletion and admin flows are covered by `FeatureFlowH2Test` and `SecurityRulesTest` (`app`).

Module diagram: [docs/modules/module-administration.puml](../../docs/modules/module-administration.puml).
