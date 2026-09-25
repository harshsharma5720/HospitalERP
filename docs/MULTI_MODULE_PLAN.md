# HospitalERP — Multi-Module Architecture Plan

**Date:** 2026-09-25
**Starting point:** commit `2bc6d31` on branch `fix/code-review-issues`
**Scope:** backend (`hospitalERP/`). The frontend is covered by an optional parallel track (Phase 5).
**Status:** plan only; nothing implemented yet.

---

## 1. Goal and Key Decisions

**Goal:** turn the single Spring Boot project, which is organised by technical layer (`controller/`, `services/`, `repositories/`, `entities/`, `dto/`), into a **modular monolith**:
- one deployable application and one database;
- the code split into business modules with enforced boundaries.

| Decision | Choice | Why |
|---|---|---|
| Architecture | **Modular monolith** (Maven multi-module), **not microservices** | One team and one database. Booking, leave approval and account deletion need local transactions. See §11. |
| End state | **Maven multi-module**, one Maven module per business module | The compiler blocks undeclared dependencies, so boundaries can't erode silently. |
| Stepping stone | **Spring Modulith** verification before the Maven split | It proves there are no cycles and no access to other modules' internals while everything is still one project. The Maven split then becomes a mechanical move. |
| Package name | Rename `ITmonteur.example.hospitalERP` → `com.itmonteur.hospitalerp` during the move | Every file moves anyway, so this costs almost nothing extra. It fixes the naming-convention issue from the code review. |
| API URLs | **Unchanged** (all 83 endpoints) | The frontend must not notice. This is guarded by a snapshot test (Phase 0). |
| Database | **Unchanged**: same schema, same tables | Only Java code moves. The one entity without `@Table` (`LeaveRequest`) gets an explicit table name first. |
| Cross-module references | JPA entity references allowed **only toward lower modules**; upward communication via **events** | Keeps JPA joins that work today; removes all cycles. |

---

## 2. Target Modules

### 2.1 Module list

| Module | Responsibility | May depend on |
|---|---|---|
| `common` | Shared kernel: exceptions + `GlobalExceptionHandler`, `ApiResponse`, `Gender`, `FileStorageService`, upload `WebConfig` | nothing |
| `notifications` | Delivery channels: email, SMS (Twilio), message templates. Knows nothing about appointments. | common |
| `identity` | Users, roles, login, JWT, OTP, password reset, login throttling, current-user lookup, first-admin bootstrap | common, notifications |
| `patients` | Patient profiles, relatives | common, identity |
| `staff` | Doctors, receptionists, leave requests (grows into HR later) | common, identity |
| `scheduling` | Doctor weekly schedules, slots, slot generation/locking | common, identity, staff |
| `appointments` | Booking, reschedule, cancel, lists, day-before reminders | common, identity, notifications, patients, staff, scheduling |
| `clinical` | Consultations, prescriptions, prescription PDF, medical history | common, identity, patients, staff, appointments |
| `administration` | Admin use cases that span modules: create users of any role, leave decisions, **account deletion** | all of the above |
| `app` | `main()` class, `SecurityConfig`, beans (`Clock`, `ModelMapper`), `application.properties`, end-to-end tests | all of the above |

### 2.2 Dependency direction (arrows point to what a module uses)

```
                          ┌──────────────┐
                          │     app      │  main class, SecurityConfig, config, E2E tests
                          └──────┬───────┘
                          ┌──────▼───────┐
                          │administration│  cross-module admin use cases, account deletion
                          └──────┬───────┘
                          ┌──────▼───────┐
                          │   clinical   │  consultations, prescriptions
                          └──────┬───────┘
                          ┌──────▼───────┐
                          │ appointments │──────────────┐
                          └──┬───────┬───┘              │
                  ┌──────────▼─┐   ┌─▼──────────┐       │
                  │ scheduling │   │  patients  │       │
                  └─────┬──────┘   └─────┬──────┘       │
                  ┌─────▼──────┐         │              │
                  │   staff    │         │              │
                  └─────┬──────┘         │              │
                        └───────┬────────┘              │
                          ┌─────▼──────┐                │
                          │  identity  │                │
                          └─────┬──────┘                │
                          ┌─────▼────────┐              │
                          │notifications │◄─────────────┘
                          └─────┬────────┘
                          ┌─────▼──────┐
                          │   common   │  (used by every module)
                          └────────────┘
```

