import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import { getRoleFromToken } from "./utils/jwtUtils.js";

export default function DoctorPage() {
  const [doctors, setDoctors] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [role, setRole] = useState("");
  const navigate = useNavigate();

  // ✅ Fetch data on mount
  useEffect(() => {
    const token = localStorage.getItem("jwtToken");

    if (token) {
      const extractedRole = getRoleFromToken(token);
      setRole(extractedRole);
      fetchAllDoctors(token, extractedRole);
    } else {
      // No login → still fetch doctors (public)
      fetchAllDoctors(null, null);
    }
  }, []);

  // ✅ Fetch all doctors (public or secure)
  const fetchAllDoctors = async (token, userRole) => {
    try {
      setLoading(true);
      const headers = token ? { Authorization: `Bearer ${token}` } : {};

      // Allow public access for all users
      const response = await axios.get(
        "http://localhost:8080/api/patient/getAllDoctors",
        { headers }
      );

      setDoctors(response.data);
      setError("");
    } catch (err) {
      console.error("Error fetching doctors:", err);
      setError("Failed to fetch doctors. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  // ✅ Search doctors by specialization (public + logged-in)
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
      console.error("Error searching doctors:", err);
      setError("No doctors found for that specialization.");
    } finally {
      setLoading(false);
    }
  };

  // ✅ Navigate to Appointment Page
  const handleBookAppointment = (doctorName) => {
    const token = localStorage.getItem("jwtToken");
    if (!token) {
      alert("Please login to book an appointment.");
      navigate("/login");
      return;
    }
    navigate("/appointments", { state: { doctorName } });
  };

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col">
      <TopNavbar />
      <Navbar />

      {/* Header */}
      <div className="text-center mt-10">
        <h2 className="text-4xl md:text-5xl font-extrabold mb-6 drop-shadow-lg bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
          Our Doctors
        </h2>
        <p className="text-gray-600 text-lg max-w-2xl mx-auto mb-8">
          Meet our experienced team of specialized doctors providing
          world-class healthcare and compassionate care.
        </p>
      </div>

      {/* Search Section */}
      <div className="max-w-3xl mx-auto mb-10 text-center">
        <form onSubmit={handleSearch} className="flex gap-2 justify-center items-center">
          <select
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-2/3 p-3 border border-gray-300 rounded-lg focus:ring-[#1E63DB] focus:border-[#1E63DB]"
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
            className="bg-gradient-to-r from-[#1E63DB] to-[#27496d] text-white px-6 py-3 rounded-lg hover:opacity-90 transition"
          >
            Search
          </button>
        </form>
      </div>

      {/* Loading or Error */}
      {loading && (
        <div className="text-center text-[#1E63DB] font-semibold">
          Loading doctors...
        </div>
      )}
      {error && (
        <div className="text-center text-red-500 font-semibold">{error}</div>
      )}

      {/* Doctor Cards */}
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8 px-8 pb-16 max-w-6xl mx-auto">
        {doctors.map((doctor) => (
          <div
            key={doctor.id}
            className="bg-white shadow-md rounded-2xl p-6 border border-blue-100 hover:shadow-2xl transition hover:-translate-y-1"
          >
            <div className="text-center">
              <img
                src="https://cdn-icons-png.flaticon.com/512/387/387561.png"
                alt="Doctor"
                className="h-24 w-24 mx-auto rounded-full mb-4"
              />
              <h3 className="text-xl font-bold text-[#1E63DB]">
                {doctor.name}
              </h3>
              <p className="text-gray-600 mt-1">
                Specialization: <span className="font-semibold">{doctor.specialist}</span>
              </p>
              <p className="text-gray-600 mt-1">Email: {doctor.email}</p>
              <p className="text-gray-600 mt-1">Phone: {doctor.phoneNumber}</p>

              <button
                onClick={() => handleBookAppointment(doctor.name)}
                className="mt-4 bg-gradient-to-r from-[#1E63DB] to-[#27496d] text-white px-4 py-2 rounded-lg hover:opacity-90 transition"
              >
                Book Appointment
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Footer */}
      <footer className="bg-gradient-to-r from-[#1E63DB] to-[#27496d] text-white py-4 text-center">
        <p className="text-sm">
          © {new Date().getFullYear()} Shreya Hospital. All Rights Reserved.
        </p>
      </footer>
    </div>
  );
}