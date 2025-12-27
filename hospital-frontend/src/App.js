import { BrowserRouter as Router, Routes, Route , Navigate } from "react-router-dom";
import React, { useState, useEffect } from "react";

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

import AdminLayout from "./pages/admin/AdminLayout";
import DoctorLayout from "./pages/doctor/DoctorLayout";

import { getRoleFromToken } from "./utils/jwtUtils";

function App() {
  const [role, setRole] = useState("");
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem("jwtToken");
    if (token) {
      const extractedRole = getRoleFromToken(token);
      setRole(extractedRole);
    }
    setLoading(false);
  }, []);

  return (
    <Router>

      {/* ADMIN */}
      {role === "ROLE_ADMIN" && (
        <Routes>
          <Route path="/admin/*" element={<AdminLayout />} />
        </Routes>
      )}

      {/* DOCTOR */}
      {role === "ROLE_DOCTOR" && (
        <Routes>
          <Route path="/doctor/*" element={<DoctorLayout />} />
        </Routes>
      )}

      {/* PATIENT / PUBLIC */}
      {!role && (
        <div
          className="
            min-h-screen
            bg-[#f8fafc] text-gray-900
            dark:bg-[#0f172a] dark:text-gray-100
            transition-colors duration-300
          "
        >
          <Routes>
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
          </Routes>

          <Footer />
        </div>
      )}
      <Routes>
          {role === "ROLE_DOCTOR" && (
           <Route
              path="/"
              element={<Navigate to="/doctor/dashboard" replace />}
            />
           )}

          {role === "ROLE_ADMIN" && (
            <Route
              path="/"
              element={<Navigate to="/admin/dashboard" replace />}
            />
          )}
      </Routes>

    </Router>
  );
}

export default App;