**Rule:** a module never depends on anything above it. When a lower module needs something to happen in a higher one (for example, staff approves a leave and appointments must be cancelled), it **publishes an event** and the higher module listens.

### 2.3 Inside each module

Spring Modulith treats a module's **top-level package as its public API**; all **sub-packages are internal** unless explicitly exported.

```
com.itmonteur.hospitalerp.appointments          ← public API
    AppointmentService.java                     (facade other modules may call)
    AppointmentDTO.java, AppointmentStatus.java
    Appointment.java                            (entity; public because clinical references it)
    AppointmentBookedEvent.java, AppointmentCancelledEvent.java
com.itmonteur.hospitalerp.appointments.internal ← hidden from other modules
    AppointmentRepository.java, AppointmentMapper.java
    AppointmentNotificationListener.java, AppointmentReminderJob.java
com.itmonteur.hospitalerp.appointments.web      ← hidden
    AppointmentController.java, DoctorAppointmentController.java, ReceptionistAppointmentController.java
```

**Repositories are always internal.** Other modules call a service method, never another module's repository. Today, for example, `UserAccountService` calls `ConsultationRepository` directly.

---

## 3. Class-to-Module Mapping (all 90 current classes)

| Module | Classes |
|---|---|
| **common** | `ApiResponse`, `BadRequestException`, `ConflictException`, `ForbiddenException`, `ResourceNotFoundException`, `TooManyRequestsException`, `GlobalExceptionHandler`, `Gender`, `FileStorageService`, `WebConfig` |
| **notifications** | `EmailService`, `SmsService`, `NotificationService`, `TwilioConfig` |
| **identity** | `User`, `Role`, `UserRepository`, `UserDTO`, `AuthService`, `AuthController`, `JWTService`, `JWTAuthenticationFilter`, `CustomUserDetailsService`, `CurrentUserService`, `OtpService`, `LoginAttemptService`, `PasswordResetService`, `AdminBootstrap`, `AuthResponseDTO`, `LoginRequestDTO`, `RegisterRequestDTO`, `ForgotPasswordRequestDTO`, `ResetPasswordRequestDTO` |
| **patients** | `PtInfo`, `PtRelative`, `RelationShip`, `PtInfoRepository`, `PtRelativeRepository`, `PtInfoService`, `PtRelativeService`, `PtInfoController`, `PtRelativeController`, `PtInfoDTO`, `PtRelativeDTO` |
| **staff** | `Doctor`, `Receptionist`, `Specialist`, `LeaveRequest`, `LeaveStatus`, `DoctorRepository`, `ReceptionistRepository`, `LeaveRequestRepository`, `DoctorService`, `ReceptionistService`, `LeaveRequestService`, `DoctorController`, `ReceptionistController`, `LeaveRequestController`, `DoctorDTO`, `ReceptionistDTO`, `LeaveRequestDTO` |
| **scheduling** | `Slot`, `Shift`, `DoctorSchedule`, `SlotRepository`, `DoctorScheduleRepository`, `SlotService`, `DoctorScheduleService`, `ScheduleDefaults`, `SlotController`, `DoctorScheduleDTO` |
| **appointments** | `Appointment`, `AppointmentStatus`, `AppointmentRepository`, `AppointmentService`, `AppointmentReminderService`, `AppointmentController`, `AppointmentDTO` |
| **clinical** | `Consultation`, `PrescriptionItem`, `ConsultationRepository`, `ConsultationService`, `PrescriptionPdfService`, `ConsultationController`, `ConsultationDTO`, `PrescriptionItemDTO` |
| **administration** | `AdminController`, `AdminService`, `UserAccountService` |
| **app** | `HospitalErpApplication`, `SecurityConfig` |
| *split up* | `EntityMapper` → `AppointmentMapper` (appointments), `DoctorMapper` (staff), `PatientMapper` (patients) |

