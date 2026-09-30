# HospitalERP — Multi-Module Architecture Plan

**Date:** 2026-09-25
**Starting point:** commit `2bc6d31` on branch `fix/code-review-issues`
**Scope:** backend (`hospitalERP/`). The frontend is covered by an optional parallel track (Phase 5).
**Status:** in progress on branch `refactor/modules`. See §12 for the progress log.

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

## 3. Class-to-Module Mapping (all 92 current classes)

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
| 1.11 | **Remove remaining cross-module repository calls** (§5 rule 2; added 2026-09-25 after step 1.2 found 29 of them). Steps 1.4–1.7 remove about a third. For the rest, the owning module gets small public methods, e.g. `DoctorService.getDoctorEntityByUserId`, `PatientService.getPatientEntity…`, identity `UserService.updateContactDetails(userId, email, phone)` (used by the profile updates in patients/staff). The checker is extended to report cross-module repository use, so Phase 2's Modulith check has nothing left to find. | rule 2 |

**Exit:** 0 cycles, **0 cross-module repository calls**, all tests green, endpoint snapshot identical, `FeatureFlowH2Test` (full booking → consultation → PDF flow) green.

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
| 4.1 | **Flyway:**<br>• baseline the current MySQL schema (`V1__baseline.sql`);<br>• drop the columns left unused by step 1.3 (`doctor.password`, `doctor.role`, `patient.role`, `patient_relative.role`, `receptionist.role`);<br>• switch `ddl-auto` to `validate`;<br>• future changes go in per-module folders (`db/migration/appointments/…`) with one global version sequence (e.g. `V2026_10_01_1__appointments_add_x.sql`). |
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

---

## 12. Progress Log

Work goes one step at a time. After each step: report, then wait for approval before the next.

