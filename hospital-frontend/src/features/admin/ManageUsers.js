import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getErrorMessage } from "../../shared/utils/apiError";
import * as adminApi from "./api";

// Accounts are deactivated, not deleted (docs/ACCOUNT_DEACTIVATION_PLAN.md): a deactivated user can't log in
// and their history is kept. "Delete permanently" only works for accounts without any appointment.
export default function ManageUsers() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const fetchUsers = async () => {
    try {
      const response = await adminApi.getAllUsers();
      setUsers(response.data);
    } catch (error) {
      console.error("Error fetching users:", error);
    } finally {
      setLoading(false);
    }
  };

  const deactivate = async (user) => {
    const ok = window.confirm(
      `Deactivate ${user.username}?\n\nThey can't log in any more and their upcoming appointments are cancelled. ` +
        "All history is kept, and you can reactivate the account later."
    );
    if (!ok) return;
    try {
      await adminApi.deactivateUser(user.id);
      alert(`${user.username} deactivated.`);
      fetchUsers();
    } catch (error) {
      alert(getErrorMessage(error, "Deactivation failed!"));
    }
  };

  const reactivate = async (user) => {
    try {
      await adminApi.reactivateUser(user.id);
      alert(`${user.username} reactivated - they can log in again.`);
      fetchUsers();
    } catch (error) {
      alert(getErrorMessage(error, "Reactivation failed!"));
    }
  };

  const deletePermanently = async (user) => {
    const ok = window.confirm(
      `Delete ${user.username} permanently?\n\nThis can't be undone. It only works for accounts without any ` +
        "appointment (medical records must be kept) - otherwise deactivate the account."
    );
    if (!ok) return;
    try {
      await adminApi.deleteUserPermanently(user.id);
      alert(`${user.username} deleted permanently.`);
      fetchUsers();
    } catch (error) {
      alert(getErrorMessage(error, "Delete failed!"));
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  if (loading) return <p className="text-center mt-5">Loading users...</p>;

  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-4">Manage Users</h1>

      <button
        onClick={() => navigate("/admin/register-user")}
        className="bg-blue-600 text-white py-2 px-4 rounded-lg mb-4"
      >
        + Add New User
      </button>

      <table className="w-full border-collapse bg-white shadow-md rounded-lg">
        <thead>
          <tr className="bg-gray-100 border-b">
            <th className="p-3 text-left">ID</th>
            <th className="p-3 text-left">Username</th>
            <th className="p-3 text-left">Role</th>
            <th className="p-3 text-left">Status</th>
            <th className="p-3 text-left">Actions</th>
          </tr>
        </thead>

        <tbody>
          {users.map((user) => (
            <tr key={user.id} className={`border-b ${user.active ? "" : "bg-gray-50 text-gray-500"}`}>
              <td className="p-3">{user.id}</td>
              <td className="p-3">{user.username}</td>
              <td className="p-3">{user.role}</td>
              <td className="p-3">
                {user.active ? (
                  <span className="px-2 py-1 rounded-full text-xs bg-green-100 text-green-700 font-semibold">Active</span>
                ) : (
                  <span
                    className="px-2 py-1 rounded-full text-xs bg-gray-200 text-gray-700 font-semibold"
                    title={user.deactivatedAt ? `since ${String(user.deactivatedAt).slice(0, 10)}` : undefined}
                  >
                    Deactivated
                  </span>
                )}
              </td>
              <td className="p-3">
                <div className="flex flex-wrap gap-2">
                  <button className="bg-yellow-500 text-white px-3 py-1 rounded">Edit</button>
                  {user.active ? (
                    <button onClick={() => deactivate(user)} className="bg-orange-600 text-white px-3 py-1 rounded">
                      Deactivate
                    </button>
                  ) : (
                    <button onClick={() => reactivate(user)} className="bg-green-600 text-white px-3 py-1 rounded">
                      Reactivate
                    </button>
                  )}
                  <button
                    onClick={() => deletePermanently(user)}
                    className="border border-red-600 text-red-600 px-3 py-1 rounded"
                  >
                    Delete permanently
                  </button>
                </div>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
