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

        // ✅ Extract role and userId using utility methods
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
      } catch (error) {
        console.error("Error fetching profile:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchProfile();
  }, []);

  // This will handle closing the modal
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
    return <p className="text-center mt-10 text-lg">Loading profile...</p>;
  if (!userData)
    return (
      <p className="text-center mt-10 text-red-600">No profile data found.</p>
    );

  return (
    <>

      <div className="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center z-50">
        <div className="bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF] text-[#003366] p-6 rounded-xl shadow-xl w-full max-w-md mx-auto transform transition-all">
         {/* Close Button (top-right corner) */}
          <button
              onClick={handleClose}
              className="absolute top-3 right-3 text-gray-500 hover:text-red-600 transition"
              aria-label="Close"
          >
             <X size={24} />
          </button>
          <div className="flex flex-col items-center">
            <img
              src={
                userData.profileImage
                  ? `http://localhost:8080${userData.profileImage}`
                  : "https://cdn-icons-png.flaticon.com/512/3135/3135715.png"
              }
              alt="Profile"
              className="w-32 h-32 rounded-full shadow-md border-2 border-gray-300 mb-4"
            />
            <h2 className="text-2xl font-semibold text-gray-800">
              {userData.name || userData.patientName || userData.username}
            </h2>
            <p className="text-gray-500">{role}</p>
          </div>

          <div className="mt-6 space-y-3 text-gray-700">
            {role === "ROLE_DOCTOR" && (
              <>
                <p>
                  <strong>Specialization:</strong> {userData.specialisation}
                </p>
                <p>
                  <strong>Email:</strong> {userData.email}
                </p>
                <p>
                  <strong>Experience:</strong> {userData.experience || "N/A"}{" "}
                  years
                </p>
                <p>
                  <strong>Phone:</strong> {userData.phone || "N/A"}
                </p>
              </>
            )}

            {role === "ROLE_PATIENT" && (
              <>
                <p>
                  <strong>Name:</strong> {userData.patientName}
                </p>
                <p>
                  <strong>Email:</strong> {userData.email}
                </p>
                <p>
                  <strong>Phone:</strong> {userData.phone || "N/A"}
                </p>
                <p>
                  <strong>Gender:</strong> {userData.gender || "N/A"}
                </p>
                <p>
                  <strong>Date of Birth:</strong> {userData.dob || "N/A"}
                </p>
                <p>
                  <strong>Address:</strong> {userData.address || "N/A"}
                </p>
              </>
            )}

            {role === "ROLE_RECEPTIONIST" && (
              <>
                <p>
                  <strong>Name:</strong> {userData.name}
                </p>
                <p>
                  <strong>Email:</strong> {userData.email}
                </p>
                <p>
                  <strong>Phone:</strong> {userData.phone || "N/A"}
                </p>
                <p>
                  <strong>Shift:</strong> {userData.shift || "N/A"}
                </p>
              </>
            )}
          </div>

          <div className="mt-8 space-y-3 text-center">
            <button
              onClick={handleAboutAppointment}
              className="w-full px-6 py-2 bg-gradient-to-br from-blue-600 to-indigo-700 hover:opacity-90 text-white rounded-lg shadow-md transition"
            >
              About Patient Appointment
            </button>

            <button
              onClick={handleEdit}
              className="w-full px-6 py-2 bg-gradient-to-br from-blue-600 to-indigo-700 hover:opacity-90 text-white rounded-lg shadow-md transition"
            >
              Edit Profile
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
