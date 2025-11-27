import React from "react";
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

function App() {
  return (
    <Router>

      {/* GLOBAL WRAPPER FOR LIGHT + DARK MODE */}
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
        </Routes>

        <Footer />
      </div>
    </Router>
  );
}

export default App;
