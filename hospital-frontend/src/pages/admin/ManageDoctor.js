import React, { useEffect, useState } from "react";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { API_BASE_URL } from "../../config";
import { getErrorMessage } from "../../utils/apiError";
import { toLocalISODate } from "../../utils/dateUtils";

export default function ManageDoctor() {
  const [doctors, setDoctors] = useState([]);
  const [filteredDoctors, setFilteredDoctors] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);
  // userIds of doctors with an approved leave covering today
  const [onLeaveUserIds, setOnLeaveUserIds] = useState(new Set());
  const [presenceFilter, setPresenceFilter] = useState(null); // null | "present" | "absent"

  const token = localStorage.getItem("jwtToken");
  const navigate = useNavigate();

  // ================= FETCH DOCTORS =================
  const fetchDoctors = async () => {
    try {
      const response = await axios.get(
        `${API_BASE_URL}/api/patient/getAllDoctors`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      const doctorsWithCounts = await Promise.all(
        response.data.map(async (doc) => {
          if (!doc.userId) return doc;

          try {
            const countRes = await axios.get(
              `${API_BASE_URL}/api/admin/doctorAppointmentCount/${doc.id}`,
              {
                headers: { Authorization: `Bearer ${token}` },
              }
            );
            return {
              ...doc,
              pendingAppointmentsCount: countRes.data.pending,
              completedAppointmentsCount: countRes.data.completed,
            };
          } catch {
            return {
              ...doc,
              pendingAppointmentsCount: 0,
              completedAppointmentsCount: 0,
            };
          }
        })
      );

      doctorsWithCounts.sort(
        (a, b) =>
          (b.completedAppointmentsCount || 0) -
          (a.completedAppointmentsCount || 0)
      );

      setDoctors(doctorsWithCounts);
      setFilteredDoctors(doctorsWithCounts);

      const today = toLocalISODate();
      const leavesRes = await axios.get(`${API_BASE_URL}/api/admin/allApproved`);
      setOnLeaveUserIds(
        new Set(
          leavesRes.data
            .filter((l) => l.startDate <= today && l.endDate >= today)
            .map((l) => l.userId)
        )
      );
    } catch (error) {
      console.error("Error fetching doctors:", error);
    } finally {
      setLoading(false);
    }
  };

  const fetchAppointmentCount = async (userId) => {
    const response = await axios.get(
      `${API_BASE_URL}/api/admin/doctorAppointmentCount/${userId}`,
      {
        headers: { Authorization: `Bearer ${token}` },
      }
    );
    return response.data;
  };

  // ================= DELETE DOCTOR =================
  const deleteDoctor = async (id) => {
    const confirmDelete = window.confirm(
      "Doctor has resigned. Remove from hospital?"
    );
    if (!confirmDelete) return;

    try {
      await axios.delete(
        `${API_BASE_URL}/api/doctor/delete/${id}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      alert("Doctor removed successfully!");
      fetchDoctors();
    } catch (error) {
      alert(getErrorMessage(error, "Delete failed!"));
    }
  };

  // ================= SEARCH =================
  useEffect(() => {
    const filtered = doctors.filter((doc) =>
      `${doc.name} ${doc.specialist} ${doc.userId}`
        .toLowerCase()
        .includes(search.toLowerCase())
    );
    setFilteredDoctors(filtered);
  }, [search, doctors]);

  useEffect(() => {
    fetchDoctors();
  }, []);

  if (loading) return <p className="text-center mt-5">Loading doctors...</p>;

  const isAbsent = (d) => onLeaveUserIds.has(d.userId);
  const presentDoctors = filteredDoctors.filter((d) => !isAbsent(d));
  const absentDoctors = filteredDoctors.filter(isAbsent);
  const visibleDoctors =
    presenceFilter === "absent" ? absentDoctors : presenceFilter === "present" ? presentDoctors : filteredDoctors;

  // ================= UI =================
  return (
    <div className="p-6">
      <h1 className="text-3xl font-bold mb-4">Manage Doctors</h1>

      {/* SEARCH BAR */}
      <input
        type="text"
        placeholder="Search by Name / Specialization / User ID"
        value={search}
        onChange={(e) => setSearch(e.target.value)}
        className="border p-2 rounded w-full mb-4"
      />

      {/* ================= ALL DOCTORS RANKING ================= */}
      <div className="bg-white shadow-md rounded-lg mb-6">
        <h2 className="text-xl font-semibold p-4 border-b">
          Doctor Ranking (Completed Appointments)
        </h2>

        <table className="w-full border-collapse">
          <thead className="bg-gray-100">
            <tr>
              <th className="p-3 text-left">Rank</th>
              <th className="p-3 text-left">Doctor</th>
              <th className="p-3 text-left">DoctorID</th>
              <th className="p-3 text-left">Specialist</th>
              <th className="p-3 text-left">Completed</th>
              <th className="p-3 text-left">Pending</th>
              <th className="p-3 text-left">Actions</th>
            </tr>
          </thead>
          <tbody>
            {visibleDoctors.map((doc, index) => (
              <tr key={doc.id} className="border-b">
                <td className="p-3 font-bold">#{index + 1}</td>
                <td className="p-3">{doc.name}</td>
                <td className="p-3">{doc.id}</td>
                <td className="p-3">{doc.specialist}</td>
                <td className="p-3">
                  <span className="px-2 py-1 rounded-full text-xs bg-green-100 text-green-700 font-semibold">
                    Completed: {doc.completedAppointmentsCount || 0}
                  </span>
                </td>
                <td className="p-3">
                  <span className="px-2 py-1 rounded-full text-xs bg-orange-100 text-orange-700 font-semibold">
                    Pending: {doc.pendingAppointmentsCount || 0}
                  </span>
                </td>
                <td className="p-3">
                  <div className="flex flex-wrap gap-2">
                      <button
                        onClick={() => deleteDoctor(doc.id)}
                        className="bg-red-600 hover:bg-red-700 text-white px-3 py-1 rounded text-xs"
                      >
                        Delete
                      </button>

                      <button
                        onClick={() =>
                          navigate(`/admin/doctor/${doc.userId}/appointments`)
                        }
                        className="bg-blue-600 hover:bg-blue-700 text-white px-3 py-1 rounded text-xs"
                      >
                        View Appointments
                      </button>
                    </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* ================= PRESENT / ABSENT PANELS ================= */}
      <div className="grid grid-cols-2 gap-6 mt-8">
        {/* ABSENT */}
        <div className="bg-red-50 shadow-md rounded-lg p-6 text-center">
          <h2 className="text-lg font-semibold text-red-700">
            Absent Doctors
          </h2>

          <p className="text-3xl font-bold my-3">
            {absentDoctors.length}
          </p>

          <button
            onClick={() => setPresenceFilter(presenceFilter === "absent" ? null : "absent")}
            className="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded"
          >
            {presenceFilter === "absent" ? "Show All Doctors" : "View Absent Doctors"}
          </button>
        </div>

        {/* PRESENT */}
        <div className="bg-green-50 shadow-md rounded-lg p-6 text-center">
          <h2 className="text-lg font-semibold text-green-700">
            Present Doctors
          </h2>

          <p className="text-3xl font-bold my-3">
            {presentDoctors.length}
          </p>

          <button
            onClick={() => setPresenceFilter(presenceFilter === "present" ? null : "present")}
            className="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded"
          >
            {presenceFilter === "present" ? "Show All Doctors" : "View Present Doctors"}
          </button>
        </div>
      </div>
    </div>
  );
}
