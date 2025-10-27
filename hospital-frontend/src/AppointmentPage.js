import React, { useState } from "react";
import axios from "axios";
import Navbar from "./Navbar";
import { useNavigate } from "react-router-dom";
import { User, UserPlus } from "lucide-react";

export default function AppointmentPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    patientName: "",
    gender: "MALE",
    age: "",
    doctorName: "",
    shift: "MORNING",
    date: "",
    message: "",
    ptInfoId: "",
  });

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const token = localStorage.getItem("jwtToken");
    if (!token) {
      alert("Please login first!");
      return;
    }

    try {
      const payload = {
        ...formData,
        age: Number(formData.age),
        ptInfoId: formData.ptInfoId ? Number(formData.ptInfoId) : null,
      };

      const response = await axios.post(
        "http://localhost:8080/appointment/NewAppointment",
        payload,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      alert(response.data);
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
    <div className="min-h-screen bg-gray-50 relative">
      {/* Top Info Bar (Same as About Us) */}
      <div className="bg-white shadow-md px-6 py-3 flex items-center border-b">
        {/* Left: Logo */}
        <div className="flex items-center gap-2">
          <img src="download.jpeg" alt="Hospital Logo" className="h-10 w-10" />
          <h1 className="text-xl font-bold text-teal-700">Shreya Hospital</h1>
        </div>

        {/* Center: Location + Timings */}
        <div className="flex-1 flex justify-center">
          <div className="bg-gray-100 border border-teal-200 shadow-md rounded-lg p-4 max-w-3xl text-center">
            <p className="text-sm text-gray-700">
              <span className="font-semibold text-teal-700">📍 Location:</span>{" "}
              Sahibabad, Plot No. 837, Shalimar Garden Main Rd, Block C,
              Sahibabad, Ghaziabad, Uttar Pradesh 201006
            </p>
            <p className="text-sm text-gray-700 mt-2">
              <span className="font-semibold text-teal-700">⏰ Service Timings:</span>{" "}
              24×7
            </p>
          </div>
        </div>

        {/* Right: Login + Register */}
        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate("/login")}
            className="flex items-center gap-2 px-3 py-1 rounded-lg bg-teal-700 text-white border border-teal-500 hover:bg-teal-800 transition"
          >
            <User size={20} /> Login
          </button>

          <button
            onClick={() => navigate("/register")}
            className="flex items-center gap-2 px-3 py-1 rounded-lg bg-teal-700 text-white border border-teal-500 hover:bg-teal-800 transition"
          >
            <UserPlus size={20} /> Register
          </button>
        </div>
      </div>

      {/* Navbar (Same as About Us) */}
      <Navbar />

      {/* Appointment Form Section */}
      <div className="flex justify-center items-center py-12 px-4 relative">
        {/* Background image behind the form */}
        <img
          src="Shreyahospital.jpg"
          alt="Hospital Background"
          className="absolute top-0 left-0 w-full h-full object-cover opacity-20 -z-10"
        />

        <div className="bg-white bg-opacity-90 backdrop-blur-md shadow-2xl rounded-2xl p-8 w-full max-w-2xl relative z-10">
          <h2 className="text-3xl font-bold text-center text-teal-700 mb-6">
            Book an Appointment
          </h2>

          <form onSubmit={handleSubmit} className="space-y-6">
            <input
              type="text"
              name="patientName"
              value={formData.patientName}
              onChange={handleChange}
              placeholder="Patient Name"
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
              required
            />

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

            <input
              type="number"
              name="age"
              value={formData.age}
              onChange={handleChange}
              placeholder="Age"
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
              required
            />

            <input
              type="text"
              name="doctorName"
              value={formData.doctorName}
              onChange={handleChange}
              placeholder="Doctor Name"
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
              required
            />

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

            <input
              type="date"
              name="date"
              value={formData.date}
              onChange={handleChange}
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
              required
            />

            <textarea
              name="message"
              value={formData.message}
              onChange={handleChange}
              placeholder="Message (optional)"
              className="w-full p-3 border border-gray-300 rounded focus:outline-none focus:ring-2 focus:ring-teal-600"
            />

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
