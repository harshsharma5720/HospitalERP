import React, { useEffect, useState } from "react";
import axios from "axios";
import { Link, useNavigate } from "react-router-dom";
import Navbar from "./Navbar";

export default function DoctorPage() {
  const [doctors, setDoctors] = useState([]);
  const [searchTerm, setSearchTerm] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const navigate = useNavigate();

  // Fetch all doctors on page load
  useEffect(() => {
    fetchAllDoctors();
  }, []);

  const fetchAllDoctors = async () => {
    try {
      setLoading(true);
      const response = await axios.get("http://localhost:8080/api/doctor/getAll");
      setDoctors(response.data);
      setError("");
    } catch (err) {
      console.error("Error fetching doctors:", err);
      setError("Failed to fetch doctors. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  // Search doctors by specialization
  const handleSearch = async (e) => {
    e.preventDefault();
    if (!searchTerm.trim()) {
      fetchAllDoctors();
      return;
    }

    try {
      setLoading(true);
      const response = await axios.get(
        `http://localhost:8080/api/doctor/getAllBySpecialization?specialisation=${searchTerm}`
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

  // Book Appointment handler
  const handleBookAppointment = (doctorName) => {
    navigate("/appointments", { state: { doctorName } });
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header Bar */}
      <div className="bg-teal-700 text-white flex justify-between items-center px-6 py-2">
        <div className="font-semibold text-sm">
          📧 info@shreyahospital.com | 📞 +91-7838737363
        </div>
        <div className="flex gap-2">
          <Link
            to="/login"
            className="bg-white text-teal-700 px-4 py-1 rounded shadow font-bold hover:bg-gray-100 transition"
          >
            Login
          </Link>
          <Link
            to="/register"
            className="bg-white text-teal-700 px-4 py-1 rounded shadow font-bold hover:bg-gray-100 transition"
          >ContactUsPage.js
            Register
          </Link>
        </div>
      </div>
      <Navbar />

      {/* Navbar */}
      <div className="flex justify-between items-center px-6 py-4 bg-white shadow">
        <img src="download.jpeg" alt="Logo" className="h-12" />
        <div className="flex gap-4">
          <Link
            to="/"
            className="font-bold bg-teal-700 text-white px-4 py-2 rounded-lg hover:bg-teal-800 transition"
          >
            Home
          </Link>
        </div>
      </div>

      {/* Search Bar */}
      <div className="max-w-3xl mx-auto mt-8 text-center">
        <h2 className="text-3xl font-bold text-teal-700 mb-4">Our Doctors</h2>
        <form onSubmit={handleSearch} className="flex gap-2 justify-center">
          <input
            type="text"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            placeholder="Search by specialization (e.g. Cardiology)"
            className="w-2/3 p-2 border border-gray-300 rounded-lg"
          />
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
                Specialization: <span className="font-semibold">{doctor.specialist}</span>
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

      {/* Footer */}
      <footer className="bg-teal-700 text-white py-4 text-center">
        <p className="text-sm">
          © {new Date().getFullYear()} Shreya Hospital. All Rights Reserved.
        </p>
      </footer>
    </div>
  );
}
