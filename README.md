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
├── hospitalERP/          # Spring Boot backend  → runs on http://localhost:8080
├── hospital-frontend/    # React frontend       → runs on http://localhost:3000
├── docs/
│   └── PROJECT_OVERVIEW.md
└── README.md             # This file
```

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

> Tables are created automatically by Hibernate (`spring.jpa.hibernate.ddl-auto=update`) when the backend starts.

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

# Build JAR
./mvnw clean package

# Run tests (no MySQL needed — they use an in-memory H2 database)
./mvnw test
```

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

---

## License

This project is for educational and development purposes.
