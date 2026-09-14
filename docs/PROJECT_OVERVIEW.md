# HospitalERP — Project Overview

This document describes the **project goal**, what has been **completed so far**, and what still **needs to be done**.

---

## 1. Project Goal

HospitalERP is an integrated **Hospital Management / ERP platform** designed to centralize healthcare facility operations in a single system.

### Long-term vision

The full ERP vision (from the backend project description) includes:

| Module | Purpose |
|--------|---------|
| **Patient Management** | Profiles, relatives, medical history, appointments |
| **Appointment System** | Doctor slots, booking, scheduling, status tracking |
| **Doctor & Staff Management** | Profiles, specializations, leave requests |
| **Receptionist Operations** | Front-desk appointment handling |
| **Admin Panel** | User management, approvals, hospital oversight |
| **Financial Accounting** | Billing, payments, revenue tracking *(planned)* |
| **Human Resources** | Staff management, payroll, attendance *(planned)* |
| **Inventory & Supply Chain** | Pharmacy, equipment, stock management *(planned)* |

### Current focus

Right now, the project is primarily a **multi-role hospital appointment and user management system**. The core booking workflow is in place; the broader ERP modules (billing, inventory, HR, supply chain) are not started yet.

### User roles

The system supports four roles:

| Role | Description |
|------|-------------|
| **ADMIN** | Manages users, doctors, leave approvals, and hospital operations |
| **DOCTOR** | Views appointments, manages profile, applies for leave |
| **PATIENT** | Books appointments, manages profile and relatives |
| **RECEPTIONIST** | Manages appointments on behalf of the hospital |

---

## 2. Tech Stack

| Layer | Technology |
|-------|------------|
| **Backend** | Java 17, Spring Boot 3.5, Spring Security, Spring Data JPA |
| **Database** | MySQL |
| **Authentication** | JWT (JSON Web Tokens) |
| **Frontend** | React 19, React Router, Tailwind CSS, Axios |
| **SMS / OTP** | Twilio |
| **Email** | Spring Mail (SMTP) |
| **Charts** | Recharts (admin dashboard) |

---

## 3. Project Structure

```
HospitalERP/
├── hospitalERP/          # Spring Boot backend (API + database)
│   └── src/main/java/ITmonteur/example/hospitalERP/
│       ├── controller/   # REST API endpoints
│       ├── entities/     # JPA database models
│       ├── services/     # Business logic
│       ├── repositories/ # Data access
│       ├── dto/          # Request/response objects
│       └── configuration/ # Security, JWT, CORS, file uploads
│
├── hospital-frontend/    # React frontend
│   └── src/
│       ├── pages/admin/  # Admin portal pages
│       ├── pages/doctor/ # Doctor portal pages
│       ├── components/   # Shared UI components
│       └── utils/        # JWT helpers, utilities
│
└── docs/                 # Project documentation
```

---

## 4. What Has Been Completed

### 4.1 Backend (Spring Boot)

#### Authentication & Security
- [x] User registration (Admin, Doctor, Patient, Receptionist)
- [x] Login with JWT token generation
- [x] JWT authentication filter and role-based access control
- [x] CORS configured for `http://localhost:3000`
- [x] BCrypt password hashing
- [x] OTP send and verify via Twilio SMS
- [x] Email service configured (SMTP)

#### REST API Controllers

| Controller | Base Path | Status |
|------------|-----------|--------|
| AuthController | `/api/auth` | Done |
| DoctorController | `/api/doctor` | Done |
| PtInfoController | `/api/patient` | Done |
| PtRelativeController | `/api/patient/relative` | Done |
| AppointmentController | `/appointment` | Done |
| SlotController | `/api/slots` | Done |
| ReceptionistController | `/api/receptionist` | Done |
| AdminController | `/api/admin` | Done |
| LeaveRequestController | `/api/leaves` | Done |

#### Database Entities
- [x] User, Doctor, PtInfo (Patient), Receptionist
- [x] Appointment, Slot, Shift
- [x] PtRelative (patient relatives)
- [x] LeaveRequest
- [x] Enums: Role, Gender, Specialist, AppointmentStatus, LeaveStatus, RelationShip

#### Core Features
- [x] Doctor CRUD with specialization and profile image upload
- [x] Patient account management and profile updates
- [x] Appointment booking with doctor, date, shift, and slot selection
- [x] Slot generation (morning: 9 AM–1 PM, evening: 2 PM–6 PM)
- [x] Appointment status: SCHEDULED, COMPLETED, CANCELLED
- [x] Patient relatives (add, list, update, delete)
- [x] Leave request workflow (apply, approve/reject, list by status)
- [x] Admin user creation (patient, doctor, receptionist)
- [x] Receptionist appointment management APIs
- [x] Profile image uploads stored in `uploads/profileImages/`
- [x] Global exception handling

---

### 4.2 Frontend (React)

#### Public / Patient Pages
- [x] Home page with dark/light theme
- [x] About Us, Contact Us, Treatments
- [x] Login and Register (with OTP flow on register)
- [x] Doctor listing and doctor profile pages
- [x] Appointment booking page (slot selection, reschedule support)
- [x] Appointment details and cancellation
- [x] Add relative and relatives list
- [x] Edit profile (role-aware for all four roles)

#### Admin Portal (`/admin/*`)
- [x] Protected routes (ROLE_ADMIN only)
- [x] Admin dashboard (UI with charts — uses placeholder data)
- [x] Manage users
- [x] Register new user
- [x] Manage doctors
- [x] Leave approval

