import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";

export default function AppointmentDetails() {
  const [appointment, setAppointment] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const savedData = localStorage.getItem("appointmentData");
    if (savedData) {
      setAppointment(JSON.parse(savedData));
    } else {
      alert("No appointment booked yet!");
      navigate("/appointment");
    }
  }, [navigate]);

  if (!appointment) return null;

  return (
    <div className="min-h-screen bg-gray-50 relative">
      <TopNavbar />
      <Navbar />

      <div className="flex justify-center items-center py-12 px-4 relative">
        <img
          src="Shreyahospital.jpg"
          alt="Hospital Background"
          className="absolute top-0 left-0 w-full h-full object-cover opacity-20 -z-10"
        />

        <div className="bg-white bg-opacity-90 backdrop-blur-md shadow-2xl rounded-2xl p-8 w-full max-w-2xl relative z-10">
          <h2 className="text-3xl font-bold text-center text-[#1E63DB] mb-6">
            Your Appointment Details
          </h2>

          <div className="space-y-4 text-gray-700">
            <p><strong>Patient Name:</strong> {appointment.patientName}</p>
            <p><strong>Gender:</strong> {appointment.gender}</p>
            <p><strong>Age:</strong> {appointment.age}</p>
            <p><strong>Doctor Name:</strong> {appointment.doctorName}</p>
            <p><strong>Shift:</strong> {appointment.shift}</p>
            <p><strong>Date:</strong> {appointment.date}</p>
            <p><strong>Message:</strong> {appointment.message || "N/A"}</p>
            <p><strong>Patient Info ID:</strong> {appointment.ptInfoId || "N/A"}</p>
          </div>

         <button
           onClick={() => navigate("/appointments")}
           className="mt-8 w-full bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white py-3 rounded-xl hover:opacity-90 transition-all duration-300"
         >
           Book New Appointment
         </button>

        </div>
      </div>
    </div>
  );
}
