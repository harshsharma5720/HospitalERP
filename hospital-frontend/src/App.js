import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import HomePage from "./HomePage";
import AppointmentPage from "./AppointmentPage";
import LoginPage from "./Login";
import RegisterPage from "./Register";
import Footer from "./Footer";

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/appointments" element={<AppointmentPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element = {<RegisterPage />} />
      </Routes>
      <Footer />
    </Router>
  );
}

export default App;