import React, { useState } from "react";
import axios from "axios";

export default function RegisterUser() {
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    phone: "",
    password: "",
    role: "",
  });

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async () => {
    try {
      await axios.post("http://localhost:8080/api/auth/register", {
        email: formData.email,
        username: formData.username,
        phoneNumber: formData.phone,
        password: formData.password,
        role: formData.role,
      });

      alert("User registered successfully!");
    } catch (error) {
      alert("Registration failed!");
      console.error(error.response || error);
    }
  };

  return (
    <div className="p-6 max-w-lg mx-auto">
      <h1 className="text-3xl font-bold mb-6">Register New User</h1>

      <select
        name="role"
        className="w-full border p-2 rounded mb-4"
        onChange={handleChange}
      >
        <option value="">Select Role</option>
        <option value="ADMIN">Admin</option>
        <option value="DOCTOR">Doctor</option>
        <option value="PATIENT">Patient</option>
        <option value="RECEPTIONIST">Receptionist</option>
      </select>

      <input
        name="username"
        placeholder="Username"
        className="w-full border p-2 rounded mb-4"
        onChange={handleChange}
      />

      <input
        name="email"
        type="email"
        placeholder="Email"
        className="w-full border p-2 rounded mb-4"
        onChange={handleChange}
      />

      <input
        name="phone"
        placeholder="Phone Number"
        className="w-full border p-2 rounded mb-4"
        onChange={handleChange}
      />

      <input
        name="password"
        type="password"
        placeholder="Password"
        className="w-full border p-2 rounded mb-4"
        onChange={handleChange}
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
