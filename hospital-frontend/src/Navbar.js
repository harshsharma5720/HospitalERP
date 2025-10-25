import React from "react";
import { Link } from "react-router-dom";
import { Home, Activity, User, Phone, Info } from "lucide-react";

export default function Navbar() {
  return (
    <div className="bg-teal-700 text-white px-6 py-3 flex gap-12 shadow-lg text-lg font-medium">
      <Link
        to="/"
        className="flex items-center gap-2 px-3 py-1 rounded-lg hover:bg-teal-800 transition"
      >
        <Home size={20} /> Home
      </Link>

      <Link
        to="/treatments"
        className="flex items-center gap-2 px-3 py-1 rounded-lg hover:bg-teal-800 transition"
      >
        <Activity size={20} /> Treatments
      </Link>

      <Link
        to="/doctors"
        className="flex items-center gap-2 px-3 py-1 rounded-lg hover:bg-teal-800 transition"
      >
        <User size={20} /> Doctors
      </Link>

      <Link
        to="/contact"
        className="flex items-center gap-2 px-3 py-1 rounded-lg hover:bg-teal-800 transition"
      >
        <Phone size={20} /> Contact Us
      </Link>

      <Link
        to="/about"
        className="flex items-center gap-2 px-3 py-1 rounded-lg hover:bg-teal-800 transition"
      >
        <Info size={20} /> About Us
      </Link>
    </div>
  );
}
