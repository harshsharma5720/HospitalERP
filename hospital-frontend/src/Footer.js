import React from "react";

export default function Footer() {
  return (
    <footer
      className="
        bg-white dark:bg-[#0a1330]
        border-t shadow-inner mt-16
        dark:border-[#111a3b]
        transition-all duration-300
      "
    >
      <div className="flex flex-col md:flex-row w-full">
        {/* Left Section - Contact Info */}
        <div
          className="
            bg-gradient-to-br from-[#1E63DB] to-[#27496d]
            dark:from-[#111a3b] dark:to-[#0a1330]
            text-white dark:text-[#50d4f2]
            md:w-1/2 w-full p-10 flex flex-col justify-center space-y-6
            transition-all
          "
        >
          {/* Logo */}
          <div>
            <img
              src="download.jpeg"
              alt="Hospital Logo"
              className="h-14 mb-4 rounded-md shadow-md bg-white p-1"
            />
          </div>

          {/* Location */}
          <div className="flex items-start space-x-3">
            <span className="text-2xl">📍</span>
            <div>
              <h3 className="font-semibold text-lg dark:text-[#50d4f2]">Location</h3>
              <p className="text-sm leading-relaxed dark:text-[#50d4f2]">
                Amman Street, Next to Bait Al Khair Building, Al Nahda 2,
                P.O.Box: 7832, Dubai, United Arab Emirates.
              </p>
            </div>
          </div>

          {/* Email */}
          <div className="flex items-start space-x-3">
            <span className="text-2xl">✉️</span>
            <div>
              <h3 className="font-semibold text-lg dark:text-[#50d4f2]">Email</h3>
              <a
                href="mailto:sphdxb.receptions@nmchospital.ae"
                className="
                  text-sm hover:underline
                  hover:text-yellow-300
                  dark:text-[#50d4f2]
                  dark:hover:text-[#3bc2df]
                  transition
                "
              >
                sphdxb.receptions@nmchospital.ae
              </a>
            </div>
          </div>

          {/* Phone */}
          <div className="flex items-start space-x-3">
            <span className="text-2xl">📞</span>
            <div>
              <h3 className="font-semibold text-lg dark:text-[#50d4f2]">Phone</h3>
              <p className="text-sm dark:text-[#50d4f2]">Landline: +971 4 267 9999</p>
              <p className="text-sm dark:text-[#50d4f2]">Toll Free: 8006624</p>
              <p className="text-sm dark:text-[#50d4f2]">Fax: +971 4 267 8889</p>
            </div>
          </div>
        </div>

        {/* Right Section - Map */}
        <div className="md:w-1/2 w-full">
          <iframe
            title="Hospital Location"
            src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d115847.42517897102!2d55.1712795!3d25.0742828!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3e5f5c9c9e4dce33%3A0xd03b05dcd01dbf9d!2sDubai%2C%20United%20Arab%20Emirates!5e0!3m2!1sen!2sin!4v1698682456532!5m2!1sen!2sin"
            width="100%"
            height="100%"
            allowFullScreen=""
            loading="lazy"
            referrerPolicy="no-referrer-when-downgrade"
            className="
              h-[400px] md:h-full
              border-l-4 border-teal-600
              dark:border-[#50d4f2]
              transition-all
            "
          ></iframe>
        </div>
      </div>

      {/* Bottom Bar */}
      <div
        className="
        bg-gradient-to-r from-gray-900 to-gray-800
        dark:from-[#111a3b] dark:to-[#0a1330]
        text-gray-300 dark:text-[#50d4f2]
        text-center py-3 text-sm tracking-wide
        transition
      "
      >
        © {new Date().getFullYear()}{" "}
        <span className="font-semibold text-white dark:text-[#3bc2df]">
          Shreya Hospital
        </span>
        . All rights reserved.
      </div>
    </footer>
  );
}
