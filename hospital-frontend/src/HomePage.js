import React from "react";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import PopupForm from "./PopupForm";
import { useNavigate } from "react-router-dom";

export default function HomePage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-gray-50 relative overflow-hidden">
      <TopNavbar />
      <Navbar />

      {/* Background Section */}
      <div
        className="relative grid grid-cols-1 md:grid-cols-2 gap-10 px-10 py-12"
        style={{
          backgroundImage: "url('/doctors-image3.jpg')",
          backgroundSize: "cover",
          backgroundPosition: "center",
          backgroundRepeat: "no-repeat",
          height: "600px",
        }}
      >
        {/* ✅ Fade effect overlay */}
        <div className="absolute bottom-0 left-0 w-full h-40 bg-gradient-to-t from-white to-transparent pointer-events-none z-10"></div>

        {/* Left content */}
        <div className="relative z-20 w-[700px] p-4 border-2 border-[#1E63DB] rounded-xl space-y-8 bg-white bg-opacity-20 backdrop-blur-md">
          <div>
            <h2 className="text-4xl font-extrabold mb-3 text-[#1E63DB]">
              To Book an Appointment
            </h2>
            <p className="text-lg mb-4">
              📞 <span className="font-semibold">Call Us:</span> +91 92895 20303
            </p>
            <button
              onClick={() => navigate("/appointments")}
              className="bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white px-4 py-2 rounded-lg shadow hover:bg-teal-800 transition"
            >
              Book Appointment
            </button>
          </div>

          <div className="w-[600px] mx-auto border-2 border-[#1E63DB] rounded-xl p-6 shadow-md space-x-2 bg-white bg-opacity-30 backdrop-blur-md hover:-translate-y-2 hover:shadow-2xl transition">
            <h3 className="text-xl font-semibold text-[#1E63DB] mb-3">
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
      </div>

       {/* ✅ New "Trusted Health Partner" Section */}
            <section className="text-center py-16 bg-gradient-to-b from-white to-[#f8fbff]">
              <div className="inline-block bg-blue-100 text-blue-700 font-medium px-4 py-1 rounded-full mb-4 shadow-sm">
                Welcome to Modern Healthcare
              </div>
              <h1 className="text-5xl font-extrabold mb-4">
                Your Trusted{" "}
                <span className="bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
                  Health Partner
                </span>
              </h1>
              <p className="text-gray-600 text-lg max-w-2xl mx-auto mb-8">
                Professional Hospital Management System for seamless patient care,
                appointment booking, and comprehensive medical services.
              </p>
              <div className="flex justify-center gap-6">
                <button
                  onClick={() => navigate("/login")}
                  className="bg-gradient-to-r from-blue-600 to-cyan-500 text-white px-6 py-2 rounded-md shadow hover:opacity-90 transition"
                >
                  Sign In →
                </button>
                <button
                  onClick={() => navigate("/register")}
                  className="border border-gray-300 px-6 py-2 rounded-md text-gray-700 hover:bg-gray-100 transition shadow-sm"
                >
                  Create Account
                </button>
              </div>
            </section>

      <PopupForm />
    </div>
  );
}
