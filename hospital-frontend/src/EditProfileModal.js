import React, { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";

export default function EditProfilePage() {
  const navigate = useNavigate();

  // Load existing data from localStorage
  const [formData, setFormData] = useState({
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
      setFormData(JSON.parse(savedProfile));
    }
  }, []);

  // Handle input change
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
  };

  // Handle image upload
  const handleImageChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setFormData({ ...formData, image: reader.result });
      };
      reader.readAsDataURL(file);
    }
  };

  // Handle form submit
  const handleSave = (e) => {
    e.preventDefault();
    localStorage.setItem("profileData", JSON.stringify(formData));
    alert("✅ Profile updated successfully!");
    navigate(-1); // Go back to Profile Page
  };

  return (
    <div
      className="min-h-screen flex items-center justify-center bg-cover bg-center"
      style={{
        backgroundImage: "url('/doctors-image.jpg')",
      }}
    >
      <div className="bg-white bg-opacity-20 backdrop-blur-md p-10 rounded-2xl shadow-2xl w-full max-w-lg border border-[#1E63DB]">
        <h2 className="text-3xl font-bold text-center text-[#1E63DB] mb-6">
          Edit Profile
        </h2>

        {/* Profile Image */}
        <div className="flex flex-col items-center mb-6">
          <img
            src={formData.image}
            alt="Profile"
            className="w-32 h-32 rounded-full border-4 border-[#1E63DB] object-cover shadow-md"
          />
          <label className="mt-3 text-sm text-blue-800 cursor-pointer font-semibold">
            Change Photo
            <input
              type="file"
              accept="image/*"
              className="hidden"
              onChange={handleImageChange}
            />
          </label>
        </div>

        {/* Form */}
        <form onSubmit={handleSave} className="space-y-4">
          <div>
            <label className="block text-gray-700 font-semibold">Full Name</label>
            <input
              type="text"
              name="name"
              value={formData.name}
              onChange={handleChange}
              className="w-full px-4 py-2 rounded-lg border border-gray-300 focus:ring-2 focus:ring-[#1E63DB]"
              required
            />
          </div>

          <div>
            <label className="block text-gray-700 font-semibold">Role</label>
            <input
              type="text"
              name="role"
              value={formData.role}
              onChange={handleChange}
              className="w-full px-4 py-2 rounded-lg border border-gray-300 focus:ring-2 focus:ring-[#1E63DB]"
              required
            />
          </div>

          <div>
            <label className="block text-gray-700 font-semibold">Email</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              className="w-full px-4 py-2 rounded-lg border border-gray-300 focus:ring-2 focus:ring-[#1E63DB]"
              required
            />
          </div>

          <div>
            <label className="block text-gray-700 font-semibold">Department</label>
            <input
              type="text"
              name="department"
              value={formData.department}
              onChange={handleChange}
              className="w-full px-4 py-2 rounded-lg border border-gray-300 focus:ring-2 focus:ring-[#1E63DB]"
              required
            />
          </div>

          <div>
            <label className="block text-gray-700 font-semibold">Phone</label>
            <input
              type="text"
              name="phone"
              value={formData.phone}
              onChange={handleChange}
              className="w-full px-4 py-2 rounded-lg border border-gray-300 focus:ring-2 focus:ring-[#1E63DB]"
              required
            />
          </div>

          <button
            type="submit"
            className="w-full bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white py-2 rounded-xl hover:opacity-90 transition-all duration-300"
          >
            Save Changes
          </button>

          <button
            type="button"
            onClick={() => navigate(-1)}
            className="w-full mt-2 bg-gray-400 text-white py-2 rounded-xl hover:bg-gray-500 transition-all duration-300"
          >
            Cancel
          </button>
        </form>
      </div>
    </div>
  );
}
