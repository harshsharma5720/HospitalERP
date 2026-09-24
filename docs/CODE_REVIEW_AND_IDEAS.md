# HospitalERP — Code Review & Improvement Ideas

**Review date:** 2026-09-24
**Scope:** Whole repository at commit `7c9aed4`: Spring Boot backend (`hospitalERP/`), React frontend (`hospital-frontend/`) and docs.

Path shorthand used below:
- `BE/` = `hospitalERP/src/main/java/ITmonteur/example/hospitalERP/`
- `FE/` = `hospital-frontend/src/`

Severity levels:
- 🔴 **Critical**: security hole or data loss. Fix before anyone else uses the app.
- 🟠 **High**: a feature is broken or produces wrong data.
- 🟡 **Medium**: works today but is fragile, inconsistent or hard to maintain.
- 🟢 **Low**: cleanup and polish.

---

## 1. Summary

The project has a good base. The layering is clean (controller → service → repository → entity/DTO), roles are modelled, slot-based booking works, leave approval cancels the affected appointments and notifies patients by SMS and email, and the UI has a consistent theme with dark mode.

The main problems:

1. **Authorization is mostly missing.** Anyone can register as `ADMIN`. Any logged-in user can delete every appointment. Most endpoints trust the ID in the URL, so a user can read or change other users' records.
2. **Appointment state is stored twice** (`status` enum and `isCompleted` boolean), and the two values drift apart. Dashboards and counts show wrong numbers as a result.
3. **Several frontend flows are broken**: booking for a relative, "Edit appointment", "Edit relative", and admin logout.
4. **There are no automated tests**, and error handling is inconsistent (`RuntimeException` → HTTP 500 with a generic body).

---

## 2. 🔴 Critical Issues (Security & Data Loss)

### 2.1 Anyone can register as ADMIN
- **Where:** `BE/services/AuthService.java:61`: `Role.valueOf(request.getRole().toUpperCase())`, reached from the public `POST /api/auth/register`.
- **Problem:** The client picks the role. A request like `{"role":"ADMIN", ...}` to the public endpoint creates an admin account and returns a JWT.
- **Fix:**
  - Public registration should always create `PATIENT` and ignore any role in the request body.
  - Creating doctors, receptionists and admins should go only through `/api/admin/**`, which already exists in `AdminController`.
  - Point `FE/pages/admin/RegisterUser.js` at the admin endpoints instead of `/api/auth/register`.

### 2.2 `/appointment/**` and `/api/slots/**` only require "any logged-in user"
- **Where:** `BE/configuration/SecurityConfig.java:51` (`anyRequest().authenticated()`).
- **Problem:** No role rule covers `AppointmentController` (`/appointment/...`) or `SlotController`, so any logged-in user, including a patient, can:
  - call `DELETE /appointment/cancelALlAppointments`, which **wipes every appointment in the hospital**;
  - call `GET /appointment/getAll`, which returns every patient's appointments;
  - cancel or update any appointment by ID;
  - generate slots for any doctor on any date.
- **Fix:**
  - Move appointments under `/api/appointments/**`.
  - Add explicit role rules for appointments and slots.
  - Remove the bulk-delete endpoints (see 2.4).

### 2.3 IDOR: endpoints trust the ID in the URL instead of the token
- **Where:** Almost every controller. Examples:
  - `GET /api/patient/getAccount/{id}`, `PUT /api/patient/updateAccount/{id}`, `DELETE /api/patient/deleteAccount/{id}`
  - `GET/PUT/DELETE /api/patient/relative/{id}`, `POST /api/patient/relative/add` (accepts any `patientId`)
  - `/appointment/patientPendingAppointments/{userId}`, `/appointment/CancelAppointment/{id}`, `/appointment/update/{id}`
  - `PUT /api/receptionist/{id}`, `GET /api/leaves/user/{userId}`, `POST /api/leaves/apply` (accepts any `userId`)
  - `POST /appointment/NewAppointment` accepts any `ptInfoId`. The booking form even shows a free-text **"Patient Info ID (optional)"** field (`FE/AppointmentPage.js:481`).
- **Problem:** Patient A can read, edit or delete patient B's profile, relatives and appointments by changing a number. For medical data this is the most serious class of bug.
- **Fix:**
  - Take the user ID from the authenticated principal (`SecurityContextHolder` or `@AuthenticationPrincipal`), not from the path or body.
  - For resources owned by a user (appointments, relatives, leaves), load the resource and check `owner == currentUser` unless the caller is ADMIN or RECEPTIONIST.
  - `DoctorController.updateDoctor` already does this check. Apply the same pattern everywhere.
  - Remove the Patient Info ID input from the booking form.

