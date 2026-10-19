import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from "react-router-dom";
import React from "react";

/* PUBLIC / PATIENT */
import HomePage from "../features/public/HomePage";
import AppointmentPage from "../features/appointments/AppointmentPage";
import LoginPage from "../features/auth/Login";
import RegisterPage from "../features/auth/Register";
import ForgotPasswordPage from "../features/auth/ForgotPasswordPage";
import Footer from "../shared/Footer";
import DoctorPage from "../features/doctors/DoctorPage";
import ContactUsPage from "../features/public/ContactUsPage";
import Treatments from "../features/public/Treatments";
import AboutUs from "../features/public/AboutUs";
import MyAccount from "../features/patients/MyAccount";
import AppointmentDetails from "../features/appointments/AppointmentDetails";
import DoctorDetailsPage from "../features/doctors/DoctorDetailsPage";
import AddRelativePage from "../features/patients/AddRelativePage";
import ReceptionistProfile from "../features/receptionist/ReceptionistProfile";
import FrontDeskAppointments from "../features/appointments/FrontDeskAppointments";
import QueueScreen from "../features/appointments/QueueScreen";
import BillsPage from "../features/billing/BillsPage";
import CashSummaryPage from "../features/billing/CashSummaryPage";
import BillPage from "../features/billing/BillPage";
import WalkInBooking from "../features/appointments/WalkInBooking";
import PatientSearch from "../features/patients/PatientSearch";
import FrontDeskPatient from "../features/patients/FrontDeskPatient";
import StaffLayout from "../shared/layout/StaffLayout";

/* ADMIN / DOCTOR */
import AdminLayout from "../features/admin/AdminLayout";
import DoctorLayout from "../features/doctors/portal/DoctorLayout";

/* AUTH */
import ProtectedRoute from "./ProtectedRoute";
import useAuthStore, { homePathForRole } from "../features/auth/useAuthStore";

/* SHARED UI */
import { ToastProvider, ConfirmProvider } from "../shared/ui";

const PATIENT = ["ROLE_PATIENT"];
const FRONT_DESK = ["ROLE_RECEPTIONIST", "ROLE_ADMIN"];

// The public footer - not under the waiting-room screen, which fills a TV, nor under the login pages, which have
// their own frame
const WITHOUT_FOOTER = ["/queue-screen", "/login", "/register", "/forgot-password"];

function PublicFooter() {
  return WITHOUT_FOOTER.includes(useLocation().pathname) ? null : <Footer />;
}

// The logged-in user's own account: a patient's here; staff have theirs in their portal
function OwnAccount() {
  const role = useAuthStore((s) => s.role);
  if (role === "ROLE_DOCTOR") return <Navigate to="/doctor/profile" replace />;
  if (role === "ROLE_RECEPTIONIST") return <Navigate to="/my-profile" replace />;
  return <MyAccount />;
}

function App() {
  // Re-renders automatically on login/logout (no page reload needed)
  const role = useAuthStore((s) => s.role);
  // Until the login is restored (or known to be gone), routes would send a logged-in user to the login page
  const ready = useAuthStore((s) => s.ready);
  if (!ready) {
    return (
      <p role="status" className="p-8 text-ink-muted">
        Loading...
      </p>
    );
  }

  return (
    <ToastProvider>
      <ConfirmProvider>
        <Router>
          <Routes>

            {/* ===================== PUBLIC ROUTES ===================== */}
            <Route path="/" element={<HomePage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
            <Route path="/doctors" element={<DoctorPage />} />
            <Route path="/contact" element={<ContactUsPage />} />
            <Route path="/treatments" element={<Treatments />} />
            <Route path="/about" element={<AboutUs />} />
            <Route path="/doctor-profile/:doctorId" element={<DoctorDetailsPage />} />
            {/* The waiting-room screen: tokens only, no login (docs/TODAYS_QUEUE_PLAN.md) */}
            <Route path="/queue-screen" element={<QueueScreen />} />

            {/* ===================== PATIENT ===================== */}
            <Route path="/appointments" element={<ProtectedRoute roles={PATIENT}><AppointmentPage /></ProtectedRoute>} />
            <Route path="/appointment-details" element={<ProtectedRoute roles={PATIENT}><AppointmentDetails /></ProtectedRoute>} />
            <Route path="/add-relative" element={<ProtectedRoute roles={PATIENT}><AddRelativePage /></ProtectedRoute>} />
            <Route path="/edit-relative" element={<ProtectedRoute roles={PATIENT}><AddRelativePage /></ProtectedRoute>} />
            {/* Family members are on My account now */}
            <Route path="/relatives" element={<ProtectedRoute roles={PATIENT}><Navigate to="/edit-profile" replace /></ProtectedRoute>} />
            <Route
              path="/edit-profile"
              element={
                <ProtectedRoute roles={["ROLE_PATIENT", "ROLE_DOCTOR", "ROLE_RECEPTIONIST"]}>
                  <OwnAccount />
                </ProtectedRoute>
              }
            />

            {/* ===================== FRONT DESK ===================== */}
            {/* Receptionists' pages in the staff layout; admins have the same pages under /admin */}
            <Route element={<ProtectedRoute roles={FRONT_DESK}><StaffLayout /></ProtectedRoute>}>
              <Route path="/receptionist-appointments" element={<FrontDeskAppointments />} />
              <Route path="/walk-in" element={<WalkInBooking />} />
              <Route path="/patients" element={<PatientSearch />} />
              <Route path="/patients/:patientId" element={<FrontDeskPatient />} />
              <Route path="/bills" element={<BillsPage />} />
              <Route path="/bills/:billId" element={<BillPage />} />
              <Route path="/bills/visit/:appointmentId" element={<BillPage />} />
              <Route path="/cash-summary" element={<CashSummaryPage />} />
              <Route path="/my-profile" element={<ProtectedRoute roles={["ROLE_RECEPTIONIST"]}><ReceptionistProfile /></ProtectedRoute>} />
            </Route>

            {/* ===================== ADMIN ===================== */}
            <Route
              path="/admin/*"
              element={<ProtectedRoute roles={["ROLE_ADMIN"]}><AdminLayout /></ProtectedRoute>}
            />

            {/* ===================== DOCTOR ===================== */}
            <Route
              path="/doctor/*"
              element={<ProtectedRoute roles={["ROLE_DOCTOR"]}><DoctorLayout /></ProtectedRoute>}
            />

            {/* ===================== REDIRECT ROOT BASED ON ROLE ===================== */}
            <Route path="/redirect" element={<Navigate to={homePathForRole(role)} replace />} />

            {/* ===================== FALLBACK ===================== */}
            <Route path="*" element={<Navigate to="/" replace />} />

          </Routes>

          {/* Footer only for public */}
          {!role && <PublicFooter />}

        </Router>
      </ConfirmProvider>
    </ToastProvider>
  );
}

export default App;
