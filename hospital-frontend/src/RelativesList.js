import React, { useEffect, useState } from "react";
import axios from "axios";
import { useLocation, useNavigate } from "react-router-dom";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";

export default function RelativesList() {
  const location = useLocation();
  const navigate = useNavigate();
  const patientId = location.state?.patientId;

  const [relatives, setRelatives] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!patientId) return;

    const fetchRelatives = async () => {
      try {
        const token = localStorage.getItem("jwtToken");
        const res = await axios.get(
          `http://localhost:8080/api/patient/relative/patient/${patientId}`,
          {
            headers: { Authorization: `Bearer ${token}` },
          }
        );

        setRelatives(res.data);
      } catch (err) {
        console.error("Error fetching relatives:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchRelatives();
  }, [patientId]);

  const handleDelete = async (id) => {
    if (!window.confirm("Are you sure you want to delete this relative?")) return;

    try {
      const token = localStorage.getItem("jwtToken");

      await axios.delete(
        `http://localhost:8080/api/patient/relative/delete/${id}`,
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      setRelatives(relatives.filter((r) => r.id !== id));
      alert("Relative deleted successfully.");
    } catch (err) {
      console.error("Error deleting:", err);
      alert("Failed to delete.");
    }
  };

  const handleEdit = (relative) => {
    navigate("/edit-relative", { state: { relative } });
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gray-100 dark:bg-[#0a1124]">
        <p className="text-lg font-medium text-gray-600 dark:text-[#50d4f2]">
          Loading relatives...
        </p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-[#0a1124] relative">
      <TopNavbar />
      <Navbar />

      <div className="flex flex-col items-center py-12 px-4 relative">
        {/* Background blur image */}
        <img
          src="Shreyahospital.jpg"
          alt="Background"
          className="absolute top-0 left-0 w-full h-full object-cover opacity-20 -z-10"
        />

        {/* Main white glass container */}
        <div
          className="
            bg-white bg-opacity-90
            dark:bg-[#111a3b]/70 dark:text-[#50d4f2]
            backdrop-blur-md shadow-2xl rounded-2xl
            p-8 w-full max-w-5xl relative z-10
          "
        >
          <h2
            className="
              text-3xl font-bold text-center
              text-[#1E63DB] dark:text-[#50d4f2]
              mb-6
            "
          >
            Your Relatives
          </h2>

          {relatives.length === 0 ? (
            <p className="text-center text-gray-600 dark:text-gray-300">
              No relatives found.
            </p>
          ) : (
            <div className="grid md:grid-cols-2 gap-6">
              {relatives.map((r) => (
                <div
                  key={r.id}
                  className="
                    bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF]
                    dark:from-[#0f172a] dark:to-[#111a3b]
                    border border-gray-200 dark:border-[#233565]
                    p-6 rounded-2xl shadow-2xl
                    hover:shadow-3xl transition-transform
                    transform hover:scale-105 duration-300
                  "
                >
                  <h3 className="text-lg font-semibold text-[#1E63DB] dark:text-[#50d4f2] mb-2">
                    {r.name}
                  </h3>

                  <p><strong>Relationship:</strong> {r.relationship}</p>
                  <p><strong>Gender:</strong> {r.gender}</p>
                  {r.age && <p><strong>Age:</strong> {r.age}</p>}
                  {r.phone && <p><strong>Phone:</strong> {r.phone}</p>}
                  {r.address && <p><strong>Address:</strong> {r.address}</p>}

                  {/* Buttons: 50% width and side-by-side */}
                  <div className="flex gap-3 mt-4 w-full">
                    <button
                      onClick={() => handleEdit(r)}
                      className="
                        w-1/2 bg-gradient-to-br
                        from-green-500 to-emerald-700
                        text-white py-2 rounded-lg font-semibold
                        hover:opacity-90 transition-all
                      "
                    >
                      Edit
                    </button>

                    <button
                      onClick={() => handleDelete(r.id)}
                      className="
                        w-1/2 bg-gradient-to-br
                        from-red-600 to-red-800
                        text-white py-2 rounded-lg font-semibold
                        hover:opacity-90 transition-all
                      "
                    >
                      Delete
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Add new relative */}
          <button
            onClick={() => navigate("/add-relative", { state: { patientId } })}
            className="
              mt-8 w-full bg-gradient-to-br
              from-[#1E63DB] to-[#27496d]
              text-white py-3 rounded-xl font-semibold
              hover:opacity-90 transition-all duration-300
            "
          >
            Add New Relative
          </button>
        </div>
      </div>
    </div>
  );
}