Some controllers are **split across modules** in Phase 1 (see §4). Their URLs stay the same, because Spring allows several controller classes under one URL prefix.

---

## 4. What Blocks the Split Today: 7 Dependency Cycles

Measured by scanning every class for references to classes in other modules, using the mapping in §3:

| # | Cycle | What causes it (today's code) | Fix (Phase 1 step) |
|---|---|---|---|
| C1 | identity ↔ patients | `AuthService.createUser` builds the `PtInfo` profile | identity publishes `UserRegisteredEvent`; patients creates the profile in a listener (1.2) |
| C2 | identity ↔ staff | `AuthService.createUser` builds `Doctor` / `Receptionist` profiles | same event; staff creates the profile (1.2) |
| C3 | appointments ↔ patients | `PtInfo.appointments` collection; `PtInfoDTO.appointment` field (unused by the frontend); `PtRelativeService` calls `AppointmentRepository.clearRelative` | remove the collection and field (1.3); `RelativeDeletedEvent` → appointments clears the link (1.4) |
| C4 | appointments ↔ staff | `Doctor.appointments` collection; `DoctorService` has complete/pending/completed/count logic; `ReceptionistController` has booking endpoints; `LeaveRequestService` cancels appointments | remove the collection (1.3); move those endpoints into appointments controllers with the same URLs (1.6); `LeaveApprovedEvent` (1.5) |
| C5 | scheduling ↔ staff | Schedule endpoints live in `DoctorController`; `LeaveRequestService` blocks slots | move endpoints to scheduling (1.6); scheduling listens to `LeaveApprovedEvent` (1.5) |
| C6 | administration ↔ patients | `PtInfoService.deletePtInfoById` calls `UserAccountService` | move the delete endpoint into administration (1.7) |
| C7 | administration ↔ staff | `DoctorService.deleteDoctor` and `ReceptionistService.deleteReceptionist` call `UserAccountService` | move those delete endpoints into administration (1.7) |

**Hidden (query-string) dependencies** that a compiler won't catch:
- `SlotRepository.deleteUnusedFromDate` refers to `Appointment` inside JPQL (scheduling → appointments). Fixed in step 1.8.
- `ConsultationRepository` queries navigate `c.appointment.ptInfo…`. That's fine: it points downward.

**Other clean-ups found on the way:**
- `PtInfoRepository` and `ReceptionistRepository` have an unused `import …Doctor`.
- `Doctor.password` and the `role` fields on `Doctor`, `PtInfo`, `PtRelative` and `Receptionist` are unused leftovers.
- `Doctor.user` has `cascade = ALL`, so deleting a doctor deletes the user from another module. Deletion becomes explicit in administration (1.7).

---

## 5. Design Rules (apply from Phase 1 onward)

1. **Dependencies point downward only** (§2.2). A new upward dependency is a design bug.
2. **No cross-module repository access.** Call the owning module's service. Cleanup operations become explicit public methods, for example `ConsultationService.deleteAllForPatient(patientId)`.
3. **Events for upward communication.** Event classes live in the **publishing** module's API package; listeners live in the **consuming** module.
4. **Pick the listener type deliberately:**

   | Situation | Listener type | Examples |
   |---|---|---|
   | Must succeed or fail together with the trigger | `@EventListener`: synchronous, same transaction | creating a profile on registration; blocking slots and cancelling appointments on leave approval |
   | Side effect after success | `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async` | emails and SMS |

   The second type also fixes a known issue: today a notification can go out even if the transaction later rolls back.
5. **Entity references only point downward.** For example, `Appointment → Doctor` is fine; `Doctor → List<Appointment>` is not.
6. **Controllers may share a URL prefix across modules.** URLs are a public contract and don't move.
7. **Move-only commits are separate from behavior changes.** Git then detects renames and reviews stay readable.

