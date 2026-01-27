import React from "react";
import { Routes, Route } from "react-router-dom";
import AdminSidebar from "../../components/AdminSidebar";
import TopNavbar from "../../components/TopNavbar";
import Navbar from "../../components/Navbar";
import AdminDashboard from "./AdminDashboard";
import LeaveApproval from "./LeaveApproval";
import ManageUsers from "./ManageUsers";
import RegisterUser from "./RegisterUser";
import ManageDoctor from "./ManageDoctor";

export default function AdminLayout({ children }) {
  return (
    <div className="flex min-h-screen bg-gray-100 dark:bg-[#0f172a]">
      {/* LEFT SIDEBAR */}
      <AdminSidebar />

      {/* RIGHT CONTENT AREA */}
      <div className="flex flex-col flex-1">
        <TopNavbar />
        <div className="p-6">
          <Routes>
                      <Route path="dashboard" element={<AdminDashboard />} />
                      <Route path="leave-approval" element={<LeaveApproval />} />
                      <Route path="manage-users" element={<ManageUsers />} />
                      <Route path="register-user" element={<RegisterUser />} />
                      <Route path="manage-doctor" element={<ManageDoctor />} />
          </Routes>
        </div>
      </div>
    </div>
  );
}
