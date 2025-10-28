import React, { useState } from "react";
import axios from "axios";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";

export default function AppointmentPage() {
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
      <TopNavbar />
      <Navbar />

      <div className="flex justify-center items-center py-12 px-4 relative">
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
