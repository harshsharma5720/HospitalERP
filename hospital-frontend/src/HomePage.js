import React from "react";
import { User, UserPlus } from "lucide-react";
import { useNavigate } from "react-router-dom";
import Navbar from "./Navbar";
import PopupForm from "./PopupForm"; // 👈 import popup here

export default function HomePage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Top Info Bar */}
      <div className="bg-white shadow-md px-6 py-3 flex items-center border-b">
        <div className="flex items-center gap-2">
          <img src="download.jpeg" alt="Hospital Logo" className="h-10 w-10" />
          <h1 className="text-xl font-bold text-teal-700">Shreya Hospital</h1>
        </div>

        <div className="flex-1 flex justify-center">
          <div className="bg-gray-100 border border-teal-200 shadow-md rounded-lg p-4 max-w-3xl text-center">
            <p className="text-sm text-gray-700">
              <span className="font-semibold text-teal-700">📍 Location:</span>{" "}
              Sahibabad, Plot No. 837, Shalimar Garden Main Rd, Block C,
              Sahibabad, Ghaziabad, Uttar Pradesh 201006
            </p>
            <p className="text-sm text-gray-700 mt-2">
              <span className="font-semibold text-teal-700">
                ⏰ Service Timings:
              </span>{" "}
              24×7
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => navigate("/login")}
            className="flex items-center gap-2 px-3 py-1 rounded-lg bg-teal-700 text-white hover:bg-teal-800 transition"
          >
            <User size={20} /> Login
          </button>

          <button
            onClick={() => navigate("/register")}
            className="flex items-center gap-2 px-3 py-1 rounded-lg bg-teal-700 text-white hover:bg-teal-800 transition"
          >
            <UserPlus size={20} /> Register
          </button>
        </div>
      </div>

      {/* Navbar */}
      <Navbar />

      {/* Main Section */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-10 px-10 py-12">
        {/* Left */}
        <div className="space-y-8">
          <div>
            <h2 className="text-2xl font-bold text-teal-700 mb-2">
              To Book an Appointment
            </h2>
            <p className="text-lg mb-4">
              📞 <span className="font-semibold">Call Us:</span> +91 92895 20303
            </p>
            <button
              onClick={() => navigate("/appointments")}
              className="bg-teal-700 text-white px-4 py-2 rounded-lg shadow hover:bg-teal-800 transition"
            >
              Book Appointment
            </button>
          </div>

          <div className="border-2 border-teal-500 rounded-xl p-6 shadow-md bg-white hover:-translate-y-2 hover:shadow-2xl transition">
            <h3 className="text-xl font-semibold text-teal-700 mb-3">
              Why Shreya-Hospital Healthcare?
            </h3>
            <ul className="list-disc list-inside space-y-2 text-gray-700">
              <li>Trusted healthcare provider with years of experience.</li>
              <li>Qualified and compassionate team of doctors & staff.</li>
              <li>Modern facilities and patient-focused care.</li>
              <li>Affordable treatment with advanced medical equipment.</li>
              <li>Convenient location and easy appointment booking.</li>
            </ul>
          </div>
        </div>

        {/* Right */}
        <div className="flex items-center justify-center">
          <img
            src="Shreyahospital.jpg"
            alt="Hospital"
            className="rounded-xl shadow-lg w-full max-w-md"
          />
        </div>
      </div>

      {/* 👇 Popup will only show on HomePage */}
      <PopupForm />
    </div>
  );
}
