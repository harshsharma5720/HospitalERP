import React, { useEffect, useState } from "react";
import axios from "axios";
import { useParams, useNavigate } from "react-router-dom";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import Lottie from "lottie-react";
import doctorAnimation from "./assets/Doctor.json";

export default function DoctorProfile() {
  const { doctorId } = useParams();
  const [doctor, setDoctor] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchDoctor = async () => {
      try {
        setLoading(true);
        const response = await axios.get(
          `http://localhost:8080/api/doctor/getDoctor/${doctorId}`
        );
        setDoctor(response.data);
        setError("");
      } catch (err) {
        console.error("Error fetching doctor:", err);
        setError("Unable to load doctor details.");
      } finally {
        setLoading(false);
      }
    };
    fetchDoctor();
  }, [doctorId]);

  if (loading)
    return (
      <div className="min-h-screen flex items-center justify-center dark:bg-[#0a1124] bg-gray-100">
        <p className="text-xl dark:text-[#50d4f2]">Loading doctor details...</p>
      </div>
    );

  if (error)
    return (
      <div className="min-h-screen flex items-center justify-center dark:bg-[#0a1124] bg-gray-100">
        <p className="text-red-500 text-lg">{error}</p>
      </div>
    );

  if (!doctor)
    return (
      <div className="min-h-screen flex items-center justify-center dark:bg-[#0a1124] bg-gray-100">
        <p className="dark:text-[#50d4f2] text-gray-700">
          No doctor data available.
        </p>
      </div>
    );

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-[#0a1124] flex flex-col relative overflow-hidden">
      {/* Background Animation */}
      <div className="absolute inset-0 opacity-20 pointer-events-none z-0">
        <Lottie animationData={doctorAnimation} loop={true} />
      </div>

      {/* Foreground Content */}
      <div className="relative z-10">
        <TopNavbar />
        <Navbar />

        {/* Profile Section */}
        <div className="bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF] max-w-5xl mx-auto mt-10 bg-white dark:bg-[#111a3b] rounded-2xl shadow-2xl p-8 flex flex-col md:flex-row items-center md:items-start gap-10">
          {/* Left Section - Image & Basic Info */}
          <div className="flex flex-col items-center md:w-1/3 text-center">
            <img
              src="https://cdn-icons-png.flaticon.com/512/387/387561.png"
              alt="Doctor Avatar"
              className="h-40 w-40 rounded-full shadow-lg mb-4"
            />
            <h2 className="text-2xl font-bold text-[#003366] dark:text-[#50d4f2]">
              {doctor.name}
            </h2>
            <p className="text-gray-600 dark:text-gray-300 mb-2">
              {doctor.specialist || "General Practitioner"}
            </p>

            {/* Rating Stars */}
            <div className="flex justify-center mb-3">
              {[1, 2, 3, 4, 5].map((star) => (
                <span key={star} className="text-yellow-400 text-xl">
                  ★
                </span>
              ))}
            </div>

            <div className="text-sm text-gray-600 dark:text-gray-300 space-y-1">
              <p>
                <strong>Gender:</strong> {doctor.gender || "N/A"}
              </p>
              <p>
                <strong>Email:</strong> {doctor.email}
              </p>
              <p>
                <strong>Phone:</strong> {doctor.phoneNumber}
              </p>
            </div>

            {/* Buttons */}
            <div className="flex flex-col gap-3 mt-6 w-full">
              <button
                onClick={() =>
                  navigate("/appointments", {
                    state: { doctorName: doctor.name },
                  })
                }
                className="bg-gradient-to-r from-[#007B9E] to-[#00A2B8] text-white py-2 rounded-full font-semibold hover:scale-105 transition-transform shadow-md"
              >
                Book Appointment
              </button>

              <button
                onClick={() => alert(`Chat feature coming soon with ${doctor.name}`)}
                className="bg-gradient-to-r from-[#1E63DB] to-[#27496d] text-white py-2 rounded-full font-semibold hover:scale-105 transition-transform shadow-md"
              >
                Chat with Doctor
              </button>
            </div>
          </div>

          {/* Right Section - Details */}
          <div className="flex-1 text-gray-700 dark:text-gray-300">
            <h3 className="text-2xl font-semibold text-[#003366] dark:text-[#50d4f2] mb-4">
              About Dr. {doctor.name}
            </h3>
            <p className="leading-relaxed mb-6">
              Dr. {doctor.name} is a highly skilled specialist in{" "}
              {doctor.specialist || "General Medicine"}. They are known for
              their compassionate approach and commitment to providing
              high-quality healthcare to all patients. With years of experience,
              Dr. {doctor.name} ensures that every patient receives personalized
              care and attention.
            </p>

            <div className="mb-4">
              <h4 className="font-semibold text-[#003366] dark:text-[#50d4f2]">
                Specialty Interests
              </h4>
              <p>{doctor.specialisation || "Internal Medicine, Surgery, etc."}</p>
            </div>

            <div className="mb-4">
              <h4 className="font-semibold text-[#003366] dark:text-[#50d4f2]">
                Experience
              </h4>
              <p>
                {doctor.experience ||
                  "Over 5 years of experience providing patient-centered care."}
              </p>
            </div>

            <div>
              <h4 className="font-semibold text-[#003366] dark:text-[#50d4f2]">
                Languages
              </h4>
              <p>{doctor.languages || "English, Hindi"}</p>
            </div>
          </div>
        </div>

        {/* Footer */}
        <footer className="bg-blue-600 dark:bg-[#111a3b] text-white py-4 text-center mt-10">
          <p>© {new Date().getFullYear()} Shreya Hospital. All Rights Reserved.</p>
        </footer>
      </div>
    </div>
  );
}
