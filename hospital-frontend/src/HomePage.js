import React from "react";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import PopupForm from "./PopupForm";
import { useNavigate } from "react-router-dom";

export default function HomePage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-gray-50">
      <TopNavbar />
      <Navbar />

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

      <PopupForm />
    </div>
  );
}
