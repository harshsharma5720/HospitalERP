import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import AdminSidebar from "./AdminSidebar";
import TopNavbar from "../../shared/TopNavbar";
import AdminDashboard from "./AdminDashboard";
import LeaveApproval from "./LeaveApproval";
import ManageUsers from "./ManageUsers";
import RegisterUser from "./RegisterUser";
import ManageDoctor from "./ManageDoctor";
import AdminDoctorAppointments from "./AdminDoctorAppointments";
import AuditLog from "./AuditLog";
import Notifications from "./Notifications";
import WalkInBooking from "../appointments/WalkInBooking";

export default function AdminLayout({ children }) {
  return (
    <div className="flex min-h-screen bg-gray-100 dark:bg-[#0f172a]">
      {/* LEFT SIDEBAR */}
      <AdminSidebar />

      {/* RIGHT CONTENT AREA */}
      <div className="flex flex-col flex-1">
        <TopNavbar variant="portal" />
        <div className="p-6">
          <Routes>
                      <Route path="dashboard" element={<AdminDashboard />} />
                      <Route path="leave-approval" element={<LeaveApproval />} />
                      <Route path="manage-users" element={<ManageUsers />} />
                      <Route path="register-user" element={<RegisterUser />} />
                      <Route path="manage-doctor" element={<ManageDoctor />} />
                      <Route path="doctor/:userId/appointments" element={<AdminDoctorAppointments />} />
                      <Route path="audit-log" element={<AuditLog />} />
                      <Route path="notifications" element={<Notifications />} />
                      <Route path="walk-in" element={<WalkInBooking />} />
                      <Route path="*" element={<Navigate to="dashboard" replace />} />
          </Routes>
        </div>
      </div>
    </div>
  );
}
