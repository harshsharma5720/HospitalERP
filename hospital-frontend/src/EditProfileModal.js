import React, { useEffect, useState } from "react";
import axios from "axios";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import { getRoleFromToken, getUserIdFromToken } from "./utils/jwtUtils";
import { useNavigate } from "react-router-dom";
import { Pencil } from "lucide-react";

export default function ProfilePage({ onClose }) {
  const [userData, setUserData] = useState(null);
  const [appointments, setAppointments] = useState([]);
  const [isEditing, setIsEditing] = useState(false);
  const [formData, setFormData] = useState({});
  const navigate = useNavigate();

  useEffect(() => {
    const token = localStorage.getItem("jwtToken");
    if (!token) return;

    const role = getRoleFromToken(token);
    const userId = getUserIdFromToken(token);
    fetchUserData(role, userId, token);
    fetchAppointments(role, userId, token);
  }, []);

  const getRoleEndpoints = (role) => {
    switch (role) {
      case "ROLE_PATIENT":
        return {
          getUrl: (id) => `http://localhost:8080/api/patient/getAccount/${id}`,
          updateUrl: (id) => `http://localhost:8080/api/patient/updateAccount/${id}`,
          appointmentUrl: `http://localhost:8080/appointment/getPatientAppointments`,
        };
      case "ROLE_DOCTOR":
        return {
          getUrl: (id) => `http://localhost:8080/api/doctor/get/${id}`,
          updateUrl: (id) => `http://localhost:8080/api/doctor/update/${id}`,
          appointmentUrl: (id) =>
            `http://localhost:8080/api/doctor/appointments/${id}`,
        };
      case "ROLE_RECEPTIONIST":
        return {
          getUrl: (id) => `http://localhost:8080/api/receptionist/get/${id}`,
          updateUrl: (id) =>
            `http://localhost:8080/api/receptionist/update/${id}`,
          appointmentUrl: (id) =>
            `http://localhost:8080/api/receptionist/appointments/${id}`,
        };
      default:
        return null;
    }
  };

  // Function to return fields based on role
  const getFieldsForRole = (role) => {
    switch (role) {
      case "ROLE_DOCTOR":
        return [
          { label: "Name", key: "name" , readOnly: true },
          { label: "Username", key: "userName" },
          { label: "Email", key: "email" },
          { label: "Phone Number", key: "phoneNumber" },
          { label: "Specialization", key: "specialization" },
        ];

      case "ROLE_PATIENT":
        return [
          { label: "Patient ID", key: "patientId" , readOnly: true  },
          { label: "Patient Name", key: "patientName" },
          { label: "Username", key: "userName" },
          { label: "Email", key: "email" },
          { label: "Contact No", key: "contactNo" },
          { label: "Gender", key: "gender" },
          { label: "DOB", key: "dob" },
          { label: "Aadhar No", key: "patientAadharNo" },
          { label: "Address", key: "patientAddress" },
        ];

      case "ROLE_RECEPTIONIST":
        return [
          { label: "Name", key: "name" },
          { label: "Username", key: "userName" },
          { label: "Email", key: "email" },
          { label: "Phone", key: "phone" },
          { label: "Gender", key: "gender" },
          { label: "Age", key: "age" },
        ];

      default:
        return [
          { label: "Name", key: "name" },
          { label: "Email", key: "email" },
        ];
    }
  };

  const fetchUserData = async (role, id, token) => {
    try {
          const urls = getRoleEndpoints(role);
      if (!urls) return;
      const response = await axios.get(urls.getUrl(id), {
        headers: { Authorization: `Bearer ${token}` },
      });
      let data = response.data;
      if (data.patientName && !data.name) {
        data.name = data.patientName;
      } else if (data.doctorName && !data.name) {
        data.name = data.doctorName;
      } else if (data.receptionistName && !data.name) {
        data.name = data.receptionistName;
      }
      setUserData(data);
      setFormData(data);
    } catch (err) {
      console.error("Error fetching user data:", err);
    }
  };

  const fetchAppointments = async (role, id, token) => {
    try {
      const urls = getRoleEndpoints(role);
      if (!urls) return;
      const response = await axios.get(urls.appointmentUrl, {
        headers: { Authorization: `Bearer ${token}` },
      });
      const data = response.data.appointments || response.data;
      setAppointments(data);
    } catch (err) {
      console.error("Error fetching appointments:", err);
    }
  };

  const handleEditToggle = () => setIsEditing(!isEditing);

  const handleChange = (e) =>
    setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleSave = async () => {
    const token = localStorage.getItem("jwtToken");
    const role = getRoleFromToken(token);
    const userId = getUserIdFromToken(token);
    const urls = getRoleEndpoints(role);

    try {
      const formDataToSend = new FormData();

      //Choose the correct JSON key based on role
      let dtoKey = "";
      if (role === "ROLE_PATIENT") dtoKey = "ptInfoDTO";
      else if (role === "ROLE_DOCTOR") dtoKey = "doctorDTO";
      else if (role === "ROLE_RECEPTIONIST") dtoKey = "receptionistDTO";
      // Add DTO JSON blob
      formDataToSend.append(
        dtoKey,
        new Blob([JSON.stringify(formData)], { type: "application/json" })
      );

      // Attach profile image if selected
      if (formData.profileImage instanceof File) {
        formDataToSend.append("profileImage", formData.profileImage);
      }

      // API call
      await axios.put(urls.updateUrl(userId), formDataToSend, {
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "multipart/form-data",
        },
      });

      setUserData(formData);
      setIsEditing(false);
      alert("Profile updated successfully!");
    } catch (err) {
      console.error("Error updating profile:", err);
      alert("Failed to update profile.");
    }
  };

  if (!userData)
    return <p className="text-center mt-10 text-gray-600">Loading...</p>;

  const role = getRoleFromToken(localStorage.getItem("jwtToken"));

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col w-full">
      <TopNavbar />
      <Navbar />

      <div className="flex flex-col lg:flex-row w-full h-[calc(100vh-6rem)] mt-0 bg-white shadow-lg overflow-hidden">
        {/* Left Sidebar */}
        <div className="lg:w-1/3 h-full bg-gradient-to-b from-blue-600 to-blue-400 text-white p-8 flex flex-col items-center justify-center">
          {/* Wrap the image and button inside a relative container */}
            <div className="relative">
              <img
                src="https://cdn-icons-png.flaticon.com/512/3135/3135715.png"
                alt="Profile"
                className="h-44 w-44 lg:h-52 lg:w-52 rounded-full border-4 border-white shadow-lg mb-6 object-cover transition-transform duration-300 hover:scale-105"
              />
              {/* Pencil icon overlay for editing */}
              <button
                className="absolute bottom-3 right-3 bg-white text-blue-600 p-2 rounded-full shadow-md opacity-90 hover:opacity-100 hover:scale-105 transition"
                onClick={() => alert('Edit profile image feature coming soon!')}
              >
                <Pencil size={18} />
              </button>
            </div>
          <h2 className="text-2xl font-semibold">{userData.name || userData.username || userData.patientName || "User"}</h2>
          <p className="text-blue-100 text-sm mt-2 uppercase">{role}</p>

          <button
            onClick={onClose || (() => navigate(-1))}
            className="mt-6 bg-white text-blue-600 px-5 py-2 rounded-full font-semibold hover:bg-blue-50 transition"
          >
            Close
          </button>
        </div>

        {/* Right Section */}
        <div className="lg:w-2/3 h-full p-8 flex flex-col gap-10 overflow-y-auto">
          {/* Top Section - Profile Info */}
          <section className="bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF] rounded-2xl p-6 shadow-2xl transition-transform transform hover:scale-[1.01] duration-300">
            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              Profile Information
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {getFieldsForRole(role).map((field) => (
                <div key={field.key} className="flex flex-col">
                  <label className="text-gray-600 font-medium mb-1">
                    {field.label}
                  </label>
                  <input
                    type="text"
                    name={field.key}
                    value={formData[field.key] || ""}
                    readOnly={!isEditing || field.key === "patientId" || field.key === "doctorId" || field.key === "receptionistId"}
                    onChange={handleChange}
                    className={`p-2 border rounded-lg ${
                      !isEditing || field.key === "patientId" || field.key === "doctorId" || field.key === "receptionistId"
                        ? "border-gray-200 bg-gray-100 cursor-not-allowed"
                        : "border-blue-400 bg-white"
                    }`}
                  />
                </div>
              ))}
            </div>

            <div className="flex justify-end gap-3 mt-6">
              {!isEditing ? (
                <button
                  onClick={handleEditToggle}
                  className="bg-blue-600 text-white px-6 py-2 rounded-lg hover:bg-blue-700 transition"
                >
                  Edit Profile
                </button>
              ) : (
                <button
                  onClick={handleSave}
                  className="bg-green-600 text-white px-6 py-2 rounded-lg hover:bg-green-700 transition"
                >
                  Save Changes
                </button>
              )}
            </div>
          </section>

          {/* Bottom Section - Appointment Summary */}
          <section className="bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF] rounded-2xl p-6 shadow-2xl transition-transform transform hover:scale-[1.01] duration-300">
            {/* Header with title + button in one line */}
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-2xl font-semibold text-gray-800">
                Appointment Summary
              </h3>
              <button
                onClick={() => navigate("/appointment-details")}
                className="text-sm font-semibold text-blue-700 border border-blue-600 px-4 py-2 rounded-lg hover:bg-blue-600 hover:text-white transition-all duration-300"
              >
                View All
              </button>
            </div>

            {appointments.length > 0 ? (
              <div className="grid md:grid-cols-3 gap-6">
                <div className="bg-white p-4 rounded-lg shadow text-center">
                  <h4 className="text-lg font-semibold text-blue-600">
                    Total Appointments
                  </h4>
                  <p className="text-2xl font-bold">{appointments.length}</p>
                </div>

                <div className="bg-white p-4 rounded-lg shadow text-center">
                  <h4 className="text-lg font-semibold text-green-600">
                    Completed
                  </h4>
                  <p className="text-2xl font-bold">
                    {appointments.filter((a) => a.status?.toLowerCase() === "completed").length}
                  </p>
                </div>

                <div className="bg-white p-4 rounded-lg shadow text-center">
                  <h4 className="text-lg font-semibold text-yellow-600">
                    Upcoming
                  </h4>
                  <p className="text-2xl font-bold">
                    {appointments.filter((a) => a.status?.toLowerCase() === "upcoming").length}
                  </p>
                </div>
              </div>
            ) : (
              <p className="text-gray-600">No appointments found.</p>
            )}
          </section>
        </div>
      </div>
    </div>
  );
}
