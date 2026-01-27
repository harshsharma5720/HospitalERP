import React, { useEffect, useState } from "react";
import axios from "axios";

export default function LeaveApproval() {
  const [leaves, setLeaves] = useState([]);
  const [filter, setFilter] = useState("PENDING"); // default tab
  const token = localStorage.getItem("jwtToken");

  useEffect(() => {
    fetchLeaves();
  }, [filter]);

  const fetchLeaves = async () => {
    try {
      const url =
        filter === "PENDING"
          ? "http://localhost:8080/api/admin/allPending"
          : "http://localhost:8080/api/admin/allApproved";

      const response = await axios.get(url, {
        headers: { Authorization: `Bearer ${token}` },
      });
      console.log("Fetched Leaves:", response.data);
      setLeaves(response.data);
    } catch (error) {
      console.error("Error fetching leave requests:", error);
    }
  };

  const approveLeave = async (leaveId) => {
    try {
      await axios.put(
        `http://localhost:8080/api/admin/approve/${leaveId}`,
        {},
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      alert("Leave Approved Successfully");
      fetchLeaves();
    } catch (error) {
      console.error("Error approving leave:", error);
    }
  };

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-white">
        Leave Approval Dashboard
      </h1>

      {/* FILTER BUTTONS */}
      <div className="flex gap-3 mb-5">
        <button
           className={`px-4 py-2 rounded-lg font-semibold ${
             filter === "APPROVED"
               ? "bg-green-600 text-white"
               : "bg-gray-200 dark:bg-gray-700 dark:text-white"
           }`}
           onClick={() => setFilter("APPROVED")}
        >
          Approved Leaves
        </button>
        <button
          className={`px-4 py-2 rounded-lg font-semibold ${
            filter === "PENDING"
              ? "bg-blue-600 text-white"
              : "bg-gray-200 dark:bg-gray-700 dark:text-white"
          }`}
          onClick={() => setFilter("PENDING")}
        >
          Pending Leaves
        </button>


      </div>

      {/* TABLE */}
      {leaves.length === 0 ? (
        <p className="text-gray-600 dark:text-gray-300 text-lg">
          No {filter} Leave Requests Available
        </p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full border rounded-xl overflow-hidden bg-white dark:bg-gray-800 shadow-lg">
            <thead className="bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-gray-200">
              <tr>
                <th className="p-3 border">Leave ID</th>
                <th className="p-3 border">User ID</th>
                <th className="p-3 border">Start Date</th>
                <th className="p-3 border">End Date</th>
                <th className="p-3 border">Status</th>
                <th className="p-3 border text-center">Action</th>
              </tr>
            </thead>

            <tbody>
              {leaves.map((leave) => (
                <tr
                  key={leave.id}
                  className="hover:bg-gray-100 dark:hover:bg-gray-700 transition"
                >
                  <td className="p-3 border">{leave.id}</td>
                  <td className="p-3 border">{leave.userId}</td>
                  <td className="p-3 border">{leave.startDate}</td>
                  <td className="p-3 border">{leave.endDate}</td>
                  <td className="p-3 border font-semibold">{leave.status}</td>

                  <td className="p-3 border text-center">
                    {filter === "PENDING" && (
                      <button
                        className="bg-green-600 hover:bg-green-700 transition text-white px-4 py-1 rounded-lg"
                        onClick={() => approveLeave(leave.id)}
                      >
                        Approve
                      </button>
                    )}

                    {filter === "APPROVED" && (
                      <span className="text-green-700 font-semibold">
                        ✔ Approved
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
