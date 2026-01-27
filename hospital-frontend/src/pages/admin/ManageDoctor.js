import React, { useEffect, useState } from "react";
import axios from "axios";

export default function ManageDoctor() {
  const [doctors, setDoctors] = useState([]);
  const [filteredDoctors, setFilteredDoctors] = useState([]);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(true);

  const token = localStorage.getItem("jwtToken");

  // ================= FETCH DOCTORS =================
  const fetchDoctors = async () => {
    try {
      const response = await axios.get(
        "http://localhost:8080/api/patient/getAllDoctors",
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      // sort by completed appointments DESC (ranking)
      const rankedDoctors = response.data.sort(
        (a, b) =>
          (b.completedAppointmentsCount || 0) -
          (a.completedAppointmentsCount || 0)
      );

      setDoctors(rankedDoctors);
      setFilteredDoctors(rankedDoctors);
    } catch (error) {
      console.error("Error fetching doctors:", error);
    } finally {
      setLoading(false);
    }
  };

  // ================= DELETE DOCTOR =================
  const deleteDoctor = async (id) => {
    const confirmDelete = window.confirm(
      "Doctor has resigned. Remove from hospital?"
    );
    if (!confirmDelete) return;

    try {
      await axios.delete(
        `http://localhost:8080/api/doctor/delete/${id}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );
      alert("Doctor removed successfully!");
      fetchDoctors();
    } catch (error) {
      console.error("Delete failed:", error);
      alert("Delete failed!");
    }
  };

  // ================= SEARCH =================
  useEffect(() => {
    const filtered = doctors.filter((doc) =>
      `${doc.name} ${doc.specialization} ${doc.userId}`
        .toLowerCase()
        .includes(search.toLowerCase())
    );
    setFilteredDoctors(filtered);
  }, [search, doctors]);

  useEffect(() => {
    fetchDoctors();
  }, []);

  if (loading) return <p className="text-center mt-5">Loading doctors...</p>;

  const presentDoctors = filteredDoctors.filter((d) => d.isPresent);
  const absentDoctors = filteredDoctors.filter((d) => !d.isPresent);

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
              <th className="p-3 text-left">Specialization</th>
              <th className="p-3 text-left">Completed</th>
              <th className="p-3 text-left">Pending</th>
              <th className="p-3 text-left">Actions</th>
            </tr>
          </thead>
          <tbody>
            {filteredDoctors.map((doc, index) => (
              <tr key={doc.id} className="border-b">
                <td className="p-3 font-bold">#{index + 1}</td>
                <td className="p-3">{doc.name}</td>
                <td className="p-3">{doc.specialization}</td>
                <td className="p-3 text-green-600 font-semibold">
                  {doc.completedAppointmentsCount || 0}
                </td>
                <td className="p-3 text-orange-600 font-semibold">
                  {doc.pendingAppointmentsCount || 0}
                </td>
                <td className="p-3">
                  <button
                    onClick={() => deleteDoctor(doc.id)}
                    className="bg-red-600 text-white px-3 py-1 rounded"
                  >
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {/* ================= PRESENT / ABSENT PANELS ================= */}
      <div className="grid grid-cols-2 gap-6">
        {/* ABSENT */}
        <div className="bg-red-50 shadow-md rounded-lg">
          <h2 className="text-lg font-semibold p-4 border-b text-red-700">
            Absent Doctors
          </h2>
          {absentDoctors.length === 0 ? (
            <p className="p-4 text-gray-500">No absent doctors</p>
          ) : (
            absentDoctors.map((doc) => (
              <div key={doc.id} className="p-4 border-b">
                <p className="font-semibold">{doc.name}</p>
                <p className="text-sm text-gray-600">
                  {doc.specialization}
                </p>
              </div>
            ))
          )}
        </div>

        {/* PRESENT */}
        <div className="bg-green-50 shadow-md rounded-lg">
          <h2 className="text-lg font-semibold p-4 border-b text-green-700">
            Present Doctors
          </h2>
          {presentDoctors.length === 0 ? (
            <p className="p-4 text-gray-500">No doctors present</p>
          ) : (
            presentDoctors.map((doc) => (
              <div key={doc.id} className="p-4 border-b">
                <p className="font-semibold">{doc.name}</p>
                <p className="text-sm text-gray-600">
                  {doc.specialization}
                </p>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
