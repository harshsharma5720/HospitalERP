import React, { useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import TopNavbar from "./TopNavbar";
import Navbar from "./Navbar";
import { getErrorMessage } from "./utils/apiError";
import { toLocalISODate } from "./utils/dateUtils";
import { useLocation } from "react-router-dom";
import { API_BASE_URL } from "./config";

// Used for both "/add-relative" and "/edit-relative" (state.relative = relative to edit)
export default function AddRelativePage() {
  const navigate = useNavigate();

  const location = useLocation();
  const editing = location.state?.relative || null;
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    name: editing?.name || "",
    gender: editing?.gender || "",
    dob: editing?.dob || "",
    relationship: editing?.relationship || "",
    patientAadharNo: editing?.patientAadharNo ? String(editing.patientAadharNo) : "",
  });


  const handleChange = (e) =>
    setForm({ ...form, [e.target.name]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    // The backend links the relative to the logged-in patient, so no patientId is sent
    const payload = {
      ...form,
      patientAadharNo: form.patientAadharNo ? Number(form.patientAadharNo) : null,
    };

    try {
      setSaving(true);
      if (editing) {
        await axios.put(`${API_BASE_URL}/api/patient/relative/update/${editing.id}`, payload);
        alert("Relative updated successfully!");
      } else {
        await axios.post(`${API_BASE_URL}/api/patient/relative/add`, payload);
        alert("Relative added successfully!");
      }
      navigate("/edit-profile"); // redirect back
    } catch (err) {
      alert(getErrorMessage(err, editing ? "Failed to update relative." : "Failed to add relative."));
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-[#0a1124]">
      <TopNavbar />
      <Navbar />

      <div className="max-w-3xl mx-auto bg-white dark:bg-[#111a3b] mt-10 p-8 rounded-xl shadow-lg">
        <h2 className="text-2xl font-bold mb-6 text-center dark:text-[#50d4f2]">
          {editing ? "Edit Relative" : "Add New Relative"}
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
              max={toLocalISODate()}
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
              <option value="GRANDFATHER">Grandfather</option>
              <option value="GRANDMOTHER">Grandmother</option>
              <option value="OTHER">Other</option>
            </select>
          </div>

          <div>
            <label className="font-semibold dark:text-gray-300">Aadhar No</label>
            <input
              type="text"
              name="patientAadharNo"
              inputMode="numeric"
              pattern="[0-9]{12}"
              title="Aadhaar number must be 12 digits"
              placeholder="12-digit Aadhaar number (optional)"
              value={form.patientAadharNo}
              onChange={handleChange}
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
              disabled={saving}
              className="px-5 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700 disabled:opacity-60"
            >
              {saving ? "Saving..." : editing ? "Save Changes" : "Add Relative"}
            </button>
          </div>

        </form>
      </div>
    </div>
  );
}
