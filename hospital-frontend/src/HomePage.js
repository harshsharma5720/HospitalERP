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
  const [isDarkMode, setIsDarkMode] = useState(false);

  const checkLoginStatus = () => {
    const token = localStorage.getItem("jwtToken");
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split(".")[1]));
        setIsLoggedIn(true);
        setUsername(payload.sub || "User");
      } catch {
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

  useEffect(() => {
    const html = document.documentElement;

    const updateTheme = () => {
      setIsDarkMode(html.classList.contains("dark"));
    };

    // Check initial
    updateTheme();

    // Listen for changes (whenever toggle button switches)
    const observer = new MutationObserver(updateTheme);
    observer.observe(html, { attributes: true, attributeFilter: ["class"] });

    return () => observer.disconnect();
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("jwtToken");
    setIsLoggedIn(false);
    navigate("/login");
    window.dispatchEvent(new Event("storage"));
  };

  return (
    <div
      className="
        min-h-screen
        bg-gray-50 dark:bg-[#0a1330]
        text-gray-900 dark:text-gray-200
        relative overflow-hidden
        transition-colors duration-300
      "
    >
      <TopNavbar />
      <Navbar />

      {/* HERO SECTION */}
      <div
        className="relative grid grid-cols-1 md:grid-cols-2 gap-10 px-10 py-12"
        style={{
          backgroundImage: isDarkMode
                ? "url('/doctor_black_theam.jpg')"
                : "url('/doctors-image3.jpg')",
          backgroundSize: "cover",
          backgroundPosition: "center",
          height: "600px",
        }}
      >
        {/* Bottom gradient overlay */}
        <div
          className="
            absolute bottom-0 left-0 w-full h-40
            bg-gradient-to-t
            from-white dark:from-[#0a1124]
            to-transparent
            z-10 pointer-events-none
          "
        ></div>

        {/* Left Hero Card */}
        <div
          className="
            relative z-20 w-[700px] p-4
            border-2 border-[#1E63DB]
            rounded-xl space-y-8
            bg-white/20 dark:bg-[#111a3b]/40
            backdrop-blur-md
            transition
          "
        >

           <h2 className="text-5xl font-extrabold mb-4">
              To Book An{" "}
              <span className="bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
                 Appointment
              </span>
           </h2>

          <p className="text-lg mb-4 dark:text-gray-300">
            📞 <span className="font-semibold">Call Us:</span> +91 92895 20303
          </p>

          <button
            onClick={() => navigate("/appointments")}
            className="
              bg-gradient-to-br
              from-[#1E63DB] to-[#27496d]
              dark:from-[#50d4f2] dark:to-[#3bc2df]
              dark:text-black
              text-white px-4 py-2 rounded-lg shadow
              hover:opacity-90 transition
            "
          >
            Book Appointment
          </button>

          {/* WHY SECTION CARD */}
          <div
            className="
              w-[600px] mx-auto border-2 border-[#1E63DB]
              rounded-xl p-6 shadow-md
              bg-white/30 dark:bg-[#16224a]/50
              backdrop-blur-md
              hover:-translate-y-2 hover:shadow-2xl transition
            "
          >
            <h3 className="text-xl font-semibold text-[#1E63DB] dark:text-[#50d4f2] mb-3">
              Why Shreya-Hospital Healthcare?
            </h3>

            <ul className="list-disc list-inside space-y-2 text-gray-700 dark:text-gray-300">
              <li>Trusted healthcare provider with years of experience.</li>
              <li>Qualified and compassionate team of doctors & staff.</li>
              <li>Modern patient-focused facilities.</li>
              <li>Affordable, advanced medical equipment.</li>
              <li>Easy and fast appointment booking.</li>
            </ul>
          </div>
        </div>
      </div>

      {/* TRUSTED HEALTH PARTNER */}
      <section
        className="
          text-center py-16
          bg-gradient-to-b from-white to-[#f8fbff]
          dark:from-[#0a1330] dark:to-[#111a3b]
          transition
        "
      >
        <div
          className="
            inline-block
            bg-blue-100 dark:bg-[#16224a]
            text-blue-700 dark:text-[#50d4f2]
            font-medium px-4 py-1 rounded-full mb-4 shadow-sm
          "
        >
          Welcome to Modern Healthcare
        </div>

        <h1 className="text-5xl font-extrabold mb-4">
          Your Trusted{" "}
          <span className="bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
            Health Partner
          </span>
        </h1>

        <p className="text-gray-600 dark:text-gray-300 text-lg max-w-2xl mx-auto mb-8">
          Professional Hospital Management System for seamless patient care,
          appointment booking, and modern services.
        </p>

        <div className="flex justify-center gap-6">
          {!isLoggedIn ? (
            <>
              <button
                onClick={() => navigate("/login")}
                className="
                  bg-gradient-to-r from-blue-600 to-cyan-500
                  dark:from-[#50d4f2] dark:to-[#3bc2df]
                  dark:text-black
                  text-white px-6 py-2 rounded-md shadow hover:opacity-90 transition
                "
              >
                Sign In →
              </button>

              <button
                onClick={() => navigate("/register")}
                className="
                  border border-gray-300 dark:border-gray-500
                  px-6 py-2 rounded-md
                  text-gray-700 dark:text-gray-300
                  hover:bg-gray-100 dark:hover:bg-[#111a3b]
                  transition shadow-sm
                "
              >
                Create Account
              </button>
            </>
          ) : (
            <>
              <span className="text-teal-700 dark:text-[#50d4f2] font-semibold">
                {username}
              </span>

              <button
                onClick={handleLogout}
                className="
                  flex items-center gap-2 px-6 py-2 rounded-md
                  bg-gradient-to-br from-red-500 to-red-700
                  dark:from-red-600 dark:to-red-800
                  text-white hover:opacity-90 transition
                "
              >
                <LogOut size={20} /> Logout
              </button>
            </>
          )}
        </div>
      </section>
      <section className="py-10 bg-white dark:bg-[#0a1330] transition">
        <div className="max-w-7xl mx-auto flex justify-between items-center px-4 md:px-10">

          {/* Facilities */}
          <div className="px-5 flex-1 text-center">
            <h2 className="text-4xl font-extrabold text-blue-600 dark:text-[#50d4f2]">70</h2>
            <p className="text-lg font-semibold text-gray-800 dark:text-gray-200">Facilities</p>
            <p className="text-gray-500 dark:text-gray-400 text-base">Multi-specialty hospitals and clinics</p>
          </div>

          {/* Thin Vertical Line */}
          <div className="hidden md:block h-16 w-[0.5px] bg-gray-300 dark:bg-gray-600"></div>

          {/* Staff */}
          <div className="px-5 flex-1 text-center">
            <h2 className="text-4xl font-extrabold text-blue-600 dark:text-[#50d4f2]">5,300+</h2>
            <p className="text-lg font-semibold text-gray-800 dark:text-gray-200">Allied & Nursing Staff</p>
            <p className="text-gray-500 dark:text-gray-400 text-base">Compassionate care and rehabilitation</p>
          </div>

          {/* Thin Vertical Line */}
          <div className="hidden md:block h-16 w-[0.5px] bg-gray-300 dark:bg-gray-600"></div>

          {/* Doctors */}
          <div className="px-5 flex-1 text-center">
            <h2 className="text-4xl font-extrabold text-blue-600 dark:text-[#50d4f2]">1,700+</h2>
            <p className="text-lg font-semibold text-gray-800 dark:text-gray-200">Doctors</p>
            <p className="text-gray-500 dark:text-gray-400 text-base">Providing personalised care that matters</p>
          </div>

          {/* Thin Vertical Line */}
          <div className="hidden md:block h-16 w-[0.5px] bg-gray-300 dark:bg-gray-600"></div>

          {/* Patients */}
          <div className="px-5 flex-1 text-center">
            <h2 className="text-4xl font-extrabold text-blue-600 dark:text-[#50d4f2]">5.3 Million</h2>
            <p className="text-lg font-semibold text-gray-800 dark:text-gray-200">Patients</p>
            <p className="text-gray-500 dark:text-gray-400 text-base">Treated every year</p>
          </div>

        </div>
      </section>


      {/* BANNER */}
      <section className="relative mx-10 my-10 rounded-2xl overflow-hidden shadow-lg">
        <div
          className="
            relative text-white text-center py-12 px-6
            bg-gradient-to-r from-[#1E63DB] to-[#27496d]
            dark:from-[#111a3b] dark:to-[#0a1330]
          "
        >
          <div className="relative z-10 flex flex-col md:flex-row items-center justify-center gap-6">
            <div className="text-3xl md:text-4xl font-extrabold tracking-wide">
              SHREYA HOSPITAL
            </div>

            <div className="hidden md:block w-[2px] h-10 bg-white/50"></div>

            <div className="text-2xl md:text-3xl font-semibold text-yellow-300 dark:text-yellow-400">
              Advanced Care. Modern Equipment. Trusted Hands.
            </div>
          </div>
        </div>
      </section>

      {/* HOSPITALS & CLINICS */}
      <section className="py-16 bg-white dark:bg-[#0a1330] text-center transition">
        <h2 className="text-5xl font-extrabold mb-10">
          <span className="text-black dark:text-white">Featured </span>
          <span className="bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
            Hospitals & Clinics
          </span>
        </h2>

        <p className="text-gray-600 dark:text-gray-300 text-lg max-w-2xl mx-auto mb-12">
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
              className="relative overflow-hidden rounded-2xl shadow-lg group transition"
            >
              <img
                src={item.img}
                alt={item.name}
                className="
                  w-full h-[400px] object-cover
                  filter grayscale group-hover:grayscale-0
                  transition-all duration-700 ease-in-out
                  transform group-hover:scale-105
                "
              />
              <div
                className="
                  absolute inset-0 bg-black/30
                  opacity-0 group-hover:opacity-100
                  flex items-end justify-center
                  transition
                "
              >
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
