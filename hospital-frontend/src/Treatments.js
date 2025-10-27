import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { User, UserPlus } from "lucide-react";
import Navbar from "./Navbar";

export default function Treatments() {
  const navigate = useNavigate();
  const [slideIn, setSlideIn] = useState(false);

  useEffect(() => {
    setSlideIn(true); // Trigger sliding animation on mount
  }, []);

  const fullText = `Shreya Hospital is a leading healthcare institution dedicated to providing
exceptional medical services with a patient-centric approach. Established with
a vision to deliver high-quality healthcare, the hospital combines advanced
technology with compassionate care to ensure the well-being of every patient.
Our multidisciplinary team of experienced doctors, nurses, and medical staff
work collaboratively to offer comprehensive treatments across various specialties.
Equipped with state-of-the-art diagnostic and treatment facilities, Shreya Hospital
emphasizes preventive care, accurate diagnosis, and effective treatment plans.
The hospital fosters a healing environment where patient comfort and safety are
top priorities. From routine check-ups to complex surgeries, every service is
administered with utmost precision and empathy. Our commitment to excellence
extends beyond medical care, focusing also on patient education, community
health awareness, and continuous improvement in healthcare standards. With a
reputation for reliability and professionalism, Shreya Hospital stands as a
trusted name in healthcare, offering personalized and accessible medical services
to the community it serves.`;

  return (
    <div className="min-h-screen bg-gray-50 relative">
      {/* Top Info Bar */}
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

      {/* Navbar */}
      <Navbar />

      {/* Middle Section with Sliding Animation */}
      <div className="relative flex items-center justify-center px-6 md:px-16">
        {/* Background Image */}
        <img
          src="Shreyahospital.jpg"
          alt="Shreya Hospital"
          className="absolute top-0 left-0 w-full h-full object-cover opacity-20 z-0"
        />

        {/* Text */}
        <div
          className={`relative z-10 max-w-5xl py-20 transition-transform duration-1000 ${
            slideIn ? "translate-x-0" : "-translate-x-96"
          }`}
        >
          <h2 className="text-4xl md:text-5xl font-extrabold text-teal-700 mb-6 drop-shadow-lg text-center">
            About Shreya Hospital
          </h2>
          <p className="text-lg md:text-xl font-bold text-gray-900 leading-relaxed drop-shadow-md whitespace-pre-wrap">
            {fullText}
          </p>
        </div>
      </div>
    </div>
  );
}