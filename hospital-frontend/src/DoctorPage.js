import React, { useEffect, useState } from "react";
import axios from "axios";
import { Link, useNavigate } from "react-router-dom";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar"; // ✅ Added

export default function DoctorPage() {
  const [doctors, setDoctors] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  // ✅ Runs once when the page loads
  useEffect(() => {
    let isMounted = true;
    console.log("✅ DoctorPage mounted");

    const token = localStorage.getItem("jwtToken");
    if (!token) {
      alert("Please login first to access this page.");
      navigate("/login");
      return;
    }

    if (isMounted) fetchAllDoctors(token);

    return () => {
      isMounted = false;
    };
  }, [navigate]);

  // ✅ Fetch all doctors
  const fetchAllDoctors = async (token) => {
    try {
      setLoading(true);
      const response = await axios.get(
        "http://localhost:8080/api/patient/getAllDoctors",
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
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
      fetchAllDoctors(token);
      return;
    }

    try {
      setLoading(true);
      const response = await axios.get(
        `http://localhost:8080/api/patient/getAllBySpecialization?specialization=${searchTerm}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
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

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col">
      {/* ✅ Reusable Top Navbar */}
      <TopNavbar />

      {/* ✅ Main Navbar */}
      <Navbar />

      {/* Main Content */}
      <div className="flex-grow">
        {/* 🔍 Search Section */}
        <div className="max-w-3xl mx-auto mt-10 text-center">
          <h2 className="text-3xl font-bold text-teal-700 mb-4">Our Doctors</h2>
          <form onSubmit={handleSearch} className="flex gap-2 justify-center items-center">
            {/* 🔽 Specialization Dropdown */}
            <select
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-2/3 p-2 border border-gray-300 rounded-lg focus:ring-teal-500 focus:border-teal-500"
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
              className="bg-teal-700 text-white px-4 py-2 rounded-lg hover:bg-teal-800 transition"
            >
              Search
            </button>
          </form>
        </div>

        {/* Loading or Error */}
        {loading && (
          <div className="text-center mt-10 text-teal-700 font-semibold">
            Loading doctors...
          </div>
        )}
        {error && (
          <div className="text-center mt-10 text-red-500 font-semibold">
            {error}
          </div>
        )}

        {/* Doctor Cards */}
        <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-6 p-8 max-w-6xl mx-auto">
          {doctors.map((doctor) => (
            <div
              key={doctor.id}
              className="bg-white shadow-md rounded-xl p-6 border border-gray-100 hover:shadow-lg transition"
            >
              <div className="text-center">
                <img
                  src="https://cdn-icons-png.flaticon.com/512/387/387561.png"
                  alt="Doctor"
                  className="h-24 w-24 mx-auto rounded-full mb-4"
                />
                <h3 className="text-xl font-bold text-teal-700">{doctor.name}</h3>
                <p className="text-gray-600 mt-1">
                  Specialization:{" "}
                  <span className="font-semibold">{doctor.specialist}</span>
                </p>
                <p className="text-gray-600 mt-1">Email: {doctor.email}</p>
                <p className="text-gray-600 mt-1">Phone: {doctor.phoneNumber}</p>
                <button
                  onClick={() => handleBookAppointment(doctor.name)}
                  className="mt-4 bg-teal-700 text-white px-4 py-2 rounded-lg hover:bg-teal-800 transition"
                >
                  Book Appointment
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Footer */}
      <footer className="bg-teal-700 text-white py-4 text-center">
        <p className="text-sm">
          © {new Date().getFullYear()} Shreya Hospital. All Rights Reserved.
        </p>
      </footer>
    </div>
  );
}
