import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import StaffLayout from "../../../shared/layout/StaffLayout";
import MyDoctorProfile from "./MyDoctorProfile";
import DoctorDashboard from "./DoctorDashboard";
import DoctorAppointments from "./DoctorAppointments";
import LeaveManagementPage from "./LeaveManagementPage";
import DoctorSchedulePage from "./DoctorSchedulePage";
import TodaysQueue from "./TodaysQueue";

// The doctor portal: its pages in the staff layout (menu: shared/layout/staffMenus.js)
export default function DoctorLayout() {
  return (
    <StaffLayout>
      <Routes>
        <Route path="/" element={<Navigate to="dashboard" />} />
        <Route path="dashboard" element={<DoctorDashboard />} />
        <Route path="queue" element={<TodaysQueue />} />
        <Route path="doctor-appointments" element={<DoctorAppointments />} />
        <Route path="leave-management" element={<LeaveManagementPage />} />
        <Route path="profile" element={<MyDoctorProfile />} />
        <Route path="schedule" element={<DoctorSchedulePage />} />
        <Route path="*" element={<Navigate to="dashboard" replace />} />
      </Routes>
    </StaffLayout>
  );
}
