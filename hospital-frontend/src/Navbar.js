import React, { useState, useEffect } from "react";
import { NavLink } from "react-router-dom";
import { Home, Activity, User, Phone, Info, UserCircle } from "lucide-react";
import ProfilePage from "./ProfilePage";
import DarkThemeToggle from "./DarkThemeToggle/DarkThemeToggle";
import { getRoleFromToken, getUserIdFromToken } from "./utils/jwtUtils";


export default function Navbar() {
  const [showProfile, setShowProfile] = useState(false);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [role, setRole] = useState(null);
  const [userId, setUserId] = useState(null);

  const navItems = [
    { name: "Home", path: "/", icon: <Home size={20} /> },
    { name: "Treatments", path: "/treatments", icon: <Activity size={20} /> },
    { name: "Doctors", path: "/doctors", icon: <User size={20} /> },
    { name: "Contact Us", path: "/contact", icon: <Phone size={20} /> },
    { name: "About Us", path: "/about", icon: <Info size={20} /> },
  ];

  const checkLoginStatus = () => {
    const token = localStorage.getItem("jwtToken");
    if (token) {
      setIsLoggedIn(true);

      const decodedRole = getRoleFromToken(token);
      console.log("Role from token:", decodedRole);
      setRole(decodedRole);

      const decodedUserId = getUserIdFromToken(token);
      setUserId(decodedUserId);
    } else {
      setIsLoggedIn(false);
      setRole(null);
      setUserId(null);
    }
  };


  useEffect(() => {
    checkLoginStatus();

    const handleStorageChange = () => {
      checkLoginStatus();
    };

    window.addEventListener("storage", handleStorageChange);
    window.addEventListener("loginStatusChanged", handleStorageChange);

    return () => {
      window.removeEventListener("storage", handleStorageChange);
      window.removeEventListener("loginStatusChanged", handleStorageChange);
    };
  }, []);

  return (
    <div
      className="
        bg-gradient-to-br
        from-[#1E63DB] to-[#27496d]
        text-white
        dark:from-[#0a1330] dark:to-[#111a3b]
        dark:text-[#50d4f2]
        px-8 py-4
        flex items-center justify-between
        shadow-lg
        text-lg font-medium
        sticky top-0 z-50
        transition-colors
      "
    >
      {/* Left: Nav links */}
      <div className="flex gap-10">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end
            className={({ isActive }) =>
              `flex items-center gap-2 px-3 py-1 rounded-md transition-all duration-200

              /* LIGHT MODE */
              hover:bg-white hover:text-teal-700

              /* DARK MODE */
              dark:hover:bg-gradient-to-br
              dark:hover:from-[#50d4f2]
              dark:hover:to-[#3bc2df] dark:hover:text-black

              ${
                isActive
                  ? `
                    bg-white text-teal-700 font-semibold shadow-sm
                    dark:bg-[#50d4f2] dark:text-black
                    `
                  : ""
              }`
            }
          >
            {item.icon}
            <span>{item.name}</span>
          </NavLink>
        ))}
        {role === "ROLE_DOCTOR" && (
          <NavLink
             to={`/doctor-appointments`}
                className={({ isActive }) =>
                              `flex items-center gap-2 px-3 py-1 rounded-md transition-all duration-200

                              /* LIGHT MODE */
                              hover:bg-white hover:text-teal-700

                              /* DARK MODE */
                              dark:hover:bg-gradient-to-br
                              dark:hover:from-[#50d4f2]
                              dark:hover:to-[#3bc2df] dark:hover:text-black

                              ${
                                isActive
                                  ? `
                                    bg-white text-teal-700 font-semibold shadow-sm
                                    dark:bg-[#50d4f2] dark:text-black
                                    `
                                  : ""
                              }`
                }
             >
               <Activity size={20} />
               <span>My Appointments</span>
          </NavLink>
        )}
      </div>

      {/* RIGHT SIDE */}
      <div className="flex items-center gap-4">

        {/* Dark Mode Toggle */}
        <DarkThemeToggle />

        {/* Profile Button */}
        {isLoggedIn && (
          <button
            onClick={() => setShowProfile(true)}
            className="
              flex items-center gap-2
              bg-white text-black
              dark:bg-[#16224a] dark:text-[#50d4f2]
              px-3 py-1 rounded-md shadow-md
              hover:bg-teal-100
              dark:hover:bg-[#1e2a4a]
              transition
            "
          >
            <UserCircle size={24} />
            <span className="font-semibold">Profile</span>
          </button>
        )}
      </div>

      {/* PROFILE POPUP */}
      {showProfile && <ProfilePage onClose={() => setShowProfile(false)} />}
    </div>
  );
}
