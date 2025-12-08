import React from "react";
import { useNavigate } from "react-router-dom";

function AdminSidebar() {
  const navigate = useNavigate();

  return (
    <div className="w-64 min-h-screen bg-[#0b1f36] text-white shadow-xl flex flex-col p-6 gap-4">
      <h2 className="text-2xl font-bold mb-4">Admin Panel</h2>

      <button onClick={() => navigate("/admin/dashboard")} className="py-2 px-3 bg-[#14365a] rounded-lg hover:bg-opacity-80 transition">
        Dashboard
      </button>
      <button onClick={() => navigate("/admin/manage-users")} className="py-2 px-3 bg-[#14365a] rounded-lg hover:bg-opacity-80 transition">
        Manage Users
      </button>
      <button onClick={() => navigate("/admin/leave-approval")} className="py-2 px-3 bg-[#14365a] rounded-lg hover:bg-opacity-80 transition">
        Approve Leaves
      </button>
      <button onClick={() => navigate("/admin/doctors")} className="py-2 px-3 bg-[#14365a] rounded-lg hover:bg-opacity-80 transition">
        Manage Doctors
      </button>
      <button onClick={() => navigate("/admin/receptionists")} className="py-2 px-3 bg-[#14365a] rounded-lg hover:bg-opacity-80 transition">
        Manage Receptionists
      </button>
      <button onClick={() => navigate("/admin/patients")} className="py-2 px-3 bg-[#14365a] rounded-lg hover:bg-opacity-80 transition">
        Manage Patients
      </button>
      <button onClick={() => navigate("/admin/departments")} className="py-2 px-3 bg-[#14365a] rounded-lg hover:bg-opacity-80 transition">
        Departments
      </button>

      <button onClick={() => navigate("/logout")} className="mt-auto py-2 px-3 bg-red-600 rounded-lg hover:bg-opacity-80 transition">
        Logout
      </button>
    </div>
  );
}

export default AdminSidebar;
