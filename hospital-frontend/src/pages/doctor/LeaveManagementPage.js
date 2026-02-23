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
      console.log("Fetched Leaves:", data);
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

      <div className="max-w-5xl mx-auto animate-scaleUp mt-10 p-6  dark:bg-[#111a3b] rounded-xl shadow-xl">
        <h2 className="text-5xl text-center font-extrabold mb-6">
          Leave{" "}
          <span className="bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
            Management
          </span>
        </h2>
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
          <div className="p-5 rounded-xl bg-blue-100 dark:bg-[#1b2640] shadow">
            <p className="text-sm text-gray-600 dark:text-gray-300">Total Leaves</p>
            <h3 className="text-3xl font-bold">
              {pendingLeaves.length + approvedLeaves.length}
            </h3>
          </div>

          <div className="p-5 rounded-xl bg-yellow-100 dark:bg-[#2c3558] shadow">
            <p className="text-sm text-gray-600 dark:text-gray-300">Pending</p>
            <h3 className="text-3xl font-bold text-yellow-600">
              {pendingLeaves.length}
            </h3>
          </div>

          <div className="p-5 rounded-xl bg-green-100 dark:bg-[#1f3b2d] shadow">
            <p className="text-sm text-gray-600 dark:text-gray-300">Approved</p>
            <h3 className="text-3xl font-bold text-green-600">
              {approvedLeaves.length}
            </h3>
          </div>

          <div className="p-5 rounded-xl bg-purple-100 dark:bg-[#2a1f3d] shadow">
            <p className="text-sm text-gray-600 dark:text-gray-300">Remaining</p>
            <h3 className="text-3xl font-bold">12</h3>
          </div>
        </div>


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
                className={`p-5 rounded-xl shadow-xl bg-white dark:bg-[#0f172a] border-l-4 ${
                  leave.status === "APPROVED"
                    ? "border-green-500"
                    : "border-yellow-500"
                }`}
              >
                <div className="flex justify-between items-center mb-2">
                  <h4 className="font-bold text-lg">
                    {leave.startDate} → {leave.endDate}
                  </h4>

                  <span
                    className={`px-3 py-1 rounded-full text-sm font-semibold ${
                      leave.status === "APPROVED"
                        ? "bg-green-100 text-green-700"
                        : "bg-yellow-100 text-yellow-700"
                    }`}
                  >
                    {leave.status}
                  </span>
                </div>

                <p className="text-gray-600 dark:text-gray-300">
                  <strong>Reason:</strong> {leave.reason}
                </p>
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
            <div className="mt-12 p-6 rounded-xl bg-gray-50 dark:bg-[#0b122f]">
              <h3 className="text-2xl font-bold mb-4">Leave Process</h3>
              <ul className="space-y-3 text-gray-600 dark:text-gray-300">
                <li> Apply leave with reason and dates</li>
                <li> Leave remains pending until admin approval</li>
                <li> Once approved, it appears in approved list</li>
                <li> You’ll be notified on status change</li>
              </ul>
            </div>

          </form>

        </div>
      )}
    </div>
  );
}
