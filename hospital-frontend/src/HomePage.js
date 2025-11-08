import React, { useEffect, useState } from "react";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import PopupForm from "./PopupForm";
import { useNavigate } from "react-router-dom";
import { LogOut } from "lucide-react";

export default function HomePage() {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [username, setUsername] = useState("");

  // 🔹 Check login status
  const checkLoginStatus = () => {
    const token = localStorage.getItem("jwtToken");
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split(".")[1]));
        setIsLoggedIn(true);
        setUsername(payload.sub || "User");
      } catch (error) {
        console.error("Invalid token:", error);
        setIsLoggedIn(false);
      }
    } else {
      setIsLoggedIn(false);
    }
  };

  useEffect(() => {
    checkLoginStatus();
    const handleStorageChange = () => checkLoginStatus();
    window.addEventListener("storage", handleStorageChange);
    return () => window.removeEventListener("storage", handleStorageChange);
  }, []);

  // 🔹 Logout handler
  const handleLogout = () => {
    localStorage.removeItem("jwtToken");
    setIsLoggedIn(false);
    navigate("/login");
    window.dispatchEvent(new Event("storage"));
  };

  return (
    <div className="min-h-screen bg-gray-50 relative overflow-hidden">
      <TopNavbar />
      <Navbar />

      {/* 🔹 Hero Section */}
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
        {/* Fade Overlay */}
        <div className="absolute bottom-0 left-0 w-full h-40 bg-gradient-to-t from-white to-transparent pointer-events-none z-10"></div>

        {/* Left Content */}
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

      {/* 🔹 "Trusted Health Partner" Section */}
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

        {/* 🔹 Login/Logout Buttons */}
        <div className="flex justify-center gap-6">
          {!isLoggedIn ? (
            <>
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
            </>
          ) : (
            <>
              <span className="text-teal-700 font-semibold">{username}</span>
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 px-6 py-2 rounded-md bg-gradient-to-br from-red-500 to-red-700 text-white hover:from-red-600 hover:to-red-800 transition"
              >
                <LogOut size={20} /> Logout
              </button>
            </>
          )}
        </div>
      </section>

      {/* 🔹 Medical Banner Section */}
      <section className="relative mx-10 my-10 rounded-2xl overflow-hidden shadow-lg">
        <div
          className="relative text-white text-center py-12 px-6"
          style={{
            backgroundImage:
              "linear-gradient(to right, #1E63DB, #27496d)",
            backgroundBlendMode: "overlay",
            backgroundSize: "cover",
            backgroundPosition: "center",
          }}
        >
          <div className="relative z-10 flex flex-col md:flex-row items-center justify-center gap-6">
            {/* Left Side */}
            <div className="text-3xl md:text-4xl font-extrabold text-white tracking-wide">
              SHREYA HOSPITAL
            </div>
            {/* Divider */}
            <div className="hidden md:block w-[2px] h-10 bg-white/50"></div>
            {/* Right Side Text */}
            <div className="text-2xl md:text-3xl font-semibold text-yellow-300">
              Advanced Care. Modern Equipment. Trusted Hands.
            </div>
          </div>
        </div>
      </section>


{/* 🔹 Our Hospitals & Clinics Section */}
<section className="py-16 bg-white text-center">
  <h2 className="text-5xl font-extrabold mb-10">
    <span className="text-black">Our </span>
    <span className="bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
      Hospitals & Clinics
    </span>
  </h2>

 {/* Underline */}
  <div className="w-40 h-1 bg-gradient-to-r from-blue-600 to-cyan-400 mx-auto mb-10 rounded-full"></div>

  {/* Subtitle line */}
  <p className="text-gray-600 text-lg max-w-2xl mx-auto mb-12">
    Delivering compassionate care through our trusted network of hospitals,
    clinics, pharmacies, and nursing centers.
  </p>

  <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-8 px-10">
    {[
      {
        img: "https://mir-s3-cdn-cf.behance.net/project_modules/1400_webp/67718f100399635.5f07f485280c4.jpg",
        name: "Shreya's Doctors",
      },
      {
        img: "https://i.pinimg.com/236x/13/d4/82/13d482936ab5d4a6172137d175366303.jpg",
        name: "Shreya's Clinics",
      },
      {
        img: "https://png.pngtree.com/thumb_back/fw800/background/20250813/pngtree-a-modern-hospital-building-with-sleek-glass-and-steel-facade-image_17907375.webp",
        name: "Shreya Pharmacy",
      },
      {
        img: "https://manage.nakshewala.com/assets/files/40x100sqft_Hospital_Front_elevation_L_63eb1ed19eb49.webp",
        name: "Shreya's Nursing",
      },
    ].map((item, index) => (
      <div
        key={index}
        className="relative overflow-hidden rounded-2xl shadow-lg group transition-all duration-500"
      >
        <img
          src={item.img}
          alt={item.name}
          className="w-full h-[400px] object-cover filter grayscale group-hover:grayscale-0 transition-all duration-700 ease-in-out transform group-hover:scale-105"
        />
        <div className="absolute inset-0 bg-black bg-opacity-30 opacity-0 group-hover:opacity-100 flex items-end justify-center transition-all duration-500">
          <h3 className="text-white text-lg font-semibold mb-4">
            {item.name}
          </h3>
        </div>
      </div>
    ))}
  </div>
</section>
      <PopupForm />
    </div>
  );
}
