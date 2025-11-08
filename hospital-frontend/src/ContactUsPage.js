import React, { useState } from "react";
import Navbar from "./Navbar";
import TopNavbar from "./TopNavbar";

function ContactUsPage() {
  const [formData, setFormData] = useState({
    enquiryType: "",
    name: "",
    email: "",
    phone: "",
    hospital: "",
    preferredTime: "",
    message: "",
  });

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    console.log("Submitted:", formData);
    alert("Thank you for your enquiry! Our team will contact you soon.");
  };

  return (
    <div className="min-h-screen bg-white text-gray-800 flex flex-col">
      <TopNavbar />
      <Navbar />

      {/* Main Section: Two Columns */}
      <div className="flex-grow flex flex-col md:flex-row  justify-center py-12 px-6 md:px-16 gap-10">

        {/* Left Content */}
        <div className="md:w-1/2 space-y-6 text-left">
          <h1 className="text-5xl font-extrabold mb-4">
                 Contact /{" "}
             <span className="bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
                 Contact Us
             </span>
          </h1>
          {/* Underline */}
          <div className="w-80 h-1 bg-gradient-to-r from-blue-600 to-cyan-400  mb-10 rounded-full"></div>
          <p className="text-lg text-gray-700 leading-relaxed">
            We’re here to help. Whether you want to book an appointment, provide
            feedback, or ask a question — please fill in the form and our team
            will get back to you promptly.
          </p>

          <div className="mt-6 space-y-3 text-gray-800">
            <p><strong>📞 Helpline:</strong> +91 98765 43210</p>
            <p><strong>✉️ Email:</strong> contact@maxhospital.com</p>
            <p><strong>🏥 Address:</strong> Max Healthcare, Ghaziabad, India</p>
          </div>
        </div>

        {/* Right Form */}
        <div className="md:w-1/2">
          <div className="bg-gradient-to-br from-[#E3FDFD] to-[#FEFFFF] shadow-2xl rounded-3xl p-8 md:p-10 w-full">
            <h2 className="text-5xl font-extrabold text-center mb-4">
                 Enquiry{" "}
               <span className="bg-gradient-to-r from-blue-600 to-cyan-400 bg-clip-text text-transparent">
                 Here
               </span>
            </h2>

            <form
              onSubmit={handleSubmit}
              className="grid grid-cols-1 md:grid-cols-2 gap-6"
            >
              {/* Enquiry Type */}
              <div>
                <label className="block mb-2 font-medium text-[#004c6d]">
                  Enquiry Type
                </label>
                <select
                  name="enquiryType"
                  value={formData.enquiryType}
                  onChange={handleChange}
                  className="w-full border border-gray-300 rounded-lg p-2 focus:ring-2 focus:ring-[#00A2B8] focus:outline-none"
                  required
                >
                  <option value="">Select Type</option>
                  <option value="appointment">Book Appointment</option>
                  <option value="feedback">Feedback</option>
                  <option value="other">Other</option>
                </select>
              </div>

              {/* Name */}
              <div>
                <label className="block mb-2 font-medium text-[#004c6d]">
                  Name
                </label>
                <input
                  type="text"
                  name="name"
                  placeholder="Enter full name"
                  value={formData.name}
                  onChange={handleChange}
                  className="w-full border border-gray-300 rounded-lg p-2 focus:ring-2 focus:ring-[#00A2B8] focus:outline-none"
                  required
                />
              </div>

              {/* Email */}
              <div>
                <label className="block mb-2 font-medium text-[#004c6d]">
                  Email ID
                </label>
                <input
                  type="email"
                  name="email"
                  placeholder="Enter email"
                  value={formData.email}
                  onChange={handleChange}
                  className="w-full border border-gray-300 rounded-lg p-2 focus:ring-2 focus:ring-[#00A2B8] focus:outline-none"
                  required
                />
              </div>

              {/* Phone */}
              <div>
                <label className="block mb-2 font-medium text-[#004c6d]">
                  Mobile No.
                </label>
                <input
                  type="text"
                  name="phone"
                  placeholder="Enter mobile number"
                  value={formData.phone}
                  onChange={handleChange}
                  className="w-full border border-gray-300 rounded-lg p-2 focus:ring-2 focus:ring-[#00A2B8] focus:outline-none"
                  required
                />
              </div>

              {/* Hospital */}
              <div>
                <label className="block mb-2 font-medium text-[#004c6d]">
                  Hospital
                </label>
                <select
                  name="hospital"
                  value={formData.hospital}
                  onChange={handleChange}
                  className="w-full border border-gray-300 rounded-lg p-2 focus:ring-2 focus:ring-[#00A2B8] focus:outline-none"
                  required
                >
                  <option value="">Select Hospital</option>
                  <option value="maxDelhi">Max Delhi</option>
                  <option value="maxNoida">Max Noida</option>
                  <option value="maxGhaziabad">Max Ghaziabad</option>
                </select>
              </div>

              {/* Preferred Time */}
              <div>
                <label className="block mb-2 font-medium text-[#004c6d]">
                  Preferred Time to Call
                </label>
                <input
                  type="time"
                  name="preferredTime"
                  value={formData.preferredTime}
                  onChange={handleChange}
                  className="w-full border border-gray-300 rounded-lg p-2 focus:ring-2 focus:ring-[#00A2B8] focus:outline-none"
                />
              </div>

              {/* Message */}
              <div className="md:col-span-2">
                <label className="block mb-2 font-medium text-[#004c6d]">
                  Message
                </label>
                <textarea
                  name="message"
                  rows="3"
                  placeholder="Enter your query or comments"
                  value={formData.message}
                  onChange={handleChange}
                  className="w-full border border-gray-300 rounded-lg p-2 focus:ring-2 focus:ring-[#00A2B8] focus:outline-none resize-none"
                ></textarea>
              </div>

              {/* Submit */}
              <div className="md:col-span-2 text-center">
                <button
                  type="submit"
                  className="bg-gradient-to-r from-[#007B9E] to-[#00A2B8] text-white px-10 py-2 rounded-full font-semibold shadow-md hover:shadow-lg hover:scale-105 transition-transform duration-300"
                >
                  Submit Enquiry
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  );
}

export default ContactUsPage;