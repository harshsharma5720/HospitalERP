import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import { getRoleFromToken, getUserIdFromToken } from "./utils/jwtUtils";

export default function DoctorAppointments() {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();
  const [userId, setUserId] = useState(null);
  const [role, setRole] = useState(null);
  const [viewType, setViewType] = useState("pending"); // pending or completed

  useEffect(() => {
    const fetchAppointments = async () => {
      const token = localStorage.getItem("jwtToken");
      const decodedRole = getRoleFromToken(token);
      setRole(decodedRole);

      const decodedUserId = getUserIdFromToken(token);
      setUserId(decodedUserId);

      if (!token || !decodedRole?.toUpperCase().includes("DOCTOR")) {
        alert("Unauthorized Access! Login as a doctor.");
        navigate("/login");
        return;
      }

      setLoading(true);
      try {
        const endpoint =
          viewType === "pending"
            ? `http://localhost:8080/api/doctor/doctorPendingAppointments/${decodedUserId}`
            : `http://localhost:8080/api/doctor/doctorCompletedAppointments/${decodedUserId}`;

        const response = await fetch(endpoint, {
          method: "GET",
          headers: { Authorization: `Bearer ${token}` },
        });

        if (!response.ok) throw new Error("Failed to fetch appointments");
        const data = await response.json();
        setAppointments(data);
      } catch (err) {
        console.error("Error:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchAppointments();
  }, [navigate, viewType]);


  // Mark appointment as completed
  const handleMarkCompleted = async (appointmentID) => {
    const confirmDone = window.confirm("Mark this appointment as completed?");
    if (!confirmDone) return;

    try {
      const token = localStorage.getItem("jwtToken");

      const response = await fetch(
        `http://localhost:8080/api/doctor/complete/${appointmentID}`,
        {
          method: "PUT",
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      const result = await response.text();
      alert(result);

      if (response.ok) {
        setAppointments((prev) =>
          prev.filter((a) => a.appointmentID !== appointmentID)
        );
      }
    } catch (err) {
      console.error("Error completing appointment:", err);
      alert("Failed to update appointment status.");
    }
  };

  if (loading) {
    return <div className="text-center mt-20">Loading appointments...</div>;
  }

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-[#0a1124]">
      <TopNavbar />
      <Navbar />

      <div className="max-w-5xl animate-scaleUp mx-auto mt-10 p-6 bg-white dark:bg-[#111a3b] rounded-xl shadow-xl">
        <h2 className="text-5xl text-center font-extrabold mb-4">
           Doctor {" "}
           <span className="bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
              Appointment
           </span>
        </h2>

        <div className="flex justify-center gap-4 mb-6">
          <button
            onClick={() => setViewType("pending")}
            className={`px-6 py-2 rounded-lg font-semibold ${
              viewType === "pending"
                ? "bg-blue-600 text-white"
                : "bg-gray-300 dark:bg-[#2c3558] dark:text-white"
            }`}
          >
            Pending Appointments
          </button>

          <button
            onClick={() => setViewType("completed")}
            className={`px-6 py-2 rounded-lg font-semibold ${
              viewType === "completed"
                ? "bg-green-600 text-white"
                : "bg-gray-300 dark:bg-[#2c3558] dark:text-white"
            }`}
          >
            Completed Appointments
          </button>
        </div>


        {appointments.length === 0 ? (
          <p className="text-center text-gray-600 dark:text-gray-300">
            No appointments assigned.
          </p>
        ) : (
          <div className="grid md:grid-cols-2 gap-6">
            {appointments.map((appt) => (
              <div
                key={appt.appointmentID}
                className="p-5 bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF]
                dark:from-[#111a3b] dark:to-[#0f172a] rounded-xl shadow-xl"
              >
                <p><strong>Appointment ID:</strong> {appt.appointmentID}</p>
                <p><strong>Patient Name:</strong> {appt.patientName}</p>
                <p><strong>Date:</strong> {appt.date}</p>
                <p><strong>Shift:</strong> {appt.shift}</p>
                <p><strong>Slot:</strong> {appt.slotId}</p>
                <p><strong>Message:</strong> {appt.message}</p>

                {viewType === "pending" && (
                  <button
                    onClick={() => handleMarkCompleted(appt.appointmentID)}
                    className="mt-4 w-full bg-green-600 hover:bg-green-700 text-white py-2 rounded-lg"
                  >
                    Mark as Completed
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
