import React, { useEffect, useState } from "react";
import axios from "axios";
import { Link, useNavigate } from "react-router-dom";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import { getRoleFromToken } from "./utils/jwtUtils";

export default function DoctorPage() {
  const [doctors, setDoctors] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [role, setRole] = useState("");
  const navigate = useNavigate();


  // ✅ Fetch data on mount
  useEffect(() => {
    let isMounted = true;
    const token = localStorage.getItem("jwtToken");

    if (!token) {
      alert("Please login first to access this page.");
      navigate("/login");
      return;
    }

    const extractedRole = getRoleFromToken(token);
    setRole(extractedRole);

    if (isMounted && extractedRole) fetchAllDoctors(token, extractedRole);

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  // ✅ Fetch all doctors
  const fetchAllDoctors = async (token, userRole) => {
    try {
      setLoading(true);
      const baseURL =
        userRole === "ROLE_DOCTOR"
          ? "http://localhost:8080/api/doctor/getAllDoctors"
          : "http://localhost:8080/api/patient/getAllDoctors";

      const response = await axios.get(baseURL, {
        headers: { Authorization: `Bearer ${token}` },
      });

      setDoctors(response.data);
      setError("");
    } catch (err) {
      console.error("Error fetching doctors:", err);
      if (err.response?.status === 401 || err.response?.status === 403) {
        alert("Session expired. Please login again.");
        localStorage.removeItem("jwtToken");
        navigate("/login");
        return;
      }
      setError("Failed to fetch doctors. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  // ✅ Search doctors by specialization
  const handleSearch = async (e) => {
    e.preventDefault();

    const token = localStorage.getItem("jwtToken");
    if (!token) {
      alert("Please login first to access this page.");
      navigate("/login");
      return;
    }

    if (!searchTerm.trim()) {
      fetchAllDoctors(token, role);
      return;
    }

    try {
      setLoading(true);
      const baseURL =
        role === "ROLE_DOCTOR"
          ? `http://localhost:8080/api/doctor/getAllBySpecialization?specialization=${searchTerm}`
          : `http://localhost:8080/api/patient/getAllBySpecialization?specialization=${searchTerm}`;

      const response = await axios.get(baseURL, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setDoctors(response.data);
      setError("");
    } catch (err) {
      console.error("Error searching doctors:", err);
      if (err.response?.status === 401 || err.response?.status === 403) {
        alert("Session expired. Please login again.");
        localStorage.removeItem("jwtToken");
        navigate("/login");
        return;
      }
      setError("No doctors found for that specialization.");
    } finally {
      setLoading(false);
    }
  };

  // ✅ Navigate to Appointment Page
  const handleBookAppointment = (doctorName) => {
    navigate("/appointments", { state: { doctorName } });
  };

  // ✅ UI Rendering
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
        <form
          onSubmit={handleSearch}
          className="flex gap-2 justify-center items-center"
        >
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
      <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8 px-8 pb-16 max-w-6xl mx-auto ">
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
                Specialization:{" "}
                <span className="font-semibold">{doctor.specialist}</span>
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
