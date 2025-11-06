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
        console.log(" Logged in user:", payload.sub);
      } catch (error) {
        console.error(" Invalid token:", error);
        setIsLoggedIn(false);
      }
    } else {
      setIsLoggedIn(false);
    }
  };

  useEffect(() => {
    // Check immediately
    checkLoginStatus();

    // Listen for token changes (from login/logout)
    const handleStorageChange = () => {
      checkLoginStatus();
    };

    window.addEventListener("storage", handleStorageChange);

    return () => {
      window.removeEventListener("storage", handleStorageChange);
    };
  }, []);

  const handleLogout = () => {
    localStorage.removeItem("jwtToken");
    setIsLoggedIn(false);
    navigate("/login");
    window.dispatchEvent(new Event("storage")); // Trigger manual update
  };

  return (
    <div className="bg-white shadow-md px-6 py-3 flex items-center border-b">
      {/* Left: Hospital logo */}
      <div className="flex items-center gap-2">
        <img src="download.jpeg" alt="Hospital Logo" className="h-10" />
      </div>

      {/* Center: Info */}
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

      {/* Right: Dynamic buttons */}
      <div className="flex items-center gap-3">
        {isLoggedIn ? (
          <>
            <span className="text-teal-700 font-semibold">
              {username}
            </span>
            <button
              onClick={handleLogout}
              className="flex items-center gap-2 px-3 py-1 rounded-lg bg-gradient-to-br from-red-500 to-red-700 text-white hover:from-red-600 hover:to-red-800 transition"
            >
              <LogOut size={20} /> Logout
            </button>
          </>
        ) : (
          <>
            <button
              onClick={() => navigate("/login")}
              className="flex items-center gap-2 px-3 py-1 rounded-lg bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white hover:bg-teal-800 transition"
            >
              <User size={20} /> Login
            </button>

            <button
              onClick={() => navigate("/register")}
              className="flex items-center gap-2 px-3 py-1 rounded-lg bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white hover:bg-teal-800 transition"
            >
              <UserPlus size={20} /> Register
            </button>
          </>
        )}
      </div>
    </div>
  );
}
