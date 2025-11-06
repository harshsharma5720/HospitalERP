import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import { getRoleFromToken } from "./utils/jwtUtils.js";
import PopupForm from "./PopupForm";

// ✅ Import Lottie animation
import Lottie from "lottie-react";
import doctorAnimation from "./assets/Doctor.json";

export default function DoctorPage() {
  const [doctors, setDoctors] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [role, setRole] = useState("");
  const [showLoginPopup, setShowLoginPopup] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem("jwtToken");
    if (token) {
      const extractedRole = getRoleFromToken(token);
      setRole(extractedRole);
      fetchAllDoctors(token, extractedRole);
    } else {
      fetchAllDoctors(null, null);
    }
  }, []);

  const fetchAllDoctors = async (token, userRole) => {
    try {
      setLoading(true);
      const headers = token ? { Authorization: `Bearer ${token}` } : {};
      const response = await axios.get(
        "http://localhost:8080/api/patient/getAllDoctors",
        { headers }
      );
      setDoctors(response.data);
      setError("");
    } catch (err) {
      console.error("Error fetching doctors:", err);
      setError("Failed to fetch doctors.");
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = async (e) => {
    e.preventDefault();
    const token = localStorage.getItem("jwtToken");
    const headers = token ? { Authorization: `Bearer ${token}` } : {};

    if (!searchTerm.trim()) {
      fetchAllDoctors(token, role);
      return;
    }

    try {
      setLoading(true);
      const response = await axios.get(
        `http://localhost:8080/api/patient/getAllBySpecialization?specialisation=${searchTerm}`,
        { headers }
      );
      setDoctors(response.data);
      setError("");
    } catch (err) {
      setError("No doctors found.");
    } finally {
      setLoading(false);
    }
  };

  const handleBookAppointment = (doctorName) => {
    const token = localStorage.getItem("jwtToken");
    if (!token) {
      setShowLoginPopup(true);
      return;
    }
    navigate("/appointments", { state: { doctorName } });
  };

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col relative overflow-hidden">
      {/* ✅ Fullscreen Lottie Background */}
      <div className="absolute inset-0 opacity-40 pointer-events-none z-0">
        <Lottie animationData={doctorAnimation} loop={true} />
      </div>

      <div className="relative z-10">
        <TopNavbar />
        <Navbar />

        {/* Header Section */}
        <div className="text-center mt-10">
          <h2 className="text-4xl md:text-5xl font-extrabold mb-6 bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
            Our Doctors
          </h2>
          <p className="text-gray-600 text-lg max-w-2xl mx-auto mb-8">
            Meet our experienced and specialized doctors.
          </p>
        </div>

        {/* Search Section */}
        <div className="max-w-3xl mx-auto mb-10 text-center">
          <form
            onSubmit={handleSearch}
            className="flex gap-2 justify-center items-center"
          >
            <select
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-2/3 p-3 border border-gray-300 rounded-lg"
            >
              <option value="">-- Select Specialization --</option>
              <option value="Cardiology">Cardiology</option>
              <option value="Dentistry">Dentistry</option>
              <option value="Orthopedics">Orthopedics</option>
              <option value="Neurology">Neurology</option>
              <option value="Pediatrics">Pediatrics</option>
              <option value="Dermatology">Dermatology</option>
            </select>
            <button
              type="submit"
              className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition"
            >
              Search
            </button>
          </form>
        </div>

        {/* Loading / Error */}
        {loading && <p className="text-center">Loading...</p>}
        {error && <p className="text-center text-red-500">{error}</p>}

        {/* Doctor Cards */}
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8 px-8 pb-16 max-w-6xl mx-auto">
          {doctors.map((doctor) => (
            <div key={doctor.id} className="bg-white p-6 rounded-2xl shadow-md">
              <div className="text-center">
                <img
                  src="https://cdn-icons-png.flaticon.com/512/387/387561.png"
                  alt="Doctor"
                  className="h-24 w-24 mx-auto rounded-full mb-4"
                />
                <h3 className="text-lg font-bold text-blue-600">
                  {doctor.name}
                </h3>
                <p className="text-gray-500">
                  Specialist: {doctor.specialist}
                </p>
                <p className="text-gray-500">Email: {doctor.email}</p>
                <p className="text-gray-500">Phone: {doctor.phoneNumber}</p>

                <button
                  onClick={() => handleBookAppointment(doctor.name)}
                  className="mt-4 bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition"
                >
                  Book Appointment
                </button>
              </div>
            </div>
          ))}
        </div>

        {/* Footer */}
        <footer className="bg-blue-600 text-white py-4 text-center">
          <p>
            © {new Date().getFullYear()} Shreya Hospital. All Rights Reserved.
          </p>
        </footer>

        {/* Popup Form */}
        {showLoginPopup && <PopupForm onClose={() => setShowLoginPopup(false)} />}
      </div>
    </div>
  );
}
