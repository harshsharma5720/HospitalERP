import React, { useState } from "react";
import { NavLink } from "react-router-dom";
import { Home, Activity, User, Phone, Info, UserCircle } from "lucide-react";
import ProfilePage from "./ProfilePage";

export default function Navbar() {
  const [showProfile, setShowProfile] = useState(false);

  const navItems = [
    { name: "Home", path: "/", icon: <Home size={20} /> },
    { name: "Treatments", path: "/treatments", icon: <Activity size={20} /> },
    { name: "Doctors", path: "/doctors", icon: <User size={20} /> },
    { name: "Contact Us", path: "/contact", icon: <Phone size={20} /> },
    { name: "About Us", path: "/about", icon: <Info size={20} /> },
  ];

  return (
    <div className="bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white px-8 py-4 flex items-center justify-between shadow-lg text-lg font-medium sticky top-0 z-50">

      {/* Left: Nav links */}
      <div className="flex gap-10">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            end
            className={({ isActive }) =>
              `flex items-center gap-2 px-3 py-1 rounded-md transition-all duration-200
              hover:bg-white hover:text-teal-700 ${
                isActive
                  ? "bg-white text-teal-700 font-semibold shadow-sm"
                  : ""
              }`
            }
          >
            {item.icon}
            <span>{item.name}</span>
          </NavLink>
        ))}
      </div>

      {/* Right: Profile button */}
      <div>
        <button
          onClick={() => setShowProfile(true)}
          className="flex items-center gap-2 bg-white text-teal-700 px-3 py-1 rounded-full shadow-md hover:bg-teal-100 transition"
        >
          <UserCircle size={24} />
          <span className="font-semibold">Profile</span>
        </button>
      </div>

      {/* Profile Popup */}
      {showProfile && (
        <ProfilePage onClose={() => setShowProfile(false)} />
      )}
    </div>
  );
}
