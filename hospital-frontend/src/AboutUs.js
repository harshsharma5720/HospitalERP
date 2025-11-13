import React from "react";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";
import { FaHeartbeat, FaUserMd, FaHospital, FaPhoneAlt } from "react-icons/fa";

export default function AboutUs() {
  return (
    <div
      className="
        min-h-screen
        text-gray-800 dark:text-[#50d4f2]
        bg-fixed bg-cover bg-center
        dark:bg-[#0a1330]
        transition-all duration-300
      "
      style={{
        backgroundImage: "url('shreya.jpg')",
      }}
    >
      {/* Dark overlay */}
      <div className="bg-black bg-opacity-60 dark:bg-[#0a1330]/80 min-h-screen">
        <TopNavbar />
        <Navbar />

        <div className="text-white">
          {/* Header */}
          <div className="text-center py-20">
            <h1 className="text-5xl font-bold mb-4 text-teal-300 dark:text-[#50d4f2]">
              About Our Hospital
            </h1>
            <p className="text-lg max-w-2xl mx-auto dark:text-[#8ddff8]">
              Caring for you, every step of the way — with compassion and expertise.
            </p>
          </div>

          {/* Introduction */}
          <section
            className="
              max-w-6xl mx-auto p-6 md:p-10 rounded-2xl shadow-lg mb-10
              bg-white bg-opacity-10 backdrop-blur-md
              dark:bg-[#111a3b]/60
              transition-all
            "
          >
            <div className="grid md:grid-cols-2 gap-8 items-center">
              <img
                src="Shreyahospital.jpg"
                alt="Hospital building"
                className="rounded-2xl shadow-lg"
              />
              <div>
                <h2 className="text-3xl font-bold mb-4 text-teal-300 dark:text-[#50d4f2]">
                  Welcome to LifeCare Hospital
                </h2>
                <p className="text-lg leading-relaxed dark:text-[#8ddff8]">
                  At <strong>LifeCare Hospital</strong>, we believe healthcare should be
                  personal, accessible, and compassionate. For over a decade, we’ve
                  been providing advanced medical services, cutting-edge technology,
                  and experienced doctors who care deeply about every patient.
                </p>
                <p className="mt-4 text-lg leading-relaxed dark:text-[#8ddff8]">
                  We aim to deliver high-quality, affordable healthcare to every
                  family with respect, transparency, and empathy.
                </p>
              </div>
            </div>
          </section>

          {/* Mission & Vision */}
          <section
            className="
              max-w-6xl mx-auto p-6 md:p-10 rounded-2xl shadow-lg mb-10
              bg-white bg-opacity-10 backdrop-blur-md
              dark:bg-[#111a3b]/60
              transition-all
            "
          >
            <h2 className="text-3xl font-bold mb-8 text-center text-teal-300 dark:text-[#50d4f2]">
              Our Mission & Vision
            </h2>
            <div className="grid md:grid-cols-2 gap-8 text-center">
              {/* Mission */}
              <div
                className="
                  p-6 rounded-2xl shadow-md
                  bg-white bg-opacity-10
                  dark:bg-[#16224a]/60
                  transition
                "
              >
                <FaHeartbeat className="text-red-400 text-4xl mb-4 mx-auto" />
                <h3 className="text-2xl font-semibold mb-3 text-white dark:text-[#50d4f2]">
                  Our Mission
                </h3>
                <p className="dark:text-[#8ddff8]">
                  To deliver exceptional and compassionate healthcare services
                  with innovation, quality, and integrity.
                </p>
              </div>

              {/* Vision */}
              <div
                className="
                  p-6 rounded-2xl shadow-md
                  bg-white bg-opacity-10
                  dark:bg-[#16224a]/60
                  transition
                "
              >
                <FaHospital className="text-teal-300 dark:text-[#50d4f2] text-4xl mb-4 mx-auto" />
                <h3 className="text-2xl font-semibold mb-3 text-white dark:text-[#50d4f2]">
                  Our Vision
                </h3>
                <p className="dark:text-[#8ddff8]">
                  To become a trusted healthcare leader recognized globally for
                  excellence in patient care and medical innovation.
                </p>
              </div>
            </div>
          </section>

          {/* Team */}
          <section
            className="
              max-w-6xl mx-auto p-6 md:p-10 rounded-2xl shadow-lg mb-10 text-center
              bg-white bg-opacity-10 backdrop-blur-md
              dark:bg-[#111a3b]/60
              transition-all
            "
          >
            <h2 className="text-3xl font-bold mb-8 text-teal-300 dark:text-[#50d4f2]">
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
                    p-6 rounded-2xl shadow-md hover:shadow-xl transition
                    bg-white bg-opacity-10
                    dark:bg-[#16224a]/60
                  "
                >
                  <img
                    src={member.img}
                    alt={member.name}
                    className="w-32 h-32 mx-auto rounded-full mb-4 object-cover border-4 border-teal-300 dark:border-[#50d4f2]"
                  />
                  <h3 className="text-xl font-semibold text-white dark:text-[#50d4f2]">
                    {member.name}
                  </h3>
                  <p className="text-gray-200 dark:text-[#8ddff8]">
                    {member.role}
                  </p>
                </div>
              ))}
            </div>
          </section>

          {/* CTA */}
          <section
            className="
              py-12 text-center
              bg-teal-700 bg-opacity-90
              dark:bg-[#16224a]/80
              text-white dark:text-[#50d4f2]
              transition
            "
          >
            <h2 className="text-3xl font-bold mb-4">Need Medical Assistance?</h2>
            <p className="text-lg mb-6 dark:text-[#8ddff8]">
              Our team is available 24/7 to support your healthcare needs.
            </p>
            <div className="flex justify-center items-center gap-3">
              <FaPhoneAlt className="text-2xl dark:text-[#50d4f2]" />
              <span className="text-xl font-semibold dark:text-[#50d4f2]">
                +91 98765 43210
              </span>
            </div>
          </section>
        </div>
      </div>
    </div>
  );
}
