import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import TopNavbar from "../../components/TopNavbar";
import DoctorRightSidebar from "../../components/DoctorRightSidebar";

import DoctorDashboard from "./DoctorDashboard";
import DoctorAppointments from "./DoctorAppointments";
import LeaveManagementPage from "./LeaveManagementPage";


export default function DoctorLayout() {
  return (
    <div className="flex min-h-screen bg-gray-100 dark:bg-[#0f172a]">

      {/* MAIN CONTENT */}
      <div className="flex flex-col flex-1">
        <TopNavbar />

        <div className="p-6">
          <Routes>
            {/* default route */}
            <Route path="/" element={<Navigate to="dashboard" />} />

            <Route path="dashboard" element={<DoctorDashboard />} />
            <Route path="doctor-appointments" element={<DoctorAppointments />} />
            <Route path="leave-management" element={<LeaveManagementPage />} />
          </Routes>
        </div>
      </div>


    </div>
  );
}
