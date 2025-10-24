import React, { useState } from "react";
import axios from "axios";
import { Link, useNavigate } from "react-router-dom";

export default function AppointmentPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    patientName: "",
    gender: "MALE", // default value
    age: "",
    doctorName: "",
    shift: "MORNING", // default value
    date: "",
    message: "",
    ptInfoId: "", // optional
  });

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    const token = localStorage.getItem("jwtToken");
    console.log("Token from localStorage:", localStorage.getItem("jwtToken"));
    if (!token) {
      alert("Please login first!");
      return;
    }

    try {
      // Convert age and ptInfoId to numbers, date to ISO format
      const payload = {
        ...formData,
        age: Number(formData.age),
        ptInfoId: formData.ptInfoId ? Number(formData.ptInfoId) : null,
        date: formData.date, // backend expects LocalDate, ISO string is fine
      };

      const response = await axios.post(
        "http://localhost:8080/appointment/NewAppointment",
        payload,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      alert(response.data);
      // Optional: reset form
      setFormData({
        patientName: "",
        gender: "MALE",
        age: "",
        doctorName: "",
        shift: "MORNING",
        date: "",
        message: "",
        ptInfoId: "",
      });
    } catch (error) {
      console.error("Error submitting form:", error);
      alert("Failed to book appointment. Please try again.");
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-100 to-gray-200 flex flex-col">
      {/* Top Info Bar */}
      <div className="bg-teal-700 text-white text-sm flex justify-between items-center px-6 py-2 sticky top-0 z-50">
        <div className="flex-1 flex justify-center gap-3 font-bold">
          <span>📧 info@shreyahospital.com</span>
          <span>📞 +91-7838737363</span>
        </div>

        <div className="flex gap-3">
          <Link
            to="/login"
            className="bg-white text-teal-700 px-4 py-1 rounded shadow font-bold hover:bg-gray-100 transition"
          >
            Login
          </Link>
          <Link
            to="/register"
            className="bg-white text-teal-700 px-4 py-1 rounded shadow font-bold hover:bg-gray-100 transition"
          >
            Register
          </Link>
        </div>
      </div>

      {/* Navbar */}
      <div className="flex justify-between items-center px-6 py-4 border-b bg-white shadow-sm sticky top-[40px] z-50">
        <img src="download.jpeg" alt="Logo" className="h-12" />
        <Link
          to="/"
          className="font-bold bg-teal-700 text-white px-4 py-2 rounded-lg shadow transition transform hover:-translate-y-1 hover:shadow-md hover:bg-teal-800"
        >
          Home
        </Link>
      </div>

      {/* Form Section */}
      <div className="flex flex-1 items-center justify-center px-4 sm:px-6 lg:px-8 mt-8 md:mt-12 lg:mt-16">
        <div className="bg-white shadow-xl rounded-2xl p-8 w-full max-w-2xl">
          <h2 className="text-3xl font-bold text-center text-teal-700 mb-6">
            Book Appointment
          </h2>

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Patient Name */}
            <input
              type="text"
              name="patientName"
              value={formData.patientName}
              onChange={handleChange}
              placeholder="Patient Name"
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
              required
            />

            {/* Gender */}
            <select
              name="gender"
              value={formData.gender}
              onChange={handleChange}
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
              required
            >
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>

            {/* Age */}
            <input
              type="number"
              name="age"
              value={formData.age}
              onChange={handleChange}
              placeholder="Age"
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
              required
            />

            {/* Doctor Name */}
            <input
              type="text"
              name="doctorName"
              value={formData.doctorName}
              onChange={handleChange}
              placeholder="Doctor Name"
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
              required
            />

            {/* Shift */}
            <select
              name="shift"
              value={formData.shift}
              onChange={handleChange}
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
              required
            >
              <option value="MORNING">Morning</option>
              <option value="AFTERNOON">Afternoon</option>
              <option value="EVENING">Evening</option>
            </select>

            {/* Date */}
            <input
              type="date"
              name="date"
              value={formData.date}
              onChange={handleChange}
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
              required
            />

            {/* Message */}
            <textarea
              name="message"
              value={formData.message}
              onChange={handleChange}
              placeholder="Message (optional)"
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
            />

            {/* Patient Info ID */}
            <input
              type="number"
              name="ptInfoId"
              value={formData.ptInfoId}
              onChange={handleChange}
              placeholder="Patient Info ID (optional)"
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
            />

            <button
              type="submit"
              className="w-full bg-teal-700 text-white font-bold py-3 rounded-lg hover:bg-teal-800 transition transform hover:-translate-y-1 shadow-md"
            >
              Submit
            </button>
          </form>
        </div>
      </div>
    </div>

  );
}
