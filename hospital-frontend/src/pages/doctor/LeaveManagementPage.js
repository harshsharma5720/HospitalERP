import React, { useEffect, useState } from "react";
import TopNavbar from "../../components/TopNavbar";
import { getRoleFromToken, getUserIdFromToken } from "../../utils/jwtUtils";
import { useNavigate } from "react-router-dom";

export default function LeaveManagementPage() {
  const [pendingLeaves, setPendingLeaves] = useState([]);
  const [approvedLeaves, setApprovedLeaves] = useState([]);
  const [loading, setLoading] = useState(true);
  const [viewType, setViewType] = useState("pending");
  const [showApplyForm, setShowApplyForm] = useState(false);
  const [formData, setFormData] = useState({ startDate: "", endDate: "", reason: "" });

  const navigate = useNavigate();
  const token = localStorage.getItem("jwtToken");
  const role = getRoleFromToken(token);
  const userId = getUserIdFromToken(token);

  useEffect(() => {
    if (!token) {
      alert("Unauthorized access, please login");
      navigate("/login");
      return;
    }
    fetchLeaves();
  }, [viewType]);

  const fetchLeaves = async () => {
    setLoading(true);
    try {
      const endpoint =
        viewType === "pending"
          ? `http://localhost:8080/api/leaves/pending/${userId}`
          : `http://localhost:8080/api/leaves/approved/${userId}`;

      const response = await fetch(endpoint, {
        headers: { Authorization: `Bearer ${token}` },
      });

      const data = await response.json();
      if (viewType === "pending") setPendingLeaves(data);
      else setApprovedLeaves(data);

    } catch (err) {
      console.error("Error fetching leaves:", err);
    }
    setLoading(false);
  };

  // Apply for leave
  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const payload = {
        userId: userId,
        role: role,
        startDate: formData.startDate,
        endDate: formData.endDate,
        reason: formData.reason,
      };

      const response = await fetch(`http://localhost:8080/api/leaves/apply`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const result = await response.json();
      alert("Leave Applied Successfully");
      setShowApplyForm(false);
      fetchLeaves();
    } catch (err) {
      console.error("Error applying leave:", err);
      alert("Failed to apply leave");
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-[#0a1124]">

      <div className="max-w-5xl mx-auto animate-scaleUp mt-10 p-6 bg-white dark:bg-[#111a3b] rounded-xl shadow-xl">
        <h2 className="text-5xl text-center font-extrabold mb-6">
          Leave{" "}
          <span className="bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
            Management
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
            Pending Leaves
          </button>

          <button
            onClick={() => setViewType("approved")}
            className={`px-6 py-2 rounded-lg font-semibold ${
              viewType === "approved"
                ? "bg-green-600 text-white"
                : "bg-gray-300 dark:bg-[#2c3558] dark:text-white"
            }`}
          >
            Approved Leaves
          </button>

          <button
            onClick={() => setShowApplyForm(true)}
            className="px-6 py-2 bg-cyan-600 hover:bg-cyan-700 text-white rounded-lg font-semibold"
          >
            Apply Leave
          </button>
        </div>

        {loading ? (
          <p className="text-center">Loading leaves...</p>
        ) : (
          <div className="grid md:grid-cols-2 gap-6">
            {(viewType === "pending" ? pendingLeaves : approvedLeaves).map((leave) => (
              <div
                key={leave.id}
                className="p-5 bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF]
                  dark:from-[#111a3b] dark:to-[#0f172a] rounded-xl shadow-xl"
              >
                <p><strong>From:</strong> {leave.startDate}</p>
                <p><strong>To:</strong> {leave.endDate}</p>
                <p><strong>Reason:</strong> {leave.reason}</p>
                <p><strong>Status:</strong> {leave.status}</p>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* APPLY LEAVE FORM */}
      {showApplyForm && (
        <div className="fixed inset-0 flex justify-center items-center bg-black bg-opacity-50">
          <form
            onSubmit={handleSubmit}
            className="bg-white dark:bg-[#111a3b] p-6 rounded-2xl shadow-2xl w-96"
          >
            <h3 className="text-xl font-bold text-center mb-4">Apply Leave</h3>

            <input
              type="date"
              className="p-2 rounded-lg w-full mb-3 bg-gray-200 dark:bg-[#1b2640]"
              onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
              required
            />
            <input
              type="date"
              className="p-2 rounded-lg w-full mb-3 bg-gray-200 dark:bg-[#1b2640]"
              onChange={(e) => setFormData({ ...formData, endDate: e.target.value })}
              required
            />
            <textarea
              placeholder="Reason"
              className="p-2 rounded-lg w-full mb-3 bg-gray-200 dark:bg-[#1b2640]"
              onChange={(e) => setFormData({ ...formData, reason: e.target.value })}
              required
            ></textarea>

            <div className="flex justify-between">
              <button
                type="button"
                onClick={() => setShowApplyForm(false)}
                className="px-5 py-2 bg-red-600 text-white rounded-lg"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-5 py-2 bg-green-600 text-white rounded-lg"
              >
                Apply
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
