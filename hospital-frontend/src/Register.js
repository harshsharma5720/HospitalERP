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
        navigate("/login");
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
      style={{ backgroundImage: "url('/background.jpeg')" }}
    >
      {/* Overlay */}
      <div className="absolute inset-0 bg-black bg-opacity-60"></div>

      {/* Home Button */}
      <button
        onClick={() => navigate("/")}
        className="absolute top-6 left-6 bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white px-5 py-2 rounded-lg font-semibold shadow-lg hover:bg-[#1e3d59] hover:text-white transition"
      >
        Home
      </button>

      {/* Main Card */}
      <div className="relative z-10 flex flex-col md:flex-row w-[90%] md:w-[70%] lg:w-[70%] rounded-3xl overflow-hidden shadow-2xl border border-white/20 bg-white/10 backdrop-blur-xl">

        {/* Left Section - Register Form */}
        <div className="md:w-1/2 w-full bg-white/10 backdrop-blur-sm p-10 flex flex-col justify-center text-white">
          <h2 className="text-3xl font-bold mb-6 text-center text-[#5A73C4] drop-shadow-md">
            Create Account
          </h2>

          {error && <p className="text-red-400 font-semibold mb-4 text-center">{error}</p>}

          <form className="space-y-5" onSubmit={handleSubmit}>
            <input
              type="email"
              name="email"
              placeholder="Email Address"
              value={formData.email}
              onChange={handleChange}
              className="w-full px-4 py-3 rounded-lg bg-white/70 text-gray-900 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-[#1e3d59]"
              required
            />

            <input
              type="text"
              name="username"
              placeholder="Username"
              value={formData.username}
              onChange={handleChange}
              className="w-full px-4 py-3 rounded-lg bg-white/70 text-gray-900 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-[#1e3d59]"
              required
            />

            <input
              type="password"
              name="password"
              placeholder="Password"
              value={formData.password}
              onChange={handleChange}
              className="w-full px-4 py-3 rounded-lg bg-white/70 text-gray-900 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-[#1e3d59]"
              required
            />

            <input
              type="password"
              name="confirmPassword"
              placeholder="Confirm Password"
              value={formData.confirmPassword}
              onChange={handleChange}
              className="w-full px-4 py-3 rounded-lg bg-white/70 text-gray-900 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-[#1e3d59]"
              required
            />

            <select
              name="role"
              value={formData.role}
              onChange={handleChange}
              className="w-full px-4 py-3 rounded-lg bg-white/70 text-gray-900 placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-[#1e3d59]"
              required
            >
              <option value="">Select Role</option>
              <option value="ADMIN">Admin</option>
              <option value="DOCTOR">Doctor</option>
              <option value="PATIENT">Patient</option>
              <option value="RECEPTIONIST">Receptionist</option>
            </select>

            <button
              type="submit"
              className="w-full bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white py-3 rounded-lg font-semibold hover:bg-[#162c42] transition-all shadow-lg"
            >
              Register
            </button>
          </form>

          <div className="text-center mt-5 text-sm text-gray-200">
            Already have an account?{" "}
            <a href="/login" className="text-[#1e3d59] font-bold hover:underline">
              Sign in
            </a>
          </div>
        </div>

        {/* Right Section - Welcome Box */}
        <div className="md:w-1/2 w-full flex flex-col justify-center items-center text-white text-center p-10 bg-gradient-to-br from-[#1E63DB] to-[#27496d]">
          <h1 className="text-4xl font-extrabold mb-3 drop-shadow-md">WELCOME</h1>
          <p className="text-sm text-gray-200">Join our trusted health community</p>
          <div className="mt-6 w-24 h-1 bg-white rounded-full"></div>
        </div>
      </div>
    </div>
  );
}
