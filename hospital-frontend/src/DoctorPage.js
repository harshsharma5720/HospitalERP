import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import { getRoleFromToken } from "./utils/jwtUtils.js";
import PopupForm from "./PopupForm";
import Lottie from "lottie-react";
import doctorAnimation from "./assets/Doctor.json";
import ScrollAnimate from "./utils/ScrollAnimate";

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
      fetchAllDoctors(token);
    } else {
      fetchAllDoctors(null);
    }
  }, []);

  const fetchAllDoctors = async (token) => {
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
      setError("Failed to fetch doctors.");
    } finally {
      setLoading(false);
    }
  };
  console.log("Doctors Data:", doctors);

  const handleSearch = async (e) => {
    e.preventDefault();
    const token = localStorage.getItem("jwtToken");
    const headers = token ? { Authorization: `Bearer ${token}` } : {};

    if (!searchTerm.trim()) {
      fetchAllDoctors(token);
      return;
    }

    try {
      setLoading(true);
      const response = await axios.get(
        `http://localhost:8080/api/patient/getAllBySpecialization?specialization=${searchTerm}`,
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

  // ✅ Navigate to profile page with doctorId
  const handleViewProfile = (doctorId) => {
    console.log("Doctor ID being fetched:", doctorId);
    navigate(`/doctor-profile/${doctorId}`);
  };

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-[#0a1124] flex flex-col relative overflow-hidden">
      {/* Background Lottie */}
      <div className="absolute inset-0 opacity-30 pointer-events-none z-0">
        <Lottie animationData={doctorAnimation} loop={true} />
      </div>

      <div className="relative z-10">
        <TopNavbar />
        <Navbar />

        {/* Header */}
        <div className="text-center mt-10">
         <ScrollAnimate>
           <h1 className="text-5xl font-extrabold mb-4">
              Our{" "}
                <span
                   className="
                     bg-gradient-to-r from-blue-600 to-cyan-400
                     bg-clip-text text-transparent
                   "
                >
                   Doctors
                </span>
           </h1>
         </ScrollAnimate>


          <div className="w-40 h-1 bg-gradient-to-r from-blue-600 to-cyan-400 dark:from-[#50d4f2] dark:to-[#63e6ff] mx-auto mb-10 rounded-full"></div>

          <p className="text-gray-900 dark:text-gray-300 text-lg max-w-2xl mx-auto mb-8 mt-1">
            Meet our experienced and specialized doctors.
          </p>
        </div>

        {/* Search */}
        <div className="max-w-3xl mx-auto mb-10 text-center">
          <form
            onSubmit={handleSearch}
            className="flex gap-2 justify-center items-center"
          >
            <select
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-2/3 p-3 border border-gray-300 rounded-lg
                dark:bg-[#111a3b] dark:border-[#233565] dark:text-[#50d4f2]"
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
              className="bg-gradient-to-r from-[#1E63DB] to-[#27496d] text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition
              dark:bg-[#1E63DB] dark:hover:bg-[#27496d]"
            >
              Search
            </button>
          </form>
        </div>

        {/* Loading / Error */}
        {loading && <p className="text-center dark:text-[#50d4f2]">Loading...</p>}
        {error && <p className="text-center text-red-500">{error}</p>}

        {/* Doctor Cards */}
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-8 px-8 pb-16 max-w-6xl mx-auto">
          {doctors.map((doctor) => (
            <div
              key={doctor.id}
              className="bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF]
              dark:from-[#111a3b] dark:to-[#0f172a]
              p-6 rounded-2xl shadow-2xl hover:shadow-3xl
              transition-transform transform hover:scale-105
              dark:border dark:border-[#233565]"
            >
              <div className="text-center">
                <img
                  src="https://cdn-icons-png.flaticon.com/512/387/387561.png"
                  alt="Doctor"
                  className="h-24 w-24 mx-auto rounded-full mb-4"
                />

                <h3 className="text-lg font-bold text-[#003366] dark:text-[#50d4f2]">
                  {doctor.name}
                </h3>

                <p className="text-gray-600 dark:text-gray-300">
                  Specialist: {doctor.specialist}
                </p>

                <p className="text-gray-600 dark:text-gray-300">
                  Email: {doctor.email}
                </p>

                <p className="text-gray-600 dark:text-gray-300">
                  Phone: {doctor.phoneNumber}
                </p>

                <div className="flex justify-center gap-3 mt-4 w-full">
                  <button
                    onClick={() => handleViewProfile(doctor.id)}
                    className="flex-1 bg-gradient-to-r from-[#1E63DB] to-[#27496d]
                      text-white py-2 rounded-lg font-semibold shadow-md
                      hover:shadow-lg hover:scale-105 transition-transform duration-300"
                  >
                    View Profile
                  </button>

                  <button
                    onClick={() => handleBookAppointment(doctor.name)}
                    className="flex-1 bg-gradient-to-r from-[#007B9E] to-[#00A2B8]
                      text-white py-2 rounded-lg font-semibold shadow-md
                      hover:shadow-lg hover:scale-105 transition-transform duration-300"
                  >
                    Book Appointment
                  </button>
                </div>

              </div>
            </div>
          ))}
        </div>

        {/* Footer */}
        <footer className="bg-blue-600 dark:bg-[#111a3b] text-white py-4 text-center">
          <p>
            © {new Date().getFullYear()} Shreya Hospital. All Rights Reserved.
          </p>
        </footer>

        {/* Popup */}
        {showLoginPopup && (
          <PopupForm onClose={() => setShowLoginPopup(false)} />
        )}
      </div>
    </div>
  );
}
