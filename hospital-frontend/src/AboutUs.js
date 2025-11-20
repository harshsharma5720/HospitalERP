import React from "react";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import { FaHeartbeat, FaUserMd, FaHospital, FaPhoneAlt } from "react-icons/fa";

export default function AboutUs() {
  return (
    <div
      className="
        min-h-screen
        bg-gray-50 dark:bg-[#0a1330]
        text-gray-900 dark:text-gray-200
        transition-all duration-300
        bg-no-repeat bg-cover
        bg-fixed
      "
      style={{ backgroundImage: "url('doctor_black_theam.jpg')" }}
    >
      {/* Overlay */}
      <div className="bg-black/40 dark:bg-[#0a1330]/80 min-h-screen">
        <TopNavbar />
        <Navbar />

        {/* HEADER */}
        <div className="text-center py-20">
          <h1 className="text-5xl text-white font-extrabold mb-4">
              About/{" "}
              <span
                className="
                     bg-gradient-to-r from-blue-600 to-cyan-400
                     bg-clip-text text-transparent
                "
              >
                 Our Hospital
             </span>
          </h1>
          <div className="w-40 h-1 bg-gradient-to-r from-blue-600 to-cyan-400 dark:from-[#50d4f2] dark:to-[#63e6ff] mx-auto mb-10 rounded-full"></div>
          <p className="text-white max-w-2xl mx-auto dark:text-gray-300">
            Caring for you, every step of the way — with compassion and expertise.
          </p>
        </div>

        {/* INTRO SECTION */}
        <section
          className="
            max-w-6xl mx-auto p-10 rounded-2xl shadow-lg mb-10
            bg-white/20 dark:bg-[#111a3b]/40
            backdrop-blur-md
          "
        >
          <div className="grid md:grid-cols-2 gap-8 items-center">
            <img
              src="Shreyahospital.jpg"
              alt="Hospital"
              className="rounded-2xl shadow-lg"
            />

            <div>
              <h2 className="text-3xl font-bold mb-4 text-[#1E63DB] dark:text-[#50d4f2]">
                Welcome to Shreya Hospital
              </h2>

              <p className="text-lg text-gray-100 dark:text-gray-300 leading-relaxed">
                At <strong>Shreya Hospital</strong>, we believe healthcare should be
                personal, accessible, and compassionate. For more than a decade, we
                have provided expert medical care using advanced technology and
                highly experienced doctors.
              </p>

              <p className="mt-4 text-lg text-gray-100 dark:text-gray-300 leading-relaxed">
                We deliver high-quality and affordable healthcare with respect,
                transparency, and empathy.
              </p>
            </div>
          </div>
        </section>

        {/* MISSION + VISION */}
        <section
          className="
            max-w-6xl mx-auto p-10 rounded-2xl shadow-lg mb-10
            bg-white/20 dark:bg-[#111a3b]/40
            backdrop-blur-md
          "
        >
          <h2 className="text-3xl font-extrabold mb-8 text-center text-[#1E63DB] dark:text-[#50d4f2]">
            Our Mission & Vision
          </h2>

          <div className="grid md:grid-cols-2 gap-8">
            {/* Mission */}
            <div
              className="
                p-6 rounded-2xl shadow-md
                bg-white/30 dark:bg-[#16224a]/50
              "
            >
              <FaHeartbeat className="text-red-400 text-4xl mb-4 mx-auto" />
              <h3 className="text-2xl font-bold text-center mb-3 dark:text-[#50d4f2]">
                Our Mission
              </h3>
              <p className="text-lg text-gray-100 dark:text-gray-300 text-center">
                To provide exceptional healthcare services with compassion,
                innovation, and integrity.
              </p>
            </div>

            {/* Vision */}
            <div
              className="
                p-6 rounded-2xl shadow-md
                bg-white/30 dark:bg-[#16224a]/50
              "
            >
              <FaHospital className="text-[#1E63DB] dark:text-[#50d4f2] text-4xl mb-4 mx-auto" />
              <h3 className="text-2xl font-bold text-center mb-3 dark:text-[#50d4f2]">
                Our Vision
              </h3>
              <p className="text-lg text-gray-100 dark:text-gray-300 text-center">
                To be a global leader in healthcare, recognized for trust, care,
                and medical excellence.
              </p>
            </div>
          </div>
        </section>

        {/* TEAM SECTION */}
        <section
          className="
            max-w-6xl mx-auto p-10 rounded-2xl shadow-lg mb-10 text-center
            bg-white/20 dark:bg-[#111a3b]/40
            backdrop-blur-md
          "
        >
          <h2 className="text-3xl font-extrabold mb-8 text-[#1E63DB] dark:text-[#50d4f2]">
            Meet Our Dedicated Team
          </h2>

          <div className="grid sm:grid-cols-2 md:grid-cols-3 gap-8">
            {[
              {
                name: "Dr. Priya Sharma",
                role: "Cardiologist",
                img: "https://randomuser.me/api/portraits/women/65.jpg",
              },
              {
                name: "Dr. Arjun Mehta",
                role: "Neurologist",
                img: "https://randomuser.me/api/portraits/men/32.jpg",
              },
              {
                name: "Dr. Neha Kapoor",
                role: "Pediatrician",
                img: "https://randomuser.me/api/portraits/women/70.jpg",
              },
            ].map((member) => (
              <div
                key={member.name}
                className="
                  p-6 rounded-2xl shadow-md
                  bg-white/30 dark:bg-[#16224a]/50
                  hover:-translate-y-2 hover:shadow-2xl transition
                "
              >
                <img
                  src={member.img}
                  alt={member.name}
                  className="
                    w-32 h-32 mx-auto rounded-full mb-4 object-cover
                    border-4 border-[#1E63DB] dark:border-[#50d4f2]
                  "
                />
                <h3 className="text-xl font-bold dark:text-[#50d4f2]">
                  {member.name}
                </h3>
                <p className="text-gray-800 dark:text-gray-300">
                  {member.role}
                </p>
              </div>
            ))}
          </div>
        </section>

        {/* CONTACT CTA */}
        <section
          className="
            py-12 text-center
            bg-gradient-to-r from-[#1E63DB] to-[#27496d]
            dark:from-[#16224a] dark:to-[#0a1330]
            text-white dark:text-[#50d4f2]
          "
        >
          <h2 className="text-3xl font-bold mb-4">Need Medical Assistance?</h2>
          <p className="text-lg mb-6 dark:text-gray-300">
            Our team is available 24/7 to support your healthcare needs.
          </p>

          <div className="flex justify-center items-center gap-3">
            <FaPhoneAlt className="text-2xl" />
            <span className="text-xl font-semibold">+91 98765 43210</span>
          </div>
        </section>
      </div>
    </div>
  );
}