#### Doctor Portal (`/doctor/*`)
- [x] Protected routes (ROLE_DOCTOR only)
- [x] Doctor dashboard
- [x] Doctor appointments (pending/completed)
- [x] Leave management (apply and view)
- [x] Doctor profile with image upload

#### Receptionist
- [x] Receptionist appointment dashboard (`/receptionist-appointments`)
- [x] Navbar link for receptionist role

#### Shared UI
- [x] JWT-based auth bootstrap on app load
- [x] Role-based login redirect (Admin → `/admin`, Doctor → `/doctor`)
- [x] Top navbar, sidebars, loaders, dark mode toggle
- [x] Responsive layout with Tailwind CSS

---

## 5. What Still Needs to Be Done

### 5.1 High Priority — Incomplete UI / Routing

| Task | Details |
|------|---------|
| **Admin sidebar dead links** | Sidebar links to `/admin/receptionists`, `/admin/patients`, and `/admin/departments` have no matching routes or pages |
| **Receptionist portal** | No dedicated protected layout like Admin/Doctor; only a single public route |
| **Patient dashboard** | Patients redirect to home (`/`) after login — no dedicated patient portal |
| **Receptionist login redirect** | Receptionists also redirect to `/` instead of their dashboard |
| **Admin dashboard data** | Dashboard uses hardcoded placeholder stats; should fetch real data from API |

### 5.2 Medium Priority — Feature Gaps

| Task | Details |
|------|---------|
| **Phone verification on register** | OTP verification exists in UI but is commented out in backend `AuthService` |
| **Departments module** | Referenced in admin sidebar but not implemented |
| **Manage receptionists page** | Backend APIs exist; frontend page missing |
| **Manage patients page (admin)** | Backend APIs exist; frontend page missing |
| **Doctor chat feature** | Placeholder alert: "Chat feature coming soon" |
| **JWT secret persistence** | JWT secret is generated at runtime — tokens invalidate on server restart |
| **API URL centralization** | Frontend hardcodes `http://localhost:8080` in many files |

### 5.3 Low Priority — Full ERP Modules (Not Started)

| Module | Status |
|--------|--------|
| **Billing & Financial Accounting** | Not started |
| **Inventory / Pharmacy Management** | Not started |
| **Supply Chain Management** | Not started |
| **Full HR (payroll, attendance, shifts)** | Not started (leave requests only) |
| **Reports & Analytics** | Not started (admin dashboard is placeholder) |
| **Notifications (email/SMS alerts)** | Partial (OTP only) |

### 5.4 Quality & DevOps

| Task | Details |
|------|---------|
| **Unit & integration tests** | Only default Spring Boot test exists |
| **API documentation** | No Swagger/OpenAPI setup |
| **Environment config for frontend** | No `.env` for API base URL |
| **Production deployment guide** | Not documented |
| **Error handling on frontend** | Inconsistent across pages |

---

## 6. Progress Summary

```
Hospital ERP Vision
├── Patient Management        ████████░░  ~80%
├── Appointment System        ████████░░  ~80%
├── Doctor Management         ███████░░░  ~70%
├── Admin Panel               ██████░░░░  ~60%
├── Receptionist Module       ████░░░░░░  ~40%
├── Auth & Security           ████████░░  ~80%
├── Billing / Finance         ░░░░░░░░░░   0%
├── Inventory / Pharmacy      ░░░░░░░░░░   0%
├── HR (full)                 ██░░░░░░░░  ~20%
└── Supply Chain              ░░░░░░░░░░   0%
```

**Overall:** ~50–60% of the hospital appointment system is complete; ~15–20% of the full ERP vision.

---

## 7. Suggested Next Steps (Roadmap)

### Phase 1 — Finish current modules
1. Wire up missing admin pages (receptionists, patients, departments)
2. Build receptionist protected portal with layout
3. Add patient dashboard (appointments, profile, relatives in one place)
4. Connect admin dashboard to real API data
5. Fix role-based login redirects for Patient and Receptionist

### Phase 2 — Polish & stability
1. Centralize API base URL in frontend config
2. Re-enable phone OTP verification on registration
3. Persist JWT secret in environment config
4. Add Swagger API documentation
5. Write backend and frontend tests

### Phase 3 — ERP expansion
1. Billing and payment module
2. Inventory / pharmacy management
3. HR module (attendance, payroll)
4. Reports and analytics dashboard
5. Email/SMS appointment reminders

---

## 8. API Endpoints Reference

### Public (no auth required)
- `POST /api/auth/register` — Register user
- `POST /api/auth/login` — Login
- `POST /api/auth/send-otp` — Send OTP
- `POST /api/auth/verify-otp` — Verify OTP
- `GET /api/patient/getAllDoctors` — List doctors
- `GET /api/patient/getAllBySpecialization` — Doctors by specialty
- `GET /api/doctor/getDoctor/{id}` — Doctor public profile

### Protected (JWT required)
- `/api/admin/**` — Admin only
- `/api/doctor/**` — Doctor and Admin
- `/api/patient/**` — Patient and Admin
- `/api/receptionist/**` — Receptionist and Admin
- `/api/leaves/**` — Admin, Doctor, Receptionist
- `/appointment/**` — Authenticated users
- `/api/slots/**` — Authenticated users

---

*Last updated: September 2026*
