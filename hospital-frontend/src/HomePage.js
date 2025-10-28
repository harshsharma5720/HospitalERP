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

      <div className="grid grid-cols-1 md:grid-cols-2 gap-10 px-10 py-12"
      style={{
           backgroundImage: "url('/doctors-image3.jpg')",
           backgroundSize: "cover",       // Makes image cover the full area
           backgroundPosition: "center",  // Keeps the image centered
           backgroundRepeat: "no-repeat",
           height: "600px",
           }}>
        {/* Left */}
        <div className="w-[700px] p-4 border-2 border-[#1E63DB] rounded-xl space-y-8 bg-white bg-opacity-20 backdrop-blur-md">
          <div>
            <h2 className="text-4xl font-extrabold mb-3 text-[#1E63DB] ">
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

      <PopupForm />
    </div>
  );
}
