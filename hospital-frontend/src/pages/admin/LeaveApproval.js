import React, { useEffect, useState } from "react";
import axios from "axios";
import { API_BASE_URL } from "../../config";
import { getErrorMessage } from "../../utils/apiError";

const TABS = [
  { key: "PENDING", label: "Pending Leaves", url: "/api/admin/allPending", active: "bg-blue-600 text-white" },
  { key: "APPROVED", label: "Approved Leaves", url: "/api/admin/allApproved", active: "bg-green-600 text-white" },
  { key: "REJECTED", label: "Rejected Leaves", url: "/api/admin/allRejected", active: "bg-red-600 text-white" },
];

export default function LeaveApproval() {
  const [leaves, setLeaves] = useState([]);
  const [filter, setFilter] = useState("PENDING"); // default tab
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    fetchLeaves();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filter]);

  const fetchLeaves = async () => {
    try {
      const tab = TABS.find((t) => t.key === filter);
      const response = await axios.get(`${API_BASE_URL}${tab.url}`);
      setLeaves(response.data);
    } catch (error) {
      alert(getErrorMessage(error, "Could not load leave requests."));
    }
  };

  // Approving a doctor's leave also cancels their appointments in that period and notifies patients
  const decideLeave = async (leaveId, decision) => {
    const question =
      decision === "approve"
        ? "Approve this leave? Appointments in this period will be cancelled and patients notified."
        : "Reject this leave request?";
    if (!window.confirm(question)) return;
    try {
      setBusyId(leaveId);
      await axios.put(`${API_BASE_URL}/api/admin/${decision}/${leaveId}`);
      alert(decision === "approve" ? "Leave Approved Successfully" : "Leave Rejected");
      fetchLeaves();
    } catch (error) {
      alert(getErrorMessage(error, "Could not update the leave request."));
    } finally {
      setBusyId(null);
    }
  };

  return (
    <div className="p-6">
      <h1 className="text-2xl font-bold mb-6 text-gray-900 dark:text-white">
        Leave Approval Dashboard
      </h1>

      {/* FILTER BUTTONS */}
      <div className="flex flex-wrap gap-3 mb-5">
        {TABS.map((tab) => (
          <button
            key={tab.key}
            className={`px-4 py-2 rounded-lg font-semibold ${
              filter === tab.key ? tab.active : "bg-gray-200 dark:bg-gray-700 dark:text-white"
            }`}
            onClick={() => setFilter(tab.key)}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* TABLE */}
      {leaves.length === 0 ? (
        <p className="text-gray-600 dark:text-gray-300 text-lg">
          No {filter.toLowerCase()} leave requests
        </p>
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full border rounded-xl overflow-hidden bg-white dark:bg-gray-800 shadow-lg">
            <thead className="bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-gray-200">
              <tr>
                <th className="p-3 border">Leave ID</th>
                <th className="p-3 border">User ID</th>
                <th className="p-3 border">Role</th>
                <th className="p-3 border">Start Date</th>
                <th className="p-3 border">End Date</th>
                <th className="p-3 border">Reason</th>
                <th className="p-3 border">Status</th>
                <th className="p-3 border text-center">Action</th>
              </tr>
            </thead>

            <tbody>
              {leaves.map((leave) => (
                <tr
                  key={leave.id}
                  className="hover:bg-gray-100 dark:hover:bg-gray-700 transition dark:text-gray-200"
                >
                  <td className="p-3 border">{leave.id}</td>
                  <td className="p-3 border">{leave.userId}</td>
                  <td className="p-3 border">{(leave.role || "").replace("ROLE_", "")}</td>
                  <td className="p-3 border">{leave.startDate}</td>
                  <td className="p-3 border">{leave.endDate}</td>
                  <td className="p-3 border">{leave.reason}</td>
                  <td className="p-3 border font-semibold">{leave.status}</td>

                  <td className="p-3 border text-center">
                    {filter === "PENDING" && (
                      <div className="flex gap-2 justify-center">
                        <button
                          disabled={busyId === leave.id}
                          className="bg-green-600 hover:bg-green-700 transition text-white px-4 py-1 rounded-lg disabled:opacity-60"
                          onClick={() => decideLeave(leave.id, "approve")}
                        >
                          Approve
                        </button>
                        <button
                          disabled={busyId === leave.id}
                          className="bg-red-600 hover:bg-red-700 transition text-white px-4 py-1 rounded-lg disabled:opacity-60"
                          onClick={() => decideLeave(leave.id, "reject")}
                        >
                          Reject
                        </button>
                      </div>
                    )}

                    {filter === "APPROVED" && (
                      <span className="text-green-700 font-semibold">
                        ✔ Approved
                      </span>
                    )}

                    {filter === "REJECTED" && (
                      <span className="text-red-600 font-semibold">
                        ✖ Rejected
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
