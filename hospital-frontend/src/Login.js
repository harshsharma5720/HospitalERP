import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import Lottie from "lottie-react";
import LoginAnim from "./assets/LoginAnim.json";

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
      className="min-h-screen bg-cover bg-center flex items-center justify-center relative overflow-hidden"
      style={{ backgroundImage: "url('/background.jpeg')" }}
    >
      {/* Background Overlay */}
      <div className="absolute inset-0 bg-black bg-opacity-60"></div>

      {/* Right-Side Background Animation */}
      <div className="absolute right-10 bottom-0 w-[300px] h-[300px] opacity-80 pointer-events-none">
        <Lottie animationData={LoginAnim} loop={true} />
      </div>

      {/* Home Button */}
      <button
        onClick={() => navigate("/")}
        className="absolute top-6 left-6 bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white px-5 py-2 rounded-lg font-semibold shadow-lg hover:bg-[#1e3d59] transition"
      >
        Home
      </button>

      {/* Main Card */}
      <div className="relative z-10 flex flex-col md:flex-row w-[90%] md:w-[70%] lg:w-[60%] rounded-3xl overflow-hidden shadow-2xl border border-white/20 bg-white/10 backdrop-blur-xl">

        {/* Left Section */}
        <div className="md:w-1/2 w-full flex flex-col justify-center items-center text-white text-center p-10 bg-gradient-to-br from-[#1E63DB] to-[#27496d]">
          <h1 className="text-4xl font-extrabold mb-3 drop-shadow-md">WELCOME BACK</h1>
          <p className="text-sm text-gray-200">Your trusted health partner</p>
          <div className="mt-6 w-24 h-1 bg-white rounded-full"></div>
        </div>

        {/* Right Section - Login Form */}
        <div className="md:w-1/2 w-full bg-white/10 backdrop-blur-sm p-10 flex flex-col justify-center text-white">
          <h2 className="text-3xl font-bold mb-6 text-center text-[#5A73C4] drop-shadow-md">
            Sign In / Login
          </h2>

          {error && <p className="text-red-400 font-semibold mb-4 text-center">{error}</p>}

          <form className="space-y-5" onSubmit={handleSubmit}>
            <input
              type="text"
              name="username"
              placeholder="Username or Email"
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

            <button
              type="submit"
              className="w-full bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white py-3 rounded-lg font-semibold hover:bg-[#162c42] transition-all shadow-lg"
            >
              Sign In
            </button>

            <button
              type="button"
              className="w-full border border-white/50 text-white py-3 rounded-lg font-semibold hover:bg-white/20 transition-all"
            >
              Sign in with other
            </button>
          </form>

          <div className="text-center mt-5 text-sm text-gray-200">
            Don’t have an account?{" "}
            <a
              href="/register"
              className="text-[#1e3d59] font-bold hover:underline"
            >
              Sign up
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