### 2.4 Destructive "delete all" endpoints
- **Where:**
  - `AdminController` `DELETE /api/admin/deleteAll` (all users)
  - `DoctorController` `DELETE /api/doctor` (all doctors, open to the DOCTOR role)
  - `ReceptionistController` `/deleteAllAppointments` and `/receptionists`
  - `AppointmentController` `/cancelALlAppointments`
- **Problem:** A single request can wipe production data. `DELETE /api/doctor` is open to any doctor.
- **Fix:** Delete these endpoints. If a reset is needed for development, put it behind a `dev` Spring profile.

### 2.5 Leaves can be self-approved
- **Where:** `BE/controller/LeaveRequestController.java:69`: `PUT /api/leaves/{id}/status`, open to ADMIN, DOCTOR and RECEPTIONIST.
- **Problem:** A doctor can set their own leave to `APPROVED`. This path also skips the logic in `AdminService.approveLeave`, so appointments are not cancelled and patients are not notified.
- **Fix:** Restrict status changes to ADMIN, and route them through a single service method that handles both approve and reject.

### 2.6 File upload path traversal and overwrite
- **Where:** `BE/controller/DoctorController.java:131`, `PtInfoController.java:91`, `ReceptionistController.java:176`.
- **Problem:** The code saves `uploadDir + profileImage.getOriginalFilename()`.
  - A filename like `../../src/main/resources/application.properties` writes outside the upload folder.
  - Two users uploading `photo.jpg` overwrite each other's image.
  - There is no check on file type or size, so HTML or SVG uploads can be served back from `/uploads/**`.
- **Fix:**
  - Generate the filename server-side (`UUID + validated extension`).
  - Allow only `image/jpeg`, `image/png` and `image/webp`.
  - Set `spring.servlet.multipart.max-file-size`.
  - Move the duplicated upload code into one `FileStorageService`.

### 2.7 JWT secret is regenerated on every restart
- **Where:** `BE/services/JWTService.java:23`: `Keys.secretKeyFor(SignatureAlgorithm.HS256)`.
- **Problem:** Every backend restart invalidates all tokens. The frontend still thinks the user is logged in, and every call fails. It also prevents running more than one backend instance.
- **Fix:**
  - Load the key from `.env` (`JWT_SECRET`, base64, at least 256 bits).
  - Upgrade jjwt from 0.11.5 to 0.12.x; the current code uses deprecated APIs.
  - Consider shortening the 10-hour expiry and adding refresh tokens.

### 2.8 OTP is weak and not enforced
- **Where:** `BE/services/SmsService.java:27`, `OTPService.java:20`, and `AuthService.register` (the verification check is commented out).
- **Problems:**
  - The server does not require OTP verification before registration. Only the frontend checks it, and that is easy to bypass.
  - OTPs never expire, even though the SMS says "valid for 5 minutes".
  - The OTP uses `java.util.Random` instead of `SecureRandom`.
  - The OTP store is a plain `HashMap`, which is not thread-safe and is lost on restart.
  - There are no attempt or send limits, so the 6-digit code can be brute-forced and `send-otp` can be abused to burn Twilio credit.
  - `OTPService` duplicates `SmsService` and prints OTPs to stdout and logs.
- **Fix:**
  - Keep one OTP service that stores `{hash(otp), expiresAt, attempts}` in a `ConcurrentHashMap`, or in Redis or a database table.
  - Allow 3 to 5 attempts and add a per-phone send cooldown.
  - Re-enable the verification check in `register`.
  - Never log OTP values.

### 2.9 Other security items
- **`.env` is not in `hospitalERP/.gitignore`.** It has not been committed yet, but a plain `git add .` would commit the DB, SMTP and Twilio secrets. Add `.env` now.
- **HTML injection in emails.** `EmailService` concatenates `patientName` into HTML without escaping. A patient named `<a href=evil>` would inject markup into emails sent to doctors. Escape the values or use Thymeleaf templates.
- **Sensitive data in logs.**
  - `AppointmentController` logs the full JWT (`"Fetching All Appointments with token :{}"`).
  - `AuthController` logs OTP values.
  - `JWTService` logs at INFO level on every claim extraction.
- **CORS is configured twice.** `@CrossOrigin` on controllers duplicates `SecurityConfig`. Keep only the global configuration and read the allowed origin from config.
- **No login throttling.** Add account lockout or rate limiting on `/api/auth/login`, for example with Bucket4j.
- **Aadhaar numbers are stored in plain text.** `patientAadharNo` is sensitive government ID. Encrypt it at rest (a JPA `AttributeConverter`) and mask it in responses.

---

## 3. 🟠 High: Functional Bugs

### Backend

