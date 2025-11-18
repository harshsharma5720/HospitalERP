import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import TopNavbar from "./TopNavbar";
import Navbar from "./Navbar";
import { getUserIdFromToken, getRoleFromToken } from "./utils/jwtUtils";
import { useLocation } from "react-router-dom";

export default function AddRelativePage() {
  const navigate = useNavigate();

  const location = useLocation();
  const patientId = location.state?.patientId;
  const [form, setForm] = useState({
    name: "",
    gender: "",
    dob: "",
    relationship: "",
    patientAadharNo: "",
    patientId: patientId,
  });


  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();

    const token = localStorage.getItem("jwtToken");
    const patientId = getUserIdFromToken(token);

    try {
      await axios.post(
        `http://localhost:8080/api/patient/relative/add`,
        form,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      alert("Relative added successfully!");
      navigate("/edit-profile"); // redirect back
    } catch (err) {
      console.error("Error adding relative:", err);
      alert("Failed to add relative.");
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-[#0a1124]">
      <TopNavbar />
      <Navbar />

      <div className="max-w-3xl mx-auto bg-white dark:bg-[#111a3b] mt-10 p-8 rounded-xl shadow-lg">
        <h2 className="text-2xl font-bold mb-6 text-center dark:text-[#50d4f2]">
          Add New Relative
        </h2>

        <form onSubmit={handleSubmit} className="grid grid-cols-1 gap-5">

          <div>
            <label className="font-semibold dark:text-gray-300">Full Name</label>
            <input
              type="text"
              name="name"
              value={form.name}
              onChange={handleChange}
              required
              className="p-2 w-full border rounded bg-gray-100 dark:bg-[#1e293b] dark:text-white"
            />
          </div>

          <div>
            <label className="font-semibold dark:text-gray-300">Gender</label>
            <select
              name="gender"
              value={form.gender}
              onChange={handleChange}
              required
              className="p-2 w-full border rounded bg-gray-100 dark:bg-[#1e293b] dark:text-white"
            >
              <option value="">Select Gender</option>
              <option value="MALE">Male</option>
              <option value="FEMALE">Female</option>
              <option value="OTHER">Other</option>
            </select>
          </div>

          <div>
            <label className="font-semibold dark:text-gray-300">Date of Birth</label>
            <input
              type="date"
              name="dob"
              value={form.dob}
              onChange={handleChange}
              required
              className="p-2 w-full border rounded bg-gray-100 dark:bg-[#1e293b] dark:text-white"
            />
          </div>

          <div>
            <label className="font-semibold dark:text-gray-300">Relationship</label>
            <select
              name="relationship"
              value={form.relationship}
              onChange={handleChange}
              required
              className="p-2 w-full border rounded bg-gray-100 dark:bg-[#1e293b] dark:text-white"
            >
              <option value="">Select Relationship</option>
              <option value="FATHER">Father</option>
              <option value="MOTHER">Mother</option>
              <option value="WIFE">Wife</option>
              <option value="HUSBAND">Husband</option>
              <option value="SON">Son</option>
              <option value="DAUGHTER">Daughter</option>
              <option value="BROTHER">Brother</option>
              <option value="SISTER">Sister</option>
            </select>
          </div>

          <div>
            <label className="font-semibold dark:text-gray-300">Aadhar No</label>
            <input
              type="text"
              name="patientAadharNo"
              value={form.patientAadharNo}
              onChange={handleChange}
              required
              className="p-2 w-full border rounded bg-gray-100 dark:bg-[#1e293b] dark:text-white"
            />
          </div>

          <div className="flex justify-between mt-6">
            <button
              type="button"
              onClick={() => navigate("/edit-profile")}
              className="px-5 py-2 rounded-lg bg-gray-400 text-white hover:bg-gray-600"
            >
              Cancel
            </button>

            <button
              type="submit"
              onClick={() => navigate("/edit-profile")}
              className="px-5 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700"
            >
              Add Relative
            </button>
          </div>

        </form>
      </div>
    </div>
  );
}
