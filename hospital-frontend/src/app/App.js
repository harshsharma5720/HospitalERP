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
import EditProfilePage from "../features/patients/EditProfileModal";
import AppointmentDetails from "../features/appointments/AppointmentDetails";
import DoctorDetailsPage from "../features/doctors/DoctorDetailsPage";
import AddRelativePage from "../features/patients/AddRelativePage";
import RelativesList from "../features/patients/RelativesList";
import FrontDeskAppointments from "../features/appointments/FrontDeskAppointments";
import QueueScreen from "../features/appointments/QueueScreen";
import BillsPage from "../features/billing/BillsPage";
import BillPage from "../features/billing/BillPage";
import WalkInBooking from "../features/appointments/WalkInBooking";
import PatientSearch from "../features/patients/PatientSearch";
import FrontDeskPatient from "../features/patients/FrontDeskPatient";
import SitePage from "../shared/SitePage";

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

// The public footer - not under the waiting-room screen, which fills a TV
function PublicFooter() {
  return useLocation().pathname === "/queue-screen" ? null : <Footer />;
}

function App() {
  // Re-renders automatically on login/logout (no page reload needed)
  const role = useAuthStore((s) => s.role);

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
            <Route path="/relatives" element={<ProtectedRoute roles={PATIENT}><RelativesList /></ProtectedRoute>} />
            <Route
              path="/edit-profile"
              element={
                <ProtectedRoute roles={["ROLE_PATIENT", "ROLE_DOCTOR", "ROLE_RECEPTIONIST"]}>
                  <EditProfilePage />
                </ProtectedRoute>
              }
            />

            {/* ===================== RECEPTIONIST ===================== */}
            <Route
              path="/receptionist-appointments"
              element={<ProtectedRoute roles={FRONT_DESK}><SitePage><FrontDeskAppointments /></SitePage></ProtectedRoute>}
            />
            <Route
              path="/walk-in"
              element={<ProtectedRoute roles={FRONT_DESK}><SitePage><WalkInBooking /></SitePage></ProtectedRoute>}
            />
            <Route
              path="/patients"
              element={<ProtectedRoute roles={FRONT_DESK}><SitePage><PatientSearch /></SitePage></ProtectedRoute>}
            />
            <Route
              path="/patients/:patientId"
              element={<ProtectedRoute roles={FRONT_DESK}><SitePage><FrontDeskPatient /></SitePage></ProtectedRoute>}
            />
            <Route
              path="/bills"
              element={<ProtectedRoute roles={FRONT_DESK}><SitePage><BillsPage /></SitePage></ProtectedRoute>}
            />
            <Route
              path="/bills/:billId"
              element={<ProtectedRoute roles={FRONT_DESK}><SitePage><BillPage /></SitePage></ProtectedRoute>}
            />
            <Route
              path="/bills/visit/:appointmentId"
              element={<ProtectedRoute roles={FRONT_DESK}><SitePage><BillPage /></SitePage></ProtectedRoute>}
            />

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