| # | Where | Bug | Fix |
|---|---|---|---|
| B1 | `SecurityConfig` + `JWTAuthenticationFilter` | The filter **skips JWT parsing** for `/api/doctor/getAll*`, but security then requires ADMIN (for `getAll`) or DOCTOR/ADMIN (for `getAllBySpecialization`). No authentication is ever set on these requests, so they **always return 403**. | Make them `permitAll` if they are meant to be public, or stop skipping JWT parsing. Keep one source of truth for public paths. |
| B2 | `AppointmentRepository.java:19` | The native query `SELECT * FROM appointment a WHERE a.doctor = :doctorName` names the wrong table (the real one is `appointments`) and a column that does not exist (`doctor_id` holds an ID, not a name). `/appointment/appointmentsByDoctor/{name}` and the receptionist "search by doctor name" **always fail**. | Replace it with the derived query `findByDoctor_Name(String name)`, or better, look up by doctor ID. |
| B3 | `Appointment` entity | `status` (enum) and `isCompleted` (boolean) are **both** used. `approveLeave` sets `CANCELLED_BY_DOCTOR` but leaves `isCompleted=false`, so cancelled appointments still show as **pending** for the doctor and patient. `markAsCompleted` sets `isCompleted=true` but leaves `status=SCHEDULED`. | Remove `isCompleted` and derive everything from `status`. Queries become `findByDoctor_IdAndStatus(...)`. |
| B4 | `AdminService.java:101` | `sendDoctorLeaveCancelSms` is **outside** the try/catch. If Twilio fails, for example on an unverified trial number, the request fails after the leave was already saved as APPROVED. Some appointments end up cancelled, others not, and no slots are blocked. | Make the method `@Transactional`, wrap each notification in its own try/catch, and ideally send notifications asynchronously after commit (`@TransactionalEventListener` + `@Async`). |
| B5 | `AppointmentService.createAppointment` | Not `@Transactional`, and there is no lock on the slot. Two users who click the same slot at the same moment can **both book it**. | Add `@Transactional`, an `@Version` column on `Slot` (optimistic locking) or `PESSIMISTIC_WRITE`, and a unique constraint on `appointments.slot_id` for active appointments. |
| B6 | `AppointmentService.java:278` (update) | The old slot is released **before** checking whether the new slot is free. If the new slot is taken, an exception is thrown and the old slot stays released. Without a transaction, someone else can then take it. | Check the new slot first, then swap, all inside one transaction. |
| B7 | `AppointmentService.java:208` (delete) | `slot.getId()` throws a NullPointerException for appointments without a slot. Receptionist-created appointments have none (see B8). The appointment row is also **hard-deleted**, so the history is lost. | Soft-cancel instead: set `status=CANCELLED_BY_PATIENT` and release the slot if it is not null. |
| B8 | `ReceptionistService.java:95` | Receptionist booking maps the DTO directly to an entity with ModelMapper. It never sets a slot, patient or doctor and never books the slot, so the doctor may end up with no link and the slot can be double-booked. It also returns `false` on any error, which hides the cause. | Reuse `AppointmentService.createAppointment`. |
| B9 | `AdminController.getAppointmentCount` → `DoctorService.java:204` | The parameter is named `userId`, but the repository counts by **doctor ID** (`countByDoctor_Id...`). The frontend currently passes `doc.id`, so it works by accident. The name will mislead the next person who calls it. | Rename the parameter to `doctorId`, or resolve the doctor from the user ID first. Pick one ID type for the whole API. |
| B10 | `LeaveRequestService.createLeaveRequest` | The overlap check also counts **REJECTED** leaves, so a doctor cannot re-apply for dates that were rejected. There is no `startDate <= endDate` or "not in the past" validation. | Exclude REJECTED leaves from the check and add date validation. |
| B11 | `AdminService.approveLeave` | `leave.getRole()` is a free-text string sent by the client. A null value causes a NullPointerException, and `"DOCTOR"` (without the `ROLE_` prefix) silently skips cancelling the doctor's appointments. | Derive the role from `leave.getUser().getRole()` and drop the `role` column. |
| B12 | Admin | There is **no reject-leave endpoint** in `AdminController`; the admin UI can only approve. | Add `PUT /api/admin/leaves/{id}/reject` with a reason, and notify the doctor. |
| B13 | `DoctorService.updateDoctor` | `Specialist.valueOf(dto.getSpecialist())` throws a NullPointerException or `IllegalArgumentException` (HTTP 500) when the value is missing or invalid. `setUserName(...)` lets a doctor rename `userName` so it no longer matches `users.username`. | Validate the value and return 400. Make `userName` read-only. |
| B14 | `PtInfoDTO.patientAadharNo` | The DTO field is primitive `long` while the entity field is `Long`, so null becomes `0`, and saving `0` overwrites a real null. | Use `Long` in the DTO. |
| B15 | Default patient DOB | `AuthService` stores `0001-01-01` as the default date of birth, so age calculations show about 2025 years. | Leave `dob` null and ask for it on first profile completion. |
| B16 | Deleting users | `deleteUserById` deletes the `users` row, but `PtInfo` and `Receptionist` reference it through a non-cascading `@OneToOne`, which causes FK errors or orphan rows. Doctor uses `cascade = ALL` from the *wrong* side. | Use soft delete (`active=false`) for users. Hospitals must keep records anyway. |
| B17 | Exception handling | Most errors are `RuntimeException`, which returns HTTP 500 with Spring's default body, so the frontend can only show "Failed…". Some controllers return 200 with an error string (`"Error occured"`, `"you entered the wrong credentials.."`). | Add typed exceptions (`BadRequestException`, `ConflictException`, `ForbiddenException`) and map them in `GlobalExceptionHandler`. Never return 200 for an error. |
| B18 | JWT filter | An expired or tampered token makes `extractAllClaims` throw inside the filter, which returns 500 instead of 401. | Catch `JwtException` in the filter and send 401 with a JSON body. |

