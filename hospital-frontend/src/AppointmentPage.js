import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

export default function AppointmentPage() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    specialist: "",
    doctor: "",
    shift: "",
    liveConsultation: "No",
    date: "",
    message: "",
  });

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    alert("Form submitted! Data: " + JSON.stringify(formData, null, 2));
  };

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top Info Bar */}
      <div className="bg-teal-700 text-white text-sm flex justify-between items-center px-6 py-2 sticky top-0 z-50">
        {/* Center Section */}
        <div className="flex-1 flex justify-center gap-3 font-bold">
          <span>📧 info@shreyahospital.com</span>
          <span>📞 +91-7838737363</span>
        </div>

        {/* Right Side Buttons */}
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

      {/* Navbar with Logo and Home button */}
      <div className="flex justify-between items-center px-6 py-4 border-b bg-white shadow-sm sticky top-[40px] z-50">
        <img src="download.jpeg" alt="Logo" className="h-12" />
        <div className="flex gap-4">
          <Link
            to="/"
            className="font-bold bg-teal-700 text-white px-4 py-2 rounded-lg shadow transition transform hover:-translate-y-1 hover:shadow-md hover:bg-teal-800"
          >
            Home
          </Link>
        </div>
      </div>

      {/* Form Section */}
      <div className="max-w-4xl mx-auto mt-8 bg-white p-6 rounded-xl shadow">
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Specialist & Doctor */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block font-bold text-gray-700">Specialist</label>
              <select
                name="specialist"
                value={formData.specialist}
                onChange={handleChange}
                className="w-full border rounded-lg px-5 py-4 mt-2"
              >
                <option value="">Select</option>
                <option value="cardiology">Cardiology</option>
                <option value="dentistry">Dentistry</option>
              </select>
            </div>
            <div>
              <label className="block font-bold text-gray-700">Doctor</label>
              <select
                name="doctor"
                value={formData.doctor}
                onChange={handleChange}
                className="w-full border rounded-lg px-5 py-4 mt-1"
              >
                <option value="">Select</option>
                <option value="drA">Dr. Akshay</option>
                <option value="drB">Dr. Harsh</option>
              </select>
            </div>
          </div>

          {/* Slot Message */}
          <div className="bg-red-100 font-bold text-red-700 p-3 rounded">
            No Slot Available
          </div>

          {/* Shift */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div>
              <label className="block font-bold text-gray-700">Shift</label>
              <select
                name="shift"
                value={formData.shift}
                onChange={handleChange}
                className="w-full border rounded-lg px-5 py-4 mt-1"
              >
                <option value="">Select</option>
                <option value="morning">Morning</option>
                <option value="evening">Evening</option>
              </select>
            </div>
          </div>

          {/* Date */}
          <div>
            <label className="block font-bold text-gray-700">Date</label>
            <input
              type="date"
              name="date"
              value={formData.date}
              onChange={handleChange}
              className="w-full border rounded-lg px-5 py-4 mt-1"
            />
          </div>

          {/* Message */}
          <div>
            <label className="block font-bold text-gray-700">
              Message <span className="text-red-500">*</span>
            </label>
            <textarea
              name="message"
              value={formData.message}
              onChange={handleChange}
              rows="3"
              className="w-full border rounded-lg px-3 py-2 mt-1"
              placeholder="Enter your message"
            ></textarea>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            className="bg-teal-700 text-white font-bold px-6 py-2 rounded-lg shadow transition transform hover:-translate-y-1 hover:shadow-md hover:bg-teal-800"
          >
            Submit
          </button>
        </form>
      </div>
    </div>
  );
}