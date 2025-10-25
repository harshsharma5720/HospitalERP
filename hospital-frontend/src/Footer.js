import React from "react";

export default function Footer() {
  return (
    <footer className="bg-white border-t mt-16">
      <div className="flex flex-col md:flex-row w-full">
        {/* Left Section - Contact Info */}
        <div className="bg-teal-700 text-white md:w-1/2 w-full p-10 flex flex-col justify-center space-y-6">
          <div>
            <img src="download.jpeg" alt="Hospital Logo" className="h-12 mb-4" />
          </div>

          {/* Location */}
          <div className="flex items-start space-x-3">
            <span className="text-2xl">📍</span>
            <div>
              <h3 className="font-bold text-lg">Location</h3>
              <p className="text-sm">
                Amman Street, Next to Bait Al Khair Building, Al Nahda 2,
                P.O.Box: 7832, Dubai, United Arab Emirates.
              </p>
            </div>
          </div>

          {/* Email */}
          <div className="flex items-start space-x-3">
            <span className="text-2xl">✉️</span>
            <div>
              <h3 className="font-bold text-lg">Email</h3>
              <a
                href="mailto:sphdxb.receptions@nmchospital.ae"
                className="text-sm hover:underline"
              >
                sphdxb.receptions@nmchospital.ae
              </a>
            </div>
          </div>

          {/* Phone */}
          <div className="flex items-start space-x-3">
            <span className="text-2xl">📞</span>
            <div>
              <h3 className="font-bold text-lg">Phone</h3>
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
            className="h-[400px] md:h-full"
          ></iframe>
        </div>
      </div>

      {/* Bottom Bar */}
      <div className="bg-gray-900 text-gray-300 text-center py-3 text-sm">
        © {new Date().getFullYear()} Shreya Hospital. All rights reserved.
      </div>
    </footer>
  );
}
