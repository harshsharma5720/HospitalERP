import React from "react";
import { NavLink } from "react-router-dom";
import { Home, Activity, User, Phone, Info } from "lucide-react";

export default function Navbar() {
  const navItems = [
    { name: "Home", path: "/", icon: <Home size={20} /> },
    { name: "Treatments", path: "/treatments", icon: <Activity size={20} /> },
    { name: "Doctors", path: "/doctors", icon: <User size={20} /> },
    { name: "Contact Us", path: "/contact", icon: <Phone size={20} /> },
    { name: "About Us", path: "/about", icon: <Info size={20} /> },
  ];

  return (
    <div className="bg-teal-700 text-white px-6 py-3 flex gap-12 shadow-lg text-lg font-medium">
      {navItems.map((item) => (
        <NavLink
          key={item.path}
          to={item.path}
          end
          className={({ isActive }) =>
            `flex items-center gap-2 px-3 py-1 rounded-lg hover:bg-teal-800 transition
             ${isActive ? "border-b-2 border-white font-semibold" : ""}`
          }
        >
          {item.icon} {item.name}
        </NavLink>
      ))}
    </div>
  );
}