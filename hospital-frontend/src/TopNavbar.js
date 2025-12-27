import React, { useEffect, useState } from "react";
import { User, UserPlus, LogOut } from "lucide-react";
import { useNavigate } from "react-router-dom";

export default function TopNavbar() {
  const navigate = useNavigate();
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [username, setUsername] = useState("");

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

  const handleLogout = () => {
    localStorage.removeItem("jwtToken");
    setIsLoggedIn(false);
    navigate("/");
    window.dispatchEvent(new Event("storage"));
  };

  return (
    <div
      className="
        bg-white
        dark:bg-[#0a1124]      /* DEEP NAVY */
        dark:text-[#50d4f2]   /* NEON TEXT */
        shadow-lg px-6 py-4 flex items-center
        border-b dark:border-[#111a3b]
        transition-all duration-300
      "
    >
      {/* LEFT: LOGO */}
      <div className="flex items-center gap-2">
        <img src="download.jpeg" alt="Hospital Logo" className="h-10 rounded-full" />
      </div>

      {/* CENTER: INFO */}
      <div className="flex-1 flex justify-center">
        <div
          className="
            bg-gray-100
            dark:bg-[#111a3b]   /* CARD BLUE */
            border border-teal-200 dark:border-[#16224a]
            rounded-xl shadow-xl p-4 max-w-3xl text-center
            transition-all
          "
        >
          <p className="text-sm text-gray-700 dark:text-[#50d4f2]">
            <span className="font-semibold text-teal-700 dark:text-[#50d4f2]">
              📍 Location:
            </span>{" "}
            Sahibabad, Plot No. 837, Shalimar Garden Main Rd, Block C,
            Sahibabad, Ghaziabad, Uttar Pradesh 201006
          </p>

          <p className="text-sm text-gray-700 dark:text-[#50d4f2] mt-2">
            <span className="font-semibold text-teal-700 dark:text-[#50d4f2]">
              ⏰ Service Timings:
            </span>{" "}
            24×7
          </p>
        </div>
      </div>

      {/* RIGHT: BUTTONS */}
      <div className="flex items-center gap-3">
        {isLoggedIn ? (
          <>
            <span className="text-teal-700 dark:text-[#50d4f2] font-semibold">
              {username}
            </span>

            <button
              onClick={handleLogout}
              className="
                flex items-center gap-2 px-4 py-2 rounded-xl
                bg-[#1E63DB] dark:bg-[#50d4f2]
                text-white dark:text-black
                shadow-lg hover:opacity-90 transition
              "
            >
              <LogOut size={18} /> Logout
            </button>
          </>
        ) : (
          <>
            <button
              onClick={() => navigate("/login")}
              className="
                flex items-center gap-2 px-4 py-2 rounded-xl
                bg-gradient-to-br
                from-[#1E63DB] to-[#27496d]
                dark:from-[#50d4f2] dark:to-[#3bc2df]
                dark:text-black
                text-white dark:text-black
                shadow-lg hover:opacity-90 transition
              "
            >
              <User size={18} /> Login
            </button>

            <button
              onClick={() => navigate("/register")}
              className="
                flex items-center gap-2 px-4 py-2 rounded-xl
                bg-[#1E63DB] dark:bg-[#50d4f2]
                text-white dark:text-black
                shadow-lg hover:opacity-90 transition
              "
            >
              <UserPlus size={18} /> Register
            </button>
          </>
        )}
      </div>
    </div>
  );
}
