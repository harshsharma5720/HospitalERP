import React, { useEffect, useState } from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import axios from "axios";
import TopNavbar from "../../components/TopNavbar";
import DoctorRightSidebar from "../../components/DoctorRightSidebar";
import { getUserIdFromToken } from "../../utils/jwtUtils";

import DoctorDashboard from "./DoctorDashboard";
import DoctorAppointments from "./DoctorAppointments";
import LeaveManagementPage from "./LeaveManagementPage";

export default function DoctorLayout() {
  const [doctor, setDoctor] = useState(null);

  useEffect(() => {
    const fetchDoctor = async () => {
      try {
        const token = localStorage.getItem("jwtToken");
        const doctorId = getUserIdFromToken(token);

        const res = await axios.get(
          `http://localhost:8080/api/doctor/get/${doctorId}`,
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );

        setDoctor(res.data);
      } catch (err) {
        console.error("Failed to load doctor profile", err);
      }
    };

    fetchDoctor();
  }, []);

  if (!doctor) {
    return (
      <div className="flex justify-center items-center h-screen text-lg">
        Loading doctor dashboard...
      </div>
    );
  }

  return (
    <div className="flex min-h-screen bg-gray-100 dark:bg-[#0f172a]">

      {/* MAIN CONTENT */}
      <div className="flex flex-col flex-1">

        <div className="p-6">
          <Routes>
            <Route path="/" element={<Navigate to="dashboard" />} />
            <Route path="dashboard" element={<DoctorDashboard />} />
            <Route path="doctor-appointments" element={<DoctorAppointments />} />
            <Route path="leave-management" element={<LeaveManagementPage />} />
          </Routes>
        </div>
      </div>

      {/* SIDEBAR */}
      <DoctorRightSidebar doctor={doctor} />
    </div>
  );
}
