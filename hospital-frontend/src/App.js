import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
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
import DoctorAppointments from "./DoctorAppointments";
import LeaveManagementPage from "./LeaveManagementPage";
import AdminDashboard from "./pages/admin/AdminDashboard";
import { getRoleFromToken, getUserIdFromToken } from "./utils/jwtUtils";
import React, { useState, useEffect } from "react";
import AdminLayout from "./pages/admin/AdminLayout";
import ReceptionistAppointmentDashboard from "./ReceptionistAppointmentDashboard";

function App() {
const [role, setRole] = useState("");

useEffect(() => {
  const token = localStorage.getItem("jwtToken");
  if (token) {
    const extractedRole = getRoleFromToken(token);
    setRole(extractedRole);
  }
}, []);
  return (
    <Router>
      {role === "ROLE_ADMIN" ? (
        <Routes>
            <Route path="/admin/*" element={<AdminLayout />} />
        </Routes>
      ) : (
        <>
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
              <Route path="/doctor-appointments" element={<DoctorAppointments />} />
              <Route path="/leave-management" element={<LeaveManagementPage />} />
              <Route path="/receptionist-appointments" element={<ReceptionistAppointmentDashboard />} />
            </Routes>

            <Footer />
          </div>
        </>
      )}
    </Router>
  );

}

export default App;
