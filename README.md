# HospitalERP

An integrated **Hospital Management / ERP platform** for managing patients, doctors, appointments, and hospital operations — with plans to expand into billing, inventory, HR, and supply chain modules.

> For project goals, completed work, and roadmap, see [docs/PROJECT_OVERVIEW.md](docs/PROJECT_OVERVIEW.md).

---

## Tech Stack

| | |
|---|---|
| **Backend** | Java 17, Spring Boot 3.5, Spring Security, JWT, JPA |
| **Database** | MySQL |
| **Frontend** | React 19, React Router, Tailwind CSS, Axios |
| **Integrations** | Twilio (SMS/OTP), SMTP (Email) |

---

## Prerequisites

Install the following before running the project:

| Tool | Version | Download |
|------|---------|----------|
| **Java JDK** | 17+ | [Adoptium](https://adoptium.net/) |
| **Maven** | 3.8+ | Bundled via `mvnw` in backend |
| **Node.js** | 18+ (LTS recommended) | [nodejs.org](https://nodejs.org/) |
| **MySQL** | 8.0+ | [MySQL Community](https://dev.mysql.com/downloads/mysql/) |
| **Git** | Any recent version | [git-scm.com](https://git-scm.com/) |

Optional (for OTP/SMS during registration):
- [Twilio](https://www.twilio.com/) account with Account SID, Auth Token, and trial phone number

---

## Project Structure

```
HospitalERP/
├── hospitalERP/              # Spring Boot backend → runs on http://localhost:8080
│   ├── pom.xml               #   parent build: module list, library versions, build rules
│   ├── common/               #   one folder (Maven module) per business module ...
│   ├── notifications/
│   ├── identity/
│   ├── patients/
│   ├── staff/
│   ├── scheduling/
│   ├── appointments/
│   ├── clinical/
│   ├── administration/
│   ├── app/                  #   ... and the runnable application (main class, config, end-to-end tests)
│   └── .env                  #   your local settings (not in git, see Step 3)
├── hospital-frontend/        # React frontend → runs on http://localhost:3000
├── docs/
│   ├── PROJECT_OVERVIEW.md
│   ├── MULTI_MODULE_PLAN.md  #   module rules and refactor progress log
│   └── modules/              #   generated module diagrams
├── tools/
│   └── check_module_deps.py  #   module boundary check
└── README.md                 # This file
```

The backend modules and what each one may use are described in [Backend modules](#backend-modules).

---

## Setup Instructions

### Step 1 — Clone the repository

```bash
git clone <your-repo-url>
cd HospitalERP
```

### Step 2 — Set up MySQL database

1. Start MySQL server.
2. Create a database:

```sql
CREATE DATABASE hospital_erp;
```

3. Note your MySQL username and password — you will need them in the next step.

> Tables are created by **Flyway** migrations when the backend starts (see [Database migrations](#database-migrations-flyway)). Hibernate only checks that the tables match the code (`ddl-auto=validate`); it no longer changes them.

---

### Step 3 — Configure backend environment

Create a `.env` file inside the `hospitalERP/` folder:

```env
# Database
DB_URL=jdbc:mysql://localhost:3306/hospital_erp
DB_USERNAME=root
DB_PASSWORD=your_mysql_password

# Email (SMTP) — optional for now
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# Twilio (SMS/OTP) — optional; required for OTP on registration
TWILIO_ACCOUNT_SID=your_twilio_account_sid
TWILIO_AUTH_TOKEN=your_twilio_auth_token
TWILIO_TRIAL_NUMBER=+1234567890

# Security
# Base64 secret, at least 32 bytes. Generate one with: openssl rand -base64 48
JWT_SECRET=paste_generated_secret_here
# Set to false for local development without Twilio (skips phone verification on sign-up)
OTP_REQUIRED=true
# Comma-separated frontend origins allowed to call the API
CORS_ALLOWED_ORIGINS=http://localhost:3000

# First admin account — created at startup if it doesn't exist yet
ADMIN_USERNAME=admin
ADMIN_PASSWORD=choose-a-strong-password
ADMIN_EMAIL=admin@hospital.com

# Appointment reminders (day before, by SMS + email) — optional
REMINDERS_ENABLED=true
# Spring cron: second minute hour day month weekday (default: every hour)
REMINDERS_CRON=0 0 * * * *

# Shown on the prescription PDF letterhead — optional
HOSPITAL_NAME=Shreya Hospital
HOSPITAL_ADDRESS=12 MG Road, Pune
HOSPITAL_PHONE=+91 20 5555 1234
```

> **Note:** Twilio and email settings are optional. Without Twilio, SMS is skipped (a warning is logged) and you should set `OTP_REQUIRED=false`, otherwise nobody can sign up. Without SMTP, emails are skipped.
>
> **Note:** `.env` is git-ignored. Never commit it.

---

### Step 4 — Run the backend

Open a terminal in the `hospitalERP/` directory:

**Windows (PowerShell):**
```powershell
cd hospitalERP
.\mvnw.cmd spring-boot:run
```

**macOS / Linux:**
```bash
cd hospitalERP
./mvnw spring-boot:run
```

This builds all backend modules and starts the `app` module. Run it from `hospitalERP/`, so the `.env` file and the `uploads/` folder there are used.

Wait until you see:
```
Started HospitalErpApplication in X seconds
```

The API will be available at **http://localhost:8080**.

---

### Step 5 — Run the frontend

Open a **second terminal** in the `hospital-frontend/` directory:

```bash
cd hospital-frontend
npm install
npm start
```

The app will open at **http://localhost:3000**.

---

## Verify Installation

1. Open **http://localhost:3000** — you should see the HospitalERP home page.
2. Go to **Register** and create a patient account (or use an admin account if one exists).
3. Log in — you should receive a JWT token and be redirected based on your role:
   - **Admin** → `/admin/dashboard`
   - **Doctor** → `/doctor/dashboard`
   - **Patient / Receptionist** → home page (`/`)

---

## Default Ports

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| MySQL | localhost:3306 |

---

## User Roles & Portals

| Role | Portal URL | Access |
|------|------------|--------|
| **Admin** | `/admin/dashboard` | User management, doctors, leave approval |
| **Doctor** | `/doctor/dashboard` | Appointments, leave, profile |
| **Patient** | `/` (public pages) | Book appointments, manage relatives |
| **Receptionist** | `/receptionist-appointments` | Manage all appointments |

---

## Creating the First Admin User

Public registration (`/register`, `POST /api/auth/register`) **always creates a patient**; any `role` sent by the client is ignored.

To get the first admin, set `ADMIN_USERNAME`, `ADMIN_PASSWORD` (8+ characters) and optionally `ADMIN_EMAIL` in `hospitalERP/.env` and start the backend. The account is created once, if it doesn't exist yet. Then log in at http://localhost:3000/login.

Doctors, receptionists and other admins are created by an admin from **Admin → Register User** (`POST /api/admin/users`).

---

## Features at a Glance

| Feature | Who | Where |
|---------|-----|-------|
| Book / reschedule / cancel appointments (also for relatives) | Patient | `/appointments`, `/appointment-details` |
| Appointment reminders the day before (SMS + email) | Automatic | runs hourly (`REMINDERS_CRON`) |
| Consultation notes, vitals and e-prescription | Doctor | Appointments → **Start Consultation** |
| Medical history and prescription PDF download | Patient | `/appointment-details` (completed visits) |
| Weekly working hours and slot length | Doctor (or admin via API) | `/doctor/schedule` |
| Leave requests and approval / rejection | Doctor, Admin | `/doctor/leave-management`, `/admin/leave-approval` |
| Forgot password (6-digit code by SMS / email) | Everyone | `/forgot-password` (link on the login page) |

Medical records (consultations, prescriptions) are visible only to the patient, their doctor(s) and admins — receptionists cannot read them.

---

## Database migrations (Flyway)

The database schema is owned by the migration scripts in `hospitalERP/app/src/main/resources/db/migration/`. Flyway runs the new ones at startup and records them in the `flyway_schema_history` table.

| File | What it does |
|---|---|
| `V1__baseline.sql` | The complete schema as of September 2026. Runs only on an **empty** database. |
| `staff/V2026_09_28_1__staff_drop_unused_columns.sql` | Drops `doctor.password`, `doctor.role`, `receptionist.role` (unused; login data lives in `users`). |
| `patients/V2026_09_28_2__patients_drop_unused_columns.sql` | Drops `patient.role`, `patient_relative.role`. |

**Changing the schema:** add a new file to the owning module's folder, e.g. `db/migration/appointments/V2026_10_05_1__appointments_add_room.sql`, with the next date-based version. Never edit a migration that has already run anywhere — Flyway checks their checksums and refuses to start. Hibernate then validates the entities against the result, so an entity change without a migration stops the app with a clear "Schema-validation" message.

### Upgrading a database created before Flyway

A database created by the old `ddl-auto=update` has tables but no `flyway_schema_history`. On the first start Flyway marks it as version 1 (it does **not** run the baseline there) and then runs only the two clean-up migrations above. Data is kept. Do it once like this:

1. **Back up** the database:
   ```bash
   mysqldump -u root -p --routines --single-transaction hospital_erp > hospital_erp_before_flyway.sql
   ```
2. Start the backend as usual (`./mvnw spring-boot:run` in `hospitalERP/`). The log should show
   `Successfully baselined schema with version: 1`, then `Successfully applied 2 migrations`, then `Started HospitalErpApplication`.
3. Check the app (login, booking, consultation, PDF).

If it stops with `Schema-validation: missing column / wrong column type …`, your database differs from what the code expects (for example a column created long ago with an old type). Nothing has been lost; send the message to the team. To go back: restore the backup (`mysql -u root -p hospital_erp < hospital_erp_before_flyway.sql`) and run the previous version of the code.

The migrations are tested on a real MySQL 8 in `DatabaseMigrationMySqlTest` (an upgrade of a pre-Flyway database with data, and a fresh install giving the identical schema). It needs Docker and is skipped without it; CI runs it.

---

## Profile Image Uploads

Profile images are stored on disk at:

```
hospitalERP/uploads/profileImages/
```

This folder is created automatically on first upload (change it with `UPLOAD_DIR`). Only JPEG, PNG and WEBP images up to 2 MB are accepted, and each file gets a random server-generated name. Images are served at:

```
http://localhost:8080/uploads/profileImages/<filename>
```

---

## Common Issues & Troubleshooting

### Backend fails to start — "Schema-validation" or "Migration checksum mismatch"
- `Schema-validation: missing column …`: the database doesn't match the code. Usually a migration is missing for an entity change; see [Database migrations](#database-migrations-flyway).
- `Migration checksum mismatch`: a migration file was edited after it ran. Restore the file; put the change in a new migration.

### Backend fails to start — database connection error
- Confirm MySQL is running.
- Check `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in `hospitalERP/.env`.
- Ensure the `hospital_erp` database exists.

### Sign-up says "Phone number not verified" / OTP never arrives
- Twilio is not configured. Either add real Twilio credentials, or set `OTP_REQUIRED=false` in `.env` for local development.

### Frontend shows CORS errors
- Ensure the backend is running on port **8080**.
- Allowed origins come from `CORS_ALLOWED_ORIGINS` (default `http://localhost:3000`). Add your frontend URL there if it runs elsewhere.
- If the API is not on `http://localhost:8080`, set `REACT_APP_API_URL` in `hospital-frontend/.env`.

### Login works but redirects to home instead of admin/doctor portal
- Clear browser localStorage: open DevTools → Application → Local Storage → delete `jwtToken`.
- Log in again; the app reads the role from the JWT and redirects accordingly.

### `npm install` fails
- Use Node.js 18 or later: `node -v`
- Delete `node_modules` and retry:
  ```bash
  rm -rf node_modules package-lock.json
  npm install
  ```

### Everyone is logged out after a backend restart
- `JWT_SECRET` is not set, so a random key is generated on every start (a warning is logged). Set `JWT_SECRET` in `.env`.

### "Too many failed login attempts"
- After 5 wrong passwords a username is locked for 15 minutes. Wait, or restart the backend in development.

---

## Development Commands

### Backend

```bash
cd hospitalERP

# Run
./mvnw spring-boot:run        # macOS/Linux
.\mvnw.cmd spring-boot:run      # Windows

# Build (all modules + tests); the runnable jar is app/target/hospital-app-0.0.1-SNAPSHOT.jar
./mvnw clean package

# Run the jar (from hospitalERP/, so .env and uploads/ are found)
java -jar app/target/hospital-app-0.0.1-SNAPSHOT.jar

# Run all tests (no MySQL needed — they use an in-memory H2 database;
# DatabaseMigrationMySqlTest also starts a throw-away MySQL in Docker, and is skipped without Docker)
./mvnw test

# Test one module (and the modules it builds on), e.g. clinical
./mvnw -pl clinical -am test

# If tests fail to start with "insufficient memory", give the test JVM a smaller heap
./mvnw test -DargLine="-Xms64m -Xmx512m"
```

Every build also checks that all modules use the same version of each library (Maven Enforcer). If it fails with "Dependency convergence error", pin that library's version in `<dependencyManagement>` of `hospitalERP/pom.xml`.

### Backend modules

The backend is a modular monolith: one application and one database, split into Maven modules. A module can only use the modules listed for it; the build fails if two modules depend on each other. See [docs/MULTI_MODULE_PLAN.md](docs/MULTI_MODULE_PLAN.md) for the rules and the progress log.

| Module | Responsibility | May use |
|---|---|---|
| [`common`](hospitalERP/common/README.md) | Shared kernel: exceptions + global error handler, `ApiResponse`, `Gender`, file storage | – |
| [`notifications`](hospitalERP/notifications/README.md) | Email and SMS delivery, message templates | common |
| [`identity`](hospitalERP/identity/README.md) | Users, roles, login, JWT, OTP, password reset, first-admin bootstrap | common, notifications |
| [`patients`](hospitalERP/patients/README.md) | Patient profiles and relatives | common, identity |
| [`staff`](hospitalERP/staff/README.md) | Doctors, receptionists, leave requests | common, identity |
| [`scheduling`](hospitalERP/scheduling/README.md) | Doctor weekly schedules and slots | common, identity, staff |
| [`appointments`](hospitalERP/appointments/README.md) | Booking, reschedule, cancel, day-before reminders | common, identity, notifications, patients, staff, scheduling |
| [`clinical`](hospitalERP/clinical/README.md) | Consultations, prescriptions, prescription PDF, medical history | common, identity, patients, staff, appointments |
| [`administration`](hospitalERP/administration/README.md) | Admin use cases across modules: create users, leave decisions, account deletion | all of the above |
| `app` | Main class, `SecurityConfig`, `application.properties`, end-to-end tests | all modules |

When a lower module needs something to happen in a higher one (for example, an approved leave must cancel appointments), it publishes an event and the higher module listens.

Each module has a short README (linked in the table): its public API, the events it sends and receives, its endpoints and settings. Inside a module, `com.itmonteur.hospitalerp.<module>` is its public API (what other modules may use); its `internal` and `web` sub-packages are private to the module. Unit tests live in their module's `src/test`; tests that start the whole application live in `app`.

Run this check before committing:

```bash
python tools/check_module_deps.py            # prints "RESULT: OK - module boundaries respected"
python tools/check_module_deps.py --verbose  # also lists every module-to-module dependency
```

It fails (exit code 1) if a module uses a module it shouldn't (including one it only reaches indirectly), if two modules depend on each other, or if a module uses another module's repository. A class belongs to the module named by its package.

`ModularityTest` checks the same boundaries with Spring Modulith on every `./mvnw test`: it fails on a cycle between modules, on one module using another module's `internal`/`web` classes, and on `@Autowired` field injection of another module's beans.

Generated module documentation lives in [docs/modules/](docs/modules/): `components.puml` (how the modules depend on each other) and one `module-<name>.puml` diagram and `module-<name>.adoc` "canvas" per module (its services, aggregates, events and which other modules' beans it uses). Open the `.puml` files with a PlantUML viewer (for example the VS Code *PlantUML* extension). After changing a module, refresh them with:

```bash
./mvnw test -Dtest=ModularityTest -Dsurefire.failIfNoSpecifiedTests=false -DupdateModuleDocs=true
```

`EndpointContractTest` freezes the public API (all URLs, methods and role checks) in `app/src/test/resources/api-endpoints.txt`. After an intended API change, regenerate it with:

```bash
./mvnw test -Dtest=EndpointContractTest -Dsurefire.failIfNoSpecifiedTests=false -DupdateEndpointSnapshot=true
```

**URL access rules.** Each module declares who may call its URLs in a small `ModuleSecurityRules` bean in its `web` package (for example `StaffSecurityRules`: `/api/doctor/**` for doctors and admins, the doctor list public). `SecurityConfig` in `app` only adds the application-wide rules (error page, health check, uploaded images) and combines the modules' rules: first every module's single-endpoint rules, then every module's URL areas. `AccessRulesContractTest` freezes the result in `app/src/test/resources/api-access.txt` — for every endpoint, whether an anonymous caller, a patient, a doctor, a receptionist and an admin get through. After an intended change, regenerate it with:

```bash
./mvnw test -Dtest=AccessRulesContractTest -Dsurefire.failIfNoSpecifiedTests=false -DupdateAccessSnapshot=true
```

### Continuous integration

A GitHub Actions workflow (`.github/workflows/ci.yml`) is **prepared but not in the repository yet**: pushing a workflow file needs a GitHub token with the `workflow` permission. Once added, it runs on every pull request and every push to `main`:

- **Backend:** `python3 tools/check_module_deps.py`, then `./mvnw -B verify` in `hospitalERP/` (Enforcer rules, all module and end-to-end tests on H2, jars). Test reports are uploaded when it fails.
- **Frontend:** `npm ci`, `npm test`, `npm run build`. CI mode treats lint warnings as errors, so keep the frontend warning-free (check locally with `CI=true npm run build`).

### Frontend

```bash
cd hospital-frontend

npm start       # Development server
npm run build   # Production build
npm test        # Run tests
```

---

## Environment Variables Reference

| Variable | Required | Description |
|----------|----------|-------------|
| `DB_URL` | Yes | MySQL JDBC connection string |
| `DB_USERNAME` | Yes | MySQL username |
| `DB_PASSWORD` | Yes | MySQL password |
| `MAIL_HOST` | No | SMTP server host |
| `MAIL_PORT` | No | SMTP port (usually 587) |
| `MAIL_USERNAME` | No | SMTP email address |
| `MAIL_PASSWORD` | No | SMTP password or app password |
| `TWILIO_ACCOUNT_SID` | No* | Twilio Account SID |
| `TWILIO_AUTH_TOKEN` | No* | Twilio Auth Token |
| `TWILIO_TRIAL_NUMBER` | No* | Twilio phone number for sending SMS |

\* Required only if using OTP verification on registration.

---

## Further Reading

- [Project Overview & Roadmap](docs/PROJECT_OVERVIEW.md) — goals, completed features, and what is left to build
- [Multi-Module Plan](docs/MULTI_MODULE_PLAN.md) — module rules, phases and progress log
- [Module diagrams](docs/modules/) — generated by `ModularityTest`

---

## License

This project is for educational and development purposes.