---

## 6. Phases

Effort estimates assume one developer who knows the code. Each phase ends with **all tests green** and the **endpoint snapshot unchanged**.

### Phase 0 — Safety Net (≈ 1 day)

| Step | Work |
|---|---|
| 0.1 | Create branch `refactor/modules` from the latest `main` (after merging `fix/code-review-issues`). |
| 0.2 | **Endpoint contract test.** Read all mappings from `RequestMappingHandlerMapping` (HTTP method + path + required role) and compare against a checked-in snapshot `src/test/resources/api-endpoints.txt` (83 entries today). Any accidental URL change fails the build. |
| 0.3 | **Commit the dependency checker** (`tools/check_module_deps.py`, used to produce §4) with the class→module mapping. Run it at every Phase 1 step; the goal is 0 cycles. |
| 0.4 | Pin `@Table(name = "leave_request")` on `LeaveRequest`, the only entity whose table name comes from the class name. Remove the two unused `Doctor` imports. |
| 0.5 | Record the baseline: 61 backend tests (1 skipped) and 13 frontend tests, all green. |

**Exit:** snapshot test and checker are in place; baseline recorded.

### Phase 1 — Break the Cycles in Place (≈ 4–6 days)

All work happens in the **current packages**, so each change is small, behavioral and testable. One commit per step.

| Step | Work | Removes |
|---|---|---|
| 1.1 | Split `EntityMapper` into `AppointmentMapper`, `DoctorMapper` and `PatientMapper`. | the "mixed" class |
| 1.2 | **Registration via event.** `AuthService.createUser` saves the `User` and publishes `UserRegisteredEvent(userId, role, username, email, phone)`. `PatientProfileCreator` (patients) and `StaffProfileCreator` (staff) create the profile in a **synchronous** listener, so registration stays atomic. `AdminService.createDoctor/Patient/Receptionist` keep working unchanged. | C1, C2 |
| 1.3 | Remove the inverse collections `PtInfo.appointments` and `Doctor.appointments`, the unused `PtInfoDTO.appointment` field, `Doctor.user` cascade, and the unused `password`/`role` fields. Account deletion already deletes children explicitly. (No schema change: `ddl-auto=update` never drops columns.) | part of C3, C4 |
| 1.4 | **Relative deletion via event.** `PtRelativeService.deleteRelative` publishes `RelativeDeletedEvent(relativeId)`; appointments clears the link in a synchronous listener. | rest of C3 |
| 1.5 | **Leave approval via event.** `LeaveRequestService.updateLeaveStatus` publishes `LeaveApprovedEvent(userId, start, end)`. Scheduling blocks the slots and appointments cancels them (both synchronous, same transaction); appointments then notifies patients after commit. `SlotService` asks staff's public `LeaveService.isOnApprovedLeave(…)` instead of `LeaveRequestRepository`. | C5 (part), staff → appointments / scheduling / notifications |
| 1.6 | **Move endpoints to their owning module, same URLs:**<br>• `/api/doctor/complete/{id}`, `/api/doctor/doctorPendingAppointments/{userId}`, `/api/doctor/doctorCompletedAppointments/{userId}` → new `DoctorAppointmentController` (appointments)<br>• `/api/receptionist/getAppointments`, `/getAppointmentByDoctor/{name}`, `/NewAppointment`, `/doctorPending…`, `/doctorCompleted…`, `/deleteAppointment/{id}` → new `ReceptionistAppointmentController` (appointments)<br>• `/api/doctor/{userId}/schedule` GET/PUT → new `DoctorScheduleController` (scheduling)<br>• `/api/patient/getAllDoctors`, `/api/patient/getAllBySpecialization` → new `DoctorDirectoryController` (staff)<br>• Matching service methods move too, for example `DoctorService.markAsCompleted` → `AppointmentService.markAsCompleted`. | rest of C4, C5 |
| 1.7 | **Account deletion lives only in administration.** New `AccountController` (administration) serves `DELETE /api/patient/deleteAccount/{id}`, `DELETE /api/doctor/delete/{id}` and `DELETE /api/receptionist/delete/{id}` (same URLs, same `@PreAuthorize`). `UserAccountService` calls public cleanup methods in foreign-key order: clinical → appointments → scheduling → patients/staff → identity. | C6, C7 |
| 1.8 | **Schedule change via event.** `DoctorScheduleService` publishes `ScheduleChangedEvent(doctorId)`. Appointments collects slot IDs still used by appointments and calls scheduling's `SlotService.deleteUnusedSlots(doctorId, fromDate, keepIds)`. The JPQL no longer mentions `Appointment`. | hidden scheduling → appointments |
| 1.9 | **Notifications after commit.** `AppointmentService` publishes `AppointmentBookedEvent` / `AppointmentCancelledEvent`. `AppointmentNotificationListener` (appointments) calls the notifications API after commit. `NotificationService` keeps plain-value methods and gains no domain dependency. | transaction/notification ordering |
| 1.10 | Run the checker: **0 cycles**. Update `SecurityRulesTest` mocks for the new controllers. Snapshot unchanged. | — |

