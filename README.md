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
```

> **Note:** Twilio and email settings are optional for basic local development. Login, appointments, and most features work without them. OTP on the register page requires valid Twilio credentials.

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

There is no default admin account. Create one via the Register page with role **ADMIN**, or insert directly via the API:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "email": "admin@hospital.com",
    "password": "admin123",
    "phoneNumber": "+919876543210",
    "role": "ADMIN"
  }'
```

Then log in at http://localhost:3000/login with those credentials.

---

## Profile Image Uploads

Profile images are stored on disk at:

```
hospitalERP/uploads/profileImages/
```

This folder is created automatically on first upload. Images are served at:

```
http://localhost:8080/uploads/profileImages/<filename>
```

---

## Common Issues & Troubleshooting

### Backend fails to start — database connection error
- Confirm MySQL is running.
- Check `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` in `hospitalERP/.env`.
- Ensure the `hospital_erp` database exists.

### Backend fails — Twilio initialization error
- If you do not need OTP, you can temporarily use placeholder Twilio values in `.env`.
- For full OTP support, sign up at [twilio.com](https://www.twilio.com/) and add real credentials.

### Frontend shows CORS errors
- Ensure the backend is running on port **8080**.
- CORS is configured for `http://localhost:3000` only — do not change the frontend port unless you update `SecurityConfig.java`.

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

### JWT token invalid after server restart
- The JWT secret is currently generated at runtime. Restarting the backend invalidates all existing tokens. Log in again after a backend restart.

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

# Run tests
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
