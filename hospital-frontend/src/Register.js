import React from "react";

export default function RegisterPage() {
  return (
    <div
      className="min-h-screen bg-cover bg-center flex items-center justify-center relative"
      style={{ backgroundImage: "url('/register.jpeg')" }}
    >
      <div className="absolute inset-0 bg-blue bg-opacity-70"></div>

      <div
        className="relative bg-white bg-opacity-10 backdrop-blur-md p-8 rounded-2xl shadow-[0_10px_25px_rgba(0,0,0,0.7)]
                   w-full max-w-xl text-center"
      >
        <h2 className="text-3xl font-bold text-sky-400 mb-6">Register</h2>

        <form className="space-y-5">
          <input
            type="text"
            placeholder="Enter your email"
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 placeholder:text-lg"
          />

            <input
            type="text"
            placeholder="Enter Username"
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 placeholder:text-lg"
          />
          <input
            type="password"
            placeholder="Enter Password"
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 placeholder:text-lg"
          />

          <input
            type="password"
            placeholder="Re-type password"
            className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 placeholder:text-lg"
          />

          <select>
          placeholder="Role"
          <option value="option1">Role</option>
          className="w-full px-4 py-4 border rounded-lg focus:outline-none focus:ring-2 focus:ring-teal-600 placeholder:text-lg"

          </select>

          <input
            type="submit"
            value="Submit"
            className="w-full px-4 py-4 border bg-sky-400 text-white rounded-lg font-semibold hover:bg-gray-900 focus:outline-none focus:ring-2 focus:ring-teal-600"
          />

        </form>
      </div>
    </div>
  );
}