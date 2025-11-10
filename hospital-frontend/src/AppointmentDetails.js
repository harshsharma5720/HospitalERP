import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";

export default function AppointmentDetails() {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  // ✅ Fetch all appointments for logged-in patient using token
  useEffect(() => {
    const fetchAppointments = async () => {
      const token = localStorage.getItem("jwtToken");
      if (!token) {
        alert("Please login first!");
        navigate("/login");
        return;
      }

      try {
        const response = await fetch(
          "http://localhost:8080/appointment/getPatientAppointments",
          {
            method: "GET",
            headers: { Authorization: `Bearer ${token}` },
          }
        );

        if (!response.ok) throw new Error("Failed to fetch appointments");

        const data = await response.json();
        setAppointments(data.appointments || data);
      } catch (err) {
        console.error("Error fetching appointments:", err);
        alert("Could not load your appointments. Please try again later.");
      } finally {
        setLoading(false);
      }
    };

    fetchAppointments();
  }, [navigate]);

  // ✅ Helper: check if appointment date has passed
  const isPastAppointment = (dateStr) => {
    const appointmentDate = new Date(dateStr);
    const today = new Date();
    return appointmentDate < today;
  };

  // ✅ Cancel appointment
  const handleCancel = async (appointmentID) => {
    const confirmCancel = window.confirm(
      "Are you sure you want to cancel this appointment?"
    );
    if (!confirmCancel) return;

    try {
      const token = localStorage.getItem("jwtToken");
      const response = await fetch(
        `http://localhost:8080/appointment/CancelAppointment/${appointmentID}`,
        {
          method: "DELETE",
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
      console.error("Error cancelling appointment:", err);
      alert("Failed to cancel appointment. Please try again.");
    }
  };

  // ✅ Edit appointment (future)
  const handleEdit = (appointment) => {
    localStorage.setItem("editAppointment", JSON.stringify(appointment));
    navigate("/edit-appointment");
  };

  // ✅ Reschedule appointment (past)
  const handleReschedule = (appointment) => {
    const confirmReschedule = window.confirm(
      "Do you want to reschedule this past appointment?"
    );
    if (!confirmReschedule) return;

    // Prefer passing via location.state (no localStorage required)
    navigate("/appointments", { state: { rescheduleAppointment: appointment } });
  };


  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100">
        <p className="text-lg font-medium text-gray-600">
          Loading appointments...
        </p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 relative">
      <TopNavbar />
      <Navbar />

      <div className="flex flex-col items-center py-12 px-4 relative">
        <img
          src="Shreyahospital.jpg"
          alt="Hospital Background"
          className="absolute top-0 left-0 w-full h-full object-cover opacity-20 -z-10"
        />

        <div className="bg-white bg-opacity-90 backdrop-blur-md shadow-2xl rounded-2xl p-8 w-full max-w-5xl relative z-10">
          <h2 className="text-3xl font-bold text-center text-[#1E63DB] mb-6">
            Your Appointments
          </h2>

          {appointments.length === 0 ? (
            <p className="text-center text-gray-600">
              No appointments found. Book one below!
            </p>
          ) : (
            <div className="grid md:grid-cols-2 gap-6">
              {appointments.map((appointment) => {
                const past = isPastAppointment(appointment.date);
                return (
                  <div
                    key={appointment.appointmentID}
                    className="border border-gray-200 p-6 rounded-xl shadow hover:shadow-lg bg-white transition-all"
                  >
                    <h3 className="text-lg font-semibold text-[#1E63DB] mb-2">
                      Appointment ID: {appointment.appointmentID}
                    </h3>
                    <p>
                      <strong>Patient Name:</strong> {appointment.patientName}
                    </p>
                    <p>
                      <strong>Doctor:</strong> {appointment.doctorName || "N/A"}
                    </p>
                    <p>
                      <strong>Date:</strong> {appointment.date}
                    </p>
                    <p>
                      <strong>Shift:</strong> {appointment.shift}
                    </p>
                    <p>
                      <strong>Slot:</strong> {appointment.slotId || "N/A"}
                    </p>
                    <p>
                      <strong>Message:</strong>{" "}
                      {appointment.message || "No message"}
                    </p>

                    <div className="flex flex-col gap-3 mt-4">
                      {past ? (
                        <>
                          {/* ✅ Theme color applied to Reschedule */}
                          <button
                            onClick={() => handleReschedule(appointment)}
                            className="w-full bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white py-2 rounded-lg font-semibold hover:opacity-90 transition-all"
                          >
                            Reschedule Appointment
                          </button>
                          <button
                            onClick={() =>
                              handleCancel(appointment.appointmentID)
                            }
                            className="w-full bg-gradient-to-br from-gray-600 to-gray-800 text-white py-2 rounded-lg font-semibold hover:opacity-90 transition-all"
                          >
                            Delete Appointment
                          </button>
                        </>
                      ) : (
                        <>
                          <button
                            onClick={() => handleEdit(appointment)}
                            className="w-full bg-gradient-to-br from-green-500 to-emerald-700 text-white py-2 rounded-lg font-semibold hover:opacity-90 transition-all"
                          >
                            Edit Appointment
                          </button>
                          <button
                            onClick={() =>
                              handleCancel(appointment.appointmentID)
                            }
                            className="w-full bg-gradient-to-br from-red-600 to-red-800 text-white py-2 rounded-lg font-semibold hover:opacity-90 transition-all"
                          >
                            Cancel Appointment
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          <button
            onClick={() => navigate("/appointments")}
            className="mt-8 w-full bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white py-3 rounded-xl font-semibold hover:opacity-90 transition-all duration-300"
          >
            Book New Appointment
          </button>
        </div>
      </div>
    </div>
  );
}