### Frontend

| # | Where | Bug | Fix |
|---|---|---|---|
| F1 | `FE/AppointmentPage.js:159,203` | **Booking for a relative is broken.** Relative options use `id: rel.patientId`, which is the *owner's* ID rather than the relative's. `ptInfoId` is sent as `""`, which becomes `null`, and the backend then fails on `findById(null)`. | Always send the logged-in patient's `ptInfoId` and add a `relativeId` field to `AppointmentDTO` and `Appointment`. |
| F2 | `FE/AppointmentDetails.js:82` | "Edit Appointment" navigates to `/edit-appointment`, which has no route, so the user lands on the home page. | Reuse the reschedule flow (`/appointments` with state), or add the route. |
| F3 | `FE/AppointmentDetails.js:44` | `isPastAppointment` compares against the current time, so **today's** appointments count as past (they get the "Reschedule/Delete" buttons). The labels are also reversed in spirit: past appointments get "Delete". | Compare dates only (and slot time). For past appointments show "Book again"; do not allow deleting history. |
| F4 | `FE/RelativesList.js:61` | "Edit" navigates to `/edit-relative`, which has no route. | Add an edit page, or reuse `AddRelativePage` in edit mode. |
| F5 | `FE/components/AdminSidebar.js:33` | Admin logout navigates to `/logout`, which has no route. The **token is never removed**, so the admin stays logged in. | Call a shared `logout()` helper. |
| F6 | `FE/pages/doctor/DoctorDashboard.js:41`, `ManageDoctor.js:206` | These navigate to `/unauthorized`, `/admin/doctors/absent`, `/admin/doctors/present` and `/admin/doctor/:id/appointments`, none of which have routes. | Add the pages or remove the buttons. |
| F7 | `FE/App.js` | `role` is read from localStorage **once** on mount. After a logout through `navigate()` (DoctorRightSidebar, TopNavbar), `role` stays stale until a full reload. The Footer and the protected routes keep using the old role. | Move auth into a Zustand store or React context (`token`, `role`, `userId`, `login()`, `logout()`) and read it everywhere. |
| F8 | `FE/Register.js:95` | After registering, the token is saved as `"token"` while the rest of the app reads `"jwtToken"`. The code then sends the user to `/login` anyway. | Either log the user straight in (save it as `jwtToken`) or do not store the token at all. |
| F9 | Routes | Patient and receptionist pages (`/appointments`, `/edit-profile`, `/relatives`, `/receptionist-appointments`) are not protected, so logged-out users see broken pages full of alerts. | Add a `<ProtectedRoute roles={[...]}>` wrapper. |
| F10 | `FE/AppointmentPage.js` | The **patient's browser** calls `POST /api/slots/generate` before every slot fetch. Slot generation is an admin or system job, not something a client should trigger. | Generate slots on the server, either with a scheduled job that creates the next N days or lazily inside `getAvailableSlots`. |
| F11 | Dates | `new Date().toISOString().split("T")[0]` gives the **UTC** date. In IST before 05:30 it returns yesterday. | Format with local time (`date-fns` `format(d, "yyyy-MM-dd")`). |
| F12 | `FE/EditProfileModal.js` | For receptionists, `appointmentUrl` is a *function* but is passed to `axios.get` as a string, and the endpoint `/api/receptionist/appointments/{id}` does not exist. | Fix the URL map. |

---

## 4. 🟡 Medium: Code Quality & Maintainability

### Backend
- **No input validation.** No DTO uses `@Valid`, `@NotBlank`, `@Email`, `@Pattern` (phone) or `@Past` (DOB). `GlobalExceptionHandler` already handles `MethodArgumentNotValidException`, but nothing triggers it. Add `spring-boot-starter-validation`.
- **Entities are returned directly.** `approveLeave` returns the `LeaveRequest` entity, which includes the whole `User` object with the **BCrypt password hash**. `SlotController` returns `Slot` entities. Always return DTOs, and add `@JsonIgnore` on `User.password` as a safety net.
- **Duplicate endpoints.**
  - `doctorPendingAppointments` and `doctorCompletedAppointments` exist in the Admin, Doctor *and* Receptionist controllers.
  - The "all doctors" and "by specialization" lookups exist in both the Doctor and Patient controllers.
  - Keep one resource-oriented API and control access with roles.
