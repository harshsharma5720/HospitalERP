import { BrowserRouter as Router, Routes, Route, Navigate } from "react-router-dom";
import React, { useEffect, useState } from "react";

/* PUBLIC / PATIENT */
import HomePage from "./HomePage";
import AppointmentPage from "./AppointmentPage";
import LoginPage from "./Login";
import RegisterPage from "./Register";
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

/* UTILS */
import { getRoleFromToken, isTokenExpired } from "./utils/jwtUtils";

function App() {
  const [role, setRole] = useState(null);
  const [loading, setLoading] = useState(true);

  // 🔐 Auth bootstrap (runs once)
  useEffect(() => {
    const token = localStorage.getItem("jwtToken");

    if (!token || isTokenExpired(token)) {
      localStorage.removeItem("jwtToken");
      setRole(null);
    } else {
      setRole(getRoleFromToken(token));
    }

    setLoading(false);
  }, []);

  // ⏳ Prevent early routing
  if (loading) {
    return <div className="text-center mt-20 text-lg">Loading...</div>;
  }

  return (
    <Router>
      <Routes>

        {/* ===================== PUBLIC ROUTES ===================== */}
        <Route path="/" element={<HomePage />} />
        <Route path="/appointments" element={<AppointmentPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/doctors" element={<DoctorPage />} />
        <Route path="/contact" element={<ContactUsPage />} />
        <Route path="/treatments" element={<Treatments />} />
        <Route path="/about" element={<AboutUs />} />
        <Route path="/edit-profile" element={<EditProfilePage />} />
        <Route path="/appointment-details" element={<AppointmentDetails />} />
        <Route path="/doctor-profile/:doctorId" element={<DoctorProfile />} />
        <Route path="/add-relative" element={<AddRelativePage />} />
        <Route path="/relatives" element={<RelativesList />} />
        <Route
          path="/receptionist-appointments"
          element={<ReceptionistAppointmentDashboard />}
        />

        {/* ===================== ADMIN ===================== */}
        <Route
          path="/admin/*"
          element={
            role === "ROLE_ADMIN"
              ? <AdminLayout />
              : <Navigate to="/login" replace />
          }
        />

        {/* ===================== DOCTOR ===================== */}
        <Route
          path="/doctor/*"
          element={
            role === "ROLE_DOCTOR"
              ? <DoctorLayout />
              : <Navigate to="/login" replace />
          }
        />

        {/* ===================== REDIRECT ROOT BASED ON ROLE ===================== */}
        <Route
          path="/redirect"
          element={
            role === "ROLE_ADMIN" ? (
              <Navigate to="/admin/dashboard" replace />
            ) : role === "ROLE_DOCTOR" ? (
              <Navigate to="/doctor/dashboard" replace />
            ) : (
              <Navigate to="/" replace />
            )
          }
        />

        {/* ===================== FALLBACK ===================== */}
        <Route path="*" element={<Navigate to="/" replace />} />

      </Routes>

      {/* Footer only for public */}
      {!role && <Footer />}

    </Router>
  );
}

export default App;
