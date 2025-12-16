import React, { useState } from "react";
import axios from "axios";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";

export default function ReceptionistDashboard() {
  const [doctorName, setDoctorName] = useState("");
  const [doctorId, setDoctorId] = useState("");
  const [appointments, setAppointments] = useState([]);
  const [pendingAppointments, setPendingAppointments] = useState([]);
  const [completedAppointments, setCompletedAppointments] = useState([]);
  const token = localStorage.getItem("jwtToken");

  // Fetch all appointments
  const fetchAllAppointments = async () => {
    try {
      const res = await axios.get("http://localhost:8080/api/receptionist/getAppointments", {
        headers: { Authorization: `Bearer ${token}` }
      });
      setAppointments(res.data);
    } catch (err) {
      console.error("Failed to fetch appointments", err);
    }
  };

  // Fetch appointments by doctor name
  const fetchByDoctorName = async () => {
    if (!doctorName.trim()) {
      alert("Enter doctor name");
      return;
    }

    try {
      const res = await axios.get(
        `http://localhost:8080/api/receptionist/getAppointmentByDoctor/${doctorName}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setAppointments(res.data);
    } catch (err) {
      console.error("Error fetching doctor appointments", err);
    }
  };

  // Fetch pending & completed appointments by doctor userId
  const fetchByDoctorId = async () => {
    if (!doctorId.trim()) {
      alert("Enter doctor userId");
      return;
    }

    try {
      const pendingRes = await axios.get(
        `http://localhost:8080/api/receptionist/doctorPendingAppointments/${doctorId}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      const completedRes = await axios.get(
        `http://localhost:8080/api/receptionist/doctorCompletedAppointments/${doctorId}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      setPendingAppointments(pendingRes.data);
      setCompletedAppointments(completedRes.data);

    } catch (err) {
      console.error("Error fetching by doctor ID", err);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-[#0a1124]">
      <TopNavbar />
      <Navbar />
      <h1 className="text-3xl font-bold mb-6">Receptionist Dashboard</h1>

      {/* Search by Doctor Name */}
      <div className="bg-white shadow rounded p-4 mb-6">
        <h2 className="text-xl font-semibold mb-2">Search Appointments by Doctor Name</h2>

        <input
          type="text"
          placeholder="Enter doctor name"
          className="border p-2 rounded w-full mb-3"
          onChange={(e) => setDoctorName(e.target.value)}
        />

        <button
          onClick={fetchByDoctorName}
          className="bg-blue-600 text-white px-4 py-2 rounded"
        >
          Search
        </button>
      </div>

      {/* Search by Doctor userId */}
      <div className="bg-white shadow rounded p-4 mb-6">
        <h2 className="text-xl font-semibold mb-3">Search Pending / Completed Appointments by Doctor ID</h2>

        <input
          type="text"
          placeholder="Enter doctor userId"
          className="border p-2 rounded w-full mb-3"
          onChange={(e) => setDoctorId(e.target.value)}
        />

        <button
          onClick={fetchByDoctorId}
          className="bg-green-600 text-white px-4 py-2 rounded"
        >
          Search
        </button>
      </div>

      {/* Pending Appointments */}
      {pendingAppointments.length > 0 && (
        <div className="bg-yellow-100 shadow rounded p-4 mb-6">
          <h2 className="text-xl font-semibold">Pending Appointments</h2>
          {pendingAppointments.map((appt) => (
            <div key={appt.appointmentID} className="border p-3 rounded mt-2">
              <p><strong>Patient:</strong> {appt.patientName}</p>
              <p><strong>Doctor:</strong> {appt.doctorName}</p>
              <p><strong>Date:</strong> {appt.date}</p>
              <p><strong>Status:</strong> {appt.status}</p>
            </div>
          ))}
        </div>
      )}

      {/* Completed Appointments */}
      {completedAppointments.length > 0 && (
        <div className="bg-green-100 shadow rounded p-4 mb-6">
          <h2 className="text-xl font-semibold">Completed Appointments</h2>
          {completedAppointments.map((appt) => (
            <div key={appt.appointmentID} className="border p-3 rounded mt-2">
              <p><strong>Patient:</strong> {appt.patientName}</p>
              <p><strong>Doctor:</strong> {appt.doctorName}</p>
              <p><strong>Date:</strong> {appt.date}</p>
              <p><strong>Status:</strong> {appt.status}</p>
            </div>
          ))}
        </div>
      )}

      {/* All Appointments */}
      <div className="bg-white shadow rounded p-4">
        <h2 className="text-xl font-semibold mb-3">All Appointments</h2>

        <button
          onClick={fetchAllAppointments}
          className="bg-purple-600 text-white px-4 py-2 rounded mb-3"
        >
          Load All Appointments
        </button>

        {appointments.map((appt) => (
          <div key={appt.appointmentID} className="border p-3 rounded mt-2">
            <p><strong>Patient:</strong> {appt.patientName}</p>
            <p><strong>Doctor:</strong> {appt.doctorName}</p>
            <p><strong>Date:</strong> {appt.date}</p>
            <p><strong>Status:</strong> {appt.status}</p>
          </div>
        ))}
      </div>
    </div>
  );
}
