import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import React from "react";

/* PUBLIC / PATIENT */
import HomePage from "./HomePage";
import AppointmentPage from "./AppointmentPage";
import LoginPage from "./Login";
import RegisterPage from "./Register";
import ForgotPasswordPage from "./ForgotPasswordPage";
import Footer from "./Footer";
import DoctorPage from "./DoctorPage";
import ContactUsPage from "./ContactUsPage";
import Treatments from "./Treatments";
import AboutUs from "./AboutUs";
import EditProfilePage from "./EditProfileModal";
import AppointmentDetails from "./AppointmentDetails";
import DoctorProfile from "./DoctorProfile";
import AddRelativePage from "./AddRelativePage";
import RelativesList from "./RelativesList";
import ReceptionistAppointmentDashboard from "./ReceptionistAppointmentDashboard";

/* ADMIN / DOCTOR */
import AdminLayout from "./pages/admin/AdminLayout";
import DoctorLayout from "./pages/doctor/DoctorLayout";

/* AUTH */
import ProtectedRoute from "./components/ProtectedRoute";
import useAuthStore, { homePathForRole } from "./Store/useAuthStore";

const PATIENT = ["ROLE_PATIENT"];

function App() {
  // Re-renders automatically on login/logout (no page reload needed)
  const role = useAuthStore((s) => s.role);

  return (
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
        <Route path="/doctor-profile/:doctorId" element={<DoctorProfile />} />

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
          element={
            <ProtectedRoute roles={["ROLE_RECEPTIONIST", "ROLE_ADMIN"]}>
              <ReceptionistAppointmentDashboard />
            </ProtectedRoute>
          }
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
      {!role && <Footer />}

    </Router>
  );
}

export default App;
