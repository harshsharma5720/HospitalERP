import React, { useEffect, useState } from "react";
import axios from "axios";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import { getRoleFromToken, getUserIdFromToken } from "./utils/jwtUtils";
import { useNavigate } from "react-router-dom";

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
          appointmentUrl: `http://localhost:8080/api/appointment/getPatientAppointments`,
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

  const fetchUserData = async (role, id, token) => {
    try {
      const urls = getRoleEndpoints(role);
      if (!urls) return;
      const response = await axios.get(urls.getUrl(id), {
        headers: { Authorization: `Bearer ${token}` },
      });
      setUserData(response.data);
      setFormData(response.data);
    } catch (err) {
      console.error("❌ Error fetching user data:", err);
    }
  };

  const fetchAppointments = async (role, id, token) => {
    try {
      const urls = getRoleEndpoints(role);
      if (!urls) return;
      const response = await axios.get(urls.appointmentUrl, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setAppointments(response.data);
    } catch (err) {
      console.error("❌ Error fetching appointments:", err);
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
      await axios.put(urls.updateUrl(userId), formData, {
        headers: { Authorization: `Bearer ${token}` },
      });
      setUserData(formData);
      setIsEditing(false);
      alert(" Profile updated successfully!");
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
          <img
            src="https://cdn-icons-png.flaticon.com/512/3135/3135715.png"
            alt="Profile"
            className="h-44 w-44 lg:h-52 lg:w-52 rounded-full border-4 border-white shadow-lg mb-6 object-cover transition-transform duration-300 hover:scale-105"
          />
          <h2 className="text-2xl font-semibold">{userData.name}</h2>
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
          <section className="bg-gray-100 rounded-xl p-6 shadow-inner">
            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              Profile Information
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {[
                { label: "Name", key: "name" },
                { label: "Email", key: "email" },
                { label: "Phone Number", key: "phoneNumber" },
                { label: "Gender", key: "gender" },
                { label: "DOB", key: "dob" },
                { label: "Address", key: "address" },
              ].map((field) => (
                <div key={field.key} className="flex flex-col">
                  <label className="text-gray-600 font-medium mb-1">
                    {field.label}
                  </label>
                  <input
                    type="text"
                    name={field.key}
                    value={formData[field.key] || ""}
                    readOnly={!isEditing}
                    onChange={handleChange}
                    className={`p-2 border rounded-lg ${
                      isEditing
                        ? "border-blue-400 bg-white"
                        : "border-gray-200 bg-gray-100"
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
          <section className="bg-gray-100 rounded-xl p-6 shadow-inner">
            <h3 className="text-2xl font-semibold text-gray-800 mb-4">
              Appointment Summary
            </h3>

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
                    {
                      appointments.filter(
                        (a) => a.status?.toLowerCase() === "completed"
                      ).length
                    }
                  </p>
                </div>
                <div className="bg-white p-4 rounded-lg shadow text-center">
                  <h4 className="text-lg font-semibold text-yellow-600">
                    Upcoming
                  </h4>
                  <p className="text-2xl font-bold">
                    {
                      appointments.filter(
                        (a) => a.status?.toLowerCase() === "upcoming"
                      ).length
                    }
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
