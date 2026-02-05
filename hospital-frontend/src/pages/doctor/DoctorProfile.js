import React, { useEffect, useState } from "react";
import axios from "axios";
import { getUserIdFromToken } from "../../utils/jwtUtils";

const SPECIALIST_OPTIONS = [
  "NOT_ASSIGNED",
  "CARDIOLOGY",
  "DENTISTRY",
  "ORTHOPEDICS",
  "NEUROLOGY",
  "PEDIATRICS",
  "DERMATOLOGY",
];

export default function DoctorProfile() {
  const [doctor, setDoctor] = useState(null);
  const [formData, setFormData] = useState({});
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    fetchDoctor();
  }, []);

  const fetchDoctor = async () => {
    const token = localStorage.getItem("jwtToken");
    const doctorId = getUserIdFromToken(token);

    try {
      const res = await axios.get(
        `http://localhost:8080/api/doctor/get/${doctorId}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      setDoctor(res.data);
      setFormData(res.data);
    } catch (err) {
      console.error("Failed to load doctor profile", err);
    }
  };

  /* ---------- HANDLE INPUT CHANGE ---------- */
  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  /* ---------- HANDLE IMAGE CHANGE ---------- */
  const handleImageChange = (e) => {
    setFormData({ ...formData, profileImage: e.target.files[0] });
  };

  /* ---------- SAVE PROFILE ---------- */
  const handleSave = async () => {
    const token = localStorage.getItem("jwtToken");
    const userIdFromToken = getUserIdFromToken(token);

    try {
      const formDataToSend = new FormData();

      const doctorPayload = {
        id: doctor.id,
        userId: userIdFromToken,
        name: formData.name,
        specialist: formData.specialist,
        email: formData.email,
        phoneNumber: formData.phoneNumber,
        userName: formData.userName,
        profileImage:
          formData.profileImage instanceof File
            ? doctor.profileImage
            : formData.profileImage,
      };

      formDataToSend.append(
        "doctorDTO",
        new Blob([JSON.stringify(doctorPayload)], {
          type: "application/json",
        })
      );

      if (formData.profileImage instanceof File) {
        formDataToSend.append("profileImage", formData.profileImage);
      }

      await axios.put(
        `http://localhost:8080/api/doctor/update/${userIdFromToken}`,
        formDataToSend,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "multipart/form-data",
          },
        }
      );

      setDoctor(doctorPayload);
      setIsEditing(false);
      alert("Profile updated successfully!");
    } catch (err) {
      console.error("Update failed:", err.response?.data);
      alert("Failed to update profile.");
    }
  };

  if (!doctor)
    return (
      <p className="text-center mt-20 text-gray-600 dark:text-gray-300">
        Loading profile...
      </p>
    );

  return (
    <div className="min-h-screen dark:bg-[#0a1124] p-8">

      {/* HEADER */}
      <div className="mb-10 flex justify-between items-center">
        <div>
          <h1 className="text-3xl font-bold text-gray-800 dark:text-white">
            My Profile —
            <span className="bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent ml-2">
              {doctor.name}
            </span>
          </h1>
          <p className="text-gray-500 dark:text-[#8ddff8]">
            {doctor.specialist}
          </p>
        </div>

        {!isEditing ? (
          <button
            onClick={() => setIsEditing(true)}
            className="px-6 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700"
          >
            Edit Profile
          </button>
        ) : (
          <button
            onClick={handleSave}
            className="px-6 py-2 rounded-lg bg-green-600 text-white hover:bg-green-700"
          >
            Save Changes
          </button>
        )}
      </div>

      {/* PROFILE CARD */}
      <div className="bg-white dark:bg-[#111a3b] p-8 rounded-2xl shadow-xl grid md:grid-cols-2 gap-6">

        {/* PROFILE IMAGE */}
        <div className="col-span-2 flex justify-center">
          <div className="text-center">
            <img
              src={
                formData.profileImage instanceof File
                  ? URL.createObjectURL(formData.profileImage)
                  : formData.profileImage
                  ? `http://localhost:8080${formData.profileImage}`
                  : "https://cdn-icons-png.flaticon.com/512/3135/3135715.png"
              }
              alt="Doctor"
              className="h-32 w-32 rounded-full border-4 border-[#50d4f2] object-cover"
            />

            {isEditing && (
              <input
                type="file"
                accept="image/*"
                onChange={handleImageChange}
                className="mt-4"
              />
            )}
          </div>
        </div>

        {/* NAME */}
        <ProfileField
          label="Name"
          name="name"
          value={formData.name}
          isEditing={false}
        />

        {/* USERNAME */}
        <ProfileField
          label="Username"
          name="userName"
          value={formData.userName}
          isEditing={isEditing}
          onChange={handleChange}
        />

        {/* EMAIL */}
        <ProfileField
          label="Email"
          name="email"
          value={formData.email}
          isEditing={isEditing}
          onChange={handleChange}
        />

        {/* PHONE */}
        <ProfileField
          label="Phone Number"
          name="phoneNumber"
          value={formData.phoneNumber}
          isEditing={isEditing}
          onChange={handleChange}
        />

        {/* SPECIALIST DROPDOWN */}
        <div>
          <p className="text-gray-500 dark:text-[#8ddff8] text-sm mb-1">
            Specialization
          </p>

          {isEditing ? (
            <select
              name="specialist"
              value={formData.specialist || "NOT_ASSIGNED"}
              onChange={handleChange}
              className="w-full px-4 py-3 rounded-lg border
                         bg-white dark:bg-[#0f172a]
                         text-gray-800 dark:text-white
                         border-gray-300 dark:border-[#1f2a52]"
            >
              {SPECIALIST_OPTIONS.map((spec) => (
                <option key={spec} value={spec}>
                  {spec.replace("_", " ")}
                </option>
              ))}
            </select>
          ) : (
            <div className="bg-gray-50 dark:bg-[#0f172a]
                            border border-gray-200 dark:border-[#1f2a52]
                            rounded-lg px-4 py-3
                            text-gray-800 dark:text-white font-semibold">
              {formData.specialist?.replace("_", " ")}
            </div>
          )}
        </div>

      </div>
    </div>
  );
}

/* ---------- FIELD COMPONENT ---------- */
function ProfileField({
  label,
  name,
  value,
  isEditing,
  onChange,
}) {
  return (
    <div>
      <p className="text-gray-500 dark:text-[#8ddff8] text-sm mb-1">
        {label}
      </p>

      {isEditing ? (
        <input
          type="text"
          name={name}
          value={value || ""}
          onChange={onChange}
          className="w-full px-4 py-3 rounded-lg border
                     bg-white dark:bg-[#0f172a]
                     text-gray-800 dark:text-white
                     border-gray-300 dark:border-[#1f2a52]"
        />
      ) : (
        <div className="bg-gray-50 dark:bg-[#0f172a]
                        border border-gray-200 dark:border-[#1f2a52]
                        rounded-lg px-4 py-3
                        text-gray-800 dark:text-white font-semibold">
          {value}
        </div>
      )}
    </div>
  );
}
