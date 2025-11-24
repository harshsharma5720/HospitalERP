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
    setFormData({ ...formData, [e.target.name]: e.target.value });
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
      className="
        min-h-screen bg-cover bg-center flex items-center justify-center relative overflow-hidden
        dark:bg-[#0a1330]
        transition-all duration-300
      "
      style={{ backgroundImage: "url('/background.jpeg')" }}
    >
      {/* Dark overlay */}
      <div className="absolute inset-0 bg-black bg-opacity-60 dark:bg-[#0a1330]/80"></div>

      {/* Right Animation */}
      <div className="absolute right-10 bottom-0 w-[300px] h-[300px] opacity-80 pointer-events-none">
        <Lottie animationData={LoginAnim} loop={true} />
      </div>

      {/* Home Button */}
      <button
        onClick={() => navigate("/")}
        className="
          absolute top-6 left-6 px-6 py-2 rounded-lg font-semibold shadow-lg transition
          bg-gradient-to-br from-[#1E63DB] to-[#27496d]
          dark:from-[#50d4f2] dark:to-[#3bc2df] dark:text-black
          text-white
        "
      >
        Home
      </button>

      {/* MAIN CARD */}
      <div
        className="
          relative z-10 animate-scaleUp flex flex-col md:flex-row w-[90%] md:w-[70%] lg:w-[60%]
          rounded-3xl overflow-hidden shadow-2xl border border-white/20
          bg-white/10 backdrop-blur-xl
          dark:bg-[#111a3b]/60 dark:border-[#16224a]
          transition
        "
      >
        {/* Left Panel */}
        <div
          className="
            md:w-1/2 w-full flex flex-col justify-center items-center text-white text-center p-10
            bg-gradient-to-br from-[#1E63DB] to-[#27496d]
            dark:from-[#111a3b] dark:to-[#0a1330]
          "
        >
          <h1 className="text-4xl font-extrabold mb-3 drop-shadow-md dark:text-[#50d4f2]">
            WELCOME BACK
          </h1>
          <p className="text-sm text-gray-200 dark:text-[#8ddff8]">
            Your trusted health partner
          </p>
          <div className="mt-6 w-24 h-1 bg-white dark:bg-[#50d4f2] rounded-full"></div>
        </div>

        {/* Right Panel - Login Form */}
        <div
          className="
            md:w-1/2 w-full p-10 flex flex-col justify-center
            bg-white/10 backdrop-blur-sm
            dark:bg-[#16224a]/40
            text-white transition
          "
        >
          <h2
            className="
              text-3xl font-bold mb-6 text-center
              bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent
              dark:from-[#50d4f2] dark:to-[#3bc2df]
            "
          >
            Sign In / Login
          </h2>

          {error && <p className="text-red-400 dark:text-red-300 font-semibold mb-4 text-center">{error}</p>}

          <form className="space-y-5" onSubmit={handleSubmit}>
            {/* Username */}
            <input
              type="text"
              name="username"
              placeholder="Username or Email"
              value={formData.username}
              onChange={handleChange}
              className="
                w-full px-4 py-3 rounded-lg
                bg-white/70 text-gray-900 placeholder-gray-500
                focus:outline-none focus:ring-2 focus:ring-blue-600
                dark:bg-[#111a3b] dark:text-[#50d4f2] dark:placeholder-gray-400
                dark:focus:ring-[#50d4f2]
              "
              required
            />

            {/* Password */}
            <input
              type="password"
              name="password"
              placeholder="Password"
              value={formData.password}
              onChange={handleChange}
              className="
                w-full px-4 py-3 rounded-lg
                bg-white/70 text-gray-900 placeholder-gray-500
                focus:outline-none focus:ring-2 focus:ring-blue-600
                dark:bg-[#111a3b] dark:text-[#50d4f2] dark:placeholder-gray-400
                dark:focus:ring-[#50d4f2]
              "
              required
            />

            {/* Submit */}
            <button
              type="submit"
              className="
                w-full py-3 rounded-lg font-semibold shadow-lg transition-all
                bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white
                hover:bg-[#162c42]
                dark:from-[#50d4f2] dark:to-[#3bc2df] dark:text-black
              "
            >
              Sign In
            </button>

            {/* Other Login */}
            <button
              type="button"
              className="
                w-full border border-white/50 dark:border-[#50d4f2]/40
                text-white dark:text-[#50d4f2]
                py-3 rounded-lg font-semibold
                hover:bg-white/20 dark:hover:bg-[#16224a]/60
                transition-all
              "
            >
              Sign in with other
            </button>
          </form>

          {/* Register Link */}
          <div className="text-center mt-5 text-sm text-gray-200 dark:text-[#8ddff8]">
            Don’t have an account?{" "}
            <a
              href="/register"
              className="font-bold text-[#1e3d59] dark:text-[#50d4f2] hover:underline"
            >
              Sign up
            </a>
          </div>
        </div>
      </div>
    </div>
  );
}
