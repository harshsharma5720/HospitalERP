import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { Users, CalendarCheck, Clock } from "lucide-react";
import { getRoleFromToken, getUserIdFromToken } from "../../utils/jwtUtils";
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";

export default function DoctorDashboard() {
  const navigate = useNavigate();

  const [doctor, setDoctor] = useState(null);
  const [appointments, setAppointments] = useState([]);
  const [completed, setCompleted] = useState([]);
  const [pending, setPending] = useState([]);

  const appointmentStatusData = [
    { name: "Completed", value: completed.length },
    { name: "Pending", value: pending.length },
  ];
  const COLORS = ["#1E63DB", "#f97316"]; // green, orange

  useEffect(() => {
    const token = localStorage.getItem("jwtToken");
    if (!token) return navigate("/login");

    const role = getRoleFromToken(token);
    const userId = getUserIdFromToken(token);

    if (role !== "ROLE_DOCTOR") {
      navigate("/unauthorized");
      return;
    }

    fetchDoctor(userId, token);
    fetchAppointments(token);
  }, []);

  useEffect(() => {
    const loadAppointments = async () => {
      const doctorId = getUserIdFromToken(localStorage.getItem("jwtToken"));

      const pending = await fetchPendingAppointments(doctorId);
      console.log("Pending:", pending);
      setPending(pending);

      const completed = await fetchCompletedAppointments(doctorId);
      console.log("Completed:", completed);
      setCompleted(completed);
    };

    loadAppointments();
  }, []);

  const fetchDoctor = async (id, token) => {
    try {
      const res = await axios.get(
        `http://localhost:8080/api/doctor/get/${id}`,
        { headers: { Authorization: `Bearer ${token}` } }
      );
      setDoctor(res.data);
    } catch (err) {
      console.error("Doctor fetch failed", err);
    }
  };

  const fetchAppointments = async (token) => {
    try {
      const res = await axios.get(
        `http://localhost:8080/appointment/getDoctorAppointments`,
        { headers: { Authorization: `Bearer ${token}` } }
      );

      const data = res.data || [];
      setAppointments(data);
    } catch (err) {
      console.error("Appointments fetch failed", err);
    }
  };
  const fetchPendingAppointments = async (doctorId) => {
    try {
      const token = localStorage.getItem("jwtToken");
      const response = await fetch(
        `http://localhost:8080/api/doctor/doctorPendingAppointments/${doctorId}`,
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        }
      );
      if (!response.ok) {
        throw new Error("Failed to fetch pending appointments");
      }
      const data = await response.json();
      return data; // list of pending appointments
    } catch (error) {
      console.error("Error fetching pending appointments:", error);
      return [];
    }
  };

  const fetchCompletedAppointments = async (doctorId) => {
    try {
      const token = localStorage.getItem("jwtToken");

      const response = await fetch(
        `http://localhost:8080/api/doctor/doctorCompletedAppointments/${doctorId}`,
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "application/json",
          },
        }
      );

      if (!response.ok) {
        throw new Error("Failed to fetch completed appointments");
      }
      const data = await response.json();
      return data; // list of completed appointments
    } catch (error) {
      console.error("Error fetching completed appointments:", error);
      return [];
    }
  };

  if (!doctor)
    return (
      <p className="text-center mt-20 text-gray-600 dark:text-gray-300">
        Loading doctor dashboard...
      </p>
    );

  return (
    <div className="min-h-screen  dark:bg-[#0a1124] p-8 transition-all">

      {/* HEADER */}
      <div className="mb-10">
        <h1 className="text-3xl font-bold text-gray-800 dark:text-white">
          Hello,{" "}
          <span className="bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
            {doctor.name}
          </span>
        </h1>
        <p className="text-gray-500 dark:text-[#8ddff8]">
          {doctor.specialist}
        </p>
      </div>

      {/* STATS */}
      <div className="grid grid-cols-1  md:grid-cols-3 gap-6">
        <Stat title="Total Appointments" value={appointments.length} icon={<Users />} />
        <Stat title="Completed Appointments" value={completed.length} icon={<CalendarCheck />} />
        <Stat title="Pending Appointments" value={pending.length} icon={<Clock />} />
      </div>

      {/* CHARTS */}
      <div className="mt-12 grid grid-cols-1 lg:grid-cols-2 gap-8">

        {/* PIE CHART */}
        <div className="bg-white dark:bg-[#111a3b] p-6 rounded-2xl shadow-xl">
          <h3 className="text-lg font-semibold mb-4 dark:text-white">
            Appointment Status Distribution
          </h3>

          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={appointmentStatusData}
                dataKey="value"
                nameKey="name"
                cx="50%"
                cy="50%"
                outerRadius={100}
                label
              >
                {appointmentStatusData.map((entry, index) => (
                  <Cell key={index} fill={COLORS[index]} />
                ))}
              </Pie>
              <Tooltip />
              <Legend />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* BAR CHART */}
        <div className="bg-white dark:bg-[#111a3b] p-6 rounded-2xl shadow-xl">
          <h3 className="text-lg font-semibold mb-4 dark:text-white">
            Completed vs Pending Appointments
          </h3>

          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={appointmentStatusData}>
              <defs>
                <linearGradient id="completedGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#1E63DB" />
                  <stop offset="100%" stopColor="#27496d" />
                </linearGradient>
              </defs>

              <XAxis dataKey="name" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Legend />

              <Bar dataKey="value" radius={[6, 6, 0, 0]}>
                {appointmentStatusData.map((entry, index) => (
                  <Cell
                    key={index}
                    fill={index === 0 ? "url(#completedGradient)" : "#f97316"}
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>

        </div>

      </div>

      {/* ACTIONS */}
      <div className="mt-10 flex gap-4">
        <button
          onClick={() => navigate("/doctor/doctor-appointments")}
          className="px-6 py-2 rounded-lg
                     bg-gradient-to-r from-[#007B9E] to-[#00A2B8]
                     dark:from-[#50d4f2] dark:to-[#3bc2df]
                     text-white dark:text-black font-semibold"
        >
          View Appointments
        </button>

        <button
          onClick={() => navigate("/doctor/leave-management")}
          className="px-6 py-2 rounded-lg bg-blue-600 text-white hover:bg-blue-700"
        >
          Apply Leave
        </button>
      </div>

    </div>
  );

}

/* ---------- COMPONENT ---------- */
function Stat({ title, value, icon }) {
  return (
    <div className="bg-white dark:bg-[#111a3b] p-6 rounded-2xl shadow-xl
                    border border-gray-100 dark:border-[#16224a]">
      <div className="flex items-center gap-4">
        <div className="p-3 bg-[#50d4f2]/20 text-[#50d4f2] rounded-xl">
          {icon}
        </div>
        <div>
          <p className="text-gray-500 dark:text-[#8ddff8]">{title}</p>
          <h2 className="text-2xl font-bold dark:text-white">{value}</h2>
        </div>
      </div>
    </div>
  );
}
