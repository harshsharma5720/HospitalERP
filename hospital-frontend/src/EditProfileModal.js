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
  const [relatives, setRelatives] = useState([]);
  const [completedAppointment, setCompletedAppointment] = useState([]);
  const [pendingAppointment, setPendingAppointment] = useState([]);


  useEffect(() => {
    const token = localStorage.getItem("jwtToken");
    if (!token) return;

    const role = getRoleFromToken(token);
    const userId = getUserIdFromToken(token);
    fetchUserData(role, userId, token);
    fetchAppointments(role, userId, token);
    fetchCompletedAppointments(role, userId , token);
    fetchPendingAppointments(role, userId , token);
  }, []);
  useEffect(() => {
    const token = localStorage.getItem("jwtToken");

    if (userData?.patientId && token) {
      // Fetch relatives when patient data becomes available
      fetchRelatives(userData.patientId, token);
    }
  }, [userData]);

  const getRoleEndpoints = (role) => {
    switch (role) {
      case "ROLE_PATIENT":
        return {
          getUrl: (id) => `http://localhost:8080/api/patient/getAccount/${id}`,
          updateUrl: (id) => `http://localhost:8080/api/patient/updateAccount/${id}`,
          appointmentUrl: `http://localhost:8080/appointment/getPatientAppointments`,
          completedAppointmentUrl: (id) => `http://localhost:8080/appointment/patientCompletedAppointments/${id}`,
          pendingAppointmentUrl: (id) => `http://localhost:8080/appointment/patientPendingAppointments/${id}`,
        };
      case "ROLE_DOCTOR":
        return {
          getUrl: (id) => `http://localhost:8080/api/doctor/get/${id}`,
          updateUrl: (id) => `http://localhost:8080/api/doctor/update/${id}`,
          appointmentUrl: `http://localhost:8080/appointment/getDoctorAppointments`,
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

  const getFieldsForRole = (role) => {
    switch (role) {
      case "ROLE_DOCTOR":
        return [
          { label: "Name", key: "name", readOnly: true },
          { label: "Username", key: "userName" },
          { label: "Email", key: "email" },
          { label: "Phone Number", key: "phoneNumber" },
          { label: "Specialization", key: "specialist" },
        ];

      case "ROLE_PATIENT":
        return [
          { label: "Patient ID", key: "patientId", readOnly: true },
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
        return [{ label: "Name", key: "name" }, { label: "Email", key: "email" }];
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

      if (data.patientName && !data.name) data.name = data.patientName;
      else if (data.doctorName && !data.name) data.name = data.doctorName;
      else if (data.receptionistName && !data.name) data.name = data.receptionistName;

      setUserData(data);
      setFormData(data);
    } catch (err) {
      console.error("Error fetching user data:", err);
    }
  };

  const fetchRelatives = async (patientId, token) => {
    try {
      const response = await axios.get(
        `http://localhost:8080/api/patient/relative/patient/${patientId}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      setRelatives(response.data);
    } catch (err) {
      console.error("Error fetching relatives:", err);
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

  const fetchCompletedAppointments = async (role, id, token) => {
    try {
      const urls = getRoleEndpoints(role);
      if (!urls || !urls.completedAppointmentUrl) return;

      const response = await axios.get(urls.completedAppointmentUrl(id), {
        headers: { Authorization: `Bearer ${token}` },
      });

      const data = response.data.appointments || response.data;
      setCompletedAppointment(data);
    } catch (err) {
      console.error("Error fetching completed appointments:", err);
    }
  }

  const fetchPendingAppointments = async (role, id, token) => {
      try {
        const urls = getRoleEndpoints(role);
        if (!urls || !urls.pendingAppointmentUrl) return;

        const response = await axios.get(urls.pendingAppointmentUrl(id), {
          headers: { Authorization: `Bearer ${token}` },
        });

        const data = response.data.appointments || response.data;
        setPendingAppointment(data);
      } catch (err) {
        console.error("Error fetching completed appointments:", err);
      }
    }

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
      let dtoKey = "";

      if (role === "ROLE_PATIENT") dtoKey = "ptInfoDTO";
      else if (role === "ROLE_DOCTOR") dtoKey = "doctorDTO";
      else if (role === "ROLE_RECEPTIONIST") dtoKey = "receptionistDTO";

      formDataToSend.append(dtoKey, new Blob([JSON.stringify(formData)], { type: "application/json" }));

      if (formData.profileImage instanceof File) {
        formDataToSend.append("profileImage", formData.profileImage);
      }

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
    return <p className="text-center mt-10 text-gray-600 dark:text-gray-200">Loading...</p>;

  const role = getRoleFromToken(localStorage.getItem("jwtToken"));

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-[#0a1124] flex flex-col w-full">
      <TopNavbar />
      <Navbar />

      <div className="flex flex-col animate-scaleUp lg:flex-row w-full h-[calc(100vh-6rem)] bg-white dark:bg-[#0f172a] shadow-lg overflow-hidden">

        {/* LEFT SIDE */}
        <div className="lg:w-1/3 h-full bg-gradient-to-b from-blue-600 to-blue-400 dark:from-[#111a3b] dark:to-[#0f172a] text-white p-8 flex flex-col items-center justify-center">

          <div className="relative">
            <img
              src={
                formData.profileImage instanceof File
                  ? URL.createObjectURL(formData.profileImage)
                  : formData.profileImage
                  ? `http://localhost:8080${formData.profileImage}`
                  : "https://cdn-icons-png.flaticon.com/512/3135/3135715.png"
              }
              alt="Profile"
              className="h-44 w-44 lg:h-52 lg:w-52 rounded-full border-4 border-white shadow-xl object-cover"
            />
            <button
              className="absolute bottom-3 right-3 bg-white text-blue-600 dark:bg-[#50d4f2] dark:text-black p-2 rounded-full shadow-md"
              onClick={() => document.getElementById("profileImageInput").click()}
            >
              <Pencil size={18} />
            </button>

            <input
              type="file"
              id="profileImageInput"
              accept="image/*"
              className="hidden"
              onChange={(e) =>
                setFormData({ ...formData, profileImage: e.target.files[0] })
              }
            />
          </div>

          <h2 className="text-2xl font-semibold dark:text-[#50d4f2]">
            {userData.name || userData.username}
          </h2>

          <p className="text-blue-100 dark:text-[#63e6ff] text-sm mt-2 uppercase">{role}</p>

          <button
            onClick={onClose || (() => navigate(-1))}
            className="mt-6 bg-white dark:bg-[#50d4f2] text-blue-600 dark:text-black px-5 py-2 rounded-full font-semibold hover:opacity-90"
          >
            Close
          </button>
        </div>

        {/* RIGHT SIDE */}
        <div className="lg:w-2/3 h-full p-8 flex flex-col gap-10 overflow-y-auto">

          {/* PROFILE INFO */}
          <section className="bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF] dark:from-[#111a3b] dark:to-[#0f172a] rounded-2xl p-6 shadow-2xl">

            <h3 className="text-2xl font-semibold text-gray-800 dark:text-[#50d4f2] mb-4">Profile Information</h3>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {getFieldsForRole(role).map((field) => (
                <div key={field.key}>
                  <label className="text-gray-600 dark:text-gray-300 font-medium mb-1">
                    {field.label}
                  </label>

                  <input
                    type="text"
                    name={field.key}
                    value={formData[field.key] || ""}
                    readOnly={!isEditing || field.readOnly}
                    onChange={handleChange}
                    className={`
                      p-2 border rounded-lg w-full
                      ${!isEditing || field.readOnly
                        ? "bg-gray-100 dark:bg-[#1e293b] border-gray-300 dark:border-[#233565] text-gray-600 dark:text-gray-400"
                        : "bg-white dark:bg-[#0a1124] border-blue-400 dark:border-[#50d4f2] text-black dark:text-[#63e6ff]"
                      }
                    `}
                  />
                </div>
              ))}
            </div>

            <div className="flex justify-end gap-3 mt-6">
              {!isEditing ? (
                <button
                  onClick={handleEditToggle}
                  className="bg-blue-600 dark:bg-[#1E63DB] text-white px-6 py-2 rounded-lg hover:opacity-90"
                >
                  Edit Profile
                </button>
              ) : (
                <button
                  onClick={handleSave}
                  className="bg-green-600 text-white px-6 py-2 rounded-lg hover:bg-green-700"
                >
                  Save Changes
                </button>
              )}
            </div>
          </section>
          {(role === "ROLE_DOCTOR" || role === "ROLE_RECEPTIONIST") && (
            <div className="flex justify-end gap-3 mt-4">
              <button
                onClick={() => navigate("/leave-management")}
                className="bg-cyan-600 hover:bg-cyan-700 text-white px-6 py-2 rounded-lg font-semibold shadow-md"
              >
                Manage Leaves
              </button>
            </div>
          )}

          <section className="bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF] dark:from-[#111a3b] dark:to-[#0f172a] rounded-2xl p-6 shadow-2xl">
              <div className="flex justify-between items-center mb-3">
                <h2 className="text-xl font-bold">Your Relatives</h2>
                <div className="flex gap-4 mt-4">
                  <button
                    onClick={() => navigate("/add-relative", { state: { patientId: userData.patientId } })}
                    className="text-sm font-semibold px-4 py-2 rounded-lg
                               bg-blue-600 text-white
                               border border-blue-600
                               hover:bg-blue-700 transition"
                  >
                    Add Relative
                  </button>

                  <button
                    onClick={() => navigate("/relatives", { state: { patientId: userData.patientId } })}
                    className="text-sm font-semibold px-4 py-2 rounded-lg
                               border border-blue-600
                               text-blue-700 hover:bg-blue-600 hover:text-white transition"
                  >
                    View All
                  </button>
                </div>

              </div>

              {relatives && relatives.length > 0 ? (
                <ul className="space-y-2">
                  {relatives.map((relative) => (
                    <li
                      key={relative.id}
                      className="p-3 bg-gray-50 rounded-lg shadow-sm border dark:bg-[#1e293b]"
                    >
                      <p className="font-semibold">{relative.name}</p>
                      <p className="text-sm text-gray-600">
                        {relative.relationship} • {relative.gender}
                      </p>
                    </li>
                  ))}
                </ul>
              ) : (
                <p className="text-gray-500">No relatives added yet.</p>
              )}
          </section>

          {/* APPOINTMENT SUMMARY */}
          <section className="bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF] dark:from-[#111a3b] dark:to-[#0f172a] rounded-2xl p-6 shadow-2xl">

            <div className="flex items-center justify-between mb-4">
              <h3 className="text-2xl font-semibold text-gray-800 dark:text-[#50d4f2]">
                Appointment Summary
              </h3>

              <div className="flex items-center gap-3">
                <button
                  onClick={() => navigate("/appointments")}
                  className="text-sm font-semibold px-4 py-2 rounded-lg
                             bg-blue-600 text-white
                             border border-blue-600
                             hover:bg-blue-700 transition"
                >
                  Book One
                </button>

                <button
                  onClick={() => navigate("/appointment-details")}
                  className="text-sm font-semibold px-4 py-2 rounded-lg
                             border border-blue-600
                             text-blue-700 hover:bg-blue-600 hover:text-white transition"
                >
                  View All
                </button>
              </div>

            </div>

            {appointments.length > 0 ? (
              <div className="grid md:grid-cols-3 gap-6">
                <div className="bg-white dark:bg-[#1e293b] p-4 rounded-lg shadow text-center">
                  <h4 className="text-lg font-semibold text-blue-600 dark:text-[#50d4f2]">
                    Total Appointments
                  </h4>
                  <p className="text-2xl font-bold dark:text-white">{appointments.length}</p>
                </div>

                <div className="bg-white dark:bg-[#1e293b] p-4 rounded-lg shadow text-center">
                  <h4 className="text-lg font-semibold text-green-600 dark:text-green-400">
                    Completed
                  </h4>
                  <p className="text-2xl font-bold dark:text-white">
                    {completedAppointment.length}
                  </p>
                </div>

                <div className="bg-white dark:bg-[#1e293b] p-4 rounded-lg shadow text-center">
                  <h4 className="text-lg font-semibold text-yellow-600 dark:text-yellow-300">
                    Upcoming
                  </h4>
                  <p className="text-2xl font-bold dark:text-white">
                    {pendingAppointment.length}
                  </p>
                </div>
              </div>
            ) : (
              <p className="text-gray-600 dark:text-gray-300">No appointments found.</p>
            )}
          </section>

        </div>
      </div>
    </div>
  );
}
