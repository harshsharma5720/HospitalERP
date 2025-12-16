import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

export default function ManageUsers() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const token = localStorage.getItem("jwtToken");

  const fetchUsers = async () => {
    try {
      console.log("Using token:", token);
      const response = await axios.get("http://localhost:8080/api/admin/allUsers", {
        headers: { Authorization: `Bearer ${token}`},
      });
      console.log("Fetched users:", response.data);
      setUsers(response.data);
    } catch (error) {
      console.error("Error fetching users:", error);
    } finally {
      setLoading(false);
    }
  };

  const deleteUser = async (id) => {
    const confirmDelete = window.confirm("Are you sure you want to delete this user?");
    if (!confirmDelete) return;

    try {
      const response = await axios.delete(`http://localhost:8080/api/admin/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
      });
      alert("User deleted successfully!");
      fetchUsers();
    } catch (error) {
      console.error("Error deleting user:", error);
      alert("Delete failed!");
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
            <th className="p-3 text-left">Actions</th>
          </tr>
        </thead>

        <tbody>
          {users.map((user) => (
            <tr key={user.id} className="border-b">
              <td className="p-3">{user.id}</td>
              <td className="p-3">{user.username}</td>
              <td className="p-3">{user.role}</td>
              <td className="p-3">
                <button className="bg-yellow-500 text-white px-3 py-1 rounded mr-2">Edit</button>
                <button
                  onClick={() => deleteUser(user.id)}
                  className="bg-red-600 text-white px-3 py-1 rounded"
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