**Exit:** 0 cycles, all tests green, endpoint snapshot identical, `FeatureFlowH2Test` (full booking → consultation → PDF flow) green.

### Phase 2 — Package by Module + Spring Modulith (≈ 2–3 days)

| Step | Work |
|---|---|
| 2.1 | Add `spring-modulith-starter-core` and `spring-modulith-starter-test` (1.4.x, the line for Spring Boot 3.5). |
| 2.2 | **Move-only commits**, one module at a time, bottom-up (common → notifications → identity → patients → staff → scheduling → appointments → clinical → administration). Each class goes to `com.itmonteur.hospitalerp.<module>` (API) or `….<module>.internal` / `….<module>.web` (internal) per §2.3. The main class and `SecurityConfig` go to the root package `com.itmonteur.hospitalerp`. Tests move to matching packages. |
| 2.3 | Mark `common` as shared: `@Modulithic(sharedModules = "common")` on the main class. |
| 2.4 | Add `ModularityTest`: `ApplicationModules.of(HospitalErpApplication.class).verify()`. It fails on any cycle or on access to another module's internals. Fix what it reports; usually a class sits in the wrong sub-package. |
| 2.5 | Generate module documentation with Modulith's `Documenter` (component diagrams + module canvases) into `docs/modules/`. |

**Exit:** `ModularityTest` green, all tests green, snapshot unchanged, app starts against MySQL.

> **Coordination:** Phase 2 touches every file. Pause other backend merges for those 1–2 days, or do the whole move in one sitting and merge immediately.

### Phase 3 — Maven Multi-Module Split (≈ 2–3 days)

**Target layout** (inside the existing `hospitalERP/` folder; the frontend is untouched):

```
hospitalERP/
├── pom.xml                      ← parent: packaging=pom, spring-boot-starter-parent 3.5.x,
│                                  <modules>, <dependencyManagement> for all internal modules,
│                                  shared plugin config (compiler, surefire)
├── common/pom.xml               ← hospital-common
├── notifications/pom.xml        ← hospital-notifications  (common, spring-boot-starter-mail, twilio)
├── identity/pom.xml             ← hospital-identity       (common, notifications, security, jjwt)
├── patients/pom.xml             ← hospital-patients       (common, identity)
├── staff/pom.xml                ← hospital-staff          (common, identity)
├── scheduling/pom.xml           ← hospital-scheduling     (common, identity, staff)
├── appointments/pom.xml         ← hospital-appointments   (+ notifications, patients, staff, scheduling)
├── clinical/pom.xml             ← hospital-clinical       (+ appointments, openpdf)
├── administration/pom.xml       ← hospital-administration (all domain modules)
└── app/
    ├── pom.xml                  ← hospital-app: all modules + spring-boot-maven-plugin (the only executable jar)
    └── src/main/resources/application.properties
```

