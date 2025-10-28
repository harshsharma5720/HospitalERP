import React from "react";
import { User, UserPlus } from "lucide-react";
import { useNavigate } from "react-router-dom";

export default function TopNavbar() {
  const navigate = useNavigate();

  return (
    <div className="bg-white shadow-md px-6 py-3 flex items-center border-b">
      {/* Left: Hospital logo and name */}
      <div className="flex items-center gap-2">
        <img src="download.jpeg" alt="Hospital Logo" className="h-10 w-10" />
        <h1 className="text-xl font-bold text-teal-700">Shreya Hospital</h1>
      </div>

      {/* Center: Location and Timings */}
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

      {/* Right: Login and Register buttons */}
      <div className="flex items-center gap-3">
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
      </div>
    </div>
  );
}
