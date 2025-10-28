import React, { useEffect, useState } from "react";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";

export default function Treatments() {
  const [slideIn, setSlideIn] = useState(false);

  useEffect(() => {
    setSlideIn(true); // Trigger sliding animation on mount
  }, []);

  const fullText = `Shreya Hospital is a leading healthcare institution dedicated to providing
exceptional medical services with a patient-centric approach. Established with
a vision to deliver high-quality healthcare, the hospital combines advanced
technology with compassionate care to ensure the well-being of every patient.
Our multidisciplinary team of experienced doctors, nurses, and medical staff
work collaboratively to offer comprehensive treatments across various specialties.
Equipped with state-of-the-art diagnostic and treatment facilities, Shreya Hospital
emphasizes preventive care, accurate diagnosis, and effective treatment plans.
The hospital fosters a healing environment where patient comfort and safety are
top priorities. From routine check-ups to complex surgeries, every service is
administered with utmost precision and empathy. Our commitment to excellence
extends beyond medical care, focusing also on patient education, community
health awareness, and continuous improvement in healthcare standards. With a
reputation for reliability and professionalism, Shreya Hospital stands as a
trusted name in healthcare, offering personalized and accessible medical services
to the community it serves.`;

  return (
    <div className="min-h-screen bg-gray-50 relative">
      {/* ✅ Reusable top info bar */}
      <TopNavbar />

      {/* ✅ Main Navbar */}
      <Navbar />

      {/* Middle Section with Sliding Animation */}
      <div className="relative flex items-center justify-center px-6 md:px-16">
        {/* Background Image */}
        <img
          src="Shreyahospital.jpg"
          alt="Shreya Hospital"
          className="absolute top-0 left-0 w-full h-full object-cover opacity-20 z-0"
        />

        {/* Text Section */}
        <div
          className={`relative z-10 max-w-5xl py-20 transition-transform duration-1000 ${
            slideIn ? "translate-x-0" : "-translate-x-96"
          }`}
        >
          <h2 className="text-4xl md:text-5xl font-extrabold text-teal-700 mb-6 drop-shadow-lg text-center">
            About Shreya Hospital
          </h2>
          <p className="text-lg md:text-xl font-bold text-gray-900 leading-relaxed drop-shadow-md whitespace-pre-wrap">
            {fullText}
          </p>
        </div>
      </div>
    </div>
  );
}