| Step | Work |
|---|---|
| 3.1 | Tag `before-maven-split`. Create the parent POM and the 10 module POMs. Each module declares **only** the dependencies in §2.1; third-party libraries go only where used (OpenPDF → clinical, Twilio → notifications, jjwt → identity). |
| 3.2 | Move each module's package folder into `<module>/src/main/java/…` (move-only). Because every module shares the root package `com.itmonteur.hospitalerp`, `@SpringBootApplication` in `app` still finds all components, entities and repositories without extra `@EntityScan`. |
| 3.3 | **Tests:**<br>• Mockito unit tests move with their classes into each module.<br>• Integration tests stay in `app`: `SecurityRulesTest`, `RepositoryQueriesTest`, `ApplicationContextH2Test`, `FeatureFlowH2Test`, the endpoint snapshot test, `ModularityTest`.<br>• H2 becomes a test dependency of `app` only. |
| 3.4 | Add the Maven Enforcer plugin (`banCircularDependencies`, `dependencyConvergence`). |
| 3.5 | Update the commands in the README:<br>• build and test: `./mvnw verify` (from `hospitalERP/`)<br>• run: `./mvnw -pl app -am spring-boot:run`<br>• jar: `app/target/hospital-app-*.jar`<br>• `.env` now lives next to where the app is started; document this. |
| 3.6 | Re-import the project in VS Code / IntelliJ. |

**Exit:** `./mvnw verify` green from the root, the app runs from `app`, the endpoint snapshot is unchanged, and a quick manual smoke test passes (login, book, consultation, PDF).

### Phase 4 — Hardening (≈ 2–4 days, can overlap with feature work)

| Step | Work |
|---|---|
| 4.1 | **Flyway:**<br>• baseline the current MySQL schema (`V1__baseline.sql`);<br>• switch `ddl-auto` to `validate`;<br>• future changes go in per-module folders (`db/migration/appointments/…`) with one global version sequence (e.g. `V2026_10_01_1__appointments_add_x.sql`). |
| 4.2 | **Per-module security rules (optional):** each module contributes its URL rules through a small `ModuleSecurityRules` bean. `SecurityConfig` in `app` then only combines them, so adding a module doesn't mean editing a central file. |
| 4.3 | **CI** (GitHub Actions): `./mvnw verify` + `npm test` + `npm run build` on every PR. `ModularityTest` and Enforcer keep the boundaries from eroding. |
| 4.4 | One short `README.md` per module: purpose, public API, events published and consumed. |
| 4.5 | Convert the remaining field `@Autowired` to constructor injection while touching each class. |

### Phase 5 — Frontend by Feature (parallel track, ≈ 3–4 days)

The backend refactor doesn't require this, because URLs don't change, but it applies the same idea to the React code.

```
src/
├── app/                 App.js (routes), ProtectedRoute, setupAxios, config
├── features/
│   ├── auth/            Login, Register, ForgotPasswordPage, useAuthStore
│   ├── appointments/    AppointmentPage, AppointmentDetails, ReceptionistAppointmentDashboard, api.js
│   ├── patients/        profile (EditProfileModal → PatientProfilePage), relatives pages, api.js
│   ├── doctors/         public DoctorPage/DoctorProfile + doctor portal (dashboard, schedule, leave)
│   ├── clinical/        ConsultationModal, downloadPrescription, api.js
│   ├── admin/           admin pages + AdminLayout
│   └── public/          HomePage, AboutUs, ContactUs, Treatments
└── shared/              one Navbar, one TopNavbar (merge the duplicates), Footer, Loader, Button, utils
```

Steps:
- move files feature by feature;
- merge the duplicate `Navbar`/`TopNavbar`/`DoctorProfile` components;
- put each feature's calls in its own `api.js`;
- keep the 13 tests green and add one test per merged component.

### Phase 6 — Adding Future Modules (template)

