import React from "react";
import {
  User,
  Calendar,
  LogOut,
  FileText,
  LayoutDashboard,
} from "lucide-react";
import { useNavigate } from "react-router-dom";

export default function DoctorRightSidebar({ doctor }) {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem("jwtToken");
    navigate("/login");
  };

  return (
    <div className="w-full lg:w-[320px] bg-white dark:bg-[#111a3b]
                    border-l dark:border-[#16224a]
                    shadow-xl rounded-2xl p-6
                    sticky top-8 h-[calc(100vh-4rem)]
                    flex flex-col justify-between">

      {/* ================= PROFILE ================= */}
      <div>
        <div className="flex flex-col items-center text-center">
          <img
            src={
              doctor.profileImage
                ? `http://localhost:8080${doctor.profileImage}`
                : "https://cdn-icons-png.flaticon.com/512/3135/3135715.png"
            }
            alt="Doctor"
            className="h-28 w-28 rounded-full border-4 border-[#50d4f2] object-cover"
          />

          <h3 className="mt-4 text-xl font-bold dark:text-white">
            Dr. {doctor.name}
          </h3>

          <p className="text-sm text-gray-500 dark:text-[#8ddff8]">
            {doctor.specialist}
          </p>

          <p className="text-sm text-gray-400 mt-1">
            {doctor.email}
          </p>
        </div>

        {/* ================= ACTIONS ================= */}
        <div className="mt-8 space-y-3">
          <SidebarButton
            icon={<LayoutDashboard size={18} />}
            label="Dashboard"
            onClick={() => navigate("/doctor/dashboard")}
          />

          <SidebarButton
            icon={<User size={18} />}
            label="My Profile"
            onClick={() => navigate("/doctor/profile")}
          />

          <SidebarButton
            icon={<Calendar size={18} />}
            label="Appointments"
            onClick={() => navigate("/doctor/appointments")}
          />

          <SidebarButton
            icon={<FileText size={18} />}
            label="Leave Management"
            onClick={() => navigate("/doctor/leave")}
          />
        </div>
      </div>

      {/* ================= LOGOUT ================= */}
      <div className="mt-6">
        <SidebarButton
          icon={<LogOut size={18} />}
          label="Logout"
          danger
          onClick={handleLogout}
        />
      </div>
    </div>
  );
}

/* ---------- BUTTON ---------- */
function SidebarButton({ icon, label, onClick, danger }) {
  return (
    <button
      onClick={onClick}
      className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg
        font-semibold transition
        ${
          danger
            ? "bg-red-100 text-red-600 hover:bg-red-200"
            : "bg-[#f6f9ff] dark:bg-[#16224a] text-gray-700 dark:text-white hover:bg-[#e9f3ff]"
        }`}
    >
      {icon}
      {label}
    </button>
  );
}