| Step | Status | Date | Notes |
|---|---|---|---|
| 0.1 Branch | ✅ done | 2026-09-25 | `refactor/modules` created from `origin/main` (`49635d8`, which already contains PR #2) |
| 0.2 Endpoint contract test | ✅ done | 2026-09-25 | `EndpointContractTest` + `src/test/resources/api-endpoints.txt` (83 endpoints). Verified it fails when a URL changes. Regenerate after an **intended** API change: `./mvnw test -Dtest=EndpointContractTest -DupdateEndpointSnapshot=true` |
| 0.3 Dependency checker | ✅ done | 2026-09-25 | `python tools/check_module_deps.py` (exit 1 while violations exist). Baseline: 18 disallowed dependencies; two-way pairs = C1–C7 + 3 caused by `EntityMapper` |
| 0.4 Pin table name, unused imports | ✅ done | 2026-09-25 | `@Table(name = "leave_request")` on `LeaveRequest` (same name as before); removed unused `Doctor` imports from `PtInfoRepository`, `ReceptionistRepository` |
| 0.5 Baseline | ✅ done | 2026-09-25 | Backend 62 tests (1 skipped: needs MySQL), frontend 13 tests, all green |
| 1.1 Split `EntityMapper` | ✅ done | 2026-09-25 | Replaced by `AppointmentMapper`, `DoctorMapper` and `PatientMapper` (mapping code unchanged). Checker: 18 → 10 disallowed dependencies; two-way pairs 10 → 7 (C1–C7 left). Backend 62 tests green (1 skipped). Tests were run with `-DargLine="-Xmx512m -javaagent:<mockito-core jar>"` because this machine was low on memory; see note below. |
| 1.1a Test setup fix (optional mini-step) | ✅ done | 2026-09-25 | `pom.xml`: Surefire loads Mockito as `-javaagent` (via `maven-dependency-plugin:properties`), with an empty `argLine` property so `-DargLine=…` still works. `.gitignore`: `hs_err_pid*.log`, `replay_pid*.log`. Plain `./mvnw test` passes again (62, 1 skipped); the self-attach warning is gone. |
| 1.2 Registration via event | ✅ done | 2026-09-25 | New `UserRegisteredEvent` (identity). `AuthService.createUser` only saves the user and publishes it. `PatientProfileCreator` (patients) and `StaffProfileCreator` (staff) create the profile in synchronous `@EventListener`s, in the same transaction. Removes **C1, C2**. Checker: 10 → 8 disallowed, pairs 7 → 5. Tests: +4 unit (`ProfileCreatorsTest`) and +1 end-to-end (`creatingUsersCreatesTheirProfiles`: admin creates doctor / receptionist / patient, self-registered patient). Backend 67 green (1 skipped), run with `-DargLine="-Xms64m -Xmx512m"` because memory was low again. |
| 1.3 Remove reverse links & unused fields | ✅ done | 2026-09-25 | Removed `PtInfo.appointments`, `Doctor.appointments`, `PtInfoDTO.appointment` (was always `null`, unused by the frontend), `Doctor.password`, and the `role` fields of `Doctor`/`PtInfo`/`PtRelative`/`Receptionist`, plus their constructor parameters. `Doctor.user` no longer cascades: `UserAccountService` deletes the login explicitly. **Pre-existing bug found and fixed:** deleting a doctor (or patient) who still had upcoming bookings failed with HTTP 500 (`TransientObjectException`, reproduced on the pre-1.3 code too). Fix: flush + clear before the bulk deletes, then delete by id. New end-to-end test `deletingAccountsRemovesProfilesAndLogins`. Checker unchanged (8 / 5) as expected: the entity-level edges are gone, and the service-level ones go in 1.4–1.6. Backend 68 green (1 skipped). No DB change: the now-unused columns (`doctor.password`, `*.role`) stay in MySQL until a Flyway migration drops them (Phase 4). |
| 1.4 Relative deletion via event | ✅ done | 2026-09-25 | New `RelativeDeletedEvent` (patients). `PtRelativeService.deleteRelative` publishes it instead of calling `AppointmentRepository`. `AppointmentRelativeUnlinker` (appointments) clears the link in a synchronous listener before the relative row is deleted. **C3 fully removed.** Checker: 8 → 7 disallowed, pairs 5 → 4. New end-to-end test `deletingARelativeKeepsTheirAppointments`, verified to fail (409) when the listener is switched off. Backend 69 green (1 skipped). |
| 1.5 Leave approval via event | ✅ done | 2026-09-25 | New `DoctorLeaveApprovedEvent(doctorId, start, end)` (staff), published by `LeaveRequestService.updateLeaveStatus`. `SlotLeaveBlocker` (scheduling) blocks the slots and `AppointmentLeaveCanceller` (appointments) cancels active bookings and notifies patients; both synchronous, same transaction, same behaviour as before. `SlotService` now asks `LeaveRequestService.isOnApprovedLeave(…)` instead of `LeaveRequestRepository`. `LeaveRequestService` no longer uses appointments, slots or notifications. End-to-end test `approvingDoctorLeaveCancelsBookingsAndBlocksSlots` was written **first** and passed on the old code; it fails if either listener is switched off (verified). +2 listener unit tests, `LeaveRequestServiceTest` adapted. Checker: 7 → 6 disallowed (staff → notifications gone); pairs still 4, because C4/C5 also come from endpoints (step 1.6). Backend 73 green (1 skipped). |
| — Commit | ✅ | 2026-09-25 | Phase 0 + steps 1.1–1.5 committed on `refactor/modules` (not pushed). |
| 1.6 Move endpoints to their module | ✅ done | 2026-09-25 | New controllers with the **same URLs**: `DoctorAppointmentController` and `ReceptionistAppointmentController` (appointments), `DoctorScheduleController` (scheduling), `DoctorDirectoryController` (staff). `markAsCompleted`, doctor pending/completed lists and counts moved from `DoctorService` to `AppointmentService`. `AdminController` now calls `AppointmentService`. `DoctorService`, `ReceptionistController`, `PtInfoController` and `DoctorController` no longer touch appointments/schedules. End-to-end test `endpointsMovedInStep16KeepWorking` (all 13 moved endpoints + "other doctor can't complete") written first and passed on the old code. Endpoint snapshot unchanged. **C4 and C5 removed**, plus patients → staff. Checker: 6 → 3 disallowed, pairs 4 → 2 (C6, C7 left). Backend 74 green (1 skipped). |
| — Commit | ✅ | 2026-09-25 | Step 1.6 committed (`e9cf34d`). |
| 1.7 Account deletion only in administration | ✅ done | 2026-09-25 | New `AccountController` (administration) serves `DELETE /api/patient/deleteAccount/{ptId}`, `DELETE /api/doctor/delete/{id}` and `DELETE /api/receptionist/delete/{receptionistId}` with the same URLs, responses and `@PreAuthorize`. The logic moved as-is to `UserAccountService.deletePatientAccount / deleteDoctorByDoctorId / deleteReceptionistByReceptionistId`. `PtInfoService`, `DoctorService` and `ReceptionistService` no longer depend on `UserAccountService`. `SecurityRulesTest` now includes `AccountController`. Covered by the existing end-to-end deletion test (step 1.3). **C6 and C7 removed: 0 two-way dependencies left.** Checker: 3 → 1 disallowed (only the hidden `@Query` one, step 1.8). Backend 74 green (1 skipped). |
| — Commit | ✅ | 2026-09-25 | Step 1.7 committed (`21600b1`). |
| 1.8 Schedule change via event | ✅ done | 2026-09-25 | New `DoctorScheduleChangedEvent` (scheduling), published by `DoctorScheduleService.updateSchedule` (no longer depends on `SlotService`). `ScheduleChangeSlotCleaner` (appointments) collects the slot ids bookings still use (`AppointmentRepository.findSlotIdsInUse`) and calls `SlotService.deleteUnusedSlots(doctorId, fromDate, keepIds)`. The slot `@Query` no longer mentions `Appointment` (`deleteFromDate` / `deleteFromDateExcept`, split so an empty `NOT IN ()` list never reaches the DB). End-to-end test `changingAScheduleKeepsBookingsAndRemovesUnusedSlots` written first and passed on the old code; with the listener off, an out-of-hours slot could still be booked (verified). +1 repository test for the empty keep-list path. **Checker: `OK - module boundaries respected` (0 disallowed, 0 cycles).** Backend 76 green (1 skipped). |
| — Commit | ✅ | 2026-09-25 | Step 1.8 committed (`397dada`). |
| 1.9 Notifications after commit | ✅ done | 2026-09-25 | New `AppointmentNotificationEvent(kind, info)` (kinds: BOOKED, CANCELLED, CANCELLED_BY_DOCTOR_LEAVE, REMINDER). The 5 senders now publish it instead of calling `NotificationService`: booking, cancel (`AppointmentService`), leave cancellation (`AppointmentLeaveCanceller`), doctor-account deletion (`UserAccountService`) and reminders (`AppointmentReminderService`, whose scheduled entry point is now `@Transactional` because a self-call skipped it). `AppointmentNotificationListener` sends the messages via `@TransactionalEventListener(AFTER_COMMIT)`. **Behaviour fix:** a change that rolls back no longer sends a message. New `NotificationsH2Test`: (1) booking/cancel notify once and a failed booking adds nothing (written first, passed on the old code); (2) rolled-back → nothing sent, committed → sent, which fails with the old immediate sending (verified). Unit tests adapted. Checker still OK. Backend 78 green (1 skipped). |
| — Commit | ✅ | 2026-09-26 | Step 1.9 committed (`7a92a6e`). |
| 1.11 No cross-module repository use | ✅ done | 2026-09-26 | Checker now also enforces plan rule 2 (found 24 uses). New `UserService` (identity) replaces `UserRepository` outside identity, incl. `updateContactDetails` for the profile updates. Owning modules got small public methods (`find…Entity…`, `delete…Entity`, `deleteAllFor…`, `findUpcomingFor…`, `hasAppointment`, `markCompletedByConsultation`). `AppointmentService`, `ConsultationService`, `SlotService`, `DoctorScheduleService`, `AdminService`, `UserAccountService` (and the profile services) now call those instead of foreign repositories; account deletion keeps the same order of operations. Unit tests switched to service mocks. **Checker: OK on all 4 rules (0 disallowed, 0 cycles, 0 foreign repositories).** Backend 78 green (1 skipped); endpoint snapshot unchanged. |
| — Commit | ✅ | 2026-09-26 | Step 1.11 committed (`7c74219`). |
| 1.10 Phase 1 wrap-up | ✅ done | 2026-09-26 | `SecurityRulesTest` now includes the 4 controllers added in 1.6 (+5 checks: public directory, patient forbidden on doctor/front-desk endpoints, anonymous 401, receptionist allowed but can't delete staff, doctor allowed). README documents the checker, the endpoint snapshot and the small-heap test command. Final run: checker OK, backend 83 green (1 skipped), frontend 13 green, frontend build OK. |

**Phase 1 exit criteria: all met (2026-09-26).**
- 0 module cycles, 0 disallowed dependencies, 0 cross-module repository calls (`tools/check_module_deps.py` → OK)
- all tests green: backend 83 (1 skipped, needs MySQL), frontend 13
- endpoint snapshot identical (83 endpoints)
- `FeatureFlowH2Test` green (full booking → consultation → PDF flow and the new safety tests)

Behaviour changes in Phase 1, all intended:
- deleting an account with upcoming bookings no longer fails (1.3);
- notifications only go out after a successful commit (1.9);
- the reminder job now really runs in a transaction (1.9).

Everything else is structural.

| Phase 2 | ✅ done | 2026-09-28 | Package by module + Spring Modulith (§6). Exit criteria met except one: the app has not been started against MySQL yet (manual smoke test still to do: login, booking, consultation, PDF). |
| 2.1 Add Spring Modulith | ✅ done | 2026-09-26 | `spring-modulith-bom` 1.4.13 (the line for Boot 3.5) imported in `<dependencyManagement>`; `spring-modulith-api` at compile scope (annotations only, no runtime behaviour); `spring-modulith-starter-test` and `spring-modulith-docs` at test scope. No code changes. Checker OK, backend 83 green (1 skipped). |
| 2.2 Package by module | ✅ done | 2026-09-26 | Move-only, one commit per module after a prep commit (main class, `SecurityConfig`, end-to-end tests → root `com.itmonteur.hospitalerp`; temporary two-package scanning, removed in the last commit). Placement: classes other modules use (entities, enums, DTOs, events, facade services) → `<module>`; repositories, listeners, jobs, config and module-only services → `<module>.internal`; controllers → `<module>.web` (`common.web` holds `GlobalExceptionHandler` and `WebConfig`). Unit tests moved next to their class; the two tests that span modules (`DoctorLeaveListenersTest`, `ProfileCreatorsTest`) stay with the end-to-end tests in the root. Only non-move changes: `SmsService.mask` and `AppointmentService.notificationInfo` became public, and the fully qualified enum names in JPQL follow the new packages. The checker now takes a class's module from its package only (the `CLASS_MODULE` table is gone). Checker OK, backend 83 green (1 skipped) and the file count checked before and after every run (an external tool deleted `src/main/java` once mid-step; restored from git). |
| 2.3 Shared module | ✅ done | 2026-09-28 | `@Modulithic(sharedModules = "common")` on `HospitalErpApplication` (annotation only, no runtime effect). A throwaway Modulith run found the 9 modules with `common` shared and **no dependency violations**; its only findings are field injection (`@Autowired` fields) in `AuthController`, `AdminService` and `AdminController`, to be fixed in 2.4. Checker OK, backend 83 green (1 skipped). |
| 2.4 ModularityTest | ✅ done | 2026-09-28 | `ModularityTest` runs `ApplicationModules.of(HospitalErpApplication.class).verify()` in every test run. To make it pass, `AuthController`, `AdminService` and `AdminController` switched from `@Autowired` fields to constructor injection (no behaviour change). Checked that it fails on a real break: a temporary class in `clinical` using `appointments.internal.AppointmentMapper` was reported ("depends on non-exposed type"), then removed. Checker OK, backend 84 green (1 skipped). |
| 2.5 Module documentation | ✅ done | 2026-09-28 | `ModularityTest.writeModuleDocumentation` runs Modulith's `Documenter`: on every test run into `target/spring-modulith-docs` (proves it still works), and with `-DupdateModuleDocs=true` into `docs/modules/` (committed): `components.puml` (C4 component diagram of all 9 modules), `module-<name>.puml` and `module-<name>.adoc` canvases, `all-docs.adoc`. Output is deterministic (two runs identical). The diagram confirms the §2.2 direction: every arrow points down; upward links are only event listeners. Checker OK, backend 85 green (1 skipped). |
| Phase 3 | ✅ done | 2026-09-28 | Maven multi-module split (§6). Tag `before-maven-split` = `822c936`. Exit criteria met except the manual MySQL smoke test (login, booking, consultation, PDF), still to do by the user. |
| 3.1 + 3.2 Parent POM, module POMs, move | ✅ done | 2026-09-28 | Done together: once `hospitalERP/pom.xml` is a parent (`packaging=pom`) nothing builds until the code sits in modules. groupId `com.itmonteur.hospitalerp`; parent `hospital-erp` with the 10 `<modules>`, `<dependencyManagement>` for our modules and the third-party versions, `spring-boot-starter-test` for all modules, the Mockito-agent Surefire setup. `hospital-<module>` POMs list only the §2.1 modules; third-party libraries only where imported (OpenPDF → clinical, Twilio + mail → notifications, jjwt → identity, ModelMapper → staff/administration/app). `hospital-app`: all modules, actuator, MySQL driver, Modulith API, the only `spring-boot-maven-plugin` (working directory `hospitalERP/`, so `.env` and `uploads/` stay put); `spring-boot.run.skip` is true everywhere else, so `./mvnw spring-boot:run` from `hospitalERP/` still works (checked: the app starts and only fails on the deliberately unreachable DB). Code moved with `git mv` (no source changes); `application.properties` and **all tests** moved to `app` for now (unit tests go to their modules in 3.3). Other changes: `ModularityTest` docs path is `../../docs/modules`; `-Dtest=…` commands need `-Dsurefire.failIfNoSpecifiedTests=false` in a reactor (README + test comments); the checker scans `hospitalERP/*/src/main/java`. Module docs regenerated (same lines, different order because classes now come from 10 folders). `./mvnw package`: checker OK, 85 green (1 skipped), `app/target/hospital-app-0.0.1-SNAPSHOT.jar`. |
| 3.3 Tests per module | ✅ done | 2026-09-28 | Unit tests moved with `git mv` into their modules' `src/test` (unchanged): common 3, identity 6, staff 5, scheduling 8, appointments 8, clinical 7 tests. Each now compiles against its own module and the ones below it only. `app` keeps the end-to-end and context tests (`ApplicationContextH2Test`, `FeatureFlowH2Test`, `NotificationsH2Test`, `HospitalErpApplicationTests`), `SecurityRulesTest`, `RepositoryQueriesTest`, `EndpointContractTest`, `ModularityTest`, and the two tests that check one event in two modules (`DoctorLeaveListenersTest`, `ProfileCreatorsTest`) — 48 tests. H2 is a test dependency of `app` only. Checker OK, 85 green (1 skipped). |
| 3.4 Maven Enforcer | ✅ done | 2026-09-28 | `maven-enforcer-plugin` in the parent, for every module: `dependencyConvergence` (one version per library across all modules) and `requireJavaVersion` (≥ 17). `banCircularDependencies` (plan) was left out: it needs `extra-enforcer-rules`, fails on `./mvnw validate` because it resolves sibling jars before they are built, and adds nothing — Maven itself refuses a reactor with a module cycle. Both checks proven with temporary POM edits (reverted): `patients` → `clinical` gave "The projects in the reactor contain a cyclic reference … patients --> clinical --> patients"; an explicit `jjwt-api` 0.11.5 in `patients` gave "Dependency convergence error for io.jsonwebtoken:jjwt-api". Current build converges in all 11 projects. `./mvnw package`: checker OK, 85 green (1 skipped). |
| 3.5 README | ✅ done | 2026-09-28 | README: project structure with the module folders; new "Backend modules" section (module table with responsibility and allowed dependencies, events for upward calls, API/`internal`/`web` packages, where tests live); commands: build → `app/target/hospital-app-0.0.1-SNAPSHOT.jar`, `java -jar` from `hospitalERP/`, one module's tests `./mvnw -pl clinical -am test`, what a dependency-convergence failure means; snapshot path `app/src/test/resources/api-endpoints.txt`. `./mvnw spring-boot:run` is unchanged and `.env` stays in `hospitalERP/`. PROJECT_OVERVIEW.md structure updated. Checked: `-pl clinical -am test` (37 tests green) and `java -jar` (starts, fails only on the deliberately unreachable DB). |
| 3.6 IDE re-import | ✅ done | 2026-09-28 | Cursor (VS Code Java extension) re-imported the reactor at 12:44: "Importing Maven project(s) … 11 new project(s)" — `hospitalERP` (parent pom, named after its folder) and the 10 `hospital-*` modules. No problem markers in any of the 10 modules; the parent only has the m2e notice "Project configuration is not up-to-date with pom.xml" (fix: *Java: Reload Projects*). No stale Eclipse/IntelliJ files in the repo. Optional clean-up for the user: the old single-project output `hospitalERP/target` (git-ignored). |
| Phase 4 | ✅ done | 2026-09-28 | Hardening (§6), done in the order 4.5 → 4.4 → 4.2 → 4.3 → 4.1. Open for you: upgrade your MySQL with the README steps (also covers the pending smoke test); the CI workflow file is still to be committed once the GitHub token has the `workflow` scope (step 4.3). |
| 4.5 Constructor injection | ✅ done | 2026-09-28 | The last 12 production classes with `@Autowired` fields now use constructor injection with `final` fields (same style as the rest): `AuthService` (7), `JWTAuthenticationFilter`, `CustomUserDetailsService`, `EmailService`, `SecurityConfig`, and the controllers `AppointmentController`, `DoctorController`, `ReceptionistController`, `LeaveRequestController`, `PtInfoController`, `PtRelativeController`, `SlotController`. No field injection left in production code (0 `@Autowired`); test classes keep field injection, which is the normal style for JUnit tests. No behaviour change. Checker OK, 85 green (1 skipped). |
| 4.4 Module READMEs | ✅ done | 2026-09-28 | `hospitalERP/<module>/README.md` for the 9 business modules, same layout: purpose, depends on / used by, public API (classes and the methods other modules call), events published and listened to (sync vs after-commit), endpoints (URL prefixes), internal classes, configuration (env variables), tests. Written from the code (class lists, `publishEvent`/listener scan, endpoint count = 83, `@Value` keys) and checked against it; corrected on the way: only deleting a *doctor* sends cancellation notices (deleting a patient releases the slots), `Gender` is not used by doctors. Main README's module table links each module README. Documentation only. |
| 4.2 Per-module security rules | ✅ done | 2026-09-28 | New `identity.ModuleSecurityRules` (API): `endpointRules` (single endpoints / exceptions) and `areaRules` (a module's URL area). One `@Component` per module in its `web` package — identity (`/api/auth/**` public), staff (public doctor directory; `/api/doctor`, `/api/receptionist`, `/api/leaves` areas), patients, scheduling, appointments, clinical, administration — holding exactly the lines `SecurityConfig` had. `SecurityConfig` keeps only the app-wide rules (`/error`, `/actuator/health`, GET `/uploads/**`), then applies all endpoint rules, then all area rules (modules sorted by class name), then `anyRequest().authenticated()`; equivalent because no two endpoint rules and no two areas overlap. Safety net written **before** the change: `AccessRulesContractTest` reads the URL rules from the `SecurityFilterChain` and snapshots, for the 83 endpoints + 4 other paths, access for anonymous / patient / doctor / receptionist / admin (`api-access.txt`, 87 lines). Identical after the change; proven sensitive (letting receptionists into `/api/consultations/**` flagged exactly the 5 consultation endpoints; reverted). `SecurityRulesTest` scans all `ModuleSecurityRules` beans. Spring Security declared in patients, scheduling and appointments POMs (now imported there). Checker OK, 86 green (1 skipped). |
| 4.3 CI | ⏸ prepared, kept local | 2026-09-28 | **Workflow file not committed yet** (the GitHub token used here lacks the `workflow` scope, so GitHub rejects pushes that contain it; kept in the working copy and in the local branch `ci-workflow-original`, excluded via `.git/info/exclude`). Add it with one commit once the token has the scope. Prepared `.github/workflows/ci.yml` on pull requests, pushes to `main` and manual runs; two parallel jobs. Backend: Temurin 17 with Maven cache, `python3 tools/check_module_deps.py`, `./mvnw -B verify`, surefire reports uploaded on failure. Frontend: Node 20 with npm cache, `npm ci`, `npm test -- --watchAll=false`, `npm run build` with `CI=false` — CI mode fails on the 19 existing lint warnings (14 `no-unused-vars`, 5 `react-hooks/exhaustive-deps`); fixing them (hook dependencies can change behaviour) is Phase 5, then drop the override. Prepared: `hospitalERP/mvnw` marked executable in git (was 644; Linux runners would fail with "Permission denied"); committed `package-lock.json` checked in sync with `package.json` (`npm ci --dry-run` on the committed files). Checked locally: YAML parses (js-yaml), the same backend (86 green, 1 skipped) and frontend commands (13 tests, build with warnings) pass. It first runs on GitHub when the branch is pushed. |
| 4.1 Flyway | ✅ done (your MySQL still to upgrade) | 2026-09-28 | Flyway 11.7.2 (+ `flyway-mysql`) in `app`; `ddl-auto=validate`; `baseline-on-migrate` at version 1. Migrations in `app/src/main/resources/db/migration/` (kept in `app` so they are found reliably inside the executable jar), one folder per owning module, one global date-based version sequence: `V1__baseline.sql` = the schema Hibernate creates on MySQL 8 for today's entities (generated with `ddl-auto=create` on a throw-away MySQL, `mysqldump --no-data`), plus the 5 columns dropped from the entities in step 1.3 and the start rows of the two `*_seq` id tables; `staff/V2026_09_28_1` drops `doctor.password`, `doctor.role`, `receptionist.role`; `patients/V2026_09_28_2` drops `patient.role`, `patient_relative.role` — each drop guarded via `information_schema` (MySQL has no `DROP COLUMN IF EXISTS`). New `DatabaseMigrationMySqlTest` (Testcontainers, real MySQL 8.0, skipped without Docker): (1) pre-Flyway database with data → baselined, 2 migrations, Hibernate validate OK, data kept, old columns gone; (2) fresh install → identical tables/columns/indexes/FKs. Proven sensitive: removing `slot_minutes` from V1 → "Schema-validation: missing column [slot_minutes]". The packaged jar migrates a fresh MySQL and starts. Testcontainers pinned to 1.21.4 (Boot's 1.21.3 can't talk to Docker Engine 29). H2 tests run with `spring.flyway.enabled=false`; `RepositoryQueriesTest` sets `create-drop` itself (never globally: `HospitalErpApplicationTests` may point at a real database). README: Flyway section with upgrade steps (backup, start, expected log, way back) and troubleshooting. `./mvnw verify`: 88 green (1 skipped). **Still to do by you:** upgrade your own MySQL once with those steps. |
| Phase 5 | ✅ done | 2026-09-30 | Frontend by feature (§6). **All five phases are done.** Steps: 5.1 move files into `app/`, `features/<feature>/`, `shared/` (move-only); 5.2 merge the duplicate `Navbar`/`TopNavbar`/`DoctorProfile` and drop unused files; 5.3 one `api.js` per feature; 5.4 fix the 19 lint warnings and drop `CI=false` from the CI workflow. |
| 5.1 Frontend by feature (move) | ✅ done | 2026-09-28 | 62 of 68 files moved with `git mv`, content unchanged except 141 import lines (recomputed from an import graph taken before the move). `app/`: App.js, config, setupAxios, ProtectedRoute. `features/`: auth (incl. `useAuthStore`), public, appointments, patients, doctors (public pages; `portal/` = the doctor portal, so the two `DoctorProfile.js` don't clash), clinical (ConsultationModal, downloadPrescription), admin (incl. AdminSidebar). `shared/`: Navbar/TopNavbar (the portal variants in `shared/portal/` until 5.2 merges them), Footer, PopupForm, ProfilePage (the navbar's profile panel for every role), DarkThemeToggle + `useAppStore`, Loader, Button, `utils/`. Stay in `src/`: index.js, index.css, App.css (global styles), reportWebVitals, setupTests, logo.svg. Unused (for 5.2): `DoctorProfileCard.js`, `Button.js`, `logo.svg`. Checked: all imports resolve (graph), 13 tests green from the new places, build OK with the same 19 warnings. |
| 5.2 Duplicates merged, unused files removed | ✅ done | 2026-09-29 | `TopNavbar`: one component in `shared/`; `variant="portal"` keeps what the admin portal had (compact logo, teal user name, plain logout button, logout to `/login`), the default keeps the site look (logout to `/`). The portal `Navbar` copy was never rendered (only an unused import in `AdminLayout`) and differed only by lacking the receptionist link → deleted; the portal `TopNavbar` was rendered only by `AdminLayout` (unused imports in `DoctorLayout`, `DoctorAppointments`, `LeaveManagementPage` removed). The two `DoctorProfile` pages are not duplicates — renamed for what they are: `doctors/DoctorDetailsPage` (public, read-only) and `doctors/portal/MyDoctorProfile` (the doctor's own, editable). Deleted unused files: `DoctorProfileCard.js`, `shared/Button.js`, `logo.svg`. New tests: `TopNavbar.test.js` (3: logged out, site logout → home, portal logout → login) and `Navbar.test.js` (4: links per role). 20 frontend tests green; build OK, lint warnings 19 → 14 (the removed imports and the deleted copy's unused variable). No visible change. |
| 5.3 One api.js per feature | ✅ done | 2026-09-29 | All 61 backend calls (55 axios + 6 `fetch`) moved out of the pages into `features/<feature>/api.js` (auth 7, doctors 9, appointments 15, patients 6, clinical 5, admin 11) and `shared/profileApi.js` (the logged-in user's own profile for patient/doctor/receptionist — replaces the role→URL tables in `ProfilePage` and `EditProfileModal`). Functions return the axios promise, so pages keep `res.data` and their try/catch. Only the api modules (and `app/setupAxios`) import axios now. Manual `Authorization` headers dropped: `setupAxios` already adds the same token to every API request (and handles 401). The 6 `fetch` calls in the doctor portal became axios calls: same results, and two fixes — applying for leave no longer says "Leave Applied Successfully" when the server rejects it (shows the server's reason), and marking an appointment complete shows the error message instead of the raw error JSON. Also removed a `console.log` that printed the JWT (`ManageUsers`). Checked: all 55 api functions match a backend endpoint (method + path) in `api-endpoints.txt`; the 3 tests that assert the exact axios URL/body (ForgotPassword, ConsultationModal, DoctorSchedule) pass unchanged; 20 tests green; build OK, lint warnings 14 → 10. |
| 5.4 Lint warnings fixed | ✅ done | 2026-09-30 | The last 10 warnings: 7 unused variables/imports removed (`fetchAppointmentCount` in ManageDoctor, never-read `role`/`userId` states in DoctorPage, DoctorAppointments and Navbar, `FaUserMd` in AboutUs, `slideIn` in Treatments) — nothing on screen depended on them. 3 hook-dependency warnings fixed without risking fetch loops: `DoctorDashboard` effect depends on `navigate` (and no longer returns `navigate(...)` from the effect); `LeaveManagementPage` `fetchLeaves` wrapped in `useCallback([userId, viewType])` so the effect still runs only when the tab changes; `EditProfileModal`'s pure `getAppointmentApi` moved to module scope. New `LeaveManagementPage.test.js`: leaves load once on open and once per tab change — proven to fail when the callback is made unstable (2 calls instead of 1). `CI=true npm run build`: "Compiled successfully" (0 warnings); `CI=false` removed from the prepared CI workflow (still local, see 4.3). 21 frontend tests green. |

**Note: running tests when memory is low (found in step 1.1).** Mockito attaches itself to the test JVM by starting a second Java process. With less than about 1 GB of free memory that process can't start, and every test using mocks fails with `Could not initialize plugin: interface org.mockito.plugins.MockMaker`. It isn't a code problem. Workarounds:
- run with `-DargLine="-Xms64m -Xmx512m -XX:+UseSerialGC -javaagent:C:/Users/<you>/.m2/repository/org/mockito/mockito-core/5.17.0/mockito-core-5.17.0.jar"`; or
- close other applications.

The permanent fix, which Mockito also recommends for Java 21+, is to load the agent in the Surefire configuration of `pom.xml`. That's a small optional step, pending approval.