For billing, pharmacy, lab, inventory and HR (section 7.5 of the code review):
1. Create `hospitalERP/<module>/` with a POM depending only on what it needs, for example billing → common, identity, patients, appointments.
2. Add the module to the parent POM, to `app`'s dependencies and to the checker mapping.
3. React to other modules through events, for example billing listens to `AppointmentCompletedEvent` to raise an invoice. The source module never calls billing.
4. Add its Flyway folder, its security rules and a module README.
5. `ModularityTest` and Enforcer confirm it fits.

---

## 7. Timeline Summary

| Phase | Effort | Depends on | Can ship to main after? |
|---|---|---|---|
| 0 Safety net | ~1 day | — | yes |
| 1 Break cycles | 4–6 days | 0 | yes, per step |
| 2 Package by module + Modulith | 2–3 days | 1 | yes |
| 3 Maven split | 2–3 days | 2 | yes |
| 4 Hardening | 2–4 days | 3 (Flyway can start after 0) | yes, per step |
| 5 Frontend by feature | 3–4 days | none (parallel) | yes |
| **Backend total** | **≈ 2.5–3.5 weeks** | | |

Every phase leaves the application working and releasable. There's no "big bang" branch that lives for weeks.

---

## 8. Branching and Commits

- One branch per phase (`refactor/modules-phase-1`, …), merged when its exit criteria pass.
- Phase 1: one commit (or small PR) per step 1.1–1.10.
- Phases 2 and 3: move-only commits (no logic edits) so `git log --follow` and reviews stay usable.
- Run the full backend suite, the frontend tests and the endpoint snapshot on every commit.
- Tag `before-maven-split` before Phase 3 for an easy rollback.

---

## 9. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Replacing direct calls with events changes behavior (lost atomicity, ordering) | Synchronous `@EventListener` for anything that must be atomic (§5 rule 4). `LeaveRequestServiceTest`, `AppointmentServiceTest` and `FeatureFlowH2Test` already cover these flows; extend them before each step. |
| An endpoint URL or role rule changes by accident | Endpoint snapshot test (0.2) + `SecurityRulesTest`. |
| Table names change after moving entities | Every entity has an explicit `@Table` after step 0.4; package names never affect table names. |
| Merge conflicts with ongoing feature work during the big move | Pause backend merges for the 1–2 days of Phase 2, or finish it in one sitting. |
| Cycles creep back later | Checker (Phase 1), `ModularityTest` (Phase 2+), Maven Enforcer (Phase 3+), all in CI. |
| Team unfamiliar with Modulith / multi-module Maven | This document + per-module READMEs + generated module diagrams (2.5). |
| `.env` / run command confusion after the split | Updated README commands (3.5). |

---

## 10. Definition of Done

- [ ] Backend is a Maven multi-module build with the 10 modules in §2.1; `./mvnw verify` passes from the root.
- [ ] 0 dependency cycles; `ModularityTest` and Maven Enforcer pass in CI.
- [ ] No module accesses another module's repository or `internal`/`web` package.
- [ ] All 83 endpoints unchanged (snapshot test); the frontend works without code changes.
- [ ] Registration, booking, reschedule/cancel, leave approval, consultation + PDF, schedule change and account deletion all work (automated tests + one manual smoke test on MySQL).
- [ ] README updated (build/run commands); each module has a short README; module diagrams in `docs/modules/`.
- [ ] Flyway baseline in place and `ddl-auto=validate` (Phase 4).

---

## 11. Explicitly Out of Scope (and When to Revisit)

| Not doing now | Reconsider when… |
|---|---|
| Microservices / separate deployables | A module needs independent scaling (e.g. notifications volume), a separate team owns it (e.g. billing), or compliance requires isolation. The events from Phase 1 are the seams to cut along. |
| Separate database per module | Only together with a microservice extraction. |
| Replacing entity references with plain IDs | Only for a module that is about to be extracted; downward entity references are fine inside a monolith. |
| API gateway, service discovery, distributed tracing | Only after the first real extraction. |