- **Inconsistent URL style.** Examples: `/NewAppointment`, `/CancelAppointment`, `/cancelALlAppointments`, `appointmentId/{myId}`, `/getAll`, `/get/{id}`. Move to REST conventions:
  - `GET /api/appointments`
  - `POST /api/appointments`
  - `PATCH /api/appointments/{id}/cancel`
  - `GET /api/doctors?specialization=CARDIOLOGY`
- **Mixed IDs.** Some endpoints take `userId`, others `doctorId`, `patientId` or `receptionistId`. This already caused B9 and F1. Use the entity's own ID in resource URLs and `/me` endpoints for the current user, for example `GET /api/patients/me`.
- **Duplicated data.** `Doctor`, `PtInfo` and `Receptionist` each copy `email`, `userName`, `phone` and `role` from `User`, and the copies drift when profiles are edited. `Doctor.password` is unused. Keep identity fields only on `User`.
- **Field injection.** `@Autowired` is used on fields everywhere. Use constructor injection with `final` fields; it is easier to test.
- **`System.out.println`** in `SlotService` and `OTPService`. Use the logger.
- **Configuration.**
  - `ddl-auto=update` and `show-sql=true` should not be used in production. Add Flyway or Liquibase migrations and `application-dev.properties` / `application-prod.properties` profiles.
  - Shift timings are hard-coded (`9–12`, `15–19`) and do not match `PROJECT_OVERVIEW.md` (which says 9–1 and 2–6). Move per-doctor schedules into the database.
  - `Twilio.init` runs at startup even when credentials are missing. Make SMS optional with a `@ConditionalOnProperty` and a no-op fallback, so local development works without Twilio.
- **Dead code.** Commented-out endpoints in `AdminController`, `AuthController` and `AppointmentController`, the unused `OTPService`, `registerDoctor` / `addNewDoctor` (which create a doctor with no user), the unused `convertToEntity` in `AdminService`, and Hinglish comments. Remove or translate them.
- **Package name.** `ITmonteur.example.hospitalERP` breaks Java naming conventions (package names should be lowercase). Consider `com.itmonteur.hospitalerp`.

### Frontend
- **The API base URL is hard-coded 64 times** (`http://localhost:8080`). Create `FE/api/client.js` with an Axios instance:
  - `baseURL` from `process.env.REACT_APP_API_URL`;
  - a request interceptor that attaches the Bearer token;
  - a response interceptor that logs the user out on a 401.
- **The token is re-read from localStorage in about 20 places**, and `getUserIdFromToken` and `getRoleFromToken` duplicate the decoding logic. Replace this with a single auth store (see F7).
- **`fetch` and `axios` are mixed.** Pick Axios and put all calls in service modules (`api/appointments.js`, `api/doctors.js`, ...).
- **Duplicate components.**
  - `Navbar.js` and `components/Navbar.js`, `TopNavbar.js` and `components/TopNavbar.js`, and `DoctorProfile.js` and `pages/doctor/DoctorProfile.js` are near-copies.
  - `EditProfileModal.js` exports a component called `ProfilePage`, while another `ProfilePage.js` also exists.
  - Consolidate these and organise the code by feature (`features/appointments`, `features/auth`, ...).
- **`alert()` is used for all feedback.** Use a toast library (`react-hot-toast` or `sonner`) and show inline form errors.
- **No loading or error states** in many lists, and no empty-state design.
- **Very long files.** `AppointmentPage.js` (531 lines) and `EditProfileModal.js` (474 lines). Split them into smaller components and hooks (`useSlots`, `useDoctors`).
- **Committed junk.**
  - `hospital-frontend/.idea/` is committed.
  - `hospitalERP/uploads/profileImages/1.jpeg` is committed; user uploads should never be in git.
  - Unused public images (`resumepic.jpg`, `download.jpeg`, three `doctor_black_theam*.jpg` variants).
  - Add these to `.gitignore` and remove them.
- **Accessibility.** Slot buttons, icon-only buttons and the dark-mode toggle have no `aria-label`. Several inputs use a placeholder instead of a `<label>`.

---

## 5. 🟢 Low: Testing, Tooling, DevOps

- **Tests.** The only backend test is the default `contextLoads`. The frontend `App.test.js` is the CRA default and will fail because it looks for a "learn react" link. Start with:
  - service unit tests for booking, cancelling, rescheduling and leave approval (JUnit 5 + Mockito);
  - `@WebMvcTest` security tests: "patient cannot access admin", "patient A cannot read patient B";
  - `@DataJpaTest` with Testcontainers MySQL for the repositories;
  - React Testing Library tests for the Login, Register and Booking forms.
