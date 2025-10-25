import React, { useState } from "react";

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
    <div className="bg-[#0083a9] min-h-screen text-white">
      {/* Header Section */}
      <div className="py-16 px-6 md:px-20 flex flex-col md:flex-row justify-between items-center">
        <div className="max-w-2xl">
          <h1 className="text-4xl font-bold mb-4">Contact Us</h1>
          <p className="text-lg leading-relaxed">
            We would love to hear from you! Let us know about your experience or
            any queries. Please fill in the required details and our team will
            get in touch with you shortly.
          </p>
        </div>
        <div className="mt-8 md:mt-0">
          <div className="bg-white/20 p-8 rounded-full">
            <svg
              xmlns="http://www.w3.org/2000/svg"
              className="h-20 w-20 text-white"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={1.5}
                d="M3 8l9 6 9-6m-9 6v8m0-8L3 8m9 6l9-6"
              />
            </svg>
          </div>
        </div>
      </div>

      {/* Book Appointment Strip */}
      <div className="bg-[#002b5c] py-4 flex justify-center items-center text-lg">
        <span className="mr-4 font-semibold">📅 To Book an Appointment</span>
        <a
          href="tel:+919268880303"
          className="bg-[#00a2b8] px-4 py-2 rounded-full text-white font-semibold"
        >
          Call Us: +91 926 888 0303
        </a>
      </div>

      {/* Enquiry Form Section */}
      <div className="bg-[#003366] text-white py-12 px-6 md:px-16">
        <div className="max-w-6xl mx-auto bg-white text-gray-800 p-8 rounded-2xl shadow-xl">
          <h2 className="text-2xl font-semibold mb-6 text-[#003366]">
            Enquiry Form
          </h2>

          <form onSubmit={handleSubmit} className="grid md:grid-cols-2 gap-6">
            {/* Enquiry Type */}
            <div>
              <label className="block mb-2 font-medium">Enquiry Type</label>
              <select
                name="enquiryType"
                value={formData.enquiryType}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg p-2"
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
              <label className="block mb-2 font-medium">Name</label>
              <input
                type="text"
                name="name"
                placeholder="Enter full name"
                value={formData.name}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg p-2"
                required
              />
            </div>

            {/* Email */}
            <div>
              <label className="block mb-2 font-medium">Email ID</label>
              <input
                type="email"
                name="email"
                placeholder="Enter email"
                value={formData.email}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg p-2"
                required
              />
            </div>

            {/* Phone */}
            <div>
              <label className="block mb-2 font-medium">Mobile No.</label>
              <input
                type="text"
                name="phone"
                placeholder="Enter mobile number"
                value={formData.phone}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg p-2"
                required
              />
            </div>

            {/* Hospital */}
            <div>
              <label className="block mb-2 font-medium">Hospital</label>
              <select
                name="hospital"
                value={formData.hospital}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg p-2"
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
              <label className="block mb-2 font-medium">
                Preferred Time to Call
              </label>
              <input
                type="time"
                name="preferredTime"
                value={formData.preferredTime}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg p-2"
              />
            </div>

            {/* Message */}
            <div className="md:col-span-2">
              <label className="block mb-2 font-medium">Message</label>
              <textarea
                name="message"
                rows="4"
                placeholder="Enter your query or comments"
                value={formData.message}
                onChange={handleChange}
                className="w-full border border-gray-300 rounded-lg p-2"
              ></textarea>
            </div>

            {/* Submit */}
            <div className="md:col-span-2 text-center">
              <button
                type="submit"
                className="bg-[#00a2b8] text-white px-8 py-2 rounded-full font-semibold hover:bg-[#008b9e]"
              >
                Submit Enquiry
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

export default ContactUsPage;
