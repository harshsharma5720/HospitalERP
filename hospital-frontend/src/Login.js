import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

export default function LoginPage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    username: "",
    password: "",
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

    try {
      const response = await axios.post("http://localhost:8080/api/auth/login", {
        username: formData.username,
        password: formData.password,
      });

      const token = response.data.token;
      console.log(token);

      if (token) {
        localStorage.setItem("jwtToken", token);
        alert("Login successful!");
        navigate("/appointments");
      } else {
        setError("Login successful, but token not received.");
      }
    } catch (err) {
      console.error("Login error:", err);
      setError("Invalid credentials. Please try again.");
    }
  };

  return (
    <div
      className="min-h-screen bg-cover bg-center flex items-center justify-center relative"
      style={{ backgroundImage: "url('/background.jpeg')" }}
    >
      {/* 🔹 Background Overlay */}
      <div className="absolute inset-0 bg-black bg-opacity-70"></div>

      {/* 🏠 Home Button - Top Left */}
      <button
        onClick={() => navigate("/")}
        className="absolute top-6 left-6 bg-teal-600 text-white px-5 py-2 rounded-lg font-semibold shadow-lg hover:bg-teal-700 transition"
      >
        ⬅ Home
      </button>

      {/* 🔹 Login Card */}
      <div className="relative bg-white bg-opacity-10 backdrop-blur-md p-8 rounded-2xl shadow-[0_10px_25px_rgba(0,0,0,0.7)] w-full max-w-md text-center">
        <h2 className="text-3xl font-bold text-teal-700 mb-6">Login</h2>

        {error && <p className="text-red-500 font-semibold mb-4">{error}</p>}

        <form className="space-y-5" onSubmit={handleSubmit}>
          <input
            type="text"
            name="username"
            placeholder="Enter Username or Email"
            value={formData.username}
            onChange={handleChange}
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 placeholder:text-lg"
            required
          />

          <input
            type="password"
            name="password"
            placeholder="Enter your Password"
            value={formData.password}
            onChange={handleChange}
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 placeholder:text-lg"
            required
          />

          <button
            type="submit"
            className="w-full bg-teal-700 text-white py-4 rounded-lg font-semibold hover:bg-teal-800 transition shadow-lg"
          >
            Login
          </button>
        </form>

        <p className="text-sm text-gray-200 mt-4">
          Don’t have an account?{" "}
          <a href="/register" className="text-teal-700 font-bold hover:underline">
            Register
          </a>
        </p>
      </div>
    </div>
  );
}
