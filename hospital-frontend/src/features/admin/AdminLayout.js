import React from "react";
import { Routes, Route, Navigate } from "react-router-dom";
import StaffLayout from "../../shared/layout/StaffLayout";
import AdminDashboard from "./AdminDashboard";
import LeaveApproval from "./LeaveApproval";
import ManageUsers from "./ManageUsers";
import RegisterUser from "./RegisterUser";
import ManageDoctor from "./ManageDoctor";
import AdminDoctorAppointments from "./AdminDoctorAppointments";
import AuditLog from "./AuditLog";
import Notifications from "./Notifications";
import Fees from "./Fees";
import BillsPage from "../billing/BillsPage";
import BillPage from "../billing/BillPage";
import WalkInBooking from "../appointments/WalkInBooking";
import FrontDeskAppointments from "../appointments/FrontDeskAppointments";
import PatientSearch from "../patients/PatientSearch";
import FrontDeskPatient from "../patients/FrontDeskPatient";

// The admin portal: its pages in the staff layout (menu: shared/layout/staffMenus.js)
export default function AdminLayout() {
  return (
    <StaffLayout>
      <Routes>
        <Route path="dashboard" element={<AdminDashboard />} />
        <Route path="leave-approval" element={<LeaveApproval />} />
        <Route path="manage-users" element={<ManageUsers />} />
        <Route path="register-user" element={<RegisterUser />} />
        <Route path="manage-doctor" element={<ManageDoctor />} />
        <Route path="doctor/:userId/appointments" element={<AdminDoctorAppointments />} />
        <Route path="audit-log" element={<AuditLog />} />
        <Route path="notifications" element={<Notifications />} />
        <Route path="fees" element={<Fees />} />
        <Route path="bills" element={<BillsPage />} />
        <Route path="bills/:billId" element={<BillPage />} />
        <Route path="bills/visit/:appointmentId" element={<BillPage />} />
        <Route path="walk-in" element={<WalkInBooking />} />
        <Route path="appointments" element={<FrontDeskAppointments />} />
        <Route path="patients" element={<PatientSearch />} />
        <Route path="patients/:patientId" element={<FrontDeskPatient />} />
        <Route path="*" element={<Navigate to="dashboard" replace />} />
      </Routes>
    </StaffLayout>
  );
}
