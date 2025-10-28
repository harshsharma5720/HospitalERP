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

function App() {
  return (
    <Router>
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
      </Routes>
      <Footer />
    </Router>
  );
}

export default App;
