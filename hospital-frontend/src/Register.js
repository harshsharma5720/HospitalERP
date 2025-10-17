import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

export default function RegisterPage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    email: "",
    username: "",
    password: "",
    confirmPassword: "",
    role: "",
  });

  const [error, setError] = useState("");

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (formData.password !== formData.confirmPassword) {
      setError("Passwords do not match!");
      return;
    }

    try {
      const response = await axios.post("http://localhost:8080/api/auth/register", {
        email: formData.email,
        username: formData.username,
        password: formData.password,
        role: formData.role,
      });

      const token = response.data.token;

      if (token) {
        localStorage.setItem("token", token);
        alert(`Welcome ${formData.username}! Registration successful.`);
        navigate("/appointment"); // directly go to appointment page
      } else {
        setError("Registration successful, but token not received.");
      }
    } catch (err) {
      console.error("Registration error:", err);
      setError("Registration failed. Please try again.");
    }
  };

  return (
    <div
      className="min-h-screen bg-cover bg-center flex items-center justify-center relative"
      style={{ backgroundImage: "url('/register.jpeg')" }}
    >
      <div className="absolute inset-0 bg-blue bg-opacity-70"></div>

      <div className="relative bg-white bg-opacity-10 backdrop-blur-md p-8 rounded-2xl shadow-[0_10px_25px_rgba(0,0,0,0.7)] w-full max-w-xl text-center">
        <h2 className="text-3xl font-bold text-sky-400 mb-6">Register</h2>

        {error && <p className="text-red-500 font-semibold mb-4">{error}</p>}

        <form className="space-y-5" onSubmit={handleSubmit}>
          <input
            type="email"
            name="email"
            placeholder="Enter your email"
            value={formData.email}
            onChange={handleChange}
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-400 placeholder:text-lg"
            required
          />

          <input
            type="text"
            name="username"
            placeholder="Enter Username"
            value={formData.username}
            onChange={handleChange}
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-400 placeholder:text-lg"
            required
          />

          <input
            type="password"
            name="password"
            placeholder="Enter Password"
            value={formData.password}
            onChange={handleChange}
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-400 placeholder:text-lg"
            required
          />

          <input
            type="password"
            name="confirmPassword"
            placeholder="Re-type Password"
            value={formData.confirmPassword}
            onChange={handleChange}
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-400 placeholder:text-lg"
            required
          />

          {/* ✅ Fixed Role Dropdown */}
          <select
            name="role"
            value={formData.role}
            onChange={handleChange}
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-sky-400 text-lg"
            required
          >
            <option value="">Select Role</option>
            <option value="ADMIN">Admin</option>
            <option value="DOCTOR">Doctor</option>
            <option value="PATIENT">Patient</option>
            <option value="RECEPTIONIST">Receptionist</option>
          </select>

          <input
            type="submit"
            value="Register"
            className="w-full px-4 py-4 border bg-sky-400 text-white rounded-lg font-semibold hover:bg-gray-900 focus:outline-none focus:ring-2 focus:ring-sky-400"
          />
        </form>
      </div>
    </div>
  );
}
