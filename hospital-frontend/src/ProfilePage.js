import React, { useEffect, useState } from "react";
import axios from "axios";
import { getRoleFromToken, getUserIdFromToken } from "./utils/jwtUtils.js";
import { useNavigate } from "react-router-dom";
import { X } from "lucide-react";

export default function ProfilePage({ onClose }) {
  const [userData, setUserData] = useState(null);
  const [role, setRole] = useState("");
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchProfile = async () => {
      try {
        const token = localStorage.getItem("jwtToken");
        if (!token) {
          alert("Please login first!");
          return;
        }

        const userRole = getRoleFromToken(token)?.toUpperCase();
        const userId = getUserIdFromToken(token);

        if (!userId) {
          alert("User ID not found in token!");
          return;
        }

        setRole(userRole);

        let url = "";
        if (userRole === "ROLE_DOCTOR") {
          url = `http://localhost:8080/api/doctor/get/${userId}`;
        } else if (userRole === "ROLE_PATIENT") {
          url = `http://localhost:8080/api/patient/getAccount/${userId}`;
        } else if (userRole === "ROLE_RECEPTIONIST") {
          url = `http://localhost:8080/api/receptionist/getReceptionist/${userId}`;
        } else {
          alert("Invalid user role detected!");
          return;
        }

        const response = await axios.get(url, {
          headers: { Authorization: `Bearer ${token}` },
        });
        setUserData(response.data);
        console.log("Fetched profile data:", response.data);
      } catch (error) {
        console.error("Error fetching profile:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, []);

  const handleClose = () => {
    if (onClose) onClose();
  };

  const handleEdit = () => {
    if (onClose) onClose();
    navigate("/edit-profile");
  };

  const handleAboutAppointment = () => {
    if (onClose) onClose();
    navigate("/appointment-details");
  };

  if (loading)
    return <p className="text-center mt-10 text-lg dark:text-[#50d4f2]">Loading profile...</p>;

  if (!userData)
    return (
      <p className="text-center mt-10 text-red-600 dark:text-red-400">
        No profile data found.
      </p>
    );

  return (
    <>
      {/* Background Overlay + Center Modal */}
      <div className="
        fixed inset-0 bg-black bg-opacity-50
        dark:bg-[#050b1f]/70
        flex justify-center items-center z-50
      ">
        <div
          className="
            relative
            bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF]
            dark:from-[#0f172a] dark:to-[#111a3b]
            text-[#003366] dark:text-[#50d4f2]
            p-6 rounded-xl shadow-2xl w-full max-w-md mx-auto
            transition-all
          "
        >
          {/* Close Button */}
          <button
            onClick={handleClose}
            className="
              absolute top-3 right-3
              text-gray-500 hover:text-red-500
              dark:text-gray-300 dark:hover:text-red-400
              transition
            "
          >
            <X size={24} />
          </button>

          {/* PROFILE IMAGE + NAME */}
          <div className="flex flex-col items-center">
            <img
              src={
                userData.profileImage
                  ? `http://localhost:8080${userData.profileImage}`
                  : "https://cdn-icons-png.flaticon.com/512/3135/3135715.png"
              }
              alt="Profile"
              className="
                w-32 h-32 rounded-full shadow-md
                border-2 border-gray-200 dark:border-[#50d4f2]
                mb-4 object-cover
              "
            />

            <h2 className="text-2xl font-semibold text-gray-800 dark:text-[#50d4f2]">
              {userData.name || userData.patientName || userData.username}
            </h2>
            <p className="text-gray-500 dark:text-gray-300">{role}</p>
          </div>

          {/* ROLE SPECIFIC DETAILS */}
          <div className="mt-6 space-y-3 text-gray-700 dark:text-gray-300">

            {role === "ROLE_DOCTOR" && (
              <>
                <p><strong>Specialization:</strong> {userData.specialist || "N/A"}</p>
                <p><strong>Email:</strong> {userData.email || "N/A"}</p>
                <p><strong>Experience:</strong> {userData.experience || "N/A"} years</p>
                <p><strong>Phone:</strong> {userData.phoneNumber || "N/A"}</p>
              </>
            )}

            {role === "ROLE_PATIENT" && (
              <>
                <p><strong>Name:</strong> {userData.patientName}</p>
                <p><strong>Email:</strong> {userData.email}</p>
                <p><strong>Phone:</strong> {userData.contactNo || "N/A"}</p>
                <p><strong>Gender:</strong> {userData.gender || "N/A"}</p>
                <p><strong>Date of Birth:</strong> {userData.dob || "N/A"}</p>
                <p><strong>Address:</strong> {userData.patientAddress || "N/A"}</p>
              </>
            )}

            {role === "ROLE_RECEPTIONIST" && (
              <>
                <p><strong>Name:</strong> {userData.name}</p>
                <p><strong>Email:</strong> {userData.email}</p>
                <p><strong>Phone:</strong> {userData.phone || "N/A"}</p>
                <p><strong>Shift:</strong> {userData.shift || "N/A"}</p>
              </>
            )}
          </div>

          {/* ACTION BUTTONS */}
          <div className="mt-8 space-y-3 text-center">

            <button
              onClick={handleAboutAppointment}
              className="
                w-full px-6 py-2 rounded-lg shadow-md transition
                bg-gradient-to-br from-blue-600 to-indigo-700
                hover:opacity-90
                text-white dark:text-white
              "
            >
              View Appointments
            </button>

            <button
              onClick={handleEdit}
              className="
                w-full px-6 py-2 rounded-lg shadow-md transition
                bg-gradient-to-br from-blue-600 to-indigo-700
                hover:opacity-90
                text-white dark:text-white
              "
            >
              View Profile
            </button>

          </div>
        </div>
      </div>
    </>
  );
}
