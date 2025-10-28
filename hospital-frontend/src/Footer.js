import React from "react";

export default function Footer() {
  return (
    <footer className="bg-white border-t shadow-inner mt-16">
      <div className="flex flex-col md:flex-row w-full">
        {/* Left Section - Contact Info */}
        <div className="bg-gradient-to-br from-[#1E63DB] to-[#27496d] text-white md:w-1/2 w-full p-10 flex flex-col justify-center space-y-6">
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
              <h3 className="font-semibold text-lg">Location</h3>
              <p className="text-sm leading-relaxed">
                Amman Street, Next to Bait Al Khair Building, Al Nahda 2,
                P.O.Box: 7832, Dubai, United Arab Emirates.
              </p>
            </div>
          </div>

          {/* Email */}
          <div className="flex items-start space-x-3">
            <span className="text-2xl">✉️</span>
            <div>
              <h3 className="font-semibold text-lg">Email</h3>
              <a
                href="mailto:sphdxb.receptions@nmchospital.ae"
                className="text-sm hover:underline hover:text-yellow-200 transition"
              >
                sphdxb.receptions@nmchospital.ae
              </a>
            </div>
          </div>

          {/* Phone */}
          <div className="flex items-start space-x-3">
            <span className="text-2xl">📞</span>
            <div>
              <h3 className="font-semibold text-lg">Phone</h3>
              <p className="text-sm">Landline: +971 4 267 9999</p>
              <p className="text-sm">Toll Free: 8006624</p>
              <p className="text-sm">Fax: +971 4 267 8889</p>
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
            className="h-[400px] md:h-full border-l-4 border-teal-600"
          ></iframe>
        </div>
      </div>

      {/* Bottom Bar */}
      <div className="bg-gradient-to-r from-gray-900 to-gray-800 text-gray-300 text-center py-3 text-sm tracking-wide">
        © {new Date().getFullYear()} <span className="font-semibold text-white">Shreya Hospital</span>. All rights reserved.
      </div>
    </footer>
  );
}
