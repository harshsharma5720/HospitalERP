import React, { useEffect, useState } from "react";
import { X } from "lucide-react";
import { useNavigate } from "react-router-dom";

export default function ProfilePage({ onClose }) {
  const navigate = useNavigate();

  const [profile, setProfile] = useState({
    name: "Dr. Shreya Mehta",
    role: "Admin / Doctor",
    email: "shreya@hospital.com",
    department: "Cardiology",
    phone: "+91 9876543210",
    image: "/resumepic.jpg",
  });

  useEffect(() => {
    const savedProfile = localStorage.getItem("profileData");
    if (savedProfile) {
      setProfile(JSON.parse(savedProfile));
    }
  }, []);

  const handleEdit = () => {
    onClose();
    navigate("/edit-profile");
  };

  const handleAboutAppointment = () => {
    onClose();
    navigate("/appointment-details");
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-[100]">
      <div className="relative bg-white rounded-2xl shadow-2xl p-8 w-full max-w-md animate-fadeIn">
        <button
          onClick={onClose}
          className="absolute top-3 right-3 text-gray-500 hover:text-red-500 transition"
        >
          <X size={24} />
        </button>

        <div className="flex flex-col items-center text-center">
          <img
            src={profile.image}
            alt={profile.name}
            className="w-32 h-32 rounded-full border-4 border-[#1E63DB] shadow-lg object-cover"
          />
          <h2 className="mt-4 text-2xl font-bold text-gray-800">
            {profile.name}
          </h2>
          <p className="text-[#1E63DB] font-medium">{profile.role}</p>
        </div>

        <div className="mt-6 space-y-3 text-gray-700">
          <div className="flex justify-between">
            <span className="font-medium text-gray-600">Department:</span>
            <span>{profile.department}</span>
          </div>

          <div className="flex justify-between">
            <span className="font-medium text-gray-600">Email:</span>
            <span>{profile.email}</span>
          </div>

          <div className="flex justify-between">
            <span className="font-medium text-gray-600">Phone:</span>
            <span>{profile.phone}</span>
          </div>
        </div>

        <button
          onClick={handleAboutAppointment}
          className="mt-6 w-full bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white py-2 rounded-xl hover:opacity-90 transition-all duration-300"
        >
          About Patient Appointment
        </button>

        <button
          onClick={handleEdit}
          className="mt-4 w-full bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white py-2 rounded-xl hover:opacity-90 transition-all duration-300"
        >
          Edit Profile
        </button>
      </div>
    </div>
  );
}
