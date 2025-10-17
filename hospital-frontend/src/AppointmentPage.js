import React, { useState } from "react";
import axios from "axios";

export default function AppointmentPage() {
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
    <div className="min-h-screen bg-gray-50 p-8">
      <h2 className="text-3xl font-bold text-center text-teal-700 mb-6">
        Book Appointment
      </h2>

      <form
        onSubmit={handleSubmit}
        className="max-w-3xl mx-auto space-y-6 bg-white p-6 rounded-xl shadow"
      >
        {/* Patient Name */}
        <input
          type="text"
          name="patientName"
          value={formData.patientName}
          onChange={handleChange}
          placeholder="Patient Name"
          className="w-full p-2 border border-gray-300 rounded"
          required
        />

        {/* Gender */}
        <select
          name="gender"
          value={formData.gender}
          onChange={handleChange}
          className="w-full p-2 border border-gray-300 rounded"
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
          className="w-full p-2 border border-gray-300 rounded"
          required
        />

        {/* Doctor Name */}
        <input
          type="text"
          name="doctorName"
          value={formData.doctorName}
          onChange={handleChange}
          placeholder="Doctor Name"
          className="w-full p-2 border border-gray-300 rounded"
          required
        />

        {/* Shift */}
        <select
          name="shift"
          value={formData.shift}
          onChange={handleChange}
          className="w-full p-2 border border-gray-300 rounded"
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
          className="w-full p-2 border border-gray-300 rounded"
          required
        />

        {/* Message */}
        <textarea
          name="message"
          value={formData.message}
          onChange={handleChange}
          placeholder="Message (optional)"
          className="w-full p-2 border border-gray-300 rounded"
        />

        {/* Patient Info ID */}
        <input
          type="number"
          name="ptInfoId"
          value={formData.ptInfoId}
          onChange={handleChange}
          placeholder="Patient Info ID (optional)"
          className="w-full p-2 border border-gray-300 rounded"
        />

        <button
          type="submit"
          className="bg-teal-700 text-white font-bold px-6 py-2 rounded-lg hover:bg-teal-800 transition"
        >
          Submit
        </button>
      </form>
    </div>
  );
}
