import React from "react";

export default function LoginPage() {
  return (
    <div
      className="min-h-screen bg-cover bg-center flex items-center justify-center relative"
      style={{ backgroundImage: "url('/background.jpeg')" }}
    >
      {/* Semi-transparent overlay over the whole background */}
      <div className="absolute inset-0 bg-black bg-opacity-70"></div>

      {/* Login box with black shadow */}
      <div className="relative bg-white bg-opacity-10 backdrop-blur-md p-8 rounded-2xl shadow-[0_10px_25px_rgba(0,0,0,0.7)] w-full max-w-md text-center">
        <h2 className="text-3xl font-bold text-teal-700 mb-6">Login</h2>

        <form className="space-y-5">
          <input
            type="text"
            placeholder="Enter your email"
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 placeholder:text-lg"
          />
          <input
            type="password"
            placeholder="Enter your password"
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 placeholder:text-lg"
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
          <a href="#" className="text-teal-700 font-bold hover:underline">
            Register
          </a>
        </p>
      </div>
    </div>
  );
}