- **CI.** Add a GitHub Actions workflow that runs `mvnw verify` and `npm test -- --watchAll=false && npm run build` on each PR.
- **API docs.** Add `springdoc-openapi-starter-webmvc-ui` for Swagger UI at `/swagger-ui.html`, which replaces the hand-written endpoint tables.
- **Docker.** Add a `docker-compose.yml` with MySQL, backend and frontend so a new developer can start the whole stack with one command.
- **Create React App is deprecated.** Plan a migration to Vite: faster builds, and `import.meta.env` for configuration.
- **Linting and formatting.** Add ESLint + Prettier on the frontend and Spotless or Checkstyle on the backend.
- **Actuator.** It is already included, so expose `/actuator/health` only and secure the rest.

---

## 6. Suggested Fix Order

1. **Security first (about 2 to 3 days):**
   - 2.1 (register role), 2.2 (security rules), 2.4 (remove delete-all), 2.5 (self-approve), 2.7 (JWT secret), 2.9 (`.env` in `.gitignore`).
2. **IDOR sweep:**
   - 2.3: add a `CurrentUser` helper and ownership checks in each service.
3. **Data correctness:**
   - B3 (single status field), B4–B7 (transactions and slot locking), B2 (broken query).
4. **Frontend plumbing:**
   - An Axios client and auth store, which fix F5, F7, F8 and F9 together and remove the 64 hard-coded URLs.
5. **Broken flows:** F1–F4, F6, F10.
6. **Validation, exceptions and tests** (sections 4 and 5).

---

## 7. New Feature Ideas

Grouped by module and roughly ordered by value against effort. ⭐ marks ideas that build directly on what already exists.

### 7.1 Patient Experience
- ⭐ **Medical records / visit history.** When a doctor marks an appointment completed, capture a **consultation note**: diagnosis, symptoms, vitals and follow-up date. The patient can see their full history. This turns the app from a booking tool into an EMR.
- ⭐ **E-prescriptions.** The doctor adds medicines (name, dose, frequency, duration) to the consultation. The patient can download a **PDF prescription** with the hospital letterhead (OpenPDF or iText).
- **Lab reports upload and view.** Upload PDFs or images per visit, stored with a generated filename and served through an authenticated endpoint, not the public `/uploads`.
- ⭐ **Appointment reminders.** A scheduled job (`@Scheduled`) sends SMS or email 24 hours and 1 hour before each appointment. The Twilio and SMTP services already exist.
- **Proper self-service reschedule and cancel**, with a cut-off policy (for example, no cancellations less than 2 hours before) and automatic slot release.
- **Waitlist.** If no slots are free, the patient joins a waitlist and is notified automatically when a slot is released.
- **Doctor ratings and feedback** after completed appointments, shown on `DoctorPage` and `DoctorProfile`.
- **Family health dashboard.** Relatives already exist; show each relative's appointments and records under the main account.
- **Forgot password / reset via OTP or email link.** There is currently no way to recover an account.

### 7.2 Doctor Portal
- ⭐ **Per-doctor schedule management.** Doctors set working days, shift times and slot length, and block individual slots. This replaces the hard-coded 9–12 and 15–19 slots.
- **Today's queue view** with patient check-in status (Waiting → In consultation → Done), updated live over WebSocket or SSE.
- **Patient history panel.** Opening an appointment shows the patient's past visits, prescriptions and reports.
- **Leave calendar view**, with a visual calendar of approved and pending leaves and warnings about conflicting appointments before applying.
- ⭐ **Analytics.** Patients seen per day or week, no-show rate and average consultation time (Recharts is already installed).

### 7.3 Receptionist / Front Desk
- ⭐ **Walk-in registration and booking.** Create a patient on the spot (phone number and name) and book the next free slot.
- **Check-in and token numbers.** Issue a queue token when the patient arrives and show it on a waiting-room display page.
- **Doctor availability board.** Show who is in, on leave, or fully booked today.
- **Search patients** by phone, name or ID, with pagination.

