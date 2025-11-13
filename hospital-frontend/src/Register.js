import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";

export default function RegisterPage() {
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    email: "",
    username: "",
    phone: "",
    otp: "",
    password: "",
    confirmPassword: "",
    role: "",
  });

  const [error, setError] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [otpVerified, setOtpVerified] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  // Send OTP
  const handleSendOtp = async () => {
    if (!formData.phone) {
      alert("Please enter your phone number first");
      return;
    }
    try {
      setLoading(true);
      const res = await axios.post("http://localhost:8080/api/auth/send-otp", {
        phone: formData.phone,
      });
      alert(res.data.message || "OTP sent successfully!");
      setOtpSent(true);
    } catch (err) {
      console.error(err);
      alert("Failed to send OTP. Try again.");
    } finally {
      setLoading(false);
    }
  };

  // Verify OTP
  const handleVerifyOtp = async () => {
    try {
      const res = await axios.post("http://localhost:8080/api/auth/verify-otp", {
        phone: formData.phone,
        otp: formData.otp,
      });
      if (res.data.success) {
        alert("OTP Verified!");
        setOtpVerified(true);
      } else {
        alert("Invalid OTP!");
      }
    } catch (err) {
      alert("OTP verification failed!");
    }
  };

  // Register user
  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");

    if (formData.password !== formData.confirmPassword) {
      setError("Passwords do not match!");
      return;
    }

    if (!otpVerified) {
      setError("Please verify your phone number before registering!");
      return;
    }

    try {
      const response = await axios.post("http://localhost:8080/api/auth/register", {
        email: formData.email,
        username: formData.username,
        phoneNumber: formData.phone,
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
      className="
        min-h-screen bg-cover bg-center flex items-center justify-center relative
        dark:bg-[#0a1330] dark:text-[#50d4f2]
      "
      style={{ backgroundImage: "url('/background.jpeg')" }}
    >
      <div className="absolute inset-0 bg-black bg-opacity-60 dark:bg-[#050b1f]/70"></div>

      <button
        onClick={() => navigate("/")}
        className="
          absolute top-6 left-6
          bg-gradient-to-br from-[#1E63DB] to-[#27496d]
          dark:from-[#50d4f2] dark:to-[#3bc2df]
          text-white dark:text-black px-5 py-2 rounded-lg font-semibold shadow-lg
          hover:opacity-90 transition
        "
      >
        Home
      </button>

      <div
        className="
          relative z-10 flex flex-col md:flex-row w-[90%] md:w-[70%] lg:w-[70%]
          rounded-3xl overflow-hidden shadow-2xl border border-white/20
          bg-white/10 backdrop-blur-xl
          dark:bg-[#111a3b]/70 dark:border-[#16224a]
        "
      >
        {/* LEFT FORM SECTION */}
        <div
          className="
            md:w-1/2 w-full bg-white/10 dark:bg-[#0f172a]/70
            backdrop-blur-sm p-10 flex flex-col justify-center
            text-white dark:text-[#50d4f2]
          "
        >
          <h2 className="text-3xl font-bold mb-6 text-center text-[#5A73C4] dark:text-[#50d4f2] drop-shadow-md">
            Create Account
          </h2>

          {error && <p className="text-red-400 font-semibold mb-4 text-center">{error}</p>}

          <form className="space-y-5" onSubmit={handleSubmit}>
            {/* Email */}
            <input
              type="email"
              name="email"
              placeholder="Email Address"
              value={formData.email}
              onChange={handleChange}
              className="
                w-full px-4 py-3 rounded-lg
                bg-white/70 text-gray-900 placeholder-gray-500
                dark:bg-[#0f172a] dark:text-[#50d4f2] dark:placeholder-gray-400
                focus:outline-none focus:ring-2 focus:ring-[#1e3d59] dark:focus:ring-[#50d4f2]
              "
              required
            />

            {/* Username */}
            <input
              type="text"
              name="username"
              placeholder="Username"
              value={formData.username}
              onChange={handleChange}
              className="
                w-full px-4 py-3 rounded-lg
                bg-white/70 text-gray-900 placeholder-gray-500
                dark:bg-[#0f172a] dark:text-[#50d4f2] dark:placeholder-gray-400
                focus:outline-none focus:ring-2 focus:ring-[#1e3d59] dark:focus:ring-[#50d4f2]
              "
              required
            />

            {/* Phone + OTP */}
            <div className="flex gap-2">
              <input
                type="text"
                name="phone"
                placeholder="Phone Number"
                value={formData.phone}
                onChange={handleChange}
                className="
                  flex-1 px-4 py-3 rounded-lg
                  bg-white/70 text-gray-900 placeholder-gray-500
                  dark:bg-[#0f172a] dark:text-[#50d4f2] dark:placeholder-gray-400
                  focus:outline-none focus:ring-2 focus:ring-[#1e3d59] dark:focus:ring-[#50d4f2]
                "
                required
              />
              <button
                type="button"
                onClick={handleSendOtp}
                disabled={loading}
                className="
                  bg-gradient-to-br from-[#1E63DB] to-[#27496d]
                  dark:from-[#50d4f2] dark:to-[#3bc2df]
                  text-white dark:text-black px-4 rounded-lg
                  hover:opacity-90 transition
                "
              >
                {loading ? "Sending..." : "Send OTP"}
              </button>
            </div>

            {otpSent && (
              <div className="flex flex-col gap-2">
                <div className="flex gap-2">
                  <input
                    type="text"
                    name="otp"
                    placeholder="Enter OTP"
                    value={formData.otp}
                    onChange={handleChange}
                    className="
                      flex-1 px-4 py-3 rounded-lg
                      bg-white/70 text-gray-900 placeholder-gray-500
                      dark:bg-[#0f172a] dark:text-[#50d4f2] dark:placeholder-gray-400
                      focus:outline-none focus:ring-2 focus:ring-[#1e3d59] dark:focus:ring-[#50d4f2]
                    "
                  />
                  <button
                    type="button"
                    onClick={handleVerifyOtp}
                    className="bg-green-600 text-white px-4 rounded-lg hover:opacity-90"
                  >
                    Verify
                  </button>
                </div>

                <div className="text-center mt-1">
                  {otpVerified ? (
                    <span className="text-green-400 font-semibold">✅ Phone Verified</span>
                  ) : (
                    <span className="text-red-400 font-semibold">❌ Not Verified</span>
                  )}
                </div>
              </div>
            )}

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
                dark:bg-[#0f172a] dark:text-[#50d4f2] dark:placeholder-gray-400
                focus:outline-none focus:ring-2 focus:ring-[#1e3d59] dark:focus:ring-[#50d4f2]
              "
              required
            />

            <input
              type="password"
              name="confirmPassword"
              placeholder="Confirm Password"
              value={formData.confirmPassword}
              onChange={handleChange}
              className="
                w-full px-4 py-3 rounded-lg
                bg-white/70 text-gray-900 placeholder-gray-500
                dark:bg-[#0f172a] dark:text-[#50d4f2] dark:placeholder-gray-400
                focus:outline-none focus:ring-2 focus:ring-[#1e3d59] dark:focus:ring-[#50d4f2]
              "
              required
            />

            {/* Role */}
            <select
              name="role"
              value={formData.role}
              onChange={handleChange}
              className="
                w-full px-4 py-3 rounded-lg
                bg-white/70 text-gray-900 placeholder-gray-500
                dark:bg-[#0f172a] dark:text-[#50d4f2] dark:placeholder-gray-400
                focus:outline-none focus:ring-2 focus:ring-[#1e3d59] dark:focus:ring-[#50d4f2]
              "
              required
            >
              <option value="">Select Role</option>
              <option value="ADMIN">Admin</option>
              <option value="DOCTOR">Doctor</option>
              <option value="PATIENT">Patient</option>
              <option value="RECEPTIONIST">Receptionist</option>
            </select>

            {/* Register */}
            <button
              type="submit"
              className={`
                w-full py-3 rounded-lg font-semibold text-white dark:text-black
                transition-all shadow-lg
                ${
                  otpVerified
                    ? "bg-gradient-to-br from-[#1E63DB] to-[#27496d] dark:from-[#50d4f2] dark:to-[#3bc2df]"
                    : "bg-gray-400 cursor-not-allowed"
                }
              `}
              disabled={!otpVerified}
            >
              Register
            </button>
          </form>

          <div className="text-center mt-5 text-sm text-gray-200 dark:text-[#8ddff8]">
            Already have an account?{" "}
            <a href="/login" className="text-[#1e3d59] dark:text-[#50d4f2] font-bold hover:underline">
              Sign in
            </a>
          </div>
        </div>

        {/* RIGHT SIDE */}
        <div
          className="
            md:w-1/2 w-full flex flex-col justify-center items-center text-white text-center p-10
            bg-gradient-to-br from-[#1E63DB] to-[#27496d]
            dark:from-[#0f172a] dark:to-[#111a3b]
          "
        >
          <h1 className="text-4xl font-extrabold mb-3 drop-shadow-md">
            WELCOME
          </h1>
          <p className="text-sm text-gray-200 dark:text-[#8ddff8]">
            Join our trusted health community
          </p>
          <div className="mt-6 w-24 h-1 bg-white dark:bg-[#50d4f2] rounded-full"></div>
        </div>
      </div>
    </div>
  );
}
