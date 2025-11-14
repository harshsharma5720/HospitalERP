import React, { useEffect, useState } from "react";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";

export default function Treatments() {
  const [slideIn, setSlideIn] = useState(false);

  useEffect(() => {
    setSlideIn(true);
  }, []);

  const treatments = [
    {
      title: "Cardiology",
      desc: "Heart and cardiovascular system treatment",
      img: "/treatment-Cardilogy.jpg",
      icon: "❤️",
    },
    {
      title: "Orthopedics",
      desc: "Bone and joint treatment",
      img: "/Treatments-Orthopedics.png",
      icon: "🦴",
    },
    {
      title: "Neurology",
      desc: "Brain and nervous system treatment",
      img: "/Treatments-Neurology.jpg",
      icon: "🧠",
    },
    {
      title: "Pediatrics",
      desc: "Child and adolescent healthcare",
      img: "/Treatments-pediatrics.jpg",
      icon: "👶",
    },
    {
      title: "Dermatology",
      desc: "Skin and hair-related treatments",
      img: "/Treatments-Dermatology.jpg",
      icon: "💆‍♀️",
    },
    {
      title: "Oncology",
      desc: "Cancer diagnosis and treatment",
      img: "/Treatments-oncology.jpg",
      icon: "🏥",
    },
  ];

  return (
    <div
      className="
        min-h-screen
        bg-gray-50 dark:bg-[#0a1330]
        text-black dark:text-gray-200
        transition-colors duration-300
        relative
      "
    >
      <TopNavbar />
      <Navbar />

      {/* Intro Section */}
      <div
        className={`
          relative flex items-center justify-center px-6 md:px-16
          transition-transform duration-1000
          ${slideIn ? "translate-x-0" : "-translate-x-96"}
        `}
      >
        <div className="relative z-10 max-w-5xl py-20">
          <h1 className="text-center text-5xl font-extrabold mb-4">
            Our{" "}
              <span
                 className="
                    bg-gradient-to-r from-blue-600 to-cyan-400
                    bg-clip-text text-transparent
                 "
              >
                Treatments
              </span>
          </h1>
          <div className="w-40 h-1 bg-gradient-to-r from-blue-600 to-cyan-400 dark:from-[#50d4f2] dark:to-[#63e6ff] mx-auto mb-10 rounded-full"></div>

          <p className="text-lg md:text-xl text-gray-800 dark:text-[#50d4f2] text-center font-semibold">
            We offer comprehensive medical services across multiple
            specializations
          </p>
        </div>
      </div>

      {/* Treatments Grid */}
      <div className="relative z-10 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8 px-8 md:px-16 pb-16">
        {treatments.map((treatment, index) => (
          <div
            key={index}
            className="
              group relative overflow-hidden rounded-2xl shadow-md cursor-pointer
              bg-white dark:bg-[#111a3b]
              transform transition-all duration-500
              hover:scale-105 hover:shadow-2xl
            "
          >
            {/* Background image */}
            <div
              className="
                absolute inset-0 bg-cover bg-center
                transition-transform duration-500 group-hover:scale-110
              "
              style={{ backgroundImage: `url(${treatment.img})` }}
            ></div>

            {/* Overlay gradient */}
            <div
              className="
              absolute inset-0
              bg-gradient-to-t from-[#1E63DB]/80 to-transparent
              dark:from-[#0a1330]/80 dark:to-transparent
            "
            ></div>

            {/* Content */}
            <div className="relative p-6 flex flex-col justify-end h-64 text-white transition-all duration-500">
              <div className="text-5xl mb-2">{treatment.icon}</div>

              {/* Title */}
              <h3 className="text-2xl font-bold mb-2 dark:text-[#50d4f2]">
                {treatment.title}
              </h3>

              {/* Description */}
              <p
                className="
                text-sm text-gray-100 dark:text-[#50d4f2]
                transform translate-y-10 opacity-0
                group-hover:translate-y-0 group-hover:opacity-100
                transition-all duration-500
              "
              >
                {treatment.desc}
              </p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