### 7.4 Admin
- ⭐ **Real dashboard KPIs.** Appointments per day, revenue (once billing exists), top specializations, doctor utilisation and cancellation rate.
- **Department management.** Replace the `Specialist` enum with a `Department` table so admins can add departments without a code change.
- **Audit log.** Record who viewed or changed which patient record and when. This is essential for medical data compliance (HIPAA, or India's DISHA / DPDP Act 2023).
- **Leave approval with reject and reason**, plus a bulk approve option.
- **User activation and deactivation** instead of hard delete.
- **Configurable settings**: hospital name, logo, working hours and notification templates.

### 7.5 ERP Modules (from the project vision)
- **Billing and payments**
  - An invoice for each appointment (consultation fee per doctor), plus lab and pharmacy line items.
  - Online payment through Razorpay or Stripe, and a PDF receipt.
  - Refunds when the doctor cancels because of leave.
- **Pharmacy and inventory**
  - A medicine master, stock levels, batch and expiry tracking, and low-stock alerts.
  - Prescriptions from 7.1 feed straight into pharmacy dispensing.
- **In-patient (IPD) management**
  - Wards, beds and room allocation, admission and discharge, a discharge summary PDF, and a bed occupancy dashboard.
- **Laboratory management**
  - Test catalogue, sample collection, result entry, and a report that is automatically attached to the patient record.
- **HR and payroll**
  - Staff master (nurses, lab technicians, etc.), attendance, shift rosters and payroll.
  - The existing leave module extends naturally to all staff.
- **Supply chain**
  - Vendors, purchase orders and goods received, linked to pharmacy and equipment inventory.

### 7.6 Platform and Technical Features
- **Notifications centre.** In-app notifications (bell icon) alongside SMS and email, stored in a `notifications` table.
- **Asynchronous messaging.** Use Spring events, RabbitMQ or Kafka for email and SMS so that booking never fails because Twilio is slow.
- **Pagination and filtering** on all list endpoints (`Pageable`), since appointment and user lists will grow.
- **Caching.** Cache the doctor list and specializations with Spring Cache, and later Redis.
- **Internationalisation.** Offer Hindi alongside English with `react-i18next`.
- **PWA or mobile app.** CRA already ships a manifest; make it an installable PWA, or build a React Native app later.
- **Telemedicine.** Video consultations through Jitsi or Daily.co, with a meeting link created at booking time.
- **AI assistant (optional).** A symptom-based "which department should I book?" helper on the home page, or automatic summaries of past visit notes for doctors.

---

## 8. Quick Wins (each under an hour)

- [ ] Add `.env` to `hospitalERP/.gitignore`.
- [ ] Force `PATIENT` on public registration.
- [ ] Remove the bulk "delete all" endpoints.
- [ ] Load the JWT secret from `.env`.
- [ ] Add `@JsonIgnore` to `User.password`.
- [ ] Remove the "Patient Info ID" input from the booking form.
- [ ] Fix the admin logout button.
- [ ] Fix the `"token"` → `"jwtToken"` key mismatch in Register.
- [ ] Stop logging JWTs and OTPs.
- [ ] Replace `findAppointmentsByDoctor` with a derived query.
- [ ] Delete `.idea/`, the committed uploads and the unused images from git.
- [ ] Fix the slot timings in `PROJECT_OVERVIEW.md` so they match the code.

---

## 9. Fix Status (updated 2026-09-24, branch `fix/code-review-issues`)

✅ = fixed · 🟨 = partly fixed · ⬜ = still open

### Critical (section 2)
| # | Item | Status | What changed |
|---|---|---|---|
| 2.1 | Register as ADMIN | ✅ | `/api/auth/register` always creates a PATIENT. Staff are created via `POST /api/admin/users`. The first admin comes from `ADMIN_USERNAME` / `ADMIN_PASSWORD` (`AdminBootstrap`). |
| 2.2 | `/appointment/**`, `/api/slots/**` open to any user | ✅ | Explicit role rules in `SecurityConfig`; JSON 401/403 responses; `@EnableMethodSecurity`. |
| 2.3 | IDOR | ✅ | New `CurrentUserService`. Every patient / relative / appointment / leave / receptionist / doctor endpoint checks ownership. Patients can no longer choose `ptInfoId`. |
| 2.4 | "Delete all" endpoints | ✅ | Removed. |
| 2.5 | Self-approved leave | ✅ | `PUT /api/leaves/{id}/status` is admin-only and uses the same logic as approve/reject. |
| 2.6 | Upload path traversal | ✅ | `FileStorageService`: UUID file names; JPEG/PNG/WEBP only (content type and magic bytes checked); 2 MB limit. |
| 2.7 | JWT secret per restart | ✅ | `JWT_SECRET` from `.env` (logs a warning if missing); jjwt upgraded to 0.12.6. |
| 2.8 | Weak OTP | ✅ | `OtpService`: `SecureRandom`, stored as a hash, 5-minute expiry, 5 attempts, 60 s resend cooldown. Enforced on register (`OTP_REQUIRED`). OTPs are not logged. Duplicate `OTPService` removed. |
| 2.9 | Other security items | 🟨 | ✅ `.env` and `uploads/` git-ignored; `.idea/` and committed uploads untracked. ✅ Email HTML escaped. ✅ JWT/OTP no longer logged. ✅ One CORS config (`CORS_ALLOWED_ORIGINS`). ✅ Login throttling (5 failures → 15 min lock). ⬜ Aadhaar encryption/masking (needs a data migration). |

### Backend bugs (section 3)
| # | Status | Notes |
|---|---|---|
| B1 | ✅ | Public doctor endpoints are `permitAll`; the JWT filter no longer skips paths. |
| B2 | ✅ | Broken native query replaced by `findByDoctor_Name`. |
| B3 | ✅ | `status` is the source of truth; `isCompleted` is kept in sync. Queries also classify legacy rows correctly, so no migration is needed. |
| B4 | ✅ | Leave approval is `@Transactional`; notifications are async (`NotificationService`) and never break the request. |
| B5 | ✅ | Booking is `@Transactional` with a `SELECT … FOR UPDATE` slot lock. |
| B6 | ✅ | The new slot is booked before the old one is released. |
| B7 | ✅ | Cancel is a soft cancel (status `CANCELLED_BY_PATIENT`); slot released; null-safe. |
| B8 | ✅ | Receptionist booking reuses `AppointmentService`. |
| B9 | ✅ | Parameter renamed to `doctorId` and documented. |
| B10 | ✅ | Rejected leaves are ignored in the overlap check; date validation added. |
| B11 | ✅ | Leave role is derived from the user. |
| B12 | ✅ | `PUT /api/admin/reject/{id}` and `GET /api/admin/allRejected`; Reject button in the admin UI. |
| B13 | ✅ | Specialization validated (400); username read-only; login email/phone kept in sync with the profile. |
| B14 | ✅ | `PtInfoDTO.patientAadharNo` is a `Long`. |
| B15 | ✅ | New patients get `dob = null` instead of `0001-01-01`. |
| B16 | 🟨 | `UserAccountService` deletes dependent rows in FK-safe order and notifies patients of cancelled future appointments. ⬜ Real soft delete still recommended. |
| B17 | ✅ | Typed exceptions → 400/403/404/409/429; no more 200-with-error-string responses. |
| B18 | ✅ | Invalid or expired tokens → 401. |

### Frontend bugs (section 3)
| # | Status | Notes |
|---|---|---|
| F1 | ✅ | Relatives are booked with `relativeId` (the backend checks the relative belongs to the patient); "Patient Info ID" input removed. |
| F2 | ✅ | "Edit" became "Reschedule", which now **updates** the appointment. Previously it created a second booking and never released the old slot (a bug found while fixing this). |
| F3 | ✅ | Date-only comparison; status badges; history is "Book again" instead of delete. |
| F4 | ✅ | `/edit-relative` route; `AddRelativePage` has an edit mode (also fixed the submit button navigating away before saving). |
| F5 | ✅ | Admin logout works. |
| F6 | 🟨 | `/unauthorized` → login; new admin page `/admin/doctor/:userId/appointments`; present/absent computed from today's approved leaves. ⬜ The sidebar links Receptionists / Patients / Departments still have no pages (they now fall back to the dashboard). |
| F7 | ✅ | `useAuthStore` (Zustand) is the single auth source; App, guards and logout all use it. |
| F8 | ✅ | After registering, the user is logged in directly. |
| F9 | ✅ | `ProtectedRoute` on all patient / receptionist / admin / doctor routes. |
| F10 | ✅ | Slots are generated server-side on first request; `/api/slots/generate` is admin-only. |
| F11 | ✅ | `toLocalISODate` helper. |
| F12 | ✅ | Receptionist URLs fixed; the profile save no longer sends the image File inside the JSON (which caused a 400). |

### Quality, tooling (sections 4–5)
- ✅ Bean Validation on auth, leave and relative DTOs (`spring-boot-starter-validation`).
- ✅ The `LeaveRequest` entity (with the password hash) is no longer returned; `User.password` is `@JsonIgnore`.
- ✅ All 64 hard-coded API URLs replaced by `API_BASE_URL` (`REACT_APP_API_URL`); a global axios interceptor adds the token and handles 401.
- ✅ Twilio and SMTP are optional. Dead code removed (`OTPService`, `addNewDoctor`, commented-out endpoints). New services use constructor injection.
- ✅ Tests:
  - Backend: 34 tests (security rules, services, repository JPQL on H2, and a full-app H2 test). `./mvnw test` needs no MySQL.
  - Frontend: 9 tests.
  - The broken CRA default test was removed.
- ⬜ Still open:
  - Flyway migrations and dev/prod profiles.
  - Per-doctor schedules in the database.
  - REST URL cleanup and merging duplicate endpoints (kept for frontend compatibility).
  - Package rename.
  - Duplicate React components and very long files.
  - Toasts instead of `alert()`.
  - Accessibility.
  - Swagger, CI, Docker, Vite, ESLint/Prettier.

### Needs manual verification
All of this was verified with automated tests and a production frontend build, **not** against a real MySQL database and not by clicking through the UI. Before merging:
- Start against your existing database. Hibernate adds the nullable column `appointments.relative_id` automatically.
- Click through: book, reschedule, cancel, book for a relative, approve/reject leave, profile update with image, admin create user.
