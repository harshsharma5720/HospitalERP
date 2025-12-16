import React, { useState } from "react";
import axios from "axios";

export default function RegisterUser() {
  const [role, setRole] = useState("");
  const [user, setUser] = useState({ username: "", password: "" });

  const token = localStorage.getItem("token"); // ⬅️ Token added here

  const handleSubmit = async () => {
    let url = "";

    if (role === "PATIENT") url = "http://localhost:8080/api/admin/patient";
    if (role === "DOCTOR") url = "http://localhost:8080/api/admin/doctor";
    if (role === "RECEPTIONIST") url = "http://localhost:8080/api/admin/receptionist";

    try {
      await axios.post(url, user, {
        headers: { Authorization: `Bearer ${token}` }, // ⬅️ Token added to headers
      });

      alert("User registered successfully!");
    } catch (error) {
      alert("Registration failed!");
      console.error(error);
    }
  };

  return (
    <div className="p-6 max-w-lg mx-auto">
      <h1 className="text-3xl font-bold mb-6">Register New User</h1>

      <select
        className="w-full border p-2 rounded mb-4"
        onChange={(e) => setRole(e.target.value)}
      >
        <option value="">Select Role</option>
        <option value="PATIENT">Patient</option>
        <option value="DOCTOR">Doctor</option>
        <option value="RECEPTIONIST">Receptionist</option>
      </select>

      <input
        type="text"
        placeholder="Username"
        className="w-full border p-2 rounded mb-4"
        onChange={(e) => setUser({ ...user, username: e.target.value })}
      />

      <input
        type="password"
        placeholder="Password"
        className="w-full border p-2 rounded mb-4"
        onChange={(e) => setUser({ ...user, password: e.target.value })}
      />

      <button
        onClick={handleSubmit}
        className="bg-green-600 text-white py-2 px-4 rounded w-full"
      >
        Register
      </button>
    </div>
  );
}
