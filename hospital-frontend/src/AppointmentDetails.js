import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import axios from "axios";
import { API_BASE_URL } from "./config";
import { toLocalISODate } from "./utils/dateUtils";
import { getErrorMessage } from "./utils/apiError";
import { downloadPrescription } from "./utils/downloadPrescription";

const STATUS_LABELS = {
  SCHEDULED: "Scheduled",
  CONFIRMED: "Confirmed",
  COMPLETED: "Completed",
  CANCELLED_BY_DOCTOR: "Cancelled (doctor on leave)",
  CANCELLED_BY_PATIENT: "Cancelled",
};

const STATUS_STYLES = {
  SCHEDULED: "bg-blue-100 text-blue-800",
  CONFIRMED: "bg-blue-100 text-blue-800",
  COMPLETED: "bg-green-100 text-green-800",
  CANCELLED_BY_DOCTOR: "bg-red-100 text-red-800",
  CANCELLED_BY_PATIENT: "bg-gray-200 text-gray-700",
};

const formatTime = (time) => (time ? String(time).slice(0, 5) : "");

export default function AppointmentDetails() {
  const [appointments, setAppointments] = useState([]);
  const [loading, setLoading] = useState(true);
  // Consultation notes by appointment id (only completed visits have one)
  const [consultations, setConsultations] = useState({});
  const navigate = useNavigate();

  useEffect(() => {
    const fetchAppointments = async () => {
      try {
        const response = await axios.get(`${API_BASE_URL}/appointment/getPatientAppointments`);
        // Upcoming first, then most recent history
        const sorted = [...response.data].sort((a, b) => {
          const aUpcoming = isUpcoming(a);
          const bUpcoming = isUpcoming(b);
          if (aUpcoming !== bUpcoming) return aUpcoming ? -1 : 1;
          return aUpcoming ? a.date.localeCompare(b.date) : b.date.localeCompare(a.date);
        });
        setAppointments(sorted);
        try {
          const history = await axios.get(`${API_BASE_URL}/api/consultations/my`);
          setConsultations(Object.fromEntries(history.data.map((c) => [c.appointmentId, c])));
        } catch {
          setConsultations({});
        }
      } catch (err) {
        alert(getErrorMessage(err, "Could not load your appointments. Please try again later."));
      } finally {
        setLoading(false);
      }
    };

    fetchAppointments();
  }, []);

  // Compares dates only, so an appointment later today is still upcoming
  const isUpcoming = (appointment) =>
    (appointment.status === "SCHEDULED" || appointment.status === "CONFIRMED") &&
    appointment.date >= toLocalISODate();

  const handleCancel = async (appointmentID) => {
    const confirmCancel = window.confirm(
      "Are you sure you want to cancel this appointment?"
    );
    if (!confirmCancel) return;

    try {
      const response = await axios.delete(`${API_BASE_URL}/appointment/CancelAppointment/${appointmentID}`);
      alert(response.data);
      // The appointment stays in the history with a "Cancelled" status
      setAppointments((prev) =>
        prev.map((a) => (a.appointmentID === appointmentID ? { ...a, status: "CANCELLED_BY_PATIENT" } : a))
      );
    } catch (err) {
      alert(getErrorMessage(err, "Failed to cancel appointment. Please try again."));
    }
  };

  const handleReschedule = (appointment) => {
    navigate("/appointments", { state: { rescheduleAppointment: appointment } });
  };

  const handleBookAgain = (appointment) => {
    navigate("/appointments", { state: { doctorName: appointment.doctorName, doctorId: appointment.doctorId } });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100 dark:bg-[#0a1124]">
        <p className="text-lg font-medium text-gray-600 dark:text-[#50d4f2]">
          Loading appointments...
        </p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-[#0a1124] relative">
      <TopNavbar />
      <Navbar />

      <div className="flex flex-col items-center py-12 px-4 relative">
        <img
          src="Shreyahospital.jpg"
          alt="Hospital Background"
          className="absolute top-0 left-0 w-full h-full object-cover opacity-20 -z-10"
        />

        <div className="
          bg-white bg-opacity-90
          dark:bg-[#111a3b]/70 dark:text-[#50d4f2]
          backdrop-blur-md shadow-2xl rounded-2xl
          p-8 w-full max-w-5xl relative z-10
        ">
          <h2 className="
            text-6xl md:text-7xl font-black tracking-tight leading-tight text-center
            text-black dark:text-[#50d4f2]
            mb-6
          ">
            Your{" "}
              <span
                 className="
                    bg-gradient-to-r from-blue-600 to-cyan-400
                    bg-clip-text text-transparent
                 "
              >
                Appointments
              </span>
          </h2>
          <div className="w-40 h-1 bg-gradient-to-r from-blue-600 to-cyan-400 dark:from-[#50d4f2] dark:to-[#63e6ff] mx-auto mb-10 rounded-full"></div>

          {appointments.length === 0 ? (
            <p className="text-center text-gray-600 dark:text-gray-300">
              No appointments found. Book one below!
            </p>
          ) : (
            <div className="grid md:grid-cols-2 gap-6">
              {appointments.map((appointment) => {
                const upcoming = isUpcoming(appointment);
                return (
                  <div
                    key={appointment.appointmentID}
                    className="
                      bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF]
                      dark:from-[#0f172a] dark:to-[#111a3b]
                      border border-gray-200 dark:border-[#233565]
                      p-6 rounded-2xl shadow-2xl
                      hover:shadow-3xl transition-transform
                      transform hover:scale-105 duration-300
                    "
                  >
                    <div className="flex items-start justify-between gap-2 mb-2">
                      <h3 className="
                        text-lg font-semibold
                        text-[#1E63DB] dark:text-[#50d4f2]
                      ">
                        Appointment ID: {appointment.appointmentID}
                      </h3>
                      <span className={`text-xs font-semibold px-2 py-1 rounded-full ${STATUS_STYLES[appointment.status] || "bg-gray-200 text-gray-700"}`}>
                        {STATUS_LABELS[appointment.status] || appointment.status}
                      </span>
                    </div>

                    <p><strong>Patient Name:</strong> {appointment.patientName}</p>
                    <p><strong>Doctor:</strong> {appointment.doctorName || "N/A"}</p>
                    <p><strong>Date:</strong> {appointment.date}</p>
                    <p><strong>Shift:</strong> {appointment.shift}</p>
                    <p>
                      <strong>Time:</strong>{" "}
                      {appointment.startTime
                        ? `${formatTime(appointment.startTime)} - ${formatTime(appointment.endTime)}`
                        : "N/A"}
                    </p>
                    <p>
                      <strong>Message:</strong>{" "}
                      {appointment.message || "No message"}
                    </p>

                    {consultations[appointment.appointmentID] && (
                      <ConsultationSummary consultation={consultations[appointment.appointmentID]} />
                    )}

                    <div className="flex flex-col gap-3 mt-4">
                      {upcoming ? (
                        <>
                          <button
                            onClick={() => handleReschedule(appointment)}
                            className="
                              w-full bg-gradient-to-br
                              from-green-500 to-emerald-700
                              text-white py-2 rounded-lg font-semibold
                              hover:opacity-90 transition-all
                            "
                          >
                            Reschedule Appointment
                          </button>
                          <button
                            onClick={() =>
                              handleCancel(appointment.appointmentID)
                            }
                            className="
                              w-full bg-gradient-to-br
                              from-red-600 to-red-800
                              text-white py-2 rounded-lg font-semibold
                              hover:opacity-90 transition-all
                            "
                          >
                            Cancel Appointment
                          </button>
                        </>
                      ) : (
                        <>
                        {consultations[appointment.appointmentID] && (
                          <button
                            onClick={() => downloadPrescription(appointment.appointmentID)}
                            className="
                              w-full bg-gradient-to-br
                              from-green-500 to-emerald-700
                              text-white py-2 rounded-lg font-semibold
                              hover:opacity-90 transition-all
                            "
                          >
                            Download Prescription
                          </button>
                        )}
                        <button
                          onClick={() => handleBookAgain(appointment)}
                          className="
                            w-full bg-gradient-to-br
                            from-[#1E63DB] to-[#27496d]
                            text-white py-2 rounded-lg font-semibold
                            hover:opacity-90 transition-all
                          "
                        >
                          Book Again
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
            className="
              mt-8 w-full bg-gradient-to-br
              from-[#1E63DB] to-[#27496d]
              text-white py-3 rounded-xl font-semibold
              hover:opacity-90 transition-all duration-300
            "
          >
            Book New Appointment
          </button>
        </div>
      </div>
    </div>
  );
}

// Diagnosis, medicines and follow-up from the doctor's consultation
function ConsultationSummary({ consultation }) {
  const [open, setOpen] = useState(false);
  return (
    <div className="mt-3 p-3 rounded-lg bg-white/70 dark:bg-[#0a1124] border border-green-200 dark:border-[#233565]">
      <p><strong>Diagnosis:</strong> {consultation.diagnosis}</p>
      {consultation.followUpDate && (
        <p><strong>Follow-up:</strong> {consultation.followUpDate}</p>
      )}
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="text-sm text-blue-700 dark:text-[#50d4f2] mt-1"
      >
        {open ? "Hide details" : `Show details (${consultation.medicines.length} medicines)`}
      </button>
      {open && (
        <div className="text-sm mt-2 space-y-1">
          {consultation.symptoms && <p><strong>Symptoms:</strong> {consultation.symptoms}</p>}
          {consultation.medicines.length > 0 && (
            <ul className="list-disc ml-5">
              {consultation.medicines.map((m, i) => (
                <li key={i}>
                  <strong>{m.medicineName}</strong>
                  {[m.dosage, m.frequency, m.duration, m.instructions].filter(Boolean).length > 0 &&
                    ` — ${[m.dosage, m.frequency, m.duration, m.instructions].filter(Boolean).join(", ")}`}
                </li>
              ))}
            </ul>
          )}
          {consultation.notes && <p><strong>Advice:</strong> {consultation.notes}</p>}
        </div>
      )}
    </div>
  );
}
