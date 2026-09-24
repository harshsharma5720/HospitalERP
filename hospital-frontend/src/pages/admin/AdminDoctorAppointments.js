import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate, useParams } from "react-router-dom";
import { API_BASE_URL } from "../../config";
import { getErrorMessage } from "../../utils/apiError";

const formatTime = (time) => (time ? String(time).slice(0, 5) : "");

// Admin view of one doctor's pending / completed appointments (route: /admin/doctor/:userId/appointments)
export default function AdminDoctorAppointments() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const [viewType, setViewType] = useState("pending");
  const [appointments, setAppointments] = useState([]);
  const [doctor, setDoctor] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    axios
      .get(`${API_BASE_URL}/api/doctor/get/${userId}`)
      .then((res) => setDoctor(res.data))
      .catch(() => setDoctor(null));
  }, [userId]);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const path = viewType === "pending" ? "doctorPendingAppointments" : "doctorCompletedAppointments";
        const res = await axios.get(`${API_BASE_URL}/api/admin/${path}/${userId}`);
        setAppointments(res.data);
      } catch (err) {
        setAppointments([]);
        alert(getErrorMessage(err, "Could not load appointments."));
      } finally {
        setLoading(false);
      }
    };
    load();
  }, [userId, viewType]);

  return (
    <div className="p-6">
      <button onClick={() => navigate("/admin/manage-doctor")} className="text-blue-600 dark:text-[#50d4f2] mb-4">
        ← Back to doctors
      </button>
      <h1 className="text-2xl font-bold mb-4 text-gray-900 dark:text-white">
        Appointments {doctor ? `— ${doctor.name} (${doctor.specialist?.replace(/_/g, " ")})` : ""}
      </h1>

      <div className="flex gap-3 mb-5">
        {["pending", "completed"].map((type) => (
          <button
            key={type}
            onClick={() => setViewType(type)}
            className={`px-4 py-2 rounded-lg font-semibold ${
              viewType === type ? "bg-blue-600 text-white" : "bg-gray-200 dark:bg-gray-700 dark:text-white"
            }`}
          >
            {type === "pending" ? "Upcoming" : "Completed"}
          </button>
        ))}
      </div>

      {loading ? (
        <p className="text-gray-600 dark:text-gray-300">Loading...</p>
      ) : appointments.length === 0 ? (
        <p className="text-gray-600 dark:text-gray-300">No {viewType} appointments.</p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full border rounded-xl overflow-hidden bg-white dark:bg-gray-800 shadow-lg dark:text-gray-200">
            <thead className="bg-gray-100 dark:bg-gray-700">
              <tr>
                <th className="p-3 border">ID</th>
                <th className="p-3 border">Patient</th>
                <th className="p-3 border">Date</th>
                <th className="p-3 border">Time</th>
                <th className="p-3 border">Status</th>
                <th className="p-3 border">Message</th>
              </tr>
            </thead>
            <tbody>
              {appointments.map((a) => (
                <tr key={a.appointmentID} className="hover:bg-gray-100 dark:hover:bg-gray-700">
                  <td className="p-3 border">{a.appointmentID}</td>
                  <td className="p-3 border">{a.patientName}</td>
                  <td className="p-3 border">{a.date}</td>
                  <td className="p-3 border">
                    {a.startTime ? `${formatTime(a.startTime)} - ${formatTime(a.endTime)}` : a.shift}
                  </td>
                  <td className="p-3 border">{a.status}</td>
                  <td className="p-3 border">{a.message || "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